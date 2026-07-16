/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

package com.persistit;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.persistit.exception.PersistitException;
import com.persistit.util.ThreadSequencer;
import com.persistit.util.ThreadSequencer.Condition;

import static com.persistit.util.SequencerConstants.STORE_PENDING_SPLIT_A;
import static com.persistit.util.SequencerConstants.STORE_PENDING_SPLIT_B;
import static com.persistit.util.SequencerConstants.STORE_PENDING_SPLIT_C;
import static com.persistit.util.SequencerConstants.STORE_PENDING_SPLIT_SCHEDULE;
import static com.persistit.util.ThreadSequencer.addSchedules;
import static com.persistit.util.ThreadSequencer.enableSequencer;
import static com.persistit.util.ThreadSequencer.sequence;
import static com.persistit.util.ThreadSequencer.setCondition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Deterministic reproductions of the residual bug-1017957 class races reported
 * in https://github.com/OpenIdentityPlatform/commons/issues/274. The original
 * {@link Bug1017957Test#induceCorruptionByStress()} reproduces them only
 * probabilistically under a 10-second two-thread stress.
 *
 * <p>
 * Mechanism reproduced by
 * {@link #pendingSplitPointerRevalidatedAfterTreeClaimDrop()}: when
 * {@code Exchange#storeInternal} has committed a split at some level, the
 * key-pointer pair for the new right sibling is inserted at the next level by
 * a later iteration of the store loop. If that iteration throws a
 * RetryException (tree height growth with a contended claim upgrade), the
 * handler releases the tree claim entirely before re-acquiring it. In that
 * window a concurrent structure delete can unlink the new page and put it on
 * a garbage chain; the resumed iteration used to insert the now-stale pointer
 * blindly, planting a dangling index entry to a garbage page. Searches then
 * fail transiently with {@code CorruptVolumeException "invalid page type 30"}
 * while the volume is consistent at rest -- exactly the CI signature of issue
 * #274.
 *
 * <p>
 * Hardening covered by {@link #staleIndexHoleActionOnReusedPageIsDropped()}:
 * a {@code CleanupIndexHole} action enqueued by {@code rebalanceSplit} may
 * run arbitrarily late. By then the page may have been unlinked, garbage
 * collected and reused as a live, indexed page of the same tree at the same
 * level (ABA). The type and reachability checks of {@code fixIndexHole} both
 * pass legitimately against the new incarnation, so the action used to
 * perform a parent-level store against a page that has no index hole. That
 * store happens to be a same-key replace only while every parent entry key
 * equals its page's first key; the parent-entry check makes the guard
 * independent of that invariant and drops the stale action outright.
 */
public class Bug1017957ResidualTest extends PersistitUnitTestCase {

  private final static String TREE_NAME = "Bug1017957Residual";

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    return getBiggerProperties(cleanup);
  }

  private Exchange getExchange() throws PersistitException {
    return _persistit.getExchange(VOLUME_NAME, TREE_NAME, true);
  }

  /**
   * A stale CleanupIndexHole action whose page has been garbage collected and
   * reused as a live indexed page of the same tree must be dropped without
   * storing anything. Before the parent-entry check the action passed both
   * guards of fixIndexHole against the reused incarnation and performed a
   * parent-level store; that store is harmless only while every parent entry
   * key equals its page's first key, a global invariant the repair action has
   * no business depending on.
   */
  @Test
  public void staleIndexHoleActionOnReusedPageIsDropped() throws Exception {
    /*
     * Keep the background CleanupManager from running anything while the
     * scenario is constructed; the stale action is performed synchronously.
     */
    _persistit.getCleanupManager().setPollInterval(3600 * 1000L);

    final Exchange ex = getExchange();
    final int treeHandle = _persistit.getJournalManager().handleForTree(ex.getTree());

    /*
     * Lay down several data pages with large keys and values, then map each
     * key to its data page so that three adjacent pages L, P, R and their
     * lowest keys are known.
     */
    final String v = createString(5500);
    final String k = createString(1040);
    for (int i = 1000; i < 1019; i++) {
      ex.clear().append(i).append(k);
      ex.getValue().put(v);
      ex.store();
    }
    final List<Long> pages = new ArrayList<Long>();
    final List<Integer> lowestKeys = new ArrayList<Integer>();
    for (int i = 1000; i < 1019; i++) {
      ex.clear().append(i).append(k);
      final long page = ex.fetchBufferCopy(0).getPageAddress();
      if (pages.isEmpty() || pages.get(pages.size() - 1) != page) {
        pages.add(page);
        lowestKeys.add(i);
      }
    }
    assertTrue("Need at least 4 data pages, got " + pages.size(), pages.size() >= 4);
    final int middle = pages.size() / 2;
    final long targetPage = pages.get(middle);

    /*
     * The stale action: enqueued for the current incarnation of targetPage,
     * performed only after the page has been unlinked and reused.
     */
    final CleanupManager.CleanupIndexHole action = new CleanupManager.CleanupIndexHole(treeHandle, targetPage, 0);

    /*
     * Unlink targetPage: remove everything from the lowest key of its left
     * neighbor up to (exclusive) the lowest key of its right neighbor, so
     * that targetPage is interior to the removed range and gets unlinked
     * onto a garbage chain. The bounds are partial keys (first segment
     * only), as in Bug1017957Test, so they never match a stored key or an
     * index separator exactly.
     */
    final Key key1 = new Key(_persistit);
    key1.append(lowestKeys.get(middle - 1));
    final Key key2 = new Key(_persistit);
    key2.append(lowestKeys.get(middle + 1));
    ex.removeKeyRange(key1, key2);

    /*
     * Reuse targetPage: append small records until the page comes back from
     * the garbage chain as a live, indexed data page of this tree. With a
     * sequential append workload the reused page starts out as the
     * rightmost data page.
     */
    int appended = -1;
    for (int i = 0; i < 4000 && appended < 0; i++) {
      ex.clear().append(5000 + i);
      ex.getValue().put(RED_FOX);
      ex.store();
      if (isLiveDataPageOfTree(ex, targetPage)) {
        appended = i;
      }
    }
    assertTrue("Page " + targetPage + " was not reused as a live data page", appended >= 0);

    assertEquals("Reused page must have exactly one parent entry before the stale action", 1,
      countIndexPointersTo(targetPage));

    final long storesBefore = ex.getTree().getStatistics().getStoreCounter();
    action.performAction(_persistit, null);

    assertEquals("Stale CleanupIndexHole on a reused page must be dropped, not stored", storesBefore,
      ex.getTree().getStatistics().getStoreCounter());
    assertEquals("Reused page must still have exactly one parent entry", 1, countIndexPointersTo(targetPage));

    final IntegrityCheck icheck = new IntegrityCheck(_persistit);
    icheck.checkVolume(_persistit.getVolume(VOLUME_NAME));
    assertEquals("Volume must be consistent", 0, icheck.getFaults().length);
  }

  /**
   * A key-pointer pair left pending by a committed split must be re-validated
   * when the tree claim was released on a RetryException: a concurrent
   * covering remove may have unlinked the page the pointer refers to.
   */
  @Test
  public void pendingSplitPointerRevalidatedAfterTreeClaimDrop() throws Exception {
    _persistit.getCleanupManager().setPollInterval(3600 * 1000L);
    try {
      enableSequencer(true);
      addSchedules(STORE_PENDING_SPLIT_SCHEDULE);

      final Exchange ex = getExchange();
      final Tree tree = ex.getTree();
      final String v = createString(5500);
      final String k = createString(1040);
      /*
       * Build a depth-2 tree: one index level (the root) over data pages.
       */
      int prefill = 1000;
      while (tree.getDepth() < 2 || prefill < 1010) {
        ex.clear().append(prefill).append(k);
        ex.getValue().put(v);
        ex.store();
        prefill++;
      }
      assertEquals(2, tree.getDepth());

      /*
       * Hold a reader claim on the tree so that the writer thread's attempt
       * to grow the tree height fails its claim upgrade and takes the
       * RetryException path, parking at STORE_PENDING_SPLIT_A after having
       * released all claims with the root split already committed.
       */
      assertTrue(tree.claim(false));
      boolean treeClaimHeld = true;

      final AtomicBoolean done = new AtomicBoolean();
      final AtomicInteger lastStored = new AtomicInteger();
      final AtomicReference<Exception> writerError = new AtomicReference<Exception>();
      final Thread writer = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            final Exchange ex1 = getExchange();
            int i = 2000;
            while (!done.get()) {
              ex1.clear().append(i).append(k);
              ex1.getValue().put(v);
              ex1.store();
              lastStored.set(i);
              i++;
            }
          } catch (final Exception e) {
            writerError.set(e);
          }
        }
      });
      setCondition(STORE_PENDING_SPLIT_A, new Condition() {
        @Override
        public boolean enabled() {
          return Thread.currentThread() == writer;
        }
      });
      writer.start();

      try {
        /*
         * Wait until the writer is parked inside the RetryException window.
         * The root split has committed: the old root has a new right
         * sibling that is not yet indexed, and the writer holds no claims.
         */
        sequence(STORE_PENDING_SPLIT_B);

        final long rootPage = tree.getRootPageAddr();
        final long pendingPage = rightSiblingOf(rootPage);
        assertTrue("Root split must have linked a right sibling", pendingPage != 0);

        tree.release();
        treeClaimHeld = false;

        /*
         * Covering remove: delete a range that spans from the old root's
         * coverage into the pending page's coverage. The index-level join
         * coalesces the pending page back into the root and unlinks it onto
         * a garbage chain, where it is retyped to PAGE_TYPE_GARBAGE.
         */
        final Key key1 = new Key(_persistit);
        key1.append(1002);
        final Key key2 = new Key(_persistit);
        key2.append(lastStored.get());
        ex.removeKeyRange(key1, key2);

        assertEquals("The pending page must have been unlinked and garbage collected",
          Buffer.PAGE_TYPE_GARBAGE, pageTypeOf(pendingPage));

        done.set(true);
        sequence(STORE_PENDING_SPLIT_C);
        writer.join(60 * 1000L);
        assertTrue("Writer thread must terminate", !writer.isAlive());
        assertNull("Writer thread must not fail: " + writerError.get(), writerError.get());

        assertEquals("No index entry may point to the garbage page", 0, countIndexPointersTo(pendingPage));

        final IntegrityCheck icheck = new IntegrityCheck(_persistit);
        icheck.checkVolume(_persistit.getVolume(VOLUME_NAME));
        assertEquals("Volume must be consistent", 0, icheck.getFaults().length);

        /*
         * The CI signature of issue #274: a traversal used to throw a
         * transient CorruptVolumeException "invalid page type 30" while
         * descending through the dangling entry.
         */
        ex.clear().to(Key.AFTER);
        int remaining = 0;
        while (ex.previous()) {
          remaining++;
        }
        assertTrue("Some keys must remain", remaining > 0);
      } finally {
        done.set(true);
        if (treeClaimHeld) {
          tree.release();
        }
        /*
         * Disabling the sequencer releases the writer if an assertion fired
         * before STORE_PENDING_SPLIT_C was reached.
         */
        ThreadSequencer.disableSequencer();
        writer.join(60 * 1000L);
      }
    } finally {
      ThreadSequencer.disableSequencer();
    }
  }

  private BufferPool pool() {
    return _persistit.getBufferPool(16384);
  }

  private Volume volume() throws PersistitException {
    return _persistit.getVolume(VOLUME_NAME);
  }

  private int pageTypeOf(final long page) throws PersistitException {
    final Buffer buffer = pool().get(volume(), page, false, true);
    try {
      return buffer.getPageType();
    } finally {
      buffer.releaseTouched();
    }
  }

  private long rightSiblingOf(final long page) throws PersistitException {
    final Buffer buffer = pool().get(volume(), page, false, true);
    try {
      return buffer.getRightSibling();
    } finally {
      buffer.releaseTouched();
    }
  }

  /**
   * Whether the page is currently a data page of the tree, reachable through
   * a search for its own first key.
   */
  private boolean isLiveDataPageOfTree(final Exchange ex, final long page) throws PersistitException {
    final Buffer buffer = pool().get(volume(), page, false, true);
    final Key firstKey = new Key(_persistit);
    try {
      if (buffer.getPageType() != Buffer.PAGE_TYPE_DATA
        || buffer.getKeyBlockEnd() <= Buffer.KEY_BLOCK_START) {
        return false;
      }
      buffer.nextKey(firstKey, buffer.toKeyBlock(0));
    } finally {
      buffer.releaseTouched();
    }
    firstKey.copyTo(ex.getKey());
    final Buffer copy = ex.fetchBufferCopy(0);
    return copy != null && copy.getPageAddress() == page;
  }

  /**
   * Counts key-pointer entries referencing the page across every index page
   * of the volume. A consistent B-tree has at most one.
   */
  private int countIndexPointersTo(final long page) throws PersistitException {
    int count = 0;
    final Volume volume = volume();
    final long nextAvailable = volume.getStorage().getNextAvailablePage();
    for (long p = 1; p < nextAvailable; p++) {
      final Buffer buffer = pool().get(volume, p, false, true);
      try {
        if (buffer.isIndexPage()) {
          for (int at = Buffer.KEY_BLOCK_START; at < buffer.getKeyBlockEnd(); at += Buffer.KEYBLOCK_LENGTH) {
            if (buffer.getPointer(at) == page) {
              count++;
            }
          }
        }
      } finally {
        buffer.releaseTouched();
      }
    }
    return count;
  }
}
