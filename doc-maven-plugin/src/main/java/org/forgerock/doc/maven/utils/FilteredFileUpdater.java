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
 * Copyright 2012-2014 ForgeRock AS
 */

package org.forgerock.doc.maven.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

/**
 * Update matching files in all directories, replacing the first occurrence of
 * the string to replace with the replacement string. Files are expected to be
 * UTF-8 encoded.
 */
public class FilteredFileUpdater extends DirectoryWalker<File> {
    /**
     * Replacements to apply. The keys are the strings to replace, the values
     * are the replacements.
     */
    private final Map<String, String> msReplacements;

    /**
     * Construct an updater with a list of replacements per file.
     *
     * @param replacements
     *            Keys are strings to replace. Values are replacements.
     * @param filterToMatch
     *            Update files matching this filter
     */
    public FilteredFileUpdater(final Map<String, String> replacements,
            final FileFilter filterToMatch) {
        super(filterToMatch, -1);
        this.msReplacements = replacements;
    }

    /**
     * Update files that match the filter.
     *
     * @param startDirectory
     *            Base directory under which to update files, recursively
     * @return List of updated files
     * @throws IOException
     *             Something went wrong changing a file's content.
     */
    public final List<File> update(final File startDirectory) throws IOException {
        List<File> results = new ArrayList<File>();
        walk(startDirectory, results);
        return results;
    }

    /**
     * Update files that match, adding them to the results.
     *
     * @param file
     *            File to update
     * @param depth
     *            Not used
     * @param results
     *            List of files updated
     * @throws IOException
     *             Something went wrong changing a file's content.
     */
    @Override
    protected final void handleFile(final File file, final int depth,
            final Collection<File> results) throws IOException {
        if (file.isFile()) {
            String data = FileUtils.readFileToString(file, "UTF-8");

            for (String key : msReplacements.keySet()) {
                data = data.replace(key, msReplacements.get(key));
            }

            FileUtils.deleteQuietly(file);
            FileUtils.writeStringToFile(file, data, "UTF-8");

            results.add(file);
        }
    }
}
