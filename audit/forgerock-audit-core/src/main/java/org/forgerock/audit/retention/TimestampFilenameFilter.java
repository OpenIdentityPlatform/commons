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
package org.forgerock.audit.retention;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;

import org.joda.time.format.DateTimeFormatter;

/**
 * A {@link FilenameFilter} that matches historical log files. The {@link FilenameFilter} matches a filename with a
 * given prefix, filename and timestamp.
 */
public class TimestampFilenameFilter implements FilenameFilter {

    private final File initialFile;
    private final String prefix;
    private final DateTimeFormatter suffixDateFormat;

    /**
     * Constructs a {@link TimestampFilenameFilter} given an initial file, prefix and suffix.
     * @param initialFile The initial filename.
     * @param prefix The audit file prefix to match.
     * @param suffixDateFormat The audit file date suffix to match.
     */
    public TimestampFilenameFilter(final File initialFile, final String prefix,
            final DateTimeFormatter suffixDateFormat) {
        this.initialFile = initialFile;
        this.prefix = prefix;
        this.suffixDateFormat = suffixDateFormat;
    }

    /**
     * Matches the name of a file to the {@link FilenameFilter}.
     * {@inheritDoc}
     */
    @Override
    public boolean accept(File dir, String name) {
        // create prefix + filename string
        final StringBuilder newFileNameBuilder = new StringBuilder();
        final Path path = initialFile.toPath();

        if (prefix != null) {
            newFileNameBuilder.append(prefix);
        }
        newFileNameBuilder.append(path.getFileName());
        final String newFileName = newFileNameBuilder.toString();

        if (name.length() < newFileName.length()) {
            // the filename is smaller or equal to the prefix + filename
            return false;
        }

        final String timestamp = name.substring(newFileName.length());
        try {
            suffixDateFormat.parseDateTime(timestamp);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            // not a valid timestamp for the given timestamp suffix
            return false;
        }

        // if the file starts with the prefix + initial filename match it.
        if (name.startsWith(newFileName)) {
            return true;
        }
        return false;
    }
}
