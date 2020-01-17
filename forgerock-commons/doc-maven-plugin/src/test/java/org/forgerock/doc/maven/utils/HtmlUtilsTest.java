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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("javadoc")
public class HtmlUtilsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File bookDir;
    private final String bookName = "book.html";
    private File bookFile;

    @Before
    public void setUp() throws IOException {
        bookDir = folder.newFolder("test");
        bookFile = new File(bookDir, bookName);
        if (!bookFile.createNewFile()) {
            throw new IOException("setUp failed to create " + bookFile.getPath());
        }
    }

    @Test
    public void shouldAddHtAccess() throws IOException {
        File htAccess = File.createTempFile("prefix", "ext");

        HtmlUtils.addHtaccess(folder.getRoot().getPath(), htAccess);
        assertThat(new File(folder.getRoot(), htAccess.getName())).exists();

        htAccess.delete();
    }

    @Test
    public void xmlCssShouldExist() throws IOException {
        File cssFile = folder.newFile("test.css");
        FileUtils.writeStringToFile(cssFile, "body { rounded-corners: true }");

        HtmlUtils.addCustomCss(cssFile, folder.getRoot(), bookName);
        assertThat(new File(bookDir, cssFile.getName() + ".xml")).exists();
    }

    @Test
    public void shouldReplaceContent() throws IOException {
        FileUtils.writeStringToFile(bookFile, "<p>Replace me</p>");
        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("Replace me", "Replaced");

        List<File> list = HtmlUtils.updateHtml(bookDir.getPath(), replacements);
        assertThat(list.size()).isEqualTo(1);

        for (File file : list) {
            assertThat(contentOf(file)).isEqualTo("<p>Replaced</p>");
        }
    }

    @Test
    public void shouldAddDotDotToHref() throws IOException {
        FileUtils.writeStringToFile(bookFile, "<a href=\"../resources\">");
        HtmlUtils.fixResourceLinks(bookDir.getPath(), "resources");
        assertThat(bookFile).hasContent("<a href=\"../../resources\">");

        FileUtils.writeStringToFile(bookFile, "<a href='../resources'>");
        HtmlUtils.fixResourceLinks(bookDir.getPath(), "resources");
        assertThat(bookFile).hasContent("<a href='../../resources'>");
    }

    @After
    public void tearDown() {
        folder.delete();
    }

}
