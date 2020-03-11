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
package org.forgerock.doc.maven.utils.helper;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.FileFilter;

/**
 * Construct a FileFilter to match files.
 */
public final class FileFilterFactory {

    /**
     * Get a file filter to match directories and files of the specified extension.
     *
     * @param extension The extension for files that the filter matches,
     *                  starting with a {@code .} such as {@code .xml}.
     * @return A file filter to match directories and files of the specified extension.
     */
    public static FileFilter getFileFilter(final String extension) {
        IOFileFilter dirFilter = FileFilterUtils
                .and(FileFilterUtils.directoryFileFilter(),
                        HiddenFileFilter.VISIBLE);
        IOFileFilter fileFilter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.suffixFileFilter(extension));
        return FileFilterUtils.or(dirFilter, fileFilter);
    }

    /**
     * Get a file filter for directories and .xml files.
     *
     * @return A file filter to match .xml files.
     */
    public static FileFilter getXmlFileFilter() {
        return getFileFilter(".xml");
    }

    /**
     * Not used.
     */
    private FileFilterFactory() {
    }
}
