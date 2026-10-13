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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.persistit.exception.PersistitClosedException;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        enableSequencer(false);
        addSchedules(COPYBACK_CLOSE_SCHEDULE);
        // copyBackPages() reaches COPYBACK_CLOSE_A on every pass of its loop;
        // only the first pass is sequenced against close().
        final AtomicBoolean once = new AtomicBoolean(true);
        setCondition(COPYBACK_CLOSE_A, new Condition() {
            @Override
            public boolean enabled() {
                return once.getAndSet(false);
            }
        });
        final Persistit persistit = _persistit;
        try {
            runConcurrentWithClose("copyBackPages()", () -> {
                try {
                    persistit.copyBackPages();
                } catch (final PersistitClosedException e) {
                    // Documented outcome when a later pass sees Persistit closed
                }
            }, COPYBACK_CLOSE_B, COPYBACK_CLOSE_C);
        } finally {
            setCondition(COPYBACK_CLOSE_A, new Condition());
        }
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
        // Let the operation continue.
        sequence(resume);

        operator.join(JOIN_TIMEOUT_MS);
        closer.join(JOIN_TIMEOUT_MS);
        assertFalse(name + " did not finish", operator.isAlive());
        assertFalse("close() did not finish", closer.isAlive());
        assertNull(name + " failed under a concurrent close(): " + operationFailure.get(), operationFailure.get());
        assertNull("close() failed under a concurrent " + name + ": " + closeFailure.get(), closeFailure.get());
        assertTrue("Persistit should be closed", persistit.isClosed());
    }
}
