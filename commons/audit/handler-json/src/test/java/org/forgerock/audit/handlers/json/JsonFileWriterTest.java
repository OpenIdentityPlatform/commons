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
package org.forgerock.audit.handlers.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JsonFileWriterTest {

    private static final String TOPIC = "access";
    private static final String LOG_FILE_NAME = TOPIC + '.' + JsonFileWriter.LOG_FILE_NAME_SUFFIX;
    private static final String ROTATION_FILE_SUFFIX = "-yyyy.MM.dd-HH.mm.ss";
    private static final DateTimeFormatter ROTATION_FILE_FORMAT = DateTimeFormat.forPattern(ROTATION_FILE_SUFFIX);
    private static final LocalDateTime HISTORY_START = new LocalDateTime(2020, 1, 1, 0, 0);
    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private Path logDirectory;
    private JsonFileWriter writer;

    @BeforeMethod
    public void setUp() throws IOException {
        logDirectory = Files.createTempDirectory("json-file-writer");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        if (writer != null) {
            writer.shutdown();
            writer = null;
        }
        try (Stream<Path> paths = Files.walk(logDirectory)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(path);
            }
        }
    }

    @Test
    public void shouldEnforceRetentionOnStartupAndAfterRotationOnly() throws Exception {
        createHistoryFiles(0, 3);
        // shutdown() waits for a started writer only, so start it right away
        writer = newWriter("1h");
        writer.startup();
        awaitHistoryFiles(1);

        createHistoryFiles(3, 2);
        awaitWriterRuns();
        assertThat(historyFiles()).hasSize(3);

        assertThat(writer.rotateFile(TOPIC)).isTrue();
        awaitHistoryFiles(1);
    }

    @Test
    public void shouldEnforceRetentionEveryCheckIntervalWithoutRotation() throws Exception {
        writer = newWriter("100 ms");
        writer.startup();
        awaitWriterRuns();

        createHistoryFiles(0, 3);
        awaitHistoryFiles(1);
    }

    @Test
    public void shouldCompressRotatedFileWhenRotationFileSuffixEndsWithGz() throws Exception {
        writer = newWriter("1h", ROTATION_FILE_SUFFIX + ".gz");
        writer.startup();
        final String id = writeEvent();

        assertThat(writer.rotateFile(TOPIC)).isTrue();

        assertThat(gunzip(awaitCompressedHistoryFile())).contains(id);
    }

    @Test
    public void shouldRotateBufferedEventsIntoArchive() throws Exception {
        writer = newWriter("1h", ROTATION_FILE_SUFFIX + ".gz");
        writer.startup();
        final String id = UUID.randomUUID().toString();

        // the writer flushes its file buffer on a run that finds the queue empty, so the event is normally still
        // buffered when the rotation is processed
        writer.put(TOPIC, json(object(field("_id", id))));
        assertThat(writer.rotateFile(TOPIC)).isTrue();

        assertThat(gunzip(awaitCompressedHistoryFile())).contains(id);
    }

    private JsonFileWriter newWriter(final String rotationRetentionCheckInterval) {
        return newWriter(rotationRetentionCheckInterval, ROTATION_FILE_SUFFIX);
    }

    private JsonFileWriter newWriter(final String rotationRetentionCheckInterval, final String rotationFileSuffix) {
        final JsonAuditEventHandlerConfiguration configuration = new JsonAuditEventHandlerConfiguration();
        configuration.setLogDirectory(logDirectory.toString());
        configuration.getBuffering().setWriteInterval("10 ms");
        configuration.getFileRotation().setRotationEnabled(true);
        configuration.getFileRotation().setRotationFileSuffix(rotationFileSuffix);
        configuration.getFileRetention().setMaxNumberOfHistoryFiles(1);
        configuration.setRotationRetentionCheckInterval(rotationRetentionCheckInterval);
        return new JsonFileWriter(singleton(TOPIC), configuration, true);
    }

    /** Creates rotated files as old as their names, so that the retention policy deletes them first. */
    private void createHistoryFiles(final int first, final int count) throws IOException {
        for (int i = first; i < first + count; i++) {
            final LocalDateTime rotationTime = HISTORY_START.plusMinutes(i);
            final File file = logDirectory.resolve(LOG_FILE_NAME + rotationTime.toString(ROTATION_FILE_FORMAT))
                    .toFile();
            assertThat(file.createNewFile()).isTrue();
            assertThat(file.setLastModified(rotationTime.toDate().getTime())).isTrue();
        }
    }

    private List<String> historyFiles() throws IOException {
        try (Stream<Path> paths = Files.list(logDirectory)) {
            return paths.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(LOG_FILE_NAME + '-'))
                    .collect(Collectors.toList());
        }
    }

    private void awaitHistoryFiles(final int count) throws Exception {
        awaitUntil(() -> historyFiles().size() == count);
    }

    /**
     * Waits until the only history file is compressed. The file is renamed first and compressed under its final
     * name, and the uncompressed file is deleted once the compressed one is complete.
     */
    private Path awaitCompressedHistoryFile() throws Exception {
        final AtomicReference<String> compressedFile = new AtomicReference<>();
        awaitUntil(() -> {
            final List<String> files = historyFiles();
            if (files.size() == 1 && files.get(0).endsWith(".gz")) {
                compressedFile.set(files.get(0));
                return true;
            }
            return false;
        });
        return logDirectory.resolve(compressedFile.get());
    }

    /**
     * Waits until the writer has completed at least one run, rotation and retention checks included, that
     * started after this method was called.
     */
    private void awaitWriterRuns() throws Exception {
        // the run that writes the second event starts after the run that wrote the first one has completed
        writeEvent();
        writeEvent();
    }

    /** Writes an event, waits until it is in the log file and returns its ID. */
    private String writeEvent() throws Exception {
        final Path logFile = logDirectory.resolve(LOG_FILE_NAME);
        final String id = UUID.randomUUID().toString();
        writer.put(TOPIC, json(object(field("_id", id))));
        writer.flushFileBuffer(TOPIC);
        awaitUntil(() -> new String(Files.readAllBytes(logFile), UTF_8).contains(id));
        return id;
    }

    private static String gunzip(final Path file) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
            return new String(in.readAllBytes(), UTF_8);
        }
    }

    private static void awaitUntil(final Callable<Boolean> condition) throws Exception {
        final long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!condition.call()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition not met within " + TIMEOUT_MILLIS + " ms");
            }
            Thread.sleep(10);
        }
    }
}
