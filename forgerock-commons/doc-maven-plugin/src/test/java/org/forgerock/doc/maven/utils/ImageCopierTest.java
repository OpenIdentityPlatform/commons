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
import static org.mockito.Mockito.*;

import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("javadoc")
public class ImageCopierTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final String srcName   = "book.xml";
    private final String imageName = "image.jpg";
    private File bookDir;
    private File subDir;
    private AbstractDocbkxMojo mojo;

    @Before
    public void setUp() throws IOException {
        // folder/book/book.xml
        File srcDir = folder.newFolder("book");
        File srcFile = new File(srcDir, srcName);
        if (!srcFile.createNewFile()) {
            throw new IOException("setUp failed to create " + srcFile.getPath());
        }

        // folder/book/images/image.jpg
        File imageDir = new File(srcDir, "images");
        if (imageDir.mkdir()) {
            File image = new File(imageDir, imageName);
            if (!image.createNewFile()) {
                throw new IOException("setUp failed to create " + image.getPath());
            }
        }

        // folder/html/book/book.html
        File outDir = folder.newFolder("html");
        bookDir = new File(outDir, "book");
        String outName = "book.html";
        if (bookDir.mkdir()) {
            File outFile = new File(bookDir, outName);
            if (!outFile.createNewFile()) {
                throw new IOException("setUp failed to create " + outFile.getPath());
            }
        }

        // folder/html/book/sub/book.html
        subDir = new File(bookDir, "sub");
        if (subDir.mkdir()) {
            File subOutFile = new File(subDir, outName);
            if (!subOutFile.createNewFile()) {
                throw new IOException("setUp failed to create " + subOutFile.getPath());
            }
        }

        mojo = mock(AbstractDocbkxMojo.class);
        when(mojo.getDocumentSrcName()).thenReturn(srcName);
        when(mojo.getDocbkxModifiableSourcesDirectory()).thenReturn(folder.getRoot());
        when(mojo.getDocbkxOutputDirectory()).thenReturn(folder.getRoot());
    }

    @Test
    public void shouldCopyOneLevel() throws IOException {
        ImageCopier.copyImages("html", null, mojo);

        File imageDir = new File(bookDir, "images");
        File image    = new File(imageDir, imageName);
        assertThat(image).exists();
    }

    @Test
    public void shouldCopyToSubDir() throws IOException {
        ImageCopier.copyImages("html", subDir.getName(), srcName, folder.getRoot(), folder.getRoot());

        File imageDir = new File(subDir, "images");
        File image    = new File(imageDir, imageName);
        assertThat(image).exists();
    }

    @After
    public void tearDown() {
        folder.delete();
    }

}
