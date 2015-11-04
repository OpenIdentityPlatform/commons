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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.audit.events.handlers.writers;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Files.newFile;
import static org.assertj.core.util.Files.temporaryFolderPath;
import static org.assertj.core.util.Strings.concat;

import java.io.File;
import java.io.IOException;

import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;
import org.forgerock.audit.retention.TimestampFilenameFilter;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.Test;

public class RotatableWriterTest {

    private static final String TIME_STAMP_FORMAT = "-MM.dd.yy-kk.mm.ss.SSS";
    private static final String PREFIX = "Prefix-";
    private static final String ONE_SECOND = "1 second";
    private static final int MAX_NUMBER_OF_HISTORY_FILES = 3;
    private static final int MAX_BYTES_TO_WRITE = 100;
    private static final int MAX_BYTES_TO_WRITE_DISABLED = 0;
    private static final long ONE_SECOND_AS_LONG = 2 * 1000;
    private static final String DISABLED = "disabled";

    @Test
    public void testCreation() throws Exception {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration =
                createFileBasedAuditEventHandlerConfiguration(
                        DISABLED,
                        MAX_BYTES_TO_WRITE,
                        0,
                        0);
        final RotatableWriter rotatableFile = new RotatableWriter(initialFile, configuration, true);

        // when

        // then
        assertThat(rotatableFile.getBytesWritten()).isEqualTo(0L);
    }

    @Test
    public void testBytesWritten() throws Exception {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration =
                createFileBasedAuditEventHandlerConfiguration(
                        DISABLED,
                        MAX_BYTES_TO_WRITE,
                        0,
                        0);
        final RotatableWriter rotatableFile = new RotatableWriter(initialFile, configuration, true);

        // when
        writeBytes(rotatableFile, MAX_BYTES_TO_WRITE);

        // then
        assertThat(rotatableFile.getBytesWritten()).isEqualTo(MAX_BYTES_TO_WRITE);
    }

    @Test
    public void testRotationWhenFileIsTooLarge() throws Exception {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration =
                createFileBasedAuditEventHandlerConfiguration(
                        DISABLED,
                        MAX_BYTES_TO_WRITE,
                        1,
                        0);
        final RotatableWriter rotatableFile = new RotatableWriter(initialFile, configuration, true);
        final TimestampFilenameFilter timestampFilenameFilter =
                new TimestampFilenameFilter(initialFile, PREFIX, DateTimeFormat.forPattern(TIME_STAMP_FORMAT));

        // when
        writeBytes(rotatableFile, MAX_BYTES_TO_WRITE+1);
        rotatableFile.rotateIfNeeded();

        // then
        assertThat(rotatableFile.getBytesWritten()).isEqualTo(0L);
        assertThat(initialFile.getParentFile()).isDirectory();
        final File[] historicalFiles = initialFile.getParentFile().listFiles(timestampFilenameFilter);
        cleanupFilesWhenDone(historicalFiles);
        assertThat(historicalFiles).isNotEmpty().hasSize(1);
    }

    @Test
    public void testRotationWhenFileIsTooOld() throws Exception {
        // given
        final int maxNumberOfHistoricalFiles = 1;
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration =
                createFileBasedAuditEventHandlerConfiguration(
                        ONE_SECOND,
                        MAX_BYTES_TO_WRITE_DISABLED,
                        maxNumberOfHistoricalFiles,
                        0);
        final RotatableWriter rotatableFile = new RotatableWriter(initialFile, configuration, true);
        final TimestampFilenameFilter timestampFilenameFilter =
                new TimestampFilenameFilter(initialFile, PREFIX, DateTimeFormat.forPattern(TIME_STAMP_FORMAT));

        // when
        writeBytes(rotatableFile, MAX_BYTES_TO_WRITE);

        Thread.sleep(ONE_SECOND_AS_LONG);

        boolean success = false;
        // loop to test the historical files eventually equal 1. Make sure loop only iterates
        // for a maximum of 1 second.
        for(int iteration = 0; iteration < 20; iteration++) {
            final File[] historicalFiles = initialFile.getParentFile().listFiles(timestampFilenameFilter);
            if (historicalFiles.length == maxNumberOfHistoricalFiles) {
                success = true;
                break;
            }
            // sleep 50 ms
            Thread.sleep(50);
        }

        // then
        assertThat(rotatableFile.getBytesWritten()).isEqualTo(0L);
        assertThat(initialFile.getParentFile()).isDirectory();
        final File[] historicalFiles = initialFile.getParentFile().listFiles(timestampFilenameFilter);
        cleanupFilesWhenDone(historicalFiles);
        assertThat(success).isTrue().as("Check the max number of historical files was %d", maxNumberOfHistoricalFiles);
    }

    @Test
    public void testRetentionWithMaxNumberOfFiles() throws Exception {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration =
                createFileBasedAuditEventHandlerConfiguration(
                        DISABLED,
                        MAX_BYTES_TO_WRITE,
                        MAX_NUMBER_OF_HISTORY_FILES,
                        0);
        final RotatableWriter rotatableFile = new RotatableWriter(initialFile, configuration, true);
        final TimestampFilenameFilter timestampFilenameFilter =
                new TimestampFilenameFilter(initialFile, PREFIX, DateTimeFormat.forPattern(TIME_STAMP_FORMAT));

        // when
        for (int i = 0; i < 4; i++) {
            // do 4 rotates of the log file.
            writeBytes(rotatableFile, MAX_BYTES_TO_WRITE + 1);
            rotatableFile.rotateIfNeeded();
        }

        // then
        assertThat(initialFile.getParentFile()).isDirectory();
        final File[] historicalFiles = initialFile.getParentFile().listFiles(timestampFilenameFilter);
        cleanupFilesWhenDone(historicalFiles);
        assertThat(historicalFiles).isNotEmpty().hasSize(MAX_NUMBER_OF_HISTORY_FILES);
    }

    @Test
    public void testRetentionWithMaxSizeOfFile() throws Exception {
        // given
        final File initialFile = getTempFile();
        final FileBasedEventHandlerConfiguration configuration =
                createFileBasedAuditEventHandlerConfiguration(
                        DISABLED,
                        MAX_BYTES_TO_WRITE,
                        0,
                        MAX_BYTES_TO_WRITE * MAX_NUMBER_OF_HISTORY_FILES);
        final RotatableWriter rotatableFile = new RotatableWriter(initialFile, configuration, true);
        final TimestampFilenameFilter timestampFilenameFilter =
                new TimestampFilenameFilter(initialFile, PREFIX, DateTimeFormat.forPattern(TIME_STAMP_FORMAT));

        // when
        for (int i = 0; i < 4; i++) {
            // do 4 rotates of the log file.
            writeBytes(rotatableFile, MAX_BYTES_TO_WRITE);
            rotatableFile.rotateIfNeeded();
        }

        // then
        assertThat(initialFile.getParentFile()).isDirectory();
        final File[] historicalFiles = initialFile.getParentFile().listFiles(timestampFilenameFilter);
        cleanupFilesWhenDone(historicalFiles);
        assertThat(historicalFiles).isNotEmpty().hasSize(MAX_NUMBER_OF_HISTORY_FILES);
    }

    private FileBasedEventHandlerConfiguration createFileBasedAuditEventHandlerConfiguration(
            final String rotationInterval, final long maxFileSize, final int maxNumberOfHistoryFiles,
            final long maxDiskSpaceToUse) {
        final FileBasedEventHandlerConfiguration configuration = new FileBasedEventHandlerConfiguration();
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFileSuffix(TIME_STAMP_FORMAT);
        configuration.getFileRotation().setRotationFilePrefix(PREFIX);
        configuration.getFileRotation().setMaxFileSize(maxFileSize);
        configuration.getFileRotation().setRotationInterval(rotationInterval);
        configuration.getFileRetention().setMaxDiskSpaceToUse(maxDiskSpaceToUse);
        configuration.getFileRetention().setMaxNumberOfHistoryFiles(maxNumberOfHistoryFiles);
        configuration.getFileRetention().setMinFreeSpaceRequired(0);
        return configuration;
    }

    private void writeBytes(final RotatableWriter writer, final int bytesToWrite) throws IOException {
            writer.write(new String(new byte[bytesToWrite]));
            writer.flush();
    }

    private File getTempFile() {
        // define file name using nanoTime instead of currentTimeMillis since the tests run so fast
        String tempFileName = concat(valueOf(System.nanoTime()), ".txt");
        File file = newFile(concat(temporaryFolderPath(), tempFileName));
        file.deleteOnExit();
        return file;
    }

    private void cleanupFilesWhenDone(File[] files) {
        if (files != null) {
            for (File file: files) {
                file.deleteOnExit();
            }
        }
    }

}
