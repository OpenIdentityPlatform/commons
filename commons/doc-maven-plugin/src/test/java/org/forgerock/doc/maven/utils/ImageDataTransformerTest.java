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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@SuppressWarnings("javadoc")
public class ImageDataTransformerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, URISyntaxException {
        File testFile = new File(getClass().getResource("/unit/utils/idt/chapter.xml").toURI());
        FileUtils.copyFileToDirectory(testFile, folder.getRoot());
    }

    @Test
    public void shouldAddImageAttributes() throws IOException {
        new ImageDataTransformer().update(folder.getRoot());

        File out = new File(folder.getRoot(), "chapter.xml");
        String expectedElement =
                "<db:imagedata xmlns:db=\"http://docbook.org/ns/docbook\""
                        + " align=\"center\" scalefit=\"1\" width=\"100%\""
                        + " contentdepth=\"100%\""
                        + " fileref=\"images/an-image.png\" format=\"PNG\"/>";

        assertThat(contentOf(out)).contains(expectedElement);
    }

    @After
    public void tearDown() {
        folder.delete();
    }
}
