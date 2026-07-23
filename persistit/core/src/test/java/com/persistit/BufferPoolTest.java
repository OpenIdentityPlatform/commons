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

import com.persistit.BufferPool.BufferHolder;
import com.persistit.exception.InUseException;
import com.persistit.exception.PersistitException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BufferPoolTest extends PersistitUnitTestCase {

    /**
     * Covers allocPage condition in which page in the avaialbleBitMap is
     * unavailable.
     * 
     * @throws Exception
     */
    @Test
    public void testInvalidatedBuffers() throws Exception {
        final Volume vol = _persistit.createTemporaryVolume();
        final Exchange ex = _persistit.getExchange(vol, "BufferPoolTest", true);
        ex.append("k").store();
        // Hold a claim on the page.
        final Buffer buffer1 = vol.getPool().get(vol, 2, false, true);
        buffer1.release();
        // Invalidate the buffers
        vol.getPool().invalidate(vol);
        // reestablish claim on now-invalid buffer
        buffer1.claim(true, 0);
        // Do work that allocates buffers
        final Exchange ex2 = _persistit.getExchange("persistit", "BufferPoolTest", true);
        ex2.getValue().put(RED_FOX);
        for (int i = 0; i < 10000; i++) {
            ex2.to(i).store();
        }
        buffer1.release();
    }

    @Test
    public void testSelectDirtyBuffers() throws Exception {
        final Volume volume = _persistit.getVolume("persistit");
        final BufferPool pool = volume.getPool();
        pool.setFlushTimestamp(-1);
        try {
            final int buffers = pool.getBufferCount();
            final int[] priorities = new int[buffers / 2];
            final BufferHolder[] holders = new BufferHolder[buffers / 2];
            for (int i = 0; i < holders.length; i++) {
                holders[i] = new BufferHolder();
            }
            final long timestamp = _persistit.getTimestampAllocator().getCurrentTimestamp();
            pool.flush(timestamp);

            int count = pool.selectDirtyBuffers(priorities, holders);
            assertEquals("Buffer pool should be clean", 0, count);

            for (int i = 1; i < buffers; i++) {
                final long page = volume.getStorage().allocNewPage();
                final Buffer buffer = pool.get(volume, page, true, false);
                buffer.setDirtyAtTimestamp(timestamp + i);
                buffer.releaseTouched();
            }

            count = pool.selectDirtyBuffers(priorities, holders);
            assertEquals("Selected buffers should fill the arrays", buffers / 2, count);
            long page = -1;
            Arrays.sort(holders);
            for (final BufferHolder holder : holders) {
                assertTrue(holder.getPage() > page);
                page = holder.getPage();
            }
        } finally {
            pool.setFlushTimestamp(1000);
        }
    }

    @Test
    public void testAddSelectedBuffer() throws Exception {
        final Volume volume = _persistit.getVolume("persistit");
        final BufferPool pool = volume.getPool();

        final int total = 100;
        final int[] priorities = new int[total];
        final BufferHolder[] holders = new BufferHolder[total];
        for (int i = 0; i < holders.length; i++) {
            holders[i] = new BufferHolder();
        }

        final Random random = new Random(1);
        final SortedSet<Integer> sorted = new TreeSet<Integer>();
        int count = 0;
        for (int index = 0; index < 10000; index++) {
            final int r = random.nextInt(1000000000);
            if (sorted.contains(r)) {
                index--;
                continue;
            }
            sorted.add(r);
            final Buffer buffer = pool.get(volume, 1, false, false);
            final Buffer copy = new Buffer(buffer);
            copy.setPageAddressAndVolume(index, volume);
            count = pool.addSelectedBufferByPriority(copy, r, priorities, holders, count);
            buffer.release();
        }
        assertEquals("Arrays should be full", total, count);
        final Integer[] sortedArray = sorted.toArray(new Integer[sorted.size()]);

        for (int i = 0; i < count; i++) {
            final int s = sortedArray[sortedArray.length - i - 1];
            final int r = priorities[i];
            assertEquals("Priority order is wrong", s, r);
        }
        long page = -1;
        Arrays.sort(holders);
        for (final BufferHolder holder : holders) {
            assertTrue(holder.getPage() > page);
            page = holder.getPage();
        }

        for (int i = 0; i < count; i++) {
            final BufferHolder holder = holders[i];
            for (int j = i + 1; j < count; j++) {
                assertTrue("Scrambled holders", holder != holders[j]);
            }
        }
    }

    @Test
    public void testWritePriority() throws Exception {
        final long m = 100 * 1000 * 1000;
        final Volume volume = _persistit.getVolume("persistit");
        final BufferPool pool = volume.getPool();
        final Buffer buffer = pool.getBufferCopy(0);
        buffer.claim(true);
        long currentTimestamp = 4 * m;
        final long checkpointTimestamp = 2 * m;
        for (long timestamp = m; timestamp < m * 20; timestamp += m) {
            buffer.setDirtyAtTimestamp(timestamp);
            final int priority = pool.writePriority(buffer, 123456, checkpointTimestamp, currentTimestamp);
            System.out.printf("Timestamp %,15d Checkpoint %,15d Current %,15d Priority %,15d\n", timestamp,
                    checkpointTimestamp, currentTimestamp, priority);
            currentTimestamp += 10000000;
        }
    }

    @Test
    public void testEvictVoume() throws Exception {
        final Volume vol = _persistit.createTemporaryVolume();
        final Exchange ex = _persistit.getExchange(vol, "BufferPoolTest", true);
        _persistit.flush();
        ex.getValue().put(RED_FOX);
        int i;
        for (i = 1;; i++) {
            ex.to(i).store();
            if (vol.getNextAvailablePage() >= 10) {
                break;
            }
        }
        vol.getPool().evict(vol);
        assertTrue("Should be no remaining dirty buffers", vol.getPool().getDirtyPageCount() == 0);
        for (int j = 0; j < i + 100; j++) {
            ex.to(j).fetch();
            assertEquals(j >= 1 && j <= i, ex.getValue().isDefined());
        }
    }

    /**
     * When every buffer in the pool is claimed, get() for a page that is not
     * in the pool must keep retrying the allocation until the caller's timeout
     * budget expires and then throw a checked InUseException — not give up
     * with a raw IllegalStateException after a single clock sweep (issue
     * #300).
     */
    @Test
    public void testGetHonorsTimeoutWhenPoolExhausted() throws Exception {
        final Volume volume = _persistit.getVolume("persistit");
        final BufferPool pool = volume.getPool();
        final long timeout = 500;
        final long[] pages = allocPages(volume, pool.getBufferCount() + 1);
        final List<Buffer> claimed = new ArrayList<Buffer>();
        try {
            InUseException exhausted = null;
            long elapsed = -1;
            for (final long page : pages) {
                final long start = System.currentTimeMillis();
                try {
                    claimed.add(pool.get(volume, page, true, false, timeout));
                } catch (final InUseException e) {
                    exhausted = e;
                    elapsed = System.currentTimeMillis() - start;
                    break;
                }
            }
            assertNotNull("Claiming " + claimed.size() + " buffers of " + pool.getBufferCount()
                    + " did not exhaust the pool", exhausted);
            assertTrue("get() failed after " + elapsed + " ms, before its " + timeout + " ms timeout expired",
                    elapsed >= timeout - 100);
        } finally {
            for (final Buffer buffer : claimed) {
                buffer.release();
            }
        }
    }

    /**
     * A get() that finds the pool exhausted must keep retrying and succeed as
     * soon as another thread releases a buffer, rather than failing after a
     * single sweep (issue #300).
     */
    @Test
    public void testGetWaitsForReleasedBuffer() throws Exception {
        final Volume volume = _persistit.getVolume("persistit");
        final BufferPool pool = volume.getPool();
        final long releaseDelay = 300;
        final long[] pages = allocPages(volume, pool.getBufferCount() + 2);
        final List<Buffer> claimed = new ArrayList<Buffer>();
        try {
            /*
             * Require two consecutive allocation failures so that a single
             * failure caused by a transient claim held by a background thread
             * is not mistaken for pool exhaustion.
             */
            int consecutiveFailures = 0;
            while (consecutiveFailures < 2 && claimed.size() <= pool.getBufferCount()) {
                try {
                    claimed.add(pool.get(volume, pages[claimed.size()], true, false, 100));
                    consecutiveFailures = 0;
                } catch (final InUseException expected) {
                    consecutiveFailures++;
                }
            }
            assertTrue("Claiming " + claimed.size() + " buffers of " + pool.getBufferCount()
                    + " did not exhaust the pool", claimed.size() <= pool.getBufferCount());

            final long page = pages[pages.length - 1];
            final AtomicLong waited = new AtomicLong(-1);
            final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
            final Thread getter = new Thread(new Runnable() {
                @Override
                public void run() {
                    final long start = System.currentTimeMillis();
                    try {
                        final Buffer buffer = pool.get(volume, page, true, false, 10000);
                        waited.set(System.currentTimeMillis() - start);
                        buffer.release();
                    } catch (final Throwable t) {
                        failure.set(t);
                    }
                }
            }, "BufferPoolTest_getter");
            getter.start();
            Thread.sleep(releaseDelay);
            claimed.remove(claimed.size() - 1).release();
            getter.join(30000);
            assertTrue("Getter thread did not finish", !getter.isAlive());
            assertNull("Getter thread failed: " + failure.get(), failure.get());
            assertTrue("get() returned after " + waited.get() + " ms, before a buffer was released",
                    waited.get() >= releaseDelay - 100);
        } finally {
            for (final Buffer buffer : claimed) {
                buffer.release();
            }
        }
    }

    private long[] allocPages(final Volume volume, final int count) throws PersistitException {
        final long[] pages = new long[count];
        for (int i = 0; i < count; i++) {
            pages[i] = volume.getStorage().allocNewPage();
        }
        return pages;
    }

}
