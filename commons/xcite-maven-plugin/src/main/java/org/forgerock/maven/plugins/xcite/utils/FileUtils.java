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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.maven.plugins.xcite.utils;

import org.codehaus.plexus.util.DirectoryScanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Utility methods for handling files.
 */
public final class FileUtils {

    /**
     * Get the content of a file as an array of Strings.
     *
     * @param file          File containing the strings.
     * @return              The array of strings contained in the file.
     * @throws IOException  Failed to read the file.
     */
    public static ArrayList<String> getStrings(final File file)
            throws IOException {

        ArrayList<String> lines = new ArrayList<String>();
        if (file == null) {
            return lines;
        }

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();

        return lines;
    }

    /**
     * A subset of formats described in the documentation for the <a
     * href="http://www.docbook.org/tdg/en/html/imagedata.html">ImageData</a>
     * element.
     */
    private static String[] imageFiles = {"**/*.bmp",
        "**/*.eps",
        "**/*.gif",
        "**/*.jpeg",
        "**/*.jpg",
        "**/*.png",
        "**/*.svg",
        "**/*.tiff"};

    /**
     * Get a list of relative file paths based on inclusion patterns.
     *
     * @param baseDirectory     Where to look for files to include.
     * @param includes          Patterns specifying files to include.
     *                          If null, match all files
     *                          except files excluded by default.
     * @param excludes          Patterns specifying files to exclude.
     *                          If null, exclude only image files
     *                          and files excluded by default.
     * @return                  Relative paths to files.
     */
    public static String[] getIncludedFiles(File baseDirectory,
                                            String[] includes,
                                            String[] excludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDirectory);
        scanner.setIncludes(includes);

        // Exclude at least image files and the defaults.
        String[] defaultExcludes = new String[imageFiles.length
                + DirectoryScanner.DEFAULTEXCLUDES.length];
        System.arraycopy(imageFiles, 0, defaultExcludes, 0, imageFiles.length);
        System.arraycopy(DirectoryScanner.DEFAULTEXCLUDES, 0,
                defaultExcludes, imageFiles.length, DirectoryScanner.DEFAULTEXCLUDES.length);

        if (excludes == null) {
            excludes = defaultExcludes;
        } else {
            String[] full = new String[excludes.length + defaultExcludes.length];
            System.arraycopy(excludes, 0, full, 0, excludes.length);
            System.arraycopy(defaultExcludes, 0, full, excludes.length, defaultExcludes.length);
            excludes = full;
        }

        scanner.setExcludes(excludes);

        scanner.scan();
        return scanner.getIncludedFiles();
    }

    /**
     * Constructor not used.
     */
    private FileUtils() {
        // Not used
    }
}
