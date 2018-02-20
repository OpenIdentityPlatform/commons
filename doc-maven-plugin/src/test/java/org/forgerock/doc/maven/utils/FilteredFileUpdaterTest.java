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
 * Copyright 2014 ForgeRock AS
 */

package org.forgerock.doc.maven.utils;

import static org.assertj.core.api.Assertions.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

@SuppressWarnings("javadoc")
public class FilteredFileUpdaterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void copyTestFiles() throws URISyntaxException, IOException {
        String[] testFileResources = {
            "/unit/utils/ffu/filterme.txt",
            "/unit/utils/ffu/ignore.me",
            "/unit/utils/ffu/unaffected.txt"
        };

        for (String resource : testFileResources) {
            File testFile = new File(getClass().getResource(resource).toURI());
            FileUtils.copyFileToDirectory(testFile, folder.getRoot());
        }
    }

    @Test
    public void shouldOnlyAffectFiltered() throws IOException {
        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("change me", "changed");

        // Match normal directories, and .txt files.
        IOFileFilter dirFilter = FileFilterUtils
                .and(FileFilterUtils.directoryFileFilter(),
                        HiddenFileFilter.VISIBLE);
        IOFileFilter fileFilter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.suffixFileFilter(".txt"));
        FileFilter filter = FileFilterUtils.or(dirFilter, fileFilter);

        new FilteredFileUpdater(replacements, filter).update(folder.getRoot());

        File filtered = new File(folder.getRoot(), "filterme.txt");
        assertThat(contentOf(filtered)).isEqualTo("changed");

        File ignored = new File(folder.getRoot(), "ignore.me");
        assertThat(contentOf(ignored)).isEqualTo("change me");

        File unaffected = new File(folder.getRoot(), "unaffected.txt");
        assertThat(contentOf(unaffected)).isEqualTo("unaffected");
    }

    @After
    public void cleanUp() {
        folder.delete();
    }
}
