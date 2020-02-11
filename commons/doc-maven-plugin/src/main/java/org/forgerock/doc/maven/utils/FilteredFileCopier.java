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
 * Copyright 2013-2014 ForgeRock AS
 */

package org.forgerock.doc.maven.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Copy files not having a particular extension.
 */
public final class FilteredFileCopier {

    /**
     * Copy all files not having the specified extension from
     * the source directory to the destination directory.
     *
     * @param extension Extension of files to skip.
     * @param sourceDir Source directory for files to copy.
     * @param destinationDir Destination directory for files to copy.
     * @throws IOException Failed to copy the files.
     */
    public static void copyOthers(final String extension,
                                  final File sourceDir,
                                  final File destinationDir)
            throws IOException {

        IOFileFilter nonExtFilter = FileFilterUtils.notFileFilter(
                FileFilterUtils.suffixFileFilter(extension));

        IOFileFilter nonExtFiles = FileFilterUtils.and(
                FileFileFilter.FILE, nonExtFilter);

        FileFilter filter = FileFilterUtils.or(
                DirectoryFileFilter.DIRECTORY, nonExtFiles);

        FileUtils.copyDirectory(sourceDir, destinationDir, filter);
    }

    private FilteredFileCopier() {
        // Not used.
    }
}
