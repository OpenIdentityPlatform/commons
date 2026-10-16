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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compresses rotated audit files whose rotation suffix ends with {@link #GZIP_SUFFIX}.
 * <p>
 * A writer renames the current file to {@link #removeGzipSuffix(File)} of its archive name, starts a new current
 * file, and then calls {@link #compressRotatedFile(File)}.
 */
public final class GzipUtil {

    /** The end of a rotated file name that requests gzip compression. */
    public static final String GZIP_SUFFIX = ".gz";

    private static final Logger logger = LoggerFactory.getLogger(GzipUtil.class);
    private static final int BUFFER_SIZE = 64 * 1024;

    private GzipUtil() {
        // utility class
    }

    /**
     * Removes {@link #GZIP_SUFFIX} from the end of a file name.
     *
     * @param file The file.
     * @return The file without the suffix, or {@code file} if its name does not end with the suffix.
     */
    public static File removeGzipSuffix(final File file) {
        final String name = file.getName();
        if (!name.endsWith(GZIP_SUFFIX)) {
            return file;
        }
        return new File(file.getParentFile(), name.substring(0, name.length() - GZIP_SUFFIX.length()));
    }

    /**
     * Compresses a rotated file into its archive, if the archive name ends with {@link #GZIP_SUFFIX}, forces the
     * archive to disk, and then deletes the uncompressed file. An existing archive is never overwritten. A failure
     * is logged, and the uncompressed file is kept.
     *
     * @param archivedFile The archive name of the rotated file, which was renamed to
     *                     {@link #removeGzipSuffix(File)} of this name.
     */
    public static void compressRotatedFile(final File archivedFile) {
        final File renamedFile = removeGzipSuffix(archivedFile);
        if (renamedFile.equals(archivedFile)) {
            return;
        }
        try {
            compress(renamedFile, archivedFile);
        } catch (IOException e) {
            logger.error("Unable to compress the rotated audit file {} into {}", renamedFile, archivedFile, e);
            return;
        }
        try {
            Files.delete(renamedFile.toPath());
        } catch (IOException e) {
            logger.error("Compressed the rotated audit file {}, but unable to delete it", renamedFile, e);
        }
    }

    private static void compress(final File source, final File target) throws IOException {
        boolean created = false;
        try (InputStream in = Files.newInputStream(source.toPath());
                FileChannel channel = FileChannel.open(target.toPath(), StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE)) {
            created = true;
            try (GZIPOutputStream out = new GZIPOutputStream(Channels.newOutputStream(channel), BUFFER_SIZE)) {
                in.transferTo(out);
                out.finish();
                // the caller deletes the source next, so the archive must be on disk before that
                channel.force(true);
            }
        } catch (IOException e) {
            if (created) {
                // remove the partly written archive
                try {
                    Files.deleteIfExists(target.toPath());
                } catch (IOException deleteFailure) {
                    e.addSuppressed(deleteFailure);
                }
            }
            throw e;
        }
    }
}
