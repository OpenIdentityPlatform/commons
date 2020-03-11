/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
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

import com.persistit.CheckpointManager.Checkpoint;
import com.persistit.JournalManager.PageNode;
import com.persistit.JournalManager.TreeDescriptor;
import com.persistit.TransactionPlayer.TransactionPlayerListener;
import com.persistit.exception.PersistitException;
import com.persistit.exception.RollbackException;
import com.persistit.exception.TransactionFailedException;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecoveryTest extends PersistitUnitTestCase {
  /*
   * This class needs to be in com.persistit because of some package-private
   * methods used in controlling the test.
   */

  private String journalSize = "10000000";
  private final String _volumeName = "persistit";
  private Configuration _config;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    _persistit.getJournalManager().setRollbackPruningEnabled(false);
    _persistit.getJournalManager().setWritePagePruningEnabled(false);
    _config = _persistit.getConfiguration();

  }

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    final Properties properties = super.getProperties(cleanup);
    properties.setProperty("journalsize", journalSize);
    return properties;
  }

  @Test
  public void testRecoveryRebuildsPageMap() throws Exception {
    _persistit.getJournalManager().setAppendOnly(true);
    store1();
    _persistit.close();
    _persistit = new Persistit(_config);
    final JournalManager logMan = _persistit.getJournalManager();
    assertTrue(logMan.getPageMapSize() + logMan.getCopiedPageCount() > 0);
    fetch1a();
    fetch1b();
  }

  @Test
  public void testCopierCleansUpJournals() throws Exception {
    store1();
    JournalManager jman = _persistit.getJournalManager();
    assertTrue(jman.getPageMapSize() > 0);
    drainJournal();
    assertEquals(0, jman.getPageMapSize());
    _persistit.close();
    _persistit = new Persistit(_config);
    jman = _persistit.getJournalManager();
    // opening a volume modifies its head page
    assertTrue(jman.getPageMapSize() <= 1);
    fetch1a();
    fetch1b();
  }

  @Test
  public void testRecoverCommittedTransactions() throws Exception {
    // create 10 transactions on the journal
    _persistit.getJournalManager().setAppendOnly(true);
    store2();
    _persistit.getJournalManager().flush();
    _persistit.crash();
    _persistit = new Persistit();
    _persistit.getJournalManager().setAppendOnly(true);
    final RecoveryManager plan = _persistit.getRecoveryManager();
    plan.setRecoveryDisabledForTestMode(true);
    _persistit.setConfiguration(_config);
    _persistit.initialize();
    assertEquals(15, plan.getCommittedCount());
    plan.setRecoveryDisabledForTestMode(false);
    final Set<Long> recoveryTimestamps = new HashSet<Long>();
    final TransactionPlayerListener actor = new TransactionPlayerListener() {

      @Override
      public void store(final long address, final long timestamp, final Exchange exchange)
        throws PersistitException {
        recoveryTimestamps.add(timestamp);
      }

      @Override
      public void removeKeyRange(final long address, final long timestamp, final Exchange exchange,
        final Key from, final Key to) throws PersistitException {
        recoveryTimestamps.add(timestamp);
      }

      @Override
      public void removeTree(final long address, final long timestamp, final Exchange exchange)
        throws PersistitException {
        recoveryTimestamps.add(timestamp);
      }

      @Override
      public void startRecovery(final long address, final long timestamp) throws PersistitException {
      }

      @Override
      public void startTransaction(final long address, final long startTmestamp, final long commitTimestamp)
        throws PersistitException {
      }

      @Override
      public void endTransaction(final long address, final long timestamp) throws PersistitException {
      }

      @Override
      public void endRecovery(final long address, final long timestamp) throws PersistitException {
      }

      @Override
      public void delta(final long address, final long timestamp, final Tree tree, final int index,
        final int accumulatorTypeOrdinal, final long value) throws PersistitException {
      }

      @Override
      public boolean requiresLongRecordConversion() {
        return true;
      }

      @Override
      public boolean createTree(final long timestamp) throws PersistitException {
        return true;
      }

    };
    plan.applyAllRecoveredTransactions(actor, plan.getDefaultRollbackListener());
    assertEquals(15, recoveryTimestamps.size());
  }

  @Test
  public void testLongRecordTransactionRecovery() throws Exception {
    // create 10 transactions on the journal having long records.
    _persistit.getJournalManager().setAppendOnly(true);
    store3();
    fetch3();
    _persistit.getJournalManager().flush();
    _persistit.crash();
    _persistit = new Persistit();
    _persistit.getJournalManager().setAppendOnly(true);
    final RecoveryManager rman = _persistit.getRecoveryManager();
    rman.setRecoveryDisabledForTestMode(true);
    _persistit.setConfiguration(_config);
    _persistit.initialize();
    assertTrue(rman.getCommittedCount() > 0);
    rman.setRecoveryDisabledForTestMode(false);
    rman.applyAllRecoveredTransactions(rman.getDefaultCommitListener(), rman.getDefaultRollbackListener());
    fetch3();
  }

  @Test
  @Ignore
  public void testRolloverDoesntDeleteLiveTransactions() throws Exception {
    final JournalManager jman = _persistit.getJournalManager();
    final long blockSize = jman.getBlockSize();
    assertEquals(0, jman.getBaseAddress() / blockSize);

    store1();

    jman.rollover();
    drainJournal();
    assertEquals(0, jman.getPageMapSize());

    final Transaction txn = _persistit.getTransaction();

    txn.begin();
    store1();
    txn.commit();
    txn.end();
    /*
     * Flush an uncommitted version of this transaction - should prevent
     * journal cleanup.
     */
    txn.begin();
    store0();
    txn.flushTransactionBuffer(true);
    txn.rollback();
    txn.end();

    jman.rollover();
    drainJournal();
    assertEquals(0, jman.getPageMapSize());
    /*
     * Because JournalManager thinks there's an open transaction it should
     * preserve the journal file containing the TX record for the
     * transaction.
     */
    assertTrue(jman.getBaseAddress() < jman.getCurrentAddress());
    txn.begin();
    store1();
    txn.commit();
    txn.end();

    jman.unitTestClearTransactionMap();

    jman.rollover();
    _persistit.flushStatistics();
    _persistit.checkpoint();
    /*
     * The TI active transaction cache may be a bit out of date, which can
     * cause the copier to preserve records on the journal that are in fact
     * obsolete. Since the following assertion looks for equality, we'll
     * artificially update the active transaction cache to verify that the
     * copier does the right thing.
     */
    _persistit.getTransactionIndex().updateActiveTransactionCache();
    drainJournal();
    assertEquals(jman.getBaseAddress(), jman.getCurrentAddress());
    assertEquals(0, jman.getPageMapSize());

    fetch1a();
    fetch1b();
  }

  // Verifies that a sequence of insert and remove transactions results
  // in the correct state after recovery. Tests fix for bug 719319.
  @Test
  public void testRecoveredTransactionsAreCorrect() throws Exception {
    final SortedSet<String> keys = new TreeSet<String>();
    Exchange[] exchanges = new Exchange[5];
    for (int index = 0; index < 5; index++) {
      final Exchange ex = _persistit.getExchange("persistit", "RecoveryTest_" + index, true);
      ex.removeAll();
      exchanges[index] = ex;
    }
    for (int index = 0; index < exchanges.length; index++) {
      final Exchange ex = exchanges[index];
      for (int a = 0; a < 5; a++) {
        ex.clear().append(a);
        ex.getValue().put(String.format("index=%d a=%d", index, a));
        tStore(ex, keys);
        for (int b = 0; b < 5; b++) {
          ex.clear().append(a).append(b);
          ex.getValue().put(String.format("index=%d a=%d b=%d", index, a, b));
          tStore(ex, keys);
        }
      }
    }
    for (int index = 0; index < exchanges.length; index++) {
      final Exchange ex = exchanges[index];
      final Key.Direction direction = index == 0 ? Key.EQ : index == 1 ? Key.GTEQ : Key.GT;
      for (int a = 0; a < 5; a++) {
        ex.clear().append(a);
        if (a % 2 == 0) {
          tRemove(ex, keys, direction);
        }
        if (a % 3 == 0) {
          for (int b = 0; b < 5; b++) {
            ex.clear().append(a).append(b);
            tRemove(ex, keys, direction);
          }
        }
      }
    }
    tDeleteTree(exchanges[4], keys);

    // Now crash Persistit
    exchanges = null;
    _persistit.getJournalManager().flush();
    _persistit.crash();
    _persistit = new Persistit(_config);
    final Volume volume = _persistit.getVolume("persistit");

    for (int index = 0; index < 5; index++) {
      final Tree tree = volume.getTree("RecoveryTest_" + index, false);
      assertEquals(index == 4, tree == null);
      if (index != 4) {
        final Exchange ex = new Exchange(tree);
        ex.clear();
        while (ex.next(true)) {
          final String ks = keyString(ex);
          assertTrue(keys.remove(ks));
        }
      }
    }
    assertTrue(keys.isEmpty());
  }

  @Test
  public void testLargePageMap() throws Exception {
    final Volume vd = new Volume("foo", 123);
    final Map<Integer, Volume> volumeMap = new TreeMap<Integer, Volume>();
    volumeMap.put(1, vd);
    // sorted to make reading hex dumps easier
    final Map<PageNode, PageNode> pageMap = new HashMap<PageNode, PageNode>();
    for (long pageAddr = 0; pageAddr < 100000; pageAddr++) {
      PageNode lastPageNode = new PageNode(1, pageAddr, pageAddr * 100, 0);
      for (long ts = 1; ts < 10; ts++) {
        final PageNode pn = new PageNode(1, pageAddr, pageAddr * 100 + ts * 10, ts * 100);
        pn.setPrevious(lastPageNode);
        lastPageNode = pn;
      }
      pageMap.put(lastPageNode, lastPageNode);
    }
    final JournalManager jman = new JournalManager(_persistit);
    final String path = DATA_PATH + "/RecoveryManagerTest_journal_";
    jman.unitTestInjectVolumes(volumeMap);
    jman.unitTestInjectPageMap(pageMap);
    // Note: moved call to init after the unitTestInject calls
    // because init now starts a the journal file.
    jman.init(null, path, 100000000);
    jman.writeCheckpointToJournal(new Checkpoint(500, 12345));
    jman.close();
    final RecoveryManager rman = new RecoveryManager(_persistit);
    rman.init(path);
    rman.buildRecoveryPlan();
    assertTrue(rman.getKeystoneAddress() != -1);
    final Map<PageNode, PageNode> pageMapCopy = new HashMap<PageNode, PageNode>();
    final Map<PageNode, PageNode> branchMapCopy = new HashMap<PageNode, PageNode>();
    rman.collectRecoveredPages(pageMapCopy, branchMapCopy);
    assertEquals(pageMap.size(), pageMapCopy.size());
    final PageNode key = new PageNode(1, 42, -1, -1);
    PageNode pn = pageMapCopy.get(key);
    int count = 0;
    while (pn != null) {
      assertTrue(pn.getTimestamp() <= 500);
      count++;
      pn = pn.getPrevious();
    }
    assertEquals(1, count);
  }

  @Test
  public void testVolumeMetadataValid() throws Exception {
    // create a junk volume to make sure the internal handle count is bumped
    // up
    final Volume vd = new Volume("foo", 123);
    final int volumeHandle = _persistit.getJournalManager().handleForVolume(vd);
    // retrieve the value of the handle counter before crashing
    final int initialHandleValue = _persistit.getJournalManager().getHandleCount();
    _persistit.close();
    _persistit = new Persistit(_config);
    // verify the value of the handle counter after recovery is
    // still valid.
    assertTrue(_persistit.getJournalManager().getHandleCount() > initialHandleValue);

    // create a junk tree to make sure the internal handle count is bumped
    // up
    final TreeDescriptor td = new TreeDescriptor(volumeHandle, "gray");
    _persistit.getJournalManager().handleForTree(td, true);
    final int updatedHandleValue = _persistit.getJournalManager().getHandleCount();
    _persistit.close();
    _persistit = new Persistit(_config);
    // verify the value of the handle counter after recovery is
    // still valid.
    assertTrue(_persistit.getJournalManager().getHandleCount() > updatedHandleValue);
  }

  private final static int T1 = 1000;
  private final static int T2 = 2000;

  @Test
  public void testIndexHoles() throws Exception {
    _persistit.getJournalManager().setAppendOnly(true);

    final Transaction transaction = _persistit.getTransaction();
    final StringBuilder sb = new StringBuilder();
    while (sb.length() < 1000) {
      sb.append(RED_FOX);
    }

    final String s = sb.toString();
    for (int cycle = 0; cycle < 2; cycle++) {
      for (int i = T1; i < T2; i++) {
        final Exchange exchange = _persistit.getExchange("persistit", "RecoveryTest" + i, true);
        transaction.begin();
        try {
          exchange.getValue().put(s);
          for (int j = 0; j < 20; j++) {
            exchange.to(j).store();
          }
          transaction.commit();
        } finally {
          transaction.end();
        }
      }

      for (int j = 0; j < 20; j++) {
        for (int i = T1; i < T2; i++) {
          final Exchange exchange = _persistit.getExchange("persistit", "RecoveryTest" + i, true);
          transaction.begin();
          try {
            exchange.to(j).remove();
            transaction.commit();
          } finally {
            transaction.end();
          }
        }
      }

      for (int i = T1; i < T2; i += 2) {
        transaction.begin();
        try {
          final Exchange exchange = _persistit.getExchange("persistit", "RecoveryTest" + i, true);
          exchange.removeTree();
          transaction.commit();
        } finally {
          transaction.end();
        }
      }
    }
    _persistit.checkAllVolumes();
    _persistit.crash();

    _persistit = new Persistit();
    _persistit.getJournalManager().setAppendOnly(true);
    _persistit.setConfiguration(_config);
    _persistit.initialize();
    _persistit.checkAllVolumes();

    final Volume volume = _persistit.getVolume("persistit");

    long page = volume.getDirectoryTree().getRootPageAddr();
    Buffer buffer = _persistit.getBufferPool(volume.getPageSize()).getBufferCopy(volume, page);
    assertEquals(0, buffer.getRightSibling());

    for (final String treeName : volume.getTreeNames()) {
      final Tree tree = volume.getTree(treeName, false);
      page = tree.getRootPageAddr();
      buffer = _persistit.getBufferPool(volume.getPageSize()).getBufferCopy(volume, page);
      assertEquals(0, buffer.getRightSibling());
    }
  }

  @Test
  public void testNormalRestart() throws Exception {
    final JournalManager jman = _persistit.getJournalManager();
    Exchange exchange = _persistit.getExchange(_volumeName, "RecoveryTest", true);
    exchange.getValue().put(RED_FOX);
    int count = 0;
    long checkpointAddr = 0;
    for (; jman.getCurrentAddress() < jman.getBlockSize() * 1.25;) {
      if (jman.getCurrentAddress() - checkpointAddr > jman.getBlockSize() * 0.8) {
        _persistit.checkpoint();
        checkpointAddr = jman.getCurrentAddress();
      }
      exchange.to(count).store();
      count++;
    }
    for (int i = 0; i < count + 100; i++) {
      assertEquals(i < count, exchange.to(i).isValueDefined());
    }
    _persistit.close();
    _persistit = new Persistit(_config);
    exchange = _persistit.getExchange(_volumeName, "RecoveryTest", false);
    for (int i = 0; i < count + 100; i++) {
      if (i < count && !exchange.to(i).isValueDefined()) {
        System.out.println("i=" + i + " count=" + count);
        break;
      }
      assertEquals(i < count, exchange.to(i).isValueDefined());
    }

    _persistit.close();
    _persistit = new Persistit(_config);
    exchange = _persistit.getExchange(_volumeName, "RecoveryTest", false);
    for (int i = 0; i < count + 100; i++) {
      assertEquals(i < count, exchange.to(i).isValueDefined());
    }
  }

  /*
   * Deprecated because the useOldVSpec param will go away soon.
   */
  @Test
  @Deprecated
  public void testNormalRestartUseOldVSpec() throws Exception {
    _persistit.close();
    cleanUpDirectory(new File(DATA_PATH));
    _config.setUseOldVSpec(true);
    _persistit = new Persistit(_config);

    final JournalManager jman = _persistit.getJournalManager();
    Exchange exchange = _persistit.getExchange(_volumeName, "RecoveryTest", true);
    exchange.getValue().put(RED_FOX);
    int count = 0;
    long checkpointAddr = 0;
    for (; jman.getCurrentAddress() < jman.getBlockSize() * 1.25;) {
      if (jman.getCurrentAddress() - checkpointAddr > jman.getBlockSize() * 0.8) {
        _persistit.checkpoint();
        checkpointAddr = jman.getCurrentAddress();
      }
      exchange.to(count).store();
      count++;
    }
    for (int i = 0; i < count + 100; i++) {
      assertEquals(i < count, exchange.to(i).isValueDefined());
    }
    _persistit.close();

    _persistit = new Persistit(_config);
    exchange = _persistit.getExchange(_volumeName, "RecoveryTest", false);
    for (int i = 0; i < count + 100; i++) {
      if (i < count && !exchange.to(i).isValueDefined()) {
        System.out.println("i=" + i + " count=" + count);
        break;
      }
      assertEquals(i < count, exchange.to(i).isValueDefined());
    }

    _persistit.close();
    _persistit = new Persistit(_config);
    exchange = _persistit.getExchange(_volumeName, "RecoveryTest", false);
    for (int i = 0; i < count + 100; i++) {
      assertEquals(i < count, exchange.to(i).isValueDefined());
    }
  }

  private void store0() throws PersistitException {
    final Exchange exchange = _persistit.getExchange(_volumeName, "RecoveryTest", true);
    exchange.removeAll();
    exchange.getValue().put(RED_FOX);
    exchange.clear().append(1).store();
  }

  private void store1() throws PersistitException {
    final Exchange exchange = _persistit.getExchange(_volumeName, "RecoveryTest", true);
    exchange.removeAll();
    final StringBuilder sb = new StringBuilder();

    for (int i = 1; i < 50000; i++) {
      sb.setLength(0);
      sb.append((char) (i / 20 + 64));
      sb.append((char) (i % 20 + 64));
      exchange.clear().append(sb);
      exchange.getValue().put("Record #" + i);
      exchange.store();
    }
  }

  private void fetch1a() throws PersistitException {
    final Exchange exchange = _persistit.getExchange(_volumeName, "RecoveryTest", false);
    final StringBuilder sb = new StringBuilder();

    for (int i = 1; i < 50000; i++) {
      sb.setLength(0);
      sb.append((char) (i / 20 + 64));
      sb.append((char) (i % 20 + 64));
      exchange.clear().append(sb);
      exchange.fetch();
      assertTrue(exchange.getValue().isDefined());
      assertEquals("Record #" + i, exchange.getValue().getString());
    }

  }

  private void fetch1b() throws PersistitException {
    final Exchange exchange = _persistit.getExchange(_volumeName, "RecoveryTest", false);
    final StringBuilder sb = new StringBuilder();
    for (int i = 1; i < 400; i++) {
      sb.setLength(0);
      sb.append((char) (i % 20 + 64));
      sb.append((char) (i / 20 + 64));
      exchange.clear().append(sb);
      exchange.fetch();
      final int k = (i / 20) + (i % 20) * 20;
      assertEquals(exchange.getValue().getString(), "Record #" + k);
    }
  }

  private void store2() throws PersistitException {
    final Exchange ex = _persistit.getExchange("persistit", "RecoveryTest", true);
    ex.removeAll();
    for (int j = 0; j++ < 10;) {

      final Transaction txn = ex.getTransaction();

      txn.begin();
      try {
        for (int i = 0; i < 10; i++) {
          ex.getValue().put("String value #" + i + " for test1");
          ex.clear().append("test1").append(j).append(i).store();
        }
        for (int i = 3; i < 10; i += 3) {
          ex.clear().append("test1").append(j).append(i).remove(Key.GTEQ);
        }
        txn.commit();
      } finally {
        txn.end();
      }
    }

    for (int j = 1; j < 10; j += 2) {
      final Transaction txn = ex.getTransaction();
      txn.begin();
      try {
        ex.clear().append("test1").append(j).remove(Key.GTEQ);
        txn.commit();
      } finally {
        txn.end();
      }
    }
  }

  private void store3() throws PersistitException {
    final Exchange ex = _persistit.getExchange("persistit", "RecoveryTest", true);
    ex.removeAll();
    for (int j = 0; j++ < 5;) {
      final StringBuilder sb = new StringBuilder(500000);
      for (int i = 0; i < 100000; i++) {
        sb.append("abcde");
      }

      final Transaction txn = ex.getTransaction();

      txn.begin();
      try {
        for (int i = 0; i < 10; i++) {
          sb.replace(0, 3, " " + i + " ");
          ex.getValue().put(sb.toString());
          ex.clear().append("test1").append(j).append(i).store();
        }
        for (int i = 3; i < 10; i += 3) {
          ex.clear().append("test1").append(j).append(i).remove(Key.GTEQ);
        }
        txn.commit();
      } finally {
        txn.end();
      }
    }

    for (int j = 1; j < 10; j += 2) {
      final Transaction txn = ex.getTransaction();
      txn.begin();
      try {
        ex.clear().append("test1").append(j).remove(Key.GTEQ);
        txn.commit();
      } finally {
        txn.end();
      }
    }
  }

  private boolean shouldBeDefined(final int j, final int i) {
    if (j % 2 != 0 || j < 1 || j > 5) {
      return false;
    }
    if (i < 0 || i > 9) {
      return false;
    }
    if (i % 3 == 0 && i > 0) {
      return false;
    }
    return true;
  }

  private void fetch3() throws Exception {
    final Exchange ex = _persistit.getExchange("persistit", "RecoveryTest", true);
    for (int j = 0; j++ < 5;) {
      final StringBuilder sb = new StringBuilder(500000);

      for (int i = 0; i < 100000; i++) {
        sb.append("abcde");
      }

      for (int i = 0; i < 10; i++) {
        ex.clear().append("test1").append(j).append(i).fetch();
        if (shouldBeDefined(j, i)) {
          sb.replace(0, 3, " " + i + " ");
          assertEquals(sb.toString(), ex.getValue().getString());
        } else {
          assertTrue(!ex.getValue().isDefined());
        }
      }
    }

  }

  private String keyString(final Exchange ex) {
    final String s = ex.getKey().toString();
    return ex.getTree().getName() + "_" + s.substring(1, s.length() - 1);
  }

  private void tStore(final Exchange ex, final SortedSet<String> keys) throws PersistitException {
    final Transaction txn = ex.getTransaction();
    int retries = 10;
    for (;;) {
      try {
        txn.begin();
        ex.store();
        txn.commit();
        final String ks = keyString(ex);
        assertTrue(keys.add(ks));
        break;
      } catch (final RollbackException e) {
        if (--retries < 0) {
          throw new TransactionFailedException();
        }
      } finally {
        txn.end();
      }
    }
  }

  private void tRemove(final Exchange ex, final SortedSet<String> keys, final Key.Direction direction)
    throws PersistitException {
    final Transaction txn = ex.getTransaction();
    int retries = 10;
    for (;;) {
      try {
        txn.begin();
        ex.remove(direction);
        txn.commit();
        final String ks = keyString(ex);
        for (final Iterator<String> it = keys.iterator(); it.hasNext();) {
          final String candidate = it.next();
          if ((direction == Key.EQ || direction == Key.GTEQ) && candidate.equals(ks)) {
            it.remove();
          } else if ((direction == Key.GTEQ || direction == Key.GT) && candidate.startsWith(ks)
            && !candidate.equals(ks)) {
            it.remove();
          }
        }
        break;
      } catch (final RollbackException e) {
        if (--retries < 0) {
          throw new TransactionFailedException();
        }
      } finally {
        txn.end();
      }
    }
  }

  private void tDeleteTree(final Exchange ex, final SortedSet<String> keys) throws PersistitException {
    final Transaction txn = ex.getTransaction();
    int retries = 10;
    for (;;) {
      try {
        txn.begin();
        ex.removeTree();
        ex.clear();
        final String ks = keyString(ex);
        for (final Iterator<String> it = keys.iterator(); it.hasNext();) {
          final String candidate = it.next();
          if (candidate.startsWith(ks)) {
            it.remove();
          }
        }
        txn.commit();
        break;
      } catch (final RollbackException e) {
        if (--retries < 0) {
          throw new TransactionFailedException();
        }
      } finally {
        txn.end();
      }
    }
  }

  public static void main(final String[] args) throws Exception {
    final int cycles = args.length > 0 ? Integer.parseInt(args[0]) : 20;
    for (int cycle = 0; cycle < cycles; cycle++) {
      final RecoveryTest rt = new RecoveryTest();
      System.out.printf("\nStarting cycle %d\n", cycle);
      rt.journalSize = "100M";
      rt.setUp();
      // rt.testIndexHoles();
      rt.testRolloverDoesntDeleteLiveTransactions();
      rt.tearDown();
    }
  }

  @Override
  public void runAllTests() throws Exception {
    testRecoveryRebuildsPageMap();
    testCopierCleansUpJournals();
    testRecoverCommittedTransactions();
    // test4();
  }
}
