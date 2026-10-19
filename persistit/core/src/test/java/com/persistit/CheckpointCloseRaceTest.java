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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.persistit.exception.PersistitClosedException;
import com.persistit.exception.PersistitInterruptedException;
import com.persistit.util.ThreadSequencer.Condition;

import static com.persistit.util.SequencerConstants.CHECKPOINT_CLOSE_B;
import static com.persistit.util.SequencerConstants.CHECKPOINT_CLOSE_C;
import static com.persistit.util.SequencerConstants.CHECKPOINT_CLOSE_SCHEDULE;
import static com.persistit.util.SequencerConstants.COPYBACK_CLOSE_A;
import static com.persistit.util.SequencerConstants.COPYBACK_CLOSE_B;
import static com.persistit.util.SequencerConstants.COPYBACK_CLOSE_C;
import static com.persistit.util.SequencerConstants.COPYBACK_CLOSE_SCHEDULE;
import static com.persistit.util.ThreadSequencer.addSchedules;
import static com.persistit.util.ThreadSequencer.enableSequencer;
import static com.persistit.util.ThreadSequencer.sequence;
import static com.persistit.util.ThreadSequencer.setCondition;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A public {@link Persistit#checkpoint()} or {@link Persistit#copyBackPages()}
 * that has already passed its closed/initialized check must not be torn down by
 * a concurrent {@link Persistit#close()}: before the fix the closing thread
 * released the configuration, volumes and journal underneath the operation,
 * which then died with a raw <code>NullPointerException</code> in
 * <code>Persistit.getSpecialVolume</code> (seen as an uncaught exception from
 * the background checkpoint thread of {@link Bug1064565Test}).
 */
public class CheckpointCloseRaceTest extends PersistitUnitTestCase {

    /**
     * How long the closing thread is given to run while the operation is held
     * inside its critical section. Without the fix close() completes well within
     * this time; with the fix it stays blocked until the operation finishes.
     */
    private final static long CLOSE_GRACE_MS = 2_000;

    private final static long JOIN_TIMEOUT_MS = 60_000;

    private final static long FAST_CLOSE_WAIT_MS = 200;

    private interface Operation {
        void run() throws Exception;
    }

    @Test
    public void checkpointConcurrentWithClose() throws Exception {
        enableSequencer(false);
        addSchedules(CHECKPOINT_CLOSE_SCHEDULE);
        final Persistit persistit = _persistit;
        runConcurrentWithClose("checkpoint()", persistit::checkpoint, CHECKPOINT_CLOSE_B, CHECKPOINT_CLOSE_C);
    }

    @Test
    public void copyBackPagesConcurrentWithClose() throws Exception {
        // copyBackPages() reaches COPYBACK_CLOSE_A on every pass of its loop;
        // only the first pass is sequenced against close().
        final AtomicBoolean once = new AtomicBoolean(true);
        setCondition(COPYBACK_CLOSE_A, new Condition() {
            @Override
            public boolean enabled() {
                return once.getAndSet(false);
            }
        });
        enableSequencer(false);
        addSchedules(COPYBACK_CLOSE_SCHEDULE);
        final Persistit persistit = _persistit;
        // Keep the first pass from ending the loop: with copy-back disabled
        // the journal keeps more than one file, so a second pass follows.
        final JournalManager jman = persistit.getJournalManager();
        jman.setAppendOnly(true);
        jman.rolloverWithNewFile();
        assertTrue("the journal should span more than one file", jman.getJournalFileCount() > 1);
        final AtomicReference<PersistitClosedException> closedOnNextPass = new AtomicReference<>();
        try {
            runConcurrentWithClose("copyBackPages()", () -> {
                try {
                    persistit.copyBackPages();
                } catch (final PersistitClosedException e) {
                    closedOnNextPass.set(e);
                }
            }, COPYBACK_CLOSE_B, COPYBACK_CLOSE_C);
            // close() waits for the parked pass only, not the whole loop, so
            // the next pass finds Persistit closed.
            assertNotNull("the pass after close() should throw PersistitClosedException", closedOnNextPass.get());
        } finally {
            setCondition(COPYBACK_CLOSE_A, new Condition());
        }
    }

    /**
     * A fast <code>close(false)</code> waits only a bounded time for a
     * checkpoint that does not finish, then closes anyway. A later
     * <code>close()</code> of the closed instance returns at once although
     * the checkpoint still holds the lock.
     */
    @Test
    public void fastCloseDoesNotWaitForStuckCheckpoint() throws Exception {
        enableSequencer(false);
        addSchedules(CHECKPOINT_CLOSE_SCHEDULE);
        final Persistit persistit = _persistit;
        persistit.setFastCloseWaitMs(FAST_CLOSE_WAIT_MS);
        final AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        final AtomicLong closeMs = new AtomicLong();
        final AtomicReference<Throwable> secondCloseFailure = new AtomicReference<>();

        final Thread operator = new Thread(() -> {
            try {
                persistit.checkpoint();
            } catch (final Throwable t) {
                // Released resources may fail the checkpoint; not checked here
            }
        }, "CheckpointCloseRaceTest-operation");
        operator.setDaemon(true);
        final Thread closer = new Thread(() -> {
            final long start = System.nanoTime();
            try {
                persistit.close(false);
            } catch (final Throwable t) {
                closeFailure.set(t);
            }
            closeMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }, "CheckpointCloseRaceTest-close");
        final Thread secondCloser = new Thread(() -> {
            try {
                persistit.close();
            } catch (final Throwable t) {
                secondCloseFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-second-close");
        secondCloser.setDaemon(true);

        operator.start();
        sequence(CHECKPOINT_CLOSE_B);
        try {
            // The checkpoint stays parked while it holds the lock.
            closer.start();
            closer.join(JOIN_TIMEOUT_MS);
            assertFalse("close(false) should not wait for a stuck checkpoint", closer.isAlive());
            assertNull("close(false) failed: " + closeFailure.get(), closeFailure.get());
            assertTrue("Persistit should be closed", persistit.isClosed());
            // A timed tryLock fails only after its timeout has passed.
            assertTrue("close(false) should wait " + FAST_CLOSE_WAIT_MS + " ms before closing anyway, waited "
                    + closeMs.get(), closeMs.get() >= FAST_CLOSE_WAIT_MS);

            secondCloser.start();
            secondCloser.join(JOIN_TIMEOUT_MS);
            assertFalse("close() of a closed Persistit should not wait for a stuck checkpoint",
                    secondCloser.isAlive());
            assertNull("close() of a closed Persistit failed: " + secondCloseFailure.get(),
                    secondCloseFailure.get());
        } finally {
            sequence(CHECKPOINT_CLOSE_C);
            operator.join(JOIN_TIMEOUT_MS);
            secondCloser.join(JOIN_TIMEOUT_MS);
        }
    }

    /**
     * An interrupt does not keep a fast <code>close(false)</code> from closing.
     */
    @Test
    public void fastCloseOnInterruptedThreadCloses() throws Exception {
        final Persistit persistit = _persistit;
        Thread.currentThread().interrupt();
        try {
            persistit.close(false);
        } finally {
            Thread.interrupted();
        }
        assertTrue("close(false) on an interrupted thread should close Persistit", persistit.isClosed());
    }

    /**
     * The fast-close wait restores the interrupt status it waits out. Called
     * directly on a free lock, since <code>doClose()</code> consumes the
     * restored status while it waits for its IO tasks to stop.
     */
    @Test
    public void fastCloseLockRestoresInterruptStatus() throws Exception {
        final Lock lock = new ReentrantReadWriteLock().writeLock();
        boolean locked = false;
        Thread.currentThread().interrupt();
        try {
            locked = _persistit.tryLockForFastClose(lock);
            assertTrue("a free write lock should be acquired", locked);
            assertTrue("the interrupt status should be restored", Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * On a closed Persistit, an interrupted thread gets the documented
     * closed-state result, not a {@link PersistitInterruptedException}.
     */
    @Test
    public void closedStateWinsOverInterrupt() throws Exception {
        final Persistit persistit = _persistit;
        persistit.close();
        Thread.currentThread().interrupt();
        try {
            assertNull("checkpoint() after close() should return null", persistit.checkpoint());
            try {
                persistit.copyBackPages();
                fail("copyBackPages() after close() should throw PersistitClosedException");
            } catch (final PersistitClosedException expected) {
                // expected
            }
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * A <code>checkpoint()</code> queued behind a running <code>close()</code>
     * stops waiting when it is interrupted.
     */
    @Test
    public void checkpointWaitingForCloseIsInterruptible() throws Exception {
        enableSequencer(false);
        addSchedules(CHECKPOINT_CLOSE_SCHEDULE);
        final Persistit persistit = _persistit;
        final AtomicReference<Throwable> waiterFailure = new AtomicReference<>();

        final Thread operator = new Thread(() -> {
            try {
                persistit.checkpoint();
            } catch (final Throwable t) {
                // not checked here
            }
        }, "CheckpointCloseRaceTest-operation");
        operator.setDaemon(true);
        final Thread closer = new Thread(() -> {
            try {
                persistit.close();
            } catch (final Throwable t) {
                // not checked here
            }
        }, "CheckpointCloseRaceTest-close");
        final Thread waiter = new Thread(() -> {
            try {
                persistit.checkpoint();
            } catch (final Throwable t) {
                waiterFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-waiter");
        waiter.setDaemon(true);

        operator.start();
        sequence(CHECKPOINT_CLOSE_B);
        try {
            closer.start();
            // close() is queued for the write lock behind the parked checkpoint.
            awaitWaiting(closer);
            waiter.start();
            // The second checkpoint() is queued behind close().
            awaitWaiting(waiter);
            waiter.interrupt();
            waiter.join(CLOSE_GRACE_MS);
            assertFalse("an interrupted checkpoint() should stop waiting for close()", waiter.isAlive());
            assertTrue("expected PersistitInterruptedException, got " + waiterFailure.get(),
                    waiterFailure.get() instanceof PersistitInterruptedException);
        } finally {
            sequence(CHECKPOINT_CLOSE_C);
            operator.join(JOIN_TIMEOUT_MS);
            closer.join(JOIN_TIMEOUT_MS);
            waiter.join(JOIN_TIMEOUT_MS);
        }
        assertFalse("close() did not finish", closer.isAlive());
    }

    /**
     * While shutdown is suspended, management tools keep Persistit open after
     * the application called <code>close()</code>; a checkpoint, e.g. from a
     * backup task, must still run in that window.
     */
    @Test
    public void checkpointRunsWhileShutdownSuspended() throws Exception {
        final Persistit persistit = _persistit;
        final AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        final AtomicReference<Object> checkpoint = new AtomicReference<>();
        final AtomicReference<Throwable> checkpointFailure = new AtomicReference<>();

        persistit.setShutdownSuspended(true);
        final Thread closer = new Thread(() -> {
            try {
                persistit.close();
            } catch (final Throwable t) {
                closeFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-close");
        final Thread operator = new Thread(() -> {
            try {
                checkpoint.set(persistit.checkpoint());
            } catch (final Throwable t) {
                checkpointFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-operation");
        try {
            closer.start();
            // Wait until close() is waiting for the suspension to be lifted.
            final long expires = System.currentTimeMillis() + JOIN_TIMEOUT_MS;
            while (closer.getState() != Thread.State.TIMED_WAITING && System.currentTimeMillis() < expires) {
                Thread.sleep(10);
            }
            assertTrue("close() should wait while shutdown is suspended", closer.isAlive());
            operator.start();
            operator.join(JOIN_TIMEOUT_MS);
            assertFalse("checkpoint() should not wait for a suspended close()", operator.isAlive());
            assertNull("checkpoint() failed: " + checkpointFailure.get(), checkpointFailure.get());
            assertNotNull("checkpoint() should run while shutdown is suspended", checkpoint.get());
            assertFalse("Persistit should stay open while shutdown is suspended", persistit.isClosed());
        } finally {
            persistit.setShutdownSuspended(false);
            closer.join(JOIN_TIMEOUT_MS);
            operator.join(JOIN_TIMEOUT_MS);
        }
        assertFalse("close() did not finish", closer.isAlive());
        assertNull("close() failed: " + closeFailure.get(), closeFailure.get());
        assertTrue("Persistit should be closed", persistit.isClosed());
    }

    private static void awaitWaiting(final Thread thread) throws InterruptedException {
        final long expires = System.currentTimeMillis() + JOIN_TIMEOUT_MS;
        while (thread.getState() != Thread.State.WAITING && System.currentTimeMillis() < expires) {
            Thread.sleep(10);
        }
        assertTrue(thread.getName() + " should be waiting for the lock", thread.getState() == Thread.State.WAITING);
    }

    /**
     * Runs <code>operation</code> in its own thread, holds it at the sequencer
     * location just after its closed/initialized check, closes Persistit from
     * another thread meanwhile, then lets the operation continue.
     */
    private void runConcurrentWithClose(final String name, final Operation operation, final int paused,
            final int resume) throws Exception {
        final Persistit persistit = _persistit;
        final AtomicReference<Throwable> operationFailure = new AtomicReference<>();
        final AtomicReference<Throwable> closeFailure = new AtomicReference<>();

        final Thread operator = new Thread(() -> {
            try {
                operation.run();
            } catch (final Throwable t) {
                operationFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-operation");
        final Thread closer = new Thread(() -> {
            try {
                persistit.close();
            } catch (final Throwable t) {
                closeFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-close");

        operator.start();
        // Returns once the operation is parked just after its
        // closed/initialized check.
        sequence(paused);
        closer.start();
        closer.join(CLOSE_GRACE_MS);
        // Checked only after the operation is released, so that a failure
        // does not leave it parked.
        final boolean closeHeldOff = closer.isAlive();
        final boolean closedUnderOperation = persistit.isClosed();
        // Let the operation continue.
        sequence(resume);

        operator.join(JOIN_TIMEOUT_MS);
        closer.join(JOIN_TIMEOUT_MS);
        assertFalse(name + " did not finish", operator.isAlive());
        assertFalse("close() did not finish", closer.isAlive());
        assertTrue("close() should wait for " + name, closeHeldOff);
        assertFalse("close() should not close Persistit under a running " + name, closedUnderOperation);
        assertNull(name + " failed under a concurrent close(): " + operationFailure.get(), operationFailure.get());
        assertNull("close() failed under a concurrent " + name + ": " + closeFailure.get(), closeFailure.get());
        assertTrue("Persistit should be closed", persistit.isClosed());
    }
}
