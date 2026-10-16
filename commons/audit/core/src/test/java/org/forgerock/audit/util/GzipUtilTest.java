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
package org.forgerock.audit.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GzipUtilTest {

    private Path directory;

    @BeforeMethod
    public void setUp() throws IOException {
        directory = Files.createTempDirectory("gzip-util");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            final List<Path> sorted = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (final Path path : sorted) {
                Files.delete(path);
            }
        }
    }

    @Test
    public void shouldRemoveGzipSuffixOnlyFromEndOfFileName() {
        final File parent = new File("logs.gz");

        assertThat(GzipUtil.removeGzipSuffix(new File(parent, "audit.gz.json-2026.gz")))
                .isEqualTo(new File(parent, "audit.gz.json-2026"));
        assertThat(GzipUtil.removeGzipSuffix(new File(parent, "audit.json-2026.gz.1")))
                .isEqualTo(new File(parent, "audit.json-2026.gz.1"));
    }

    @Test
    public void shouldReplaceRenamedFileWithCompressedArchive() throws IOException {
        final File renamedFile = createFile("audit.json-2026", "content");
        final File archivedFile = directory.resolve("audit.json-2026.gz").toFile();

        GzipUtil.compressRotatedFile(archivedFile);

        assertThat(renamedFile).doesNotExist();
        assertThat(gunzip(archivedFile)).isEqualTo("content");
    }

    @Test
    public void shouldNeitherOverwriteArchiveNorDeleteRenamedFile() throws IOException {
        final File renamedFile = createFile("audit.json-2026", "new");
        final File archivedFile = createGzipFile("audit.json-2026.gz", "old");

        GzipUtil.compressRotatedFile(archivedFile);

        assertThat(gunzip(archivedFile)).isEqualTo("old");
        assertThat(renamedFile).hasContent("new");
    }

    @Test
    public void shouldDeletePartlyWrittenArchiveAndKeepRenamedFile() throws IOException {
        // a directory opens as an input stream on Linux and macOS, so reading it fails after the archive was
        // created; on Windows opening it fails, and the archive is never created
        final File renamedFile = Files.createDirectory(directory.resolve("audit.json-2026")).toFile();
        final File archivedFile = directory.resolve("audit.json-2026.gz").toFile();

        GzipUtil.compressRotatedFile(archivedFile);

        assertThat(archivedFile).doesNotExist();
        assertThat(renamedFile).exists();
    }

    @Test
    public void shouldNotCreateArchiveWhenRenamedFileIsMissing() {
        final File archivedFile = directory.resolve("audit.json-2026.gz").toFile();

        GzipUtil.compressRotatedFile(archivedFile);

        assertThat(archivedFile).doesNotExist();
    }

    @Test
    public void shouldNotCompressFileWithoutGzipSuffix() throws IOException {
        final File archivedFile = createFile("audit.json-2026", "content");

        GzipUtil.compressRotatedFile(archivedFile);

        assertThat(archivedFile).hasContent("content");
        assertThat(directory.toFile().list()).containsOnly("audit.json-2026");
    }

    private File createFile(final String name, final String content) throws IOException {
        return Files.write(directory.resolve(name), content.getBytes(UTF_8)).toFile();
    }

    private File createGzipFile(final String name, final String content) throws IOException {
        final Path file = directory.resolve(name);
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(content.getBytes(UTF_8));
        }
        return file.toFile();
    }

    private static String gunzip(final File file) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file.toPath()))) {
            return new String(in.readAllBytes(), UTF_8);
        }
    }
}
