/**
 * Copyright 2012 Akiban Technologies, Inc.
 * 
 * Portions Copyrighted 2026 3A Systems, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.persistit;

import org.junit.Test;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WarmupTest extends PersistitUnitTestCase {

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    final Properties p = super.getProperties(cleanup);
    p.setProperty("bufferinventory", "true");
    p.setProperty("bufferpreload", "true");
    return p;
  }

  @Test
  public void testWarmup() throws Exception {
    /*
     * Disable background cleanup/pruning on both the pre-shutdown and the
     * post-restart instance so the pool is not mutated concurrently while
     * residentPages() takes its unsynchronized getBufferCopy() snapshots.
     */
    disableBackgroundCleanup();

    Exchange ex = _persistit.getExchange("persistit", "WarmupTest", true);
    BufferPool pool = ex.getBufferPool();
    for (int i = 1; i <= 1000; i++) {
      ex.getValue().put(RED_FOX);
      ex.clear().append(i).store();
    }

    /*
     * Warmup (bufferinventory + bufferpreload) guarantees that the pages
     * resident in the pool at shutdown are read back into the pool on restart.
     * It does not guarantee that a page returns to the same buffer slot:
     * preloadBufferInventory() sorts the recorded pages by read order and
     * reallocates buffers via the clock algorithm, so the slot index depends on
     * timing in both runs. The old per-slot assertion (expected:<0> but was:<2>
     * -- an empty slot where the page was resident, just relocated) only matched
     * by coincidence; warmup provides no slot-stability property. Compare the
     * set of resident pages instead.
     */
    final Set<String> before = residentPages(pool);
    assertFalse("Test setup should leave pages resident in the pool", before.isEmpty());
    /*
     * The subset comparison below is only meaningful while the pool stays
     * under-filled: while free buffers remain the pool never evicts, so a
     * buffer's (volume, page) identity does not change under it and the
     * unsynchronized getBufferCopy() reads in residentPages() cannot tear. Fail
     * loudly if a future dataset change fills the pool.
     */
    assertTrue("Pool must stay under-filled for this comparison to be meaningful",
        validPageCount(pool) < pool.getBufferCount());

    _persistit.close();

    _persistit = new Persistit(_config);
    disableBackgroundCleanup();
    pool = _persistit.getVolume(VOLUME_NAME).getStructure().getPool();
    assertTrue("Restarted pool must also stay under-filled for the comparison",
        validPageCount(pool) < pool.getBufferCount());

    /*
     * One-way invariant (before is a subset of after): warmup must reload every
     * page that was resident at shutdown. Slot position is intentionally not
     * asserted (it is not preserved), and after may legitimately hold extra
     * pages, so this deliberately drops the old bidirectional per-slot check.
     */
    final Set<String> after = residentPages(pool);
    final Set<String> missing = new HashSet<String>(before);
    missing.removeAll(after);
    assertTrue("Warmup should preload every previously-resident page; missing=" + missing,
        missing.isEmpty());
  }

  /**
   * Identity ({@code volume:page:type}) of every valid, recordable buffer
   * resident in the pool, applying the same volume filter as
   * {@link BufferPool#recordBufferInventory} (temporary and lock volumes are
   * skipped) so the set is exactly what warmup is expected to reload. Callers
   * disable background cleanup and assert the pool is under-filled, so no
   * eviction reallocates a buffer while it is read; a buffer's identity is
   * therefore stable and the unlocked {@link BufferPool#getBufferCopy(int)}
   * needs no torn-read spin guard.
   */
  private static Set<String> residentPages(final BufferPool pool) {
    final Set<String> pages = new HashSet<String>();
    for (int i = 0; i < pool.getBufferCount(); i++) {
      final Buffer b = pool.getBufferCopy(i);
      final Volume volume = b.getVolume();
      if (b.isValid() && volume != null && !volume.isTemporary() && !volume.isLockVolume()) {
        pages.add(volume.getName() + ':' + b.getPageAddress() + ':' + b.getPageType());
      }
    }
    return pages;
  }

  /**
   * Number of buffers currently holding a page. The pool is full (and starts
   * evicting) once this reaches {@link BufferPool#getBufferCount()}; counting
   * valid buffers directly keeps the under-fill guard independent of the
   * filtered/deduplicated {@link #residentPages} set.
   */
  private static int validPageCount(final BufferPool pool) {
    int count = 0;
    for (int i = 0; i < pool.getBufferCount(); i++) {
      if (pool.getBufferCopy(i).isValid()) {
        count++;
      }
    }
    return count;
  }

  @Test
  public void readOrderIsSequential() throws Exception {

    Exchange ex = _persistit.getExchange("persistit", "WarmupTest", true);
    BufferPool pool = ex.getBufferPool();

    final int full = pool.getBufferCount() * (pool.getBufferSize() / RED_FOX.length());
    /*
     * Overflow the buffer pool
     */
    for (int i = 1; i <= full * 3; i++) {
      ex.getValue().put(RED_FOX);
      ex.clear().append(i).store();
    }
    /*
     * Pull some low-address pages in to scramble the pool
     */
    for (int i = full * 2; i >= 0; i -= 1000) {
      ex.clear().append(i).fetch();
    }
    /*
     * Verify that buffers in pool now have somewhat scrambled page
     * addresses
     */
    int breaks = 0;
    long previous = -1;

    for (int i = 0; i < pool.getBufferCount(); i++) {
      final Buffer b = pool.getBufferCopy(i);
      assertTrue("Every buffer should be valid at this point", b.isValid());
      if (b.getPageAddress() < previous) {
        breaks++;
      }
      previous = b.getPageAddress();
    }

    assertTrue("Buffer pool should have scrambled page address", breaks > 0);

    ex = null;
    pool = null;
    _persistit.copyBackPages();
    _persistit.close();

    _persistit = new Persistit();
    _config.setBufferInventoryEnabled(false);
    _config.setBufferPreloadEnabled(false);
    _persistit.setConfiguration(_config);
    _persistit.initialize();

    final Volume volume = _persistit.getVolume("persistit");
    final MediatedFileChannel mfc = (MediatedFileChannel) volume.getStorage().getChannel();
    final TrackingFileChannel tfc = new TrackingFileChannel();
    mfc.injectChannelForTests(tfc);
    pool = volume.getStructure().getPool();
    pool.preloadBufferInventory();
    assertTrue("Preload should have loaded pages from journal file", tfc.getReadPositionList().size() > 0);
    tfc.assertOrdered(true, true);
  }
}
