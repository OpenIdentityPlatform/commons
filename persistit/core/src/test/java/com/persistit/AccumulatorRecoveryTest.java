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

import com.persistit.Accumulator.SumAccumulator;
import com.persistit.TransactionPlayer.TransactionPlayerListener;
import com.persistit.exception.PersistitException;
import com.persistit.exception.RollbackException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccumulatorRecoveryTest extends PersistitUnitTestCase {
  final static int ROW_COUNT_ACCUMULATOR_INDEX = 17;
  /*
   * This class needs to be in com.persistit because of some package-private
   * methods used in controlling the test.
   */
  private final String journalSize = "100000000";
  private final Random random = new Random();
  final AtomicBoolean running = new AtomicBoolean();
  final AtomicLong counter = new AtomicLong();

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    final Properties properties = getBiggerProperties(cleanup);
    properties.setProperty("journalsize", journalSize);
    return properties;
  }

  @Test
  public void testRecoverCommittedTransactions() throws Exception {
    _persistit.getJournalManager().setAppendOnly(true);
    final Exchange ex = _persistit.getExchange("persistit", "RecoveryTest", true);
    final SumAccumulator rowCount = ex.getTree().getSumAccumulator(0);
    for (int j = 0; j++ < 10;) {

      final Transaction txn = ex.getTransaction();

      txn.begin();
      try {
        for (int i = 0; i < 10; i++) {
          ex.getValue().put("String value #" + i + " for test1");
          ex.clear().append("test1").append(j).append(i).store();
          rowCount.add(1);
        }
        for (int i = 3; i < 10; i += 3) {
          ex.clear().append("test1").append(j).append(i).remove(Key.GTEQ);
          rowCount.add(-1);
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
        final boolean removed = ex.clear().append("test1").append(j).remove(Key.GTEQ);
        if (removed) {
          rowCount.add(-7);
        }
        txn.commit();
      } finally {
        txn.end();
      }
    }

    _persistit.getJournalManager().flush();
    final Configuration config = _persistit.getConfiguration();
    _persistit.crash();
    _persistit = new Persistit();
    _persistit.getJournalManager().setAppendOnly(true);
    final RecoveryManager plan = _persistit.getRecoveryManager();
    plan.setRecoveryDisabledForTestMode(true);
    _persistit.setConfiguration(config);
    _persistit.initialize();
    assertEquals(15, plan.getCommittedCount());
    plan.setRecoveryDisabledForTestMode(false);
    final Set<Long> recoveryTimestamps = new HashSet<Long>();
    final AtomicLong recoveredRowCount = new AtomicLong();
    final AtomicLong expectedRowCount = new AtomicLong();
    final TransactionPlayerListener commitListener = new TransactionPlayerListener() {

      @Override
      public void store(final long address, final long timestamp, final Exchange exchange)
        throws PersistitException {
        recoveryTimestamps.add(timestamp);
        expectedRowCount.incrementAndGet();
      }

      @Override
      public void removeKeyRange(final long address, final long timestamp, final Exchange exchange,
        final Key from, final Key to) throws PersistitException {
        recoveryTimestamps.add(timestamp);
        expectedRowCount.addAndGet(from.getDepth() == 2 ? -7 : -1); // because
                                                                    // there
                                                                    // are
                                                                    // 7
                                                                    // rows
                                                                    // being
                                                                    // deleted
                                                                    // by
                                                                    // each
                                                                    // range
                                                                    // delete
                                                                    // operation
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
        recoveredRowCount.addAndGet(value);
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
    plan.applyAllRecoveredTransactions(commitListener, plan.getDefaultRollbackListener());
    assertEquals(15, recoveryTimestamps.size());
    assertEquals(expectedRowCount.get(), recoveredRowCount.get());
  }

  /**
   * Insert "rows" within transactions in concurrent threads. Crash Persistit.
   * Verify that accumulators match stored data.
   */
  @Test
  public void testAccumulatorRecovery1() throws Exception {

    running.set(true);
    counter.set(0);
    // Make sure the helper methods work
    accumulateRows(10000);
    final long accumulated = verifyRowCount();
    assertEquals(counter.get(), accumulated);
  }

  @Test
  public void testAccumulatorRecovery2() throws Exception {
    // Make sure the helper methods work in concurrent transactions
    counter.set(0);
    running.set(true);
    final Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      final int index = i;
      threads[i] = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            accumulateRows(1000);
            verifyRowCount();
          } catch (final Exception e) {
            System.out.println("Thread " + index + " failed");
            e.printStackTrace();
          }
        }
      });
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].start();
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }
    assertEquals(counter.get(), verifyRowCount());
  }

  @Test
  public void testAccumulatorRecovery3() throws Exception {
    // Crash Persistit while transactions are being executed, then
    // verify the accumulator status
    counter.set(0);
    running.set(true);

    final Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      final int index = i;
      threads[i] = new Thread(new Runnable() {
        @Override
        public void run() {
          for (int j = 0; j < 10; j++) {
            try {
              accumulateRows(1000);
              verifyRowCount();
            } catch (final Exception e) {
              System.out.println("Thread " + index + " failed");
              e.printStackTrace();
            }
          }
        }
      });
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].start();
    }
    Thread.sleep(10000);
    _persistit.checkpoint();
    Thread.sleep(5000);
    running.set(false);
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }
    final Configuration config = _persistit.getConfiguration();
    _persistit.crash();
    _persistit = new Persistit(config);

    verifyRowCount();
  }

  private void accumulateRows(final int max) throws Exception {
    final Exchange exchange = _persistit.getExchange("persistit", "AccumulatorRecoveryTest", true);
    final Transaction txn = _persistit.getTransaction();
    int count = 0;
    while (running.get() && count++ < max) {
      final int key = random.nextInt(1000000);
      final int op = random.nextInt(100);
      int retryCount = 0;
      while (true) {
        txn.begin();
        try {
          final boolean exists = exchange.to(key).isValueDefined();
          int update = 0;
          if (op < 80) { // TODO - when constant is 80, test causes
                         // corruption during recovery
            if (!exists) {
              exchange.getValue().put(RED_FOX);
              exchange.store();
              final SumAccumulator rowCount = exchange.getTree().getSumAccumulator(
                ROW_COUNT_ACCUMULATOR_INDEX);
              rowCount.add(1);
              update = 1;
            }
          } else {
            if (exists) {
              exchange.remove();
              final SumAccumulator rowCount = exchange.getTree().getSumAccumulator(
                ROW_COUNT_ACCUMULATOR_INDEX);
              rowCount.add(-1);
              update = -1;
            }
          }
          txn.commit();
          counter.addAndGet(update);
          break;
        } catch (final RollbackException re) {
          retryCount++;
          assertTrue(retryCount < 5);
          System.out.println("(Acceptable) rollback in " + Thread.currentThread().getName());
        } finally {
          txn.end();
        }
      }
    }
  }

  private long verifyRowCount() throws Exception {
    final Exchange exchange = _persistit.getExchange("persistit", "AccumulatorRecoveryTest", false);
    final Transaction txn = _persistit.getTransaction();
    txn.begin();
    try {
      final Accumulator rowCount = exchange.getTree().getAccumulator(Accumulator.Type.SUM,
        ROW_COUNT_ACCUMULATOR_INDEX);
      final long accumulated = rowCount.getSnapshotValue();
      long counted = 0;
      exchange.to(Key.BEFORE);
      while (exchange.next()) {
        counted++;
        assertFalse(exchange.getValue().isAntiValue());
      }
      final long accumulated2 = rowCount.getSnapshotValue();
      if (accumulated != counted || accumulated != accumulated2) {
        synchronized (this) {
          System.out.printf("%s accumulated=%,d accumulated2=%,d counted=%,d\n", Thread.currentThread()
            .getName(), accumulated, accumulated2, counted);
        }
        assertEquals(accumulated, accumulated2);
        assertEquals(accumulated, counted);
      }
      txn.commit();
      return counted;
    } finally {
      txn.end();
    }
  }
}
