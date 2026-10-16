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
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.retention;

import static org.forgerock.audit.util.GzipUtil.GZIP_SUFFIX;

import java.io.File;
import java.io.FilenameFilter;

import org.joda.time.Chronology;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimeParserBucket;

/**
 * A {@link FilenameFilter} that matches historical log files. The {@link FilenameFilter} matches a filename with a
 * given prefix, filename and timestamp, optionally followed by the collision counter and the compression suffix
 * that {@link TimeStampFileNamingPolicy} appends.
 */
public class TimestampFilenameFilter implements FilenameFilter {

    /** The prefix followed by the initial file name. */
    private final String fileNamePrefix;
    private final DateTimeFormatter suffixDateFormat;
    private final DateTimeParser suffixDateParser;
    private final Chronology suffixDateChronology;

    /**
     * Constructs a {@link TimestampFilenameFilter} given an initial file, prefix and suffix.
     * @param initialFile The initial filename.
     * @param prefix The audit file prefix to match.
     * @param suffixDateFormat The audit file date suffix to match.
     */
    public TimestampFilenameFilter(final File initialFile, final String prefix,
            final DateTimeFormatter suffixDateFormat) {
        this.fileNamePrefix = (prefix != null ? prefix : "") + initialFile.toPath().getFileName();
        this.suffixDateFormat = suffixDateFormat;
        this.suffixDateParser = suffixDateFormat.getParser();
        // the chronology that DateTimeFormatter.parseDateTime() parses with
        final Chronology chronology = DateTimeUtils.getChronology(suffixDateFormat.getChronology());
        this.suffixDateChronology = suffixDateFormat.getZone() != null
                ? chronology.withZone(suffixDateFormat.getZone())
                : chronology;
    }

    /**
     * Matches the name of a file to the {@link FilenameFilter}.
     * {@inheritDoc}
     */
    @Override
    public boolean accept(File dir, String name) {
        // the directory may hold many other files, so reject them before allocating anything
        if (!name.startsWith(fileNamePrefix) || suffixDateParser == null) {
            return false;
        }
        // strip a trailing compression suffix, unless it is part of the prefix
        final int compressedEnd = name.length() - GZIP_SUFFIX.length();
        final int end = name.endsWith(GZIP_SUFFIX) && compressedEnd >= fileNamePrefix.length()
                ? compressedEnd
                : name.length();
        final String timestamp = name.substring(fileNamePrefix.length(), end);

        // parse as DateTimeFormatter.parseDateTime() does, but allow a collision counter after the timestamp and
        // reject a malformed timestamp without an exception
        final DateTimeParserBucket bucket = new DateTimeParserBucket(0L, suffixDateChronology,
                suffixDateFormat.getLocale(), suffixDateFormat.getPivotYear(), suffixDateFormat.getDefaultYear());
        final int position = suffixDateParser.parseInto(bucket, timestamp, 0);
        if (position < 0 || (position < timestamp.length() && !isCollisionCounter(timestamp, position))) {
            return false;
        }
        try {
            bucket.computeMillis(true, timestamp);
            return true;
        } catch (IllegalArgumentException e) {
            // a field is out of range, e.g. second 70
            return false;
        }
    }

    /**
     * Checks whether the rest of a timestamp is the unique suffix that {@link TimeStampFileNamingPolicy} appends
     * when the timestamped name is already taken: a dot followed by digits.
     */
    private static boolean isCollisionCounter(final String timestamp, final int start) {
        if (timestamp.charAt(start) != '.' || start + 1 == timestamp.length()) {
            return false;
        }
        for (int i = start + 1; i < timestamp.length(); i++) {
            final char c = timestamp.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
