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

import org.apache.tools.ant.DirectoryScanner;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

@SuppressWarnings("javadoc")
public class SyntaxHighlighterCopierTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldCopySyntaxHighlighterFiles() throws IOException {
        String[] outputDirectories = { folder.getRoot().getPath() };
        SyntaxHighlighterCopier copier = new SyntaxHighlighterCopier(outputDirectories);
        copier.copy();

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(folder.getRoot());
        String[] includes = { "**/*.css", "**/*.js" };
        scanner.setIncludes(includes);
        scanner.scan();

        String[] shFiles = scanner.getIncludedFiles();
        assertThat(shFiles).contains("sh/css/shCore.css", "sh/js/shAll.js");
    }

    @After
    public void tearDown() {
        folder.delete();
    }
}
