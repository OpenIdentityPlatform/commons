/**
 * Copyright 2013 Akiban Technologies, Inc.
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

import com.persistit.exception.InUseException;
import com.persistit.exception.InvalidKeyException;
import com.persistit.exception.PersistitException;
import org.junit.Test;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExchangeLockTest extends PersistitUnitTestCase {
  private final static long DMILLIS = SharedResource.DEFAULT_MAX_WAIT_TIME;
  private final Semaphore _coordinator = new Semaphore(0);

  @Override
  public Properties doGetProperties(final boolean cleanup) {
    return getBiggerProperties(cleanup);
  }

  @Test
  public void singleThreadedLock() throws Exception {
    final Exchange ex = _persistit.getExchange("persistit", "ExchangeLockTest", true);
    final Transaction txn = ex.getTransaction();
    try {
      ex.lock();
      fail("Expected to fail");
    } catch (final IllegalStateException e) {
      // expected
    }
    txn.begin();
    try {
      try {
        ex.lock();
        fail("Expected to fail");
      } catch (final InvalidKeyException e) {
        // expected
      }
      ex.append("motor");
      ex.lock();
      final Tree tree = _persistit.getLockVolume().getTree("ExchangeLockTest", false);
      assertTrue("Expected tree to be defined", tree != null);
      final Exchange ex2 = new Exchange(tree);
      ex2.ignoreMVCCFetch(true);
      assertTrue("Expect a key in the temp volume", ex2.next(true));
      txn.commit();
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      txn.end();
    }

    txn.begin();
    try {
      ex.lock();
      txn.commit();
    } finally {
      txn.end();
    }
  }

  private class Locker implements Runnable {
    final Semaphore _semaphore = new Semaphore(0);
    final long _timeout;
    final int[] _sequence;
    Exception _exception;
    volatile int _expectedReleases;

    volatile boolean _committed;

    private Locker(final long timeout, final int... sequence) {
      _timeout = timeout;
      _sequence = sequence;
    }

    private void go(final int waitFor) throws InterruptedException {
      _semaphore.release();
      _coordinator.acquire(waitFor);
    }

    private int cycles() {
      return _sequence.length + 2;
    }

    @Override
    public void run() {
      try {
        final Exchange ex = _persistit.getExchange("persistit", "ExchangeLockTest", true);
        final Transaction txn = ex.getTransaction();
        _expectedReleases = cycles();
        txn.begin();
        try {
          for (final int k : _sequence) {
            _semaphore.acquire();
            ex.clear().append(k).lock(ex.getKey(), _timeout);
            _coordinator.release();
            _expectedReleases--;
          }
          _semaphore.acquire();
          txn.commit();
          _committed = true;
          _coordinator.release();
          _expectedReleases--;
        } catch (final Exception e) {
          _exception = e;
          txn.rollback();
        } finally {
          if (_expectedReleases > 0) {
            _coordinator.release(_expectedReleases);
          }
          _semaphore.acquire();
          txn.end();
          _coordinator.release();
          _expectedReleases--;
        }

      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  private Thread[] start(final Locker... lockers) {
    final Thread[] threads = new Thread[lockers.length];
    int count = 0;
    for (final Locker locker : lockers) {
      final Thread t = new Thread(locker);
      t.start();
      threads[count++] = t;
    }
    return threads;
  }

  private void join(final Thread[] threads) throws InterruptedException {
    for (final Thread t : threads) {
      t.join();
    }
  }

  @Test
  public void nonConflictingLocks() throws Exception {
    final Locker a = new Locker(DMILLIS, 1, 5);
    final Locker b = new Locker(DMILLIS, 2, 6);
    final Locker c = new Locker(DMILLIS, 3, 7);
    final Locker d = new Locker(DMILLIS, 4, 8);
    final Thread[] threads = start(a, b, c, d);
    for (int i = 0; i < a.cycles(); i++) {
      a.go(1);
      b.go(1);
      c.go(1);
      d.go(1);
    }
    join(threads);
    assertTrue(a._committed);
    assertTrue(b._committed);
    assertTrue(c._committed);
    assertTrue(d._committed);
  }

  @Test
  public void simpleConflictingLocks() throws Exception {
    final Locker a = new Locker(1000, 1);
    final Locker b = new Locker(1000, 1);
    final Thread[] threads = start(a, b);
    final long start = System.currentTimeMillis();
    for (int i = 0; i < a.cycles(); i++) {
      a.go(1);
      b.go(1);
    }
    join(threads);
    final long end = System.currentTimeMillis();
    assertTrue(end - start >= 1000);
    assertTrue(a._committed ^ b._committed);
  }

  @Test
  public void deadlock() throws Exception {
    final Locker a = new Locker(DMILLIS, 1, 2);
    final Locker b = new Locker(DMILLIS, 2, 1);
    final Thread[] threads = start(a, b);
    final long start = System.currentTimeMillis();
    for (int i = 0; i < a.cycles(); i++) {
      a.go(i == 0 ? 1 : 0);
      b.go(i == 0 ? 1 : 0);
    }
    join(threads);
    final long end = System.currentTimeMillis();
    assertTrue(end - start < DMILLIS);
    assertTrue(a._committed ^ b._committed);
  }

  @Test
  public void multiWayDeadlock() throws Exception {
    final Locker a = new Locker(DMILLIS, 1, 2, 3, 4, 5);
    final Locker b = new Locker(DMILLIS, 2, 3, 4, 5, 1);
    final Locker c = new Locker(DMILLIS, 3, 4, 5, 1, 2);
    final Locker d = new Locker(DMILLIS, 4, 5, 1, 2, 3);
    final Locker e = new Locker(DMILLIS, 5, 1, 2, 3, 4);
    final Thread[] threads = start(a, b, c, d, e);
    final long start = System.currentTimeMillis();
    for (int i = 0; i < a.cycles(); i++) {
      final int w = (i == 0) ? 1 : 0;
      a.go(w);
      b.go(w);
      c.go(w);
      d.go(w);
      e.go(w);
    }
    join(threads);
    final long end = System.currentTimeMillis();
    assertTrue(end - start < DMILLIS);
    int succeeded = 0;
    for (final Locker l : new Locker[] {a, b, c, d, e}) {
      if (l._committed) {
        succeeded++;
      }
    }
    assertEquals(1, succeeded);
  }

  /**
   * This test is intended to exercise lock management for transactions
   * executed sequentially, each of which performs lots of locks (similar to
   * DataLoadingTest.)
   * 
   * @throws Exception
   */
  @Test
  public void lockTablePruning() throws Exception {
    final Exchange ex = _persistit.getExchange("persistit", "ExchangeLockTest", true);
    final Random random = new Random();
    final Transaction txn = ex.getTransaction();
    for (int j = 0; j < 100; j++) {
      txn.begin();
      for (int i = 0; i < 10000; i++) {
        final int k = random.nextInt(100000);
        ex.clear().append(k + (j * 100000)).append(RED_FOX).lock();
      }
      txn.commit();
      txn.end();
    }
    assertTrue("Too many lock volume pages uses",
      _persistit.getLockVolume().getStorage().getNextAvailablePage() < 100);

    final Exchange lockExchange = new Exchange(_persistit.getLockVolume().getTree("ExchangeLockTest", false));
    final int count = keyCount(lockExchange);

    assertEquals("Unpruned lock records", 0, count);
  }

  private int keyCount(final Exchange ex) throws PersistitException {
    int count = 0;
    ex.clear();
    while (ex.next(true)) {
      count++;
    }
    return count;
  }

  @Test
  public void timeout() throws Exception {
    /*
     * A cursory check of the Exchange timeout value
     */
    final Exchange ex = _persistit.getExchange("persistit", "gogo", true);
    ex.to("a").store();
    final long latchedPage = ex.fetchBufferCopy(0).getPageAddress();
    final BufferPool pool = ex.getBufferPool();
    final Volume volume = ex.getVolume();
    ex.setTimeoutMillis(1000);
    final long start = System.currentTimeMillis();
    final Semaphore a = new Semaphore(0);
    final Semaphore b = new Semaphore(0);

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          final Buffer buffer = pool.get(volume, latchedPage, true, true);
          a.release();
          b.acquire();
          buffer.release();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
    a.acquire();
    try {
      ex.to("a").store();
      fail("Expected an InUseException");

    } catch (final InUseException e) {
      // expected
    }
    final long end = System.currentTimeMillis();
    b.release();
    final long interval = end - start;
    assertTrue("Should have waited about 1 second", interval >= 1000 && interval < 2000);
  }

  @Test
  public void bug1125603test() throws Exception {
    final Exchange ex = _persistit.getExchange("persistit", "ExchangeLockTest", true);
    final Transaction txn = ex.getTransaction();
    txn.begin();
    try {
      ex.append("motor");
      ex.lock();
      txn.commit();
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      txn.end();
    }
    _persistit.getJournalManager().rolloverWithNewFile();
    _persistit.checkpoint();
    _persistit.close();
    _persistit = new Persistit(_config);
    _persistit.initialize();
  }
}
