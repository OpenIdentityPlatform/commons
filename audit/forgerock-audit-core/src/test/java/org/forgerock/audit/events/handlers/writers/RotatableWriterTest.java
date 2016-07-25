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
 */
package org.forgerock.audit.events.handlers.writers;

import static java.util.Arrays.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.util.Files.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;
import org.forgerock.audit.retention.TimestampFilenameFilter;
import org.forgerock.audit.rotation.FixedTimeRotationPolicy;
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
    private static final String ROTATION_FILE_SUFFIX = "-yyyy.MM.dd-HH.mm.ss.SSS";

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
                new TimestampFilenameFilter(file, prefix, DateTimeFormat.forPattern(ROTATION_FILE_SUFFIX));
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

    private static class DefaultFileBasedAuditEventHandlerConfiguration extends FileBasedEventHandlerConfiguration {
        @Override
        public boolean isUsableForQueries() {
            return true;
        }
    }

}
