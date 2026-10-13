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

import java.util.concurrent.atomic.AtomicReference;

import static com.persistit.util.SequencerConstants.CHECKPOINT_CLOSE_B;
import static com.persistit.util.SequencerConstants.CHECKPOINT_CLOSE_C;
import static com.persistit.util.SequencerConstants.CHECKPOINT_CLOSE_SCHEDULE;
import static com.persistit.util.ThreadSequencer.addSchedules;
import static com.persistit.util.ThreadSequencer.enableSequencer;
import static com.persistit.util.ThreadSequencer.sequence;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A public {@link Persistit#checkpoint()} that has already passed its
 * closed/initialized check must not be torn down by a concurrent
 * {@link Persistit#close()}: before the fix the closing thread released the
 * configuration, volumes and journal underneath the checkpoint, which then died
 * with a raw <code>NullPointerException</code> in
 * <code>Persistit.getSpecialVolume</code> (seen as an uncaught exception from
 * the background checkpoint thread of {@link Bug1064565Test}).
 */
public class CheckpointCloseRaceTest extends PersistitUnitTestCase {

    /**
     * How long the closing thread is given to run while the checkpoint is held
     * inside its critical section. Without the fix close() completes well within
     * this time; with the fix it stays blocked until the checkpoint finishes.
     */
    private final static long CLOSE_GRACE_MS = 2_000;

    private final static long JOIN_TIMEOUT_MS = 60_000;

    @Test
    public void checkpointConcurrentWithClose() throws Exception {
        enableSequencer(false);
        addSchedules(CHECKPOINT_CLOSE_SCHEDULE);

        final Persistit persistit = _persistit;
        final AtomicReference<Throwable> checkpointFailure = new AtomicReference<>();
        final AtomicReference<Throwable> closeFailure = new AtomicReference<>();

        final Thread checkpointer = new Thread(() -> {
            try {
                persistit.checkpoint();
            } catch (final Throwable t) {
                checkpointFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-checkpoint");
        final Thread closer = new Thread(() -> {
            try {
                persistit.close();
            } catch (final Throwable t) {
                closeFailure.set(t);
            }
        }, "CheckpointCloseRaceTest-close");

        checkpointer.start();
        // Returns once the checkpointer is parked at CHECKPOINT_CLOSE_A, i.e.
        // after checkpoint() has passed its closed/initialized check.
        sequence(CHECKPOINT_CLOSE_B);
        closer.start();
        closer.join(CLOSE_GRACE_MS);
        // Let the checkpoint continue.
        sequence(CHECKPOINT_CLOSE_C);

        checkpointer.join(JOIN_TIMEOUT_MS);
        closer.join(JOIN_TIMEOUT_MS);
        assertFalse("checkpoint() did not finish", checkpointer.isAlive());
        assertFalse("close() did not finish", closer.isAlive());
        assertNull("checkpoint() failed under a concurrent close(): " + checkpointFailure.get(),
                checkpointFailure.get());
        assertNull("close() failed under a concurrent checkpoint(): " + closeFailure.get(), closeFailure.get());
        assertTrue("Persistit should be closed", persistit.isClosed());
    }
}
