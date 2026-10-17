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
package org.forgerock.audit.events.handlers.writers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AsynchronousTextWriterTest {

    @Test
    public void testFailsTheNextWriteOnceTheWrappedWriterFailed() throws Exception {
        final AsynchronousTextWriter writer = new AsynchronousTextWriter("test", false, new FailingTextWriter(true));
        try {
            // the records are written by the thread of the writer, so the failure shows at a later write
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                try {
                    writer.write("event");
                } catch (IOException e) {
                    // the handler retries the event with a new writer
                    assertThat(e).hasRootCauseMessage("No space left on device");
                    return;
                }
                Thread.sleep(10);
            }
            fail("No write failed after the wrapped writer failed");
        } finally {
            writer.shutdown();
        }
    }

    @Test
    public void testFlushThrowsTheFailureOfTheWrappedWriter() throws Exception {
        final AsynchronousTextWriter writer = new AsynchronousTextWriter("test", false, new FailingTextWriter(false));
        try {
            // the handler flushes the writer after each event with autoFlush
            assertThatThrownBy(writer::flush).hasMessage("No space left on device");

            assertThatThrownBy(() -> writer.write("event")).hasRootCauseMessage("No space left on device");
        } finally {
            writer.shutdown();
        }
    }

    /** Fails as a full disk does: every write, or every flush. */
    private static final class FailingTextWriter implements TextWriter {
        private final boolean failWrites;

        private FailingTextWriter(final boolean failWrites) {
            this.failWrites = failWrites;
        }

        @Override
        public void write(final String text) throws IOException {
            if (failWrites) {
                throw new IOException("No space left on device");
            }
        }

        @Override
        public void flush() throws IOException {
            if (!failWrites) {
                throw new IOException("No space left on device");
            }
        }

        @Override
        public void shutdown() {
            // nothing to release
        }

        @Override
        public long getBytesWritten() {
            return 0;
        }
    }
}
