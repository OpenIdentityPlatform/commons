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
package org.forgerock.audit.retention;

import static org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration.FileRotation.DEFAULT_ROTATION_FILE_SUFFIX;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.audit.util.LastModifiedTimeFileComparator;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a time stamp based file naming policy. Rotated files are renamed with a given prefix and a timestamp suffix.
 */
public class TimeStampFileNamingPolicy implements FileNamingPolicy {

    private static final int INITIAL_STRING_BUFFER_SIZE = 64;

    private static final Logger logger = LoggerFactory.getLogger(TimeStampFileNamingPolicy.class);

    private final File initialFile;
    private DateTimeFormatter suffixDateFormat;
    private final String prefix;
    private final TimestampFilenameFilter timestampFilenameFilter;
    private final LastModifiedTimeFileComparator lastModifiedTimeFileComparator = new LastModifiedTimeFileComparator();
    private final AtomicInteger collisionCounter = new AtomicInteger();

    /**
     * Constructs a TimeStampFileNaming policy with a given initial file, a timestamp format, and a prefix string.
     * @param initialFile The initial file that will be archived.
     * @param timeStampFormat The timestamp format to append to the archived files. Should be a format that is
     *                        understood by {@link DateTimeFormat}.
     * @param prefix The prefix to prefix to the archived files.
     */
    public TimeStampFileNamingPolicy(final File initialFile, final String timeStampFormat, final String prefix) {
        this.initialFile = initialFile;
        this.prefix = prefix;

        if (timeStampFormat != null && timeStampFormat.trim().length() > 0) {
            try {
                suffixDateFormat = DateTimeFormat.forPattern(timeStampFormat);
            } catch (IllegalArgumentException iae) {
                logger.info("Date format invalid: {}", timeStampFormat, iae);
            }
        }
        if (suffixDateFormat == null) {
            // fallback to a default date format, so the filenames will differ
            suffixDateFormat = DateTimeFormat.forPattern(DEFAULT_ROTATION_FILE_SUFFIX);
        }
        this.timestampFilenameFilter = new TimestampFilenameFilter(initialFile, prefix, suffixDateFormat);
    }

    /**
     * Gets the initial file.
     * @return The initial file.
     */
    @Override
    public File getInitialName() {
        return initialFile;
    }

    /**
     * Gets the next name for this {@link FileNamingPolicy}. The next name will be formatted with prefix first,
     * then the initial filename and finally the timestamp will be appended.
     * @return The next archived file according to this {@link FileNamingPolicy}.
     */
    @Override
    public File getNextName() {
        final StringBuilder newFileName = new StringBuilder(INITIAL_STRING_BUFFER_SIZE);
        final Path path = initialFile.toPath();

        if (prefix != null) {
            newFileName.append(prefix);
        }

        newFileName.append(path.getFileName());

        if (suffixDateFormat != null) {
            newFileName.append(LocalDateTime.now().toString(suffixDateFormat));
        }

        Path newFilePath = path.resolveSibling(newFileName.toString());
        if (Files.exists(newFilePath)) {
            // prevent filename collision with unique suffix
            newFileName.append('.').append(collisionCounter.incrementAndGet());
            newFilePath = path.resolveSibling(newFileName.toString());
        }
        return newFilePath.toFile();
    }

    /**
     * List the files in the initial file directory that match the prefix, name and suffix format.
     * {@inheritDoc}
     */
    @Override
    public List<File> listFiles() {
        List<File> fileList =
                new LinkedList<>(Arrays.asList(initialFile.getParentFile().listFiles(timestampFilenameFilter)));
        // make sure the files are sorted from oldest to newest.
        Collections.sort(fileList, Collections.reverseOrder(lastModifiedTimeFileComparator));
        return fileList;
    }
}
