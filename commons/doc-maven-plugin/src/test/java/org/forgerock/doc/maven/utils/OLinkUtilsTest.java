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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TreeSet;

@SuppressWarnings("javadoc")
public class OLinkUtilsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File chunked;
    private File epub;
    private File pdf;
    private File rtf;
    private File html;
    private File webhelp;
    private File xhtml5;
    private File bootstrap;

    private AbstractDocbkxMojo mojo;

    @Before
    public void setUp() throws MojoExecutionException {

        chunked = new File(folder.getRoot(), "chunked");
        epub = new File(folder.getRoot(), "epub");
        pdf = new File(folder.getRoot(), "pdf");
        rtf = new File(folder.getRoot(), "rtf");
        html = new File(folder.getRoot(), "html");
        webhelp = new File(folder.getRoot(), "webhelp");
        xhtml5 = new File(folder.getRoot(), "xhtml5");
        bootstrap = new File(folder.getRoot(), "bootstrap");


        TreeSet<String> docNames = new TreeSet<String>();
        docNames.add("guide");
        docNames.add("reference");

        mojo = mock(AbstractDocbkxMojo.class);
        when(mojo.getBuildDirectory()).thenReturn(new File("/path/to/target"));
        when(mojo.path(mojo.getBuildDirectory())).thenReturn("/path/to/target");
        when(mojo.getDocNames()).thenReturn(docNames);
        when(mojo.getDocumentSrcName()).thenReturn("index.xml");
        when(mojo.getProjectName()).thenReturn("ForgeRock");
        when(mojo.getProjectVersion()).thenReturn("1.0.0");
    }

    private File file(String resource) throws URISyntaxException {
        return new File(getClass().getResource(resource).toURI());
    }

    @Test
    public void shouldCreateChunkedHtmlDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(chunked, "html", mojo, true);
        File expected = file("/unit/utils/olu/olinkdb-chunked-html.xml");
        assertThat(chunked).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreateEpubDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(epub, "epub", mojo);
        File expected = file("/unit/utils/olu/olinkdb-epub.xml");
        assertThat(epub).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreatePdfDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(pdf, "pdf", mojo);
        File expected = file("/unit/utils/olu/olinkdb-pdf.xml");
        assertThat(pdf).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreateRtfDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        // Try with an empty version, as for a nightly build.
        when(mojo.getProjectVersion()).thenReturn("");

        OLinkUtils.createTargetDatabase(rtf, "rtf", mojo);
        File expected = file("/unit/utils/olu/olinkdb-rtf.xml");
        assertThat(rtf).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreateSinglePageDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(html, "html", mojo);
        File expected = file("/unit/utils/olu/olinkdb-single-page-html.xml");
        assertThat(html).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreateBootstrapDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(bootstrap, "bootstrap", mojo);
        File expected = file("/unit/utils/olu/olinkdb-bootstrap.xml");
        assertThat(bootstrap).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreateWebhelpDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(webhelp, "webhelp", mojo);
        File expected = file("/unit/utils/olu/olinkdb-webhelp.xml");
        assertThat(webhelp).hasContentEqualTo(expected);
    }

    @Test
    public void shouldCreateXhtmlDatabase()
            throws IOException, MojoExecutionException, URISyntaxException {

        OLinkUtils.createTargetDatabase(xhtml5, "xhtml5", mojo);
        File expected = file("/unit/utils/olu/olinkdb-xhtml.xml");
        assertThat(xhtml5).hasContentEqualTo(expected);
    }

    @After
    public void tearDown() {
        folder.delete();
    }
}
