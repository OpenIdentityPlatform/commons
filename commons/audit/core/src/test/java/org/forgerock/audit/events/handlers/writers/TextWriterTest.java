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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class TextWriterTest {

    @Test
    public void testStreamWritesTheText() throws Exception {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final TextWriter.Stream writer = new TextWriter.Stream(outputStream);

        writer.write("event");
        writer.flush();

        assertThat(outputStream.toString()).isEqualTo("event");
        assertThat(writer.getBytesWritten()).isEqualTo(5L);
    }

    @Test
    public void testStreamThrowsTheFailureOfTheOutputStream() throws Exception {
        final TextWriter.Stream writer = new TextWriter.Stream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // as on a full disk
                throw new IOException("No space left on device");
            }
        });
        writer.write("event");

        assertThatThrownBy(writer::flush)
                .isInstanceOf(IOException.class)
                .hasMessage("No space left on device");
    }

    @Test
    public void testStreamThrowsWhenWritingAfterShutdown() throws Exception {
        final TextWriter.Stream writer = new TextWriter.Stream(new ByteArrayOutputStream());
        writer.shutdown();

        assertThatThrownBy(() -> writer.write("event")).isInstanceOf(IOException.class);
    }

    @Test
    public void testStreamKeepsFailingAfterAFailure() throws Exception {
        final FailingOnceOutputStream outputStream = new FailingOnceOutputStream();
        final TextWriter.Stream writer = new TextWriter.Stream(outputStream);
        // two publishers write their events, and the flush of the first one fails
        writer.write("event1");
        writer.write("event2");
        assertThatThrownBy(writer::flush).isInstanceOf(IOException.class);

        // the second publisher learns that its event is lost too, and so does any later one
        assertThatThrownBy(writer::flush).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> writer.write("event3")).isInstanceOf(IOException.class);
        assertThat(outputStream.written.size()).isZero();
    }

    @Test
    public void testStreamGivesTheFirstFailureAsTheCauseOfTheNextOnes() throws Exception {
        final TextWriter.Stream writer = new TextWriter.Stream(new FailingOnceOutputStream());
        writer.write("event1");
        assertThatThrownBy(writer::flush).hasMessage("No space left on device");

        // a caller that logs only the next failure, such as a flush in a finally block, still logs why
        assertThatThrownBy(writer::flush).hasRootCauseMessage("No space left on device");
    }

    @Test
    public void testStreamDropsTheTextOfAFailedWriteAtShutdown() throws Exception {
        final FailingOnceOutputStream outputStream = new FailingOnceOutputStream();
        final TextWriter.Stream writer = new TextWriter.Stream(outputStream);
        // longer than the buffers, so that the write itself reaches the output stream
        final String event = new String(new char[100_000]).replace('\0', 'x');
        assertThatThrownBy(() -> writer.write(event)).isInstanceOf(IOException.class);

        writer.shutdown();

        // the retry of the handler writes the event again, with a new writer: no part of it may reach this one
        assertThat(outputStream.written.size()).isZero();
    }

    @Test(timeOut = 10_000)
    public void testStreamFailsTheFlushThatWaitsForAFailingFlush() throws Exception {
        final FailingOnReleaseOutputStream outputStream = new FailingOnReleaseOutputStream();
        final TextWriter.Stream writer = new TextWriter.Stream(outputStream);
        // two publishers write their events, and flush them at the same time
        writer.write("event1");
        writer.write("event2");
        final FutureTask<Void> firstFlush = newFlush(writer);
        final FutureTask<Void> secondFlush = newFlush(writer);
        new Thread(firstFlush).start();
        outputStream.entered.await();
        final Thread secondPublisher = new Thread(secondFlush);
        secondPublisher.start();
        // the second flush waits for the first one, which is writing to the stream
        while (secondPublisher.getState() != Thread.State.BLOCKED
                && secondPublisher.getState() != Thread.State.WAITING) {
            Thread.yield();
        }
        outputStream.release.countDown();

        assertThatThrownBy(firstFlush::get).hasRootCauseMessage("No space left on device");
        // the event of the second publisher is lost too: its flush fails, instead of returning as if it was written
        assertThatThrownBy(secondFlush::get).hasRootCauseMessage("No space left on device");
        assertThat(outputStream.written.size()).isZero();
    }

    @Test
    public void testStreamClosesTheOutputStreamWhenTheShutdownFails() throws Exception {
        final FailingOnceOutputStream outputStream = new FailingOnceOutputStream();
        final TextWriter.Stream writer = new TextWriter.Stream(outputStream);
        // the text is still in the buffers, so the close writes it, and fails
        writer.write("event");

        writer.shutdown();

        // on JDK 11, the close of the buffers leaves the stream open when its write fails, and the file with it
        assertThat(outputStream.closed).isTrue();
    }

    private static FutureTask<Void> newFlush(final TextWriter writer) {
        return new FutureTask<>(() -> {
            writer.flush();
            return null;
        });
    }

    /** Fails its first write once the test releases it, as a full disk does, and keeps what is written afterwards. */
    private static final class FailingOnReleaseOutputStream extends OutputStream {
        private final ByteArrayOutputStream written = new ByteArrayOutputStream();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private boolean failed;

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (!failed) {
                failed = true;
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new IOException("No space left on device");
            }
            written.write(b, off, len);
        }
    }

    /** Fails its first write, as a full disk does, and keeps what is written to it afterwards. */
    private static final class FailingOnceOutputStream extends OutputStream {
        private final ByteArrayOutputStream written = new ByteArrayOutputStream();
        private boolean failed;
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (!failed) {
                failed = true;
                throw new IOException("No space left on device");
            }
            written.write(b, off, len);
        }
    }
}
