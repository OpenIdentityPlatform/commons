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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.events.handlers.writers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.util.Files.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;
import org.forgerock.audit.events.handlers.writers.RotatableWriter.RolloverLifecycleHook;
import org.forgerock.audit.retention.TimestampFilenameFilter;
import org.forgerock.audit.rotation.FixedTimeRotationPolicy;
import org.forgerock.audit.rotation.RotationContext;
import org.forgerock.audit.rotation.RotationHooks;
import org.forgerock.audit.rotation.RotationPolicy;
import org.forgerock.audit.rotation.SizeBasedRotationPolicy;
import org.forgerock.audit.rotation.TimeLimitRotationPolicy;
import org.forgerock.util.time.Duration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RotatableWriterTest {

    private static final String ONE_SECOND = "1 second";
    private static final int MAX_BYTES_TO_WRITE = 100;
    private static final String ROTATION_FILE_SUFFIX = "-yyyy.MM.dd-HH.mm.ss.SSS.gz";

    private RotatableWriter rotatableWriter;

    @AfterMethod
    protected void tearDown() throws IOException {
        if (rotatableWriter != null) {
            rotatableWriter.close(); // ensure that ScheduledExecutorService gets shutdown
        }
    }

    @Test
    public void testInitializesBytesWrittenAndLastRotationTimeFromFileMetaData() throws Exception {
        // given
        final File file = getTempFile();
        Files.write(file.toPath(), new byte[]{1});
        final long oneSecondAgo = setLastModifiedToOneSecondAgo(file);
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();

        // when
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // then
        assertThat(rotatableWriter.getBytesWritten()).isEqualTo(1L);
        assertThat(rotatableWriter.getLastRotationTime().getMillis()).isEqualTo(oneSecondAgo);
    }

    @Test
    public void testBytesWritten() throws Exception {
        // given
        final File file = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // when
        writeThenFlushBytes(rotatableWriter, MAX_BYTES_TO_WRITE);

        // then
        assertThat(rotatableWriter.getBytesWritten()).isEqualTo(MAX_BYTES_TO_WRITE);
    }

    @Test
    public void testCreatesNoRotationPoliciesForDefaultConfiguration() throws IOException {
        // given
        final File file = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();

        // when
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.isEmpty()).isTrue();
    }

    @Test
    public void testCreatesSizeBasedRotationPolicyIfMaxFileSizeConfigured() throws IOException {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setMaxFileSize(100);

        // when
        rotatableWriter = new RotatableWriter(initialFile, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.size()).isEqualTo(1);
        assertThat(((SizeBasedRotationPolicy) rotationPolicies.get(0)).getMaxFileSizeInBytes()).isEqualTo(100);
    }

    @Test
    public void testSkipsCreationOfSizeBasedRotationPolicyIfConfiguredMaxFileSizeLessThanOrEqualToZero()
            throws IOException {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setMaxFileSize(0);

        // when
        rotatableWriter = new RotatableWriter(initialFile, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.isEmpty()).isTrue();
    }

    @Test
    public void testCreatesFixedTimeRotationPolicyIfRotationTimesConfigured() throws IOException {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationTimes(asList("0 seconds", "12 hours"));

        // when
        rotatableWriter = new RotatableWriter(initialFile, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.size()).isEqualTo(1);
        List<Duration> dailyRotationTimes = ((FixedTimeRotationPolicy) rotationPolicies.get(0)).getDailyRotationTimes();
        assertThat(dailyRotationTimes.get(0).to(SECONDS)).isEqualTo(0);
        assertThat(dailyRotationTimes.get(1).to(TimeUnit.HOURS)).isEqualTo(12);
    }

    @Test
    public void testSkipsCreationOfFixedTimeRotationPolicyIfConfiguredRotationTimeIsUnlimitedOrInvalid()
            throws IOException {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationTimes(asList("unlimited", "winter"));

        // when
        rotatableWriter = new RotatableWriter(initialFile, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.isEmpty()).isTrue();
    }

    @Test
    public void testCreatesTimeLimitRotationPolicyIfRotationIntervalConfigured() throws IOException {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationInterval("1 hour");

        // when
        rotatableWriter = new RotatableWriter(initialFile, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.size()).isEqualTo(1);
        Duration rotationInterval = ((TimeLimitRotationPolicy) rotationPolicies.get(0)).getRotationInterval();
        assertThat(rotationInterval.to(TimeUnit.HOURS)).isEqualTo(1);
    }

    @DataProvider
    private Object[][] ignoredRotationIntervals() {
        return new Object[][] {
                {"unlimited"},
                {"zero"},
                {"winter"}
        };
    }

    @Test(dataProvider = "ignoredRotationIntervals")
    public void testSkipsCreationOfTimeLimitRotationPolicyIfConfiguredRotationIntervalIsZeroOrUnlimitedOrInvalid(
            String rotationInterval) throws IOException {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationInterval(rotationInterval);

        // when
        rotatableWriter = new RotatableWriter(initialFile, configuration, true);

        // then
        List<RotationPolicy> rotationPolicies = rotatableWriter.getRotationPolicies();
        assertThat(rotationPolicies.isEmpty()).isTrue();
    }

    @Test
    public void testRotationForSizeBasedRotationPolicy() throws Exception {
        // given
        final File file = getTempFile();
        final String prefix = "testRotationForSizeBasedRotationPolicy";
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.setRotationRetentionCheckInterval("1 hour"); // ensure asynchronous check is inactive
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFilePrefix(prefix);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        configuration.getFileRotation().setMaxFileSize(MAX_BYTES_TO_WRITE);
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // when
        writeThenFlushBytes(rotatableWriter, MAX_BYTES_TO_WRITE);
        writeThenFlushBytes(rotatableWriter, 1); // need to let the policies see the flushed bytes

        // then
        // the extra byte may or may not have been written due to buffer
        assertThat(rotatableWriter.getBytesWritten()).isBetween(0L, 1L);
    }

    @Test
    public void testRotationForTimeLimitRotationPolicy() throws Exception {
        // given
        final File file = getTempFile();
        final String prefix = "testRotationForTimeLimitRotationPolicy";
        setLastModifiedToOneSecondAgo(file);
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFilePrefix(prefix);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        configuration.getFileRotation().setRotationInterval(ONE_SECOND);
        configuration.getFileRetention().setMaxNumberOfHistoryFiles(1);
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // when
        writeThenFlushBytes(rotatableWriter, 1);

        // then
        assertRetainedHistoricalFiles(file, prefix, 1);
        assertThat(rotatableWriter.getBytesWritten()).isEqualTo(0L);
    }

    // TODO: testRotationForFixedTimeRotationPolicy

    @Test
    public void testForceRotationRunsTheRolloverLifecycleHook() throws Exception {
        // given
        final File file = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        final List<String> calls = new ArrayList<>();
        rotatableWriter = new RotatableWriter(file, configuration, true, new RolloverLifecycleHook() {
            @Override
            public void beforeRollingOver() {
                calls.add("beforeRollingOver");
            }

            @Override
            public void afterRollingOver() {
                calls.add("afterRollingOver");
            }
        });
        rotatableWriter.registerRotationHooks(new RotationHooks() {
            @Override
            public void preRotationAction(RotationContext context) {
                calls.add("preRotationAction");
            }

            @Override
            public void postRotationAction(RotationContext context) {
                calls.add("postRotationAction");
            }
        });

        // when
        final boolean rotated = rotatableWriter.forceRotation();

        // then
        assertThat(rotated).isTrue();
        assertThat(calls).containsExactly(
                "beforeRollingOver", "preRotationAction", "postRotationAction", "afterRollingOver");
    }

    @Test(timeOut = 60_000)
    public void testFlushWaitsForTheRotationToComplete() throws Exception {
        // given
        final File file = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        rotatableWriter = new RotatableWriter(file, configuration, true);
        final FutureTask<Void> flush = new FutureTask<>(() -> {
            rotatableWriter.flush();
            return null;
        });
        final Thread flusher = new Thread(flush, "flusher");
        flusher.setDaemon(true);
        final List<Boolean> flushWaitedForTheRotation = new ArrayList<>();
        rotatableWriter.registerRotationHooks(new RotationHooks() {
            @Override
            public void preRotationAction(RotationContext context) throws IOException {
                // the rotation closes and replaces the writer after this hook
                flusher.start();
                flushWaitedForTheRotation.add(awaitWaitingForLockOf(flusher, Thread.currentThread(), flush));
            }

            @Override
            public void postRotationAction(RotationContext context) {
                // nothing to do
            }
        });

        // when
        rotatableWriter.forceRotation();

        // then
        flush.get(10, SECONDS);
        assertThat(flushWaitedForTheRotation).containsExactly(true);
    }

    /**
     * Waits until the thread waits for a lock that the owner holds, or until the task of the thread is done.
     *
     * @return {@code true} if the thread waits for a lock that the owner holds
     */
    private static boolean awaitWaitingForLockOf(final Thread thread, final Thread owner, final FutureTask<?> task) {
        final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        final long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (!task.isDone() && System.nanoTime() < deadline) {
            final ThreadInfo threadInfo = threads.getThreadInfo(thread.getId());
            if (threadInfo != null && threadInfo.getLockOwnerId() == owner.getId()) {
                return true;
            }
            Thread.yield();
        }
        return false;
    }

    @Test
    public void testCanForceRotation() throws Exception {
        // given
        final File file = getTempFile();
        final String prefix = "testAutomaticallyEvaluatesPolicesPeriodicallyIfRotationIntervalSpecified";
        setLastModifiedToOneSecondAgo(file);
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFilePrefix(prefix);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        configuration.getFileRetention().setMaxNumberOfHistoryFiles(1);
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // when
        rotatableWriter.forceRotation();

        // then
        assertRetainedHistoricalFiles(file, prefix, 1);
        assertThat(rotatableWriter.getBytesWritten()).isEqualTo(0L);
    }

    @Test
    public void testCompressesRotatedFileInDirectoryWhoseNameContainsGzipSuffix() throws Exception {
        // given
        final File directory = Files.createTempDirectory("rotation.gz").toFile();
        directory.deleteOnExit();
        final File file = new File(directory, "audit.csv");
        file.deleteOnExit();
        final String prefix = "testCompressesRotatedFileInDirectoryWhoseNameContainsGzipSuffix";
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.setRotationRetentionCheckInterval("1 hour"); // ensure asynchronous check is inactive
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFilePrefix(prefix);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        rotatableWriter = new RotatableWriter(file, configuration, true);
        rotatableWriter.write("event");

        // when
        final boolean rotated = rotatableWriter.forceRotation();

        // then
        assertThat(rotated).isTrue();
        final File[] rotatedFiles = directory.listFiles((dir, name) -> name.startsWith(prefix));
        cleanupFilesWhenDone(rotatedFiles);
        assertThat(rotatedFiles).hasSize(1);
        assertThat(rotatedFiles[0].getName()).endsWith(".gz");
        assertThat(gunzip(rotatedFiles[0])).isEqualTo("event");
    }

    @Test
    public void testFailsEveryWriteAndFlushAfterAFailure() throws Exception {
        // given
        final File file = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        rotatableWriter = new RotatableWriter(file, configuration, true, FailingOnceOutputStream::open);
        // two publishers write their events, and the flush of the first one fails
        rotatableWriter.write("event1");
        rotatableWriter.write("event2");
        assertThatThrownBy(rotatableWriter::flush).hasMessage("No space left on device");

        // then the second publisher learns that its event is lost too, and so does any later one
        assertThatThrownBy(rotatableWriter::flush).hasRootCauseMessage("No space left on device");
        assertThatThrownBy(() -> rotatableWriter.write("event3")).hasRootCauseMessage("No space left on device");
        assertThat(file.length()).isZero();
    }

    @Test
    public void testDoesNotWriteTheTextOfAFailedWriteWhenClosed() throws Exception {
        // given
        final File file = getTempFile();
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        rotatableWriter = new RotatableWriter(file, configuration, true, FailingOnceOutputStream::open);
        // longer than the buffers, so that the write itself reaches the output stream
        final String event = new String(new char[100_000]).replace('\0', 'x');
        assertThatThrownBy(() -> rotatableWriter.write(event)).isInstanceOf(IOException.class);

        // when
        rotatableWriter.close();

        // then
        assertThat(file.length()).isZero();
    }

    @Test
    public void testDoesNotRotateAfterAFailure() throws Exception {
        // given
        final File file = getTempFile();
        final String prefix = "testDoesNotRotateAfterAFailure";
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.setRotationRetentionCheckInterval("1 hour"); // ensure asynchronous check is inactive
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFilePrefix(prefix);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        rotatableWriter = new RotatableWriter(file, configuration, true, FailingOnceOutputStream::open);
        rotatableWriter.write("event");
        assertThatThrownBy(rotatableWriter::flush).isInstanceOf(IOException.class);

        // when the rotation would write what the buffers hold into the archive, and go on with a writer that has not
        // failed, so that the flush of an event that the failure dropped would succeed
        assertThatThrownBy(rotatableWriter::forceRotation).hasRootCauseMessage("No space left on device");

        // then
        assertThat(file.getParentFile().listFiles((dir, name) -> name.startsWith(prefix + file.getName()))).isEmpty();
    }

    @Test
    public void testAutomaticallyEvaluatesPolicesPeriodicallyIfRotationIntervalSpecified() throws Exception {
        // given
        final File file = getTempFile();
        final String prefix = "testAutomaticallyEvaluatesPolicesPeriodicallyIfRotationIntervalSpecified";
        setLastModifiedToOneSecondAgo(file);
        final FileBasedEventHandlerConfiguration configuration = new DefaultFileBasedAuditEventHandlerConfiguration();
        configuration.setRotationRetentionCheckInterval("100 ms");
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFilePrefix(prefix);
        configuration.getFileRotation().setRotationFileSuffix(ROTATION_FILE_SUFFIX);
        configuration.getFileRotation().setRotationInterval("1 second");
        configuration.getFileRetention().setMaxNumberOfHistoryFiles(1);
        rotatableWriter = new RotatableWriter(file, configuration, true);

        // when
        Thread.sleep(3000);

        // then
        assertRetainedHistoricalFiles(file, prefix, 1);
        assertThat(rotatableWriter.getBytesWritten()).isEqualTo(0L);
    }

    private void writeThenFlushBytes(final RotatableWriter writer, final int bytesToWrite) throws IOException {
        writer.write(new String(new byte[bytesToWrite]));
        writer.flush();
    }

    private static String gunzip(final File file) throws IOException {
        try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
            return new String(in.readAllBytes(), UTF_8);
        }
    }

    private File getTempFile() throws IOException {
        File file = File.createTempFile(getClass().getCanonicalName(), ".txt", temporaryFolder());
        file.deleteOnExit();
        return file;
    }

    private long setLastModifiedToOneSecondAgo(final File file) {
        final DateTime currentTime = new DateTime();
        // round down to nearest second in case filesystem doesn't support millisecond accuracy
        final long currentTimeMillisTruncatedToSecond = currentTime.getMillis() - currentTime.getMillisOfSecond();
        final long oneSecondAgo = currentTimeMillisTruncatedToSecond - 1000;
        boolean modified = file.setLastModified(oneSecondAgo);
        assertThat(modified).as("can update file timestamps from tests").isTrue();
        return oneSecondAgo;
    }

    private void assertRetainedHistoricalFiles(File file, String prefix, int expectedNumber) throws Exception {
        Set<File> retainedHistoricalFiles = getAllHistoricalFiles(file, prefix, expectedNumber, new HashSet<File>());
        assertThat(retainedHistoricalFiles).isNotEmpty().hasSize(expectedNumber);
    }

    private Set<File> getAllHistoricalFiles(File file, String prefix, int expectedNumber, Set<File> existingFiles)
            throws InterruptedException {
        // try to load the expected number of historical files, give up if unsuccessful after 1 second
        Set<File> allHistoricalFiles = new HashSet<>(existingFiles);
        for (int iteration = 0; iteration < 20; iteration++) {
            allHistoricalFiles.addAll(getRetainedHistoricalFiles(file, prefix));
            if (allHistoricalFiles.size() == expectedNumber) {
                return allHistoricalFiles;
            }
            Thread.sleep(50);
        }
        return allHistoricalFiles;
    }

    private Set<File> getRetainedHistoricalFiles(File file, String prefix) {
        final TimestampFilenameFilter timestampFilenameFilter =
                new TimestampFilenameFilter(file, prefix, DateTimeFormat.forPattern(ROTATION_FILE_SUFFIX.replace(".gz","")));
        assertThat(file.getParentFile()).isDirectory();
        final File[] historicalFiles = file.getParentFile().listFiles(timestampFilenameFilter);
        cleanupFilesWhenDone(historicalFiles);
        return new HashSet<>(Arrays.asList(historicalFiles));
    }

    private void cleanupFilesWhenDone(File[] files) {
        if (files != null) {
            for (File file: files) {
                file.deleteOnExit();
            }
        }
    }

    /** Writes to a file, and fails its first write, as a full disk does. */
    private static final class FailingOnceOutputStream extends FilterOutputStream {
        private boolean failed;

        private FailingOnceOutputStream(final OutputStream out) {
            super(out);
        }

        private static OutputStream open(final File file, final boolean append) throws IOException {
            return new FailingOnceOutputStream(new FileOutputStream(file, append));
        }

        @Override
        public void write(final int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (!failed) {
                failed = true;
                throw new IOException("No space left on device");
            }
            out.write(b, off, len);
        }
    }

    private static class DefaultFileBasedAuditEventHandlerConfiguration extends FileBasedEventHandlerConfiguration {
        @Override
        public boolean isUsableForQueries() {
            return true;
        }
    }

}
