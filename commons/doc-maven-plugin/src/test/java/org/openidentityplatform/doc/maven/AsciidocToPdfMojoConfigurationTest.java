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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class AsciidocToPdfMojoConfigurationTest extends AbstractMojoTestCase {

    private File buildDirectory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        buildDirectory = Files.createTempDirectory("pdf-configuration").toFile();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            FileUtils.deleteDirectory(buildDirectory);
        } finally {
            super.tearDown();
        }
    }

    public void testEmptyPomEntryOverridesProjectVersion() throws Exception {
        MavenProject project = readProject();
        AsciidocToPdfMojo mojo = (AsciidocToPdfMojo) lookupConfiguredMojo(project, "asciidoc-to-pdf");
        assertPreparedLinks(mojo, "");
    }

    public void testPomVersionsReachPreparedPdfSource() throws Exception {
        MavenProject project = readProject();
        Xpp3Dom configuration = (Xpp3Dom) project.getBuildPlugins().get(0).getConfiguration();
        Xpp3Dom versions = configuration.getChild("componentDocVersions");
        versions.removeChild(versions.getChild("openam"));
        AsciidocToPdfMojo mojo = (AsciidocToPdfMojo) lookupConfiguredMojo(project, "asciidoc-to-pdf");
        assertPreparedLinks(mojo, "15.2/");
    }

    public void testMavenUserPropertySelectsHistoricalVersion() throws Exception {
        MavenProject project = readProject();
        Xpp3Dom configuration = (Xpp3Dom) project.getBuildPlugins().get(0).getConfiguration();
        configuration.removeChild(configuration.getChild("docVersion"));
        Xpp3Dom versions = configuration.getChild("componentDocVersions");
        versions.removeChild(versions.getChild("openam"));
        MavenSession session = newMavenSession(project);
        session.getUserProperties().setProperty("docVersion", "15.2");
        AsciidocToPdfMojo mojo = (AsciidocToPdfMojo) lookupConfiguredMojo(session,
                newMojoExecution("asciidoc-to-pdf"));
        assertPreparedLinks(mojo, "15.2/");
    }

    public void testComponentDocVersionsIsADescriptorParameter() throws Exception {
        assertTrue(newMojoExecution("asciidoc-to-pdf").getMojoDescriptor()
                .getParameterMap().containsKey("componentDocVersions"));
    }

    public void testAbsentMapKeepsOtherComponentsUnversioned() throws Exception {
        MavenProject project = readProject();
        Xpp3Dom configuration = (Xpp3Dom) project.getBuildPlugins().get(0).getConfiguration();
        configuration.removeChild(configuration.getChild("componentDocVersions"));
        AsciidocToPdfMojo mojo = (AsciidocToPdfMojo) lookupConfiguredMojo(project, "asciidoc-to-pdf");
        FileUtils.writeStringToFile(new File(mojo.getAsciidocBuildSourceDirectory(), "how-to/page.adoc"),
                "xref:reference:page.adoc[A] xref:opendj::index.adoc[B]", StandardCharsets.UTF_8);

        mojo.execute();

        assertThat(new File(mojo.getPdfSourceDirectory(), "how-to/page.adoc")).hasContent(
                "link:https://doc.openidentityplatform.org/openam/15.2/reference/page.html[A] "
                        + "link:https://doc.openidentityplatform.org/opendj/index.html[B]");
    }

    private MavenProject readProject() throws Exception {
        File pom = getTestFile("src/test/resources/asciidoc-to-pdf/pom.xml");
        try (Reader reader = Files.newBufferedReader(pom.toPath(), StandardCharsets.UTF_8)) {
            MavenProject project = new MavenProject(new MavenXpp3Reader().read(reader));
            project.setFile(pom);
            project.getBuild().setDirectory(buildDirectory.getAbsolutePath());
            return project;
        }
    }

    private void assertPreparedLinks(AsciidocToPdfMojo mojo, String versionPrefix) throws Exception {
        FileUtils.writeStringToFile(new File(mojo.getAsciidocBuildSourceDirectory(), "how-to/page.adoc"),
                "xref:reference:page.adoc#a[A] xref:opendj::index.adoc[B]", StandardCharsets.UTF_8);
        mojo.execute();
        assertThat(new File(mojo.getPdfSourceDirectory(), "how-to/page.adoc")).hasContent(
                "link:https://doc.openidentityplatform.org/openam/" + versionPrefix + "reference/page.html#a[A] "
                        + "link:https://doc.openidentityplatform.org/opendj/4.10/index.html[B]");
    }
}
