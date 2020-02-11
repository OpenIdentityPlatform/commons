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

import com.persistit.Version.PrunableVersion;
import com.persistit.Version.VersionCreator;
import com.persistit.exception.PersistitException;
import com.persistit.exception.RollbackException;
import com.persistit.unit.ConcurrentUtil.ThrowingRunnable;
import com.persistit.unit.ConcurrentUtil.UncaughtExceptionHandler;
import com.persistit.util.Util;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static com.persistit.TransactionIndex.tss2vh;
import static com.persistit.TransactionStatus.UNCOMMITTED;
import static com.persistit.unit.ConcurrentUtil.assertSuccess;
import static com.persistit.unit.ConcurrentUtil.createThread;
import static com.persistit.unit.ConcurrentUtil.join;
import static com.persistit.unit.ConcurrentUtil.start;
import static com.persistit.util.Util.NS_PER_S;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimelyResourceTest extends PersistitUnitTestCase {

    private int _idCounter;

    static class TestVersion implements PrunableVersion {
        final int _id;
        final TimelyResourceTest _test;
        final AtomicInteger _pruned = new AtomicInteger();

        TestVersion(final int id, final TimelyResourceTest test) {
            _id = id;
            _test = test;
        }

        @Override
        public boolean prune() {
            _pruned.incrementAndGet();
            return true;
        }

        @Override
        public void vacate() {
            System.out.println("No more versions");
        }

        @Override
        public String toString() {
            return String.format("<%,d:%,d>", _id, _pruned.get());
        }

        TimelyResourceTest getContainer() {
            return _test;
        }
    }

    @Test
    public void testAddAndPruneResources() throws Exception {
        testAddAndPruneResources1(false);
        testAddAndPruneResources1(true);
    }

    private void testAddAndPruneResources1(final boolean withTransactions) throws Exception {
        final Transaction txn = _persistit.getTransaction();
        final TimelyResource<TestVersion> tr = new TimelyResource<TestVersion>(_persistit);
        final long[] history = new long[5];
        final TestVersion[] resources = new TestVersion[5];
        for (int i = 0; i < 5; i++) {
            if (withTransactions) {
                txn.begin();
            }
            final TestVersion resource = new TestVersion(i, this);
            resources[i] = resource;
            if (!tr.isEmpty()) {
                tr.delete();
            }
            tr.addVersion(resource, txn);
            if (withTransactions) {
                txn.commit();
                txn.end();
            }
            history[i] = _persistit.getTimestampAllocator().updateTimestamp();
        }
        assertEquals("Incorrect version count " + withTransactions, 9, tr.getVersionCount());

        for (int i = 0; i < 5; i++) {
            final TestVersion t = tr.getVersion(tss2vh(history[i], 0));
            assertTrue("Missing version " + withTransactions, t != null);
            assertEquals("Wrong version " + withTransactions, i, t._id);
        }
        _persistit.getTransactionIndex().updateActiveTransactionCache();
        tr.prune();
        assertEquals("Should have one version left " + withTransactions, 1, tr.getVersionCount());
        assertEquals("Wrong version " + withTransactions, 4, tr.getVersion(tss2vh(UNCOMMITTED, 0))._id);

        tr.delete();

        assertEquals("Should have two versions left " + withTransactions, 2, tr.getVersionCount());
        _persistit.getTransactionIndex().updateActiveTransactionCache();
        tr.prune();
        assertEquals("Should have no versions left " + withTransactions, 0, tr.getVersionCount());

        for (int i = 0; i < 5; i++) {
            assertEquals("Should have been pruned " + withTransactions, 1, resources[i]._pruned.get());
        }
    }

    @Test
    public void concurrentAddAndPruneResources() throws Exception {
        final TimelyResource<TestVersion> tr = new TimelyResource<TestVersion>(_persistit);
        final Random random = new Random(1);
        final long expires = System.nanoTime() + 10 * NS_PER_S;
        final AtomicInteger sequence = new AtomicInteger();
        final AtomicInteger rollbackCount = new AtomicInteger();
        final List<Thread> threads = new ArrayList<Thread>();
        final UncaughtExceptionHandler handler = new UncaughtExceptionHandler();
        int threadCounter = 0;
        while (System.nanoTime() < expires) {
            for (final Iterator<Thread> iter = threads.iterator(); iter.hasNext();) {
                if (!iter.next().isAlive()) {
                    iter.remove();
                }
            }
            while (threads.size() < 20) {
                final Thread t = createThread(String.format("Thread_%06d", ++threadCounter), new ThrowingRunnable() {
                    @Override
                    public void run() throws Exception {
                        doConcurrentTransaction(tr, random, sequence, rollbackCount);
                    }
                });
                threads.add(t);
                start(handler, t);
            }
            Util.sleep(10);
            tr.prune();
        }
        join(Long.MAX_VALUE, handler.getThrowableMap(), threads.toArray(new Thread[threads.size()]));
        assertSuccess(handler.getThrowableMap());
        assertTrue("Every transaction rolled back", rollbackCount.get() < sequence.get());
        System.out.printf("%,d entries, %,d rollbacks\n", sequence.get(), rollbackCount.get());
    }

    private void doConcurrentTransaction(final TimelyResource<TestVersion> tr, final Random random,
            final AtomicInteger sequence, final AtomicInteger rollbackCount) throws PersistitException {
        try {
            final Transaction txn = _persistit.getTransaction();
            for (int i = 0; i < 25; i++) {
                txn.begin();
                try {
                    final int id = sequence.incrementAndGet();
                    tr.addVersion(new TestVersion(id, this), txn);
                    final int delay = (1 << random.nextInt(3));
                    // Up to 7/1000 of a second
                    Util.sleep(delay);
                    final TestVersion mine = tr.getVersion();
                    assertEquals("Should not have been pruned yet", 0, mine._pruned.get());
                    assertEquals("Wrong resource", id, mine._id);
                    if (random.nextInt(10) == 0) {
                        txn.rollback();
                    } else {
                        txn.commit();
                    }
                } catch (final RollbackException e) {
                    txn.rollback();
                    rollbackCount.incrementAndGet();
                } finally {
                    txn.end();
                }
            }
        } catch (final RollbackException e) {
            rollbackCount.incrementAndGet();
        }
    }

    @Test
    public void deleteResource() throws Exception {
        final TimelyResource<TestVersion> tr = new TimelyResource<TestVersion>(_persistit);
        _idCounter = 0;
        final VersionCreator<TestVersion> creator = new VersionCreator<TestVersion>() {

            @Override
            public TestVersion createVersion(final TimelyResource<? extends TestVersion> resource)
                    throws PersistitException {
                return new TestVersion(++_idCounter, TimelyResourceTest.this);
            }
        };
        final Transaction txn1 = _persistit.getTransaction();
        _persistit.setSessionId(new SessionId());
        final Transaction txn2 = _persistit.getTransaction();
        TestVersion v1;
        txn1.begin();
        v1 = tr.getVersion(creator);
        assertTrue("Version ID mismatch", v1._id == _idCounter);
        txn2.begin();
        txn1.incrementStep();
        tr.delete();
        tr.prune();
        assertEquals("Should still be two versions", 2, tr.getVersionCount());
        txn2.commit();
        txn1.commit();
        _persistit.getTransactionIndex().updateActiveTransactionCache();
        tr.prune();
        assertEquals("Should now have no versions", 0, tr.getVersionCount());
        txn1.end();
        txn2.end();

    }

    @Test
    public void versions() throws Exception {
        final TimelyResource<TestVersion> tr = new TimelyResource<TestVersion>(_persistit);
        _idCounter = 0;
        final VersionCreator<TestVersion> creator = new VersionCreator<TestVersion>() {

            @Override
            public TestVersion createVersion(final TimelyResource<? extends TestVersion> resource)
                    throws PersistitException {
                return new TestVersion(++_idCounter, TimelyResourceTest.this);
            }
        };
        final Semaphore semaphore1 = new Semaphore(0);
        final Transaction txn = _persistit.getTransaction();
        final Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            try {
                txn.begin();
                tr.addVersion(creator.createVersion(tr), txn);
                txn.commit();
            } finally {
                txn.end();
            }
            final Semaphore semaphore2 = new Semaphore(0);
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Transaction txn = _persistit.getTransaction();
                    try {
                        txn.begin();
                        final TestVersion t = tr.getVersion();
                        assertEquals(t._id, _idCounter);
                        semaphore2.release();
                        semaphore1.acquire();
                        txn.commit();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        txn.end();
                    }
                }
            });
            threads[i].start();
            semaphore2.acquire();
        }
        _persistit.getTransactionIndex().updateActiveTransactionCache();
        tr.prune();
        assertEquals(10, tr.getVersionCount());
        semaphore1.release(10);
        for (final Thread thread : threads) {
            thread.join();
        }
        _persistit.getTransactionIndex().updateActiveTransactionCache();
        tr.prune();
        assertEquals(1, tr.getVersionCount());
        assertEquals("Surviving primordial version should be last one committed", 10, tr.getVersion(null)._id);
    }

}
