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

import static org.assertj.core.api.Assertions.*;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("javadoc")
public class FileUtilsTest {

    @Test
    public void readTemporaryFile() throws IOException {
        ArrayList<String> content = new ArrayList<String>();
        content.add("Hello world");
        content.add("Hello again");

        File tempFile = File.createTempFile(getClass().getName(), ".txt");
        org.apache.commons.io.FileUtils.writeLines(tempFile, content);

        ArrayList<String> fileContent = FileUtils.getStrings(tempFile);
        assertThat(fileContent).isEqualTo(content);

        tempFile.deleteOnExit();
    }

    @Test
    public void checkIncludedFiles() throws IOException {
        File temporaryTxtFile = File.createTempFile("test", ".txt");
        File temporaryTmpFile = File.createTempFile("test", null);

        File baseDirectory = temporaryTmpFile.getParentFile();

        String[] includes = new String[1];
        includes[0] = "**/*.txt";

        String[] files = FileUtils.getIncludedFiles(baseDirectory, includes, null);

        assertThat(files).contains(temporaryTxtFile.getName());
        assertThat(files).doesNotContain(temporaryTmpFile.getName());

        temporaryTmpFile.deleteOnExit();
        temporaryTmpFile.deleteOnExit();
    }

    @Test
    public void nullIncludesMatchesAllFiles() throws IOException {
        File temporaryTxtFile = File.createTempFile("test", ".txt");
        File temporaryTmpFile = File.createTempFile("test", null);

        File baseDirectory = temporaryTmpFile.getParentFile();

        String[] files = FileUtils.getIncludedFiles(baseDirectory, null, null);

        assertThat(files).contains(temporaryTxtFile.getName());
        assertThat(files).contains(temporaryTmpFile.getName());

        temporaryTmpFile.deleteOnExit();
        temporaryTmpFile.deleteOnExit();
    }

    @Test
    public void checkExcludedFiles() throws IOException {
        File temporaryTxtFile = File.createTempFile("test", ".txt");
        File temporaryTmpFile = File.createTempFile("test", ".tmp");

        File baseDirectory = temporaryTmpFile.getParentFile();

        String[] excludes = new String[1];
        excludes[0] = "**/*.tmp";

        String[] files = FileUtils.getIncludedFiles(baseDirectory, null, excludes);

        assertThat(files).contains(temporaryTxtFile.getName());
        assertThat(files).doesNotContain(temporaryTmpFile.getName());

        temporaryTmpFile.deleteOnExit();
        temporaryTmpFile.deleteOnExit();
    }

}
