/**
 * Copyright 2011-2012 Akiban Technologies, Inc.
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

import com.persistit.CleanupManager.CleanupAction;
import com.persistit.exception.PersistitException;
import com.persistit.util.ThreadSequencer.Condition;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.persistit.util.SequencerConstants.CLEANUP_OFFER_A;
import static com.persistit.util.SequencerConstants.CLEANUP_OFFER_B;
import static com.persistit.util.SequencerConstants.CLEANUP_OFFER_C;
import static com.persistit.util.SequencerConstants.CLEANUP_OFFER_SCHEDULE;
import static com.persistit.util.ThreadSequencer.addSchedules;
import static com.persistit.util.ThreadSequencer.enableSequencer;
import static com.persistit.util.ThreadSequencer.sequence;
import static com.persistit.util.ThreadSequencer.setCondition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CleanupManagerTest extends PersistitUnitTestCase {

    private final static long JOIN_TIMEOUT_MS = 60_000;

    volatile int _counter = 0;
    volatile int _last = 0;
    volatile boolean _outOfOrder = false;

    private CleanupManager cm() {
        return _persistit.getCleanupManager();
    }

    /**
     * A CleanupManager of its own that is never started: no CLEANUP_MANAGER
     * thread polls it and no Persistit thread offers to it, so only the test
     * changes its counters.
     */
    private CleanupManager unstartedCleanupManager() {
        return new CleanupManager(_persistit);
    }

    private class CleanupMockAction implements CleanupAction {
        final int _sequence;

        CleanupMockAction(final int sequence) {
            _sequence = sequence;
        }

        @Override
        public int compareTo(final CleanupAction action) {
            return _sequence - ((CleanupMockAction) action)._sequence;

        }

        @Override
        public void performAction(final Persistit persistit, final List<CleanupAction> consequentActions)
                throws PersistitException {
            synchronized (this) {
                // Record ordering instead of asserting here: this runs on the
                // background CLEANUP_MANAGER thread, where a thrown AssertionError
                // is swallowed rather than failing the test. testCleanupHappens
                // verifies _outOfOrder on the test thread.
                if (_sequence != _last + 1) {
                    _outOfOrder = true;
                }
                _last = _sequence;
            }
            _counter++;
            if (_sequence == 123) {
                throw new ExpectedException();
            }
        }

        @Override
        public String toString() {
            return "CleanupMockAction(" + _sequence + ")";
        }

    }

    private static class ExpectedException extends PersistitException {

        private static final long serialVersionUID = 1L;

    }

    @Test
    public void testCleanupHappens() throws Exception {
        /*
         * The manager's performed/error counters are cumulative for the
         * Persistit instance, and startup maintenance may already have
         * performed cleanup actions before this test enqueues its own (e.g.
         * the pruneTimelyResources() call at the end of
         * Persistit.initialize()). Compare against a baseline taken before
         * enqueueing rather than against absolute values.
         */
        final long performedBefore = cm().getPerformedCount();
        final long errorsBefore = cm().getErrorCount();
        for (int i = 1; i <= 500; i++) {
            cm().offer(new CleanupMockAction(i));
        }
        cm().setPollInterval(100);
        /*
         * Wait until the actions have actually been performed. getEnqueuedCount()
         * drops to 0 as soon as the background CLEANUP_MANAGER thread dequeues the
         * batch -- before performAction runs -- so looping on it can exit with the
         * actions not yet run and _counter still 0 on a slow runner
         * (expected:<500> but was:<0>). Poll the manager's own performed/error
         * counters, which advance only after each action completes.
         */
        final long expires = System.currentTimeMillis() + 30000;
        while (cm().getPerformedCount() - performedBefore + cm().getErrorCount() - errorsBefore < 500
                && System.currentTimeMillis() < expires) {
            Thread.sleep(50);
        }
        assertEquals(500, _counter);
        assertEquals(1, cm().getErrorCount() - errorsBefore);
        assertEquals(499, cm().getPerformedCount() - performedBefore);
        assertFalse("cleanup actions did not run in sequence order", _outOfOrder);
    }

    @Test
    public void testOverflow() throws Exception {
        final CleanupManager cm = unstartedCleanupManager();
        int accepted = 0;
        for (int i = 1; i <= CleanupManager.DEFAULT_QUEUE_SIZE * 2; i++) {
            if (cm.offer(new CleanupMockAction(i))) {
                accepted++;
            }
        }

        // Nothing drains this queue, so it refuses every action past its capacity
        assertEquals(CleanupManager.DEFAULT_QUEUE_SIZE, accepted);
        assertEquals(CleanupManager.DEFAULT_QUEUE_SIZE, cm.getAcceptedCount());
        assertEquals(CleanupManager.DEFAULT_QUEUE_SIZE, cm.getRefusedCount());
        final String s = cm.toString();
        assertTrue(s.contains("CleanupMockAction("));
        cm.clear();
        assertEquals(0, cm.getEnqueuedCount());
    }

    /**
     * A RuntimeException from an action counts as an error, as a
     * PersistitException does, and poll() goes on with the rest of its work
     * list. Before the fix the exception left poll(), and the actions after
     * the failed one were lost without being performed or counted.
     */
    @Test
    public void testRuntimeExceptionCountedAsError() throws Exception {
        final CleanupManager cm = unstartedCleanupManager();
        final RuntimeException failure = new IllegalStateException("expected");
        // Sorts before the action that follows it
        cm.offer(new CleanupMockAction(1) {
            @Override
            public void performAction(final Persistit persistit, final List<CleanupAction> consequentActions) {
                throw failure;
            }
        });
        cm.offer(new CleanupMockAction(2));
        cm.poll();

        assertEquals(1, cm.getErrorCount());
        assertEquals(1, cm.getPerformedCount());
        assertEquals("the action after the failed one should be performed", 1, _counter);
        assertSame(failure, cm.getLastException());
    }

    /**
     * clear() counts the actions it discards, so that the accepted count
     * still equals the performed, error and discarded counts.
     */
    @Test
    public void testClearCountsDiscardedActions() throws Exception {
        final CleanupManager cm = unstartedCleanupManager();
        for (int i = 1; i <= 3; i++) {
            cm.offer(new CleanupMockAction(i));
        }
        cm.poll();
        for (int i = 4; i <= 10; i++) {
            cm.offer(new CleanupMockAction(i));
        }
        cm.clear();

        assertEquals(0, cm.getEnqueuedCount());
        assertEquals(3, cm.getPerformedCount());
        assertEquals(7, cm.getDiscardedCount());
        assertEquals(cm.getAcceptedCount(), cm.getPerformedCount() + cm.getErrorCount() + cm.getDiscardedCount());
    }

    /**
     * poll() drains the queue without the monitor that offer() holds, so it
     * can perform an action before offer() returns. Before the fix offer()
     * counted the action as accepted only after it was in the queue, so for
     * that moment the performed count included an action the accepted count
     * did not.
     */
    @Test
    public void testActionAcceptedBeforePerformed() throws Exception {
        final CleanupManager cm = unstartedCleanupManager();
        final AtomicLong acceptedWhenPerformed = new AtomicLong(-1);
        final CountDownLatch performed = new CountDownLatch(1);
        final CleanupAction action = new CleanupAction() {
            @Override
            public int compareTo(final CleanupAction other) {
                return 0;
            }

            @Override
            public void performAction(final Persistit persistit, final List<CleanupAction> consequentActions) {
                acceptedWhenPerformed.set(cm.getAcceptedCount());
                performed.countDown();
            }
        };
        final AtomicBoolean offered = new AtomicBoolean();
        final AtomicReference<Throwable> pollFailure = new AtomicReference<>();

        final Thread offerer = new Thread(() -> offered.set(cm.offer(action)), "CleanupManagerTest-offer");
        offerer.setDaemon(true);
        // No CLEANUP_MANAGER thread polls this CleanupManager, so another thread does
        final Thread poller = new Thread(() -> {
            try {
                cm.poll();
            } catch (final Throwable t) {
                pollFailure.set(t);
            }
        }, "CleanupManagerTest-poll");
        poller.setDaemon(true);

        enableSequencer(false);
        addSchedules(CLEANUP_OFFER_SCHEDULE);
        // Park only the test's offer(), not one that a Persistit thread makes to its own CleanupManager
        setCondition(CLEANUP_OFFER_A, new Condition() {
            @Override
            public boolean enabled() {
                return Thread.currentThread() == offerer;
            }
        });
        offerer.start();
        // Returns once offer() has put the action into the queue; offer() stays parked there
        sequence(CLEANUP_OFFER_B);
        try {
            poller.start();
            assertTrue("poll() should perform the action while offer() is running",
                    performed.await(JOIN_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals("the action should count as accepted by the time poll() performs it", 1,
                    acceptedWhenPerformed.get());
        } finally {
            sequence(CLEANUP_OFFER_C);
            offerer.join(JOIN_TIMEOUT_MS);
            poller.join(JOIN_TIMEOUT_MS);
            setCondition(CLEANUP_OFFER_A, new Condition());
        }
        assertFalse("offer() did not return", offerer.isAlive());
        assertFalse("poll() did not return", poller.isAlive());
        assertNull("poll() failed: " + pollFailure.get(), pollFailure.get());
        assertTrue("offer() should accept the action", offered.get());
        assertEquals(1, cm.getAcceptedCount());
        assertEquals(1, cm.getPerformedCount());
    }

    @Test
    public void testMemoryReleasedOnCrash() throws Exception {
        final WeakReference<Persistit> ref = new WeakReference<Persistit>(_persistit);
        CleanupManager cm = cm();
        cm.offer(new CleanupMockAction(1));
        _persistit.crash();
        _persistit = new Persistit();
        cm = null;
        assertTrue(doesRefBecomeNull(ref));
    }
}
