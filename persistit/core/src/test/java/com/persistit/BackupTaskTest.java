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

import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class BackupTaskTest extends PersistitUnitTestCase {
  private final static int TRANSACTION_COUNT = 50000;

  @Override
  protected Properties doGetProperties(final boolean cleanup) {
    return getBiggerProperties(cleanup);
  }

  @Test
  public void testSimpleBackup() throws Exception {

    final PrintWriter writer = new PrintWriter(System.out);
    final PersistitMap<Integer, String> pmap1 = new PersistitMap<Integer, String>(_persistit.getExchange(
      "persistit", "BackupTest", true));
    for (int index = 0; index < 50000; index++) {
      pmap1.put(new Integer(index), "This is the record for index=" + index);
    }

    final TreeMap<Integer, String> tmap = new TreeMap<Integer, String>(pmap1);
    final File file = File.createTempFile("backup", ".zip");
    file.deleteOnExit();

    final BackupTask backup1 = (BackupTask) CLI
      .parseTask(_persistit, "backup -z -c file=" + file.getAbsolutePath());

    backup1.setMessageWriter(writer);
    backup1.setup(1, "backup file=" + file.getAbsolutePath(), "cli", 0, 5);
    backup1.run();

    final Configuration config = _persistit.getConfiguration();
    _persistit.close();

    final BackupTask backup2 = new BackupTask();
    backup2.setMessageWriter(writer);
    backup2.setPersistit(_persistit);
    backup2.doRestore(file.getAbsolutePath());

    _persistit = new Persistit(config);
    _persistit.checkAllVolumes();

    final PersistitMap<Integer, String> pmap2 = new PersistitMap<Integer, String>(_persistit.getExchange(
      "persistit", "BackupTest", false));
    final boolean comparison = pmap2.equals(tmap);
    assertTrue(comparison);
  }

  @Test
  public void testBackupWithConcurrentTransactions() throws Exception {
    final PrintWriter writer = new PrintWriter(System.out);
    final TransactionWriter tw = new TransactionWriter();
    final Thread twThread = new Thread(tw, "BackupTest_TW");
    twThread.start();

    while (tw.counter.get() < TRANSACTION_COUNT) {
      Thread.sleep(1000);
    }

    final File file = temp.newFile("backup.zip");

    final BackupTask backup1 = (BackupTask) CLI
      .parseTask(_persistit, "backup -y -c file=" + file.getAbsolutePath().replaceAll("\\\\", "/"));

    backup1.setMessageWriter(writer);
    backup1.setPersistit(_persistit);
    backup1.setup(1, "backup file=" + file.getAbsolutePath(), "cli", 0, 5);
    tw.backupStarted.set(true);
    backup1.run();
    tw.stop.set(true);
    twThread.join();

    final Configuration config = _persistit.getConfiguration();
    _persistit.crash();
    cleanUpDirectory(new File(DATA_PATH));

    final BackupTask backup2 = new BackupTask();
    backup2.setMessageWriter(writer);
    backup2.setPersistit(_persistit);
    backup2.doRestore(file.getAbsolutePath());

    config.setAppendOnly(true);
    _persistit = new Persistit(config);
    _persistit.checkAllVolumes();
    final Exchange exchange = _persistit.getExchange("persistit", "BackupTest", false);
    exchange.to(Key.BEFORE);
    int extras = 0;
    while (exchange.next()) {
      final int key = exchange.getKey().reset().decodeInt();
      final boolean found = tw.commitTransactions.remove(key);
      if (!found) {
        extras++;
      }
    }
    assertTrue(tw.commitTransactions.isEmpty());
  }

  private class TransactionWriter implements Runnable {

    final Set<Integer> commitTransactions = new HashSet<Integer>();
    final AtomicInteger counter = new AtomicInteger();
    final Random random = new Random(1);
    final AtomicBoolean stop = new AtomicBoolean();
    final AtomicBoolean backupStarted = new AtomicBoolean();

    @Override
    public void run() {
      try {
        final Exchange ex = _persistit.getExchange("persistit", "BackupTest", true);
        final Transaction transaction = ex.getTransaction();
        while (!stop.get()) {
          final int key = random.nextInt();
          transaction.begin();
          ex.to(key);
          ex.getValue().put("Record for key=" + key);
          ex.store();
          transaction.commit();
          if (!backupStarted.get()) {
            commitTransactions.add(key);
          }
          final int count = counter.incrementAndGet();
          transaction.end();
          // Once the counter has advanced to TRANSACTION_COUNT,
          // throttle this
          // thread back to a more "realistic" rate and let the backup
          // thread proceed.
          if (count > TRANSACTION_COUNT) {
            Thread.sleep(10);
          }
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void runAllTests() throws Exception {
    testSimpleBackup();
    testBackupWithConcurrentTransactions();
  }

}
