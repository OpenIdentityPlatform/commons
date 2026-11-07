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
 * Copyright 2024-2026 3A Systems, LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AntoraMojoTest extends AbstractMojoTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testExecute() throws Exception {
        File pom = getTestFile("src/test/resources/antora/pom.xml");
        assertThat(pom).isNotNull();

        AntoraMojo antoraMojo = (AntoraMojo) lookupMojo("antora", pom);
        assertThat(antoraMojo).isNotNull();
        this.configureMojo(antoraMojo, "doc-maven-plugin", pom);
        this.getVariablesAndValuesFromObject(antoraMojo);
    }

    @Test
    public void testGeneratedRootPageUsesReusableDocumentList() throws Exception {
        AntoraMojo mojo = (AntoraMojo) lookupMojo("antora", getTestFile("src/test/resources/antora/pom.xml"));
        configureMojo(mojo, "doc-maven-plugin", getTestFile("src/test/resources/antora/pom.xml"));
        Path build = Files.createTempDirectory("antora-index-test");
        try {
            FileUtils.copyDirectory(getTestFile("src/test/resources/asciidoc"), build.resolve("source").toFile());
            Files.writeString(build.resolve("source/reference/index.adoc"), "= Reference\n\ninclude::./chapter.adoc[]\n");
            Files.writeString(build.resolve("source/admin-guide/index.adoc"), "= Administration\n\ninclude::./chapter.adoc[]\n");
            mojo.buildDirectory = build.toFile();
            mojo.projectName = "OpenDJ";
            mojo.execute();

            Path root = build.resolve("antora/modules/ROOT");
            assertThat(Files.readString(root.resolve("pages/index.adoc"), StandardCharsets.UTF_8))
                    .contains("include::partial$documentation-list.adoc[]");
            assertThat(Files.readString(root.resolve("partials/documentation-list.adoc"), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n"))
                    .contains(":generated-documentation-component: opendj:",
                            "ifeval::[\"{page-component-name}\" == \"opendj\"]",
                            "* xref:{generated-documentation-component}reference:index.adoc[]\n"
                                    + "* xref:{generated-documentation-component}admin-guide:index.adoc[]")
                    .doesNotContain("API Reference");

            setVariableValueToObject(mojo, "antoraComponentName", "directory-service");
            setVariableValueToObject(mojo, "javadocUrl", "/opendj/apidocs/index.html");
            mojo.execute();
            assertThat(Files.readString(root.resolve("partials/documentation-list.adoc"), StandardCharsets.UTF_8))
                    .contains(":generated-documentation-component: directory-service:",
                            "ifeval::[\"{page-component-name}\" == \"directory-service\"]",
                            "link:/opendj/apidocs/index.html[API Reference (Javadoc)]");

            Path sourcePartials = build.resolve("source/partials");
            Files.createDirectories(sourcePartials);
            Files.writeString(sourcePartials.resolve("documentation-list.adoc"), "Existing content");
            assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("documentation-list.adoc");
        } finally {
            FileUtils.deleteDirectory(build.toFile());
        }
    }

    @Test
    public void testConvertXrefWithCurrentDirPrefix() {
        assertThat(AntoraMojo.convertXrefsToAntora(
                "see xref:./chap-jee-agent-config.adoc#configure-j2ee-policy-agent[Configure]"))
                .isEqualTo("see xref:chap-jee-agent-config.adoc#configure-j2ee-policy-agent[Configure]");
    }

    @Test
    public void testConvertXrefWithRepeatedCurrentDirSegments() {
        assertThat(AntoraMojo.convertXrefsToAntora("xref:./././a.adoc[A]"))
                .isEqualTo("xref:a.adoc[A]");
        assertThat(AntoraMojo.convertXrefsToAntora("xref:a/././b.adoc[B]"))
                .isEqualTo("xref:a:b.adoc[B]");
        assertThat(AntoraMojo.convertXrefsToAntora("xref:../mod/././p.adoc[P]"))
                .isEqualTo("xref:mod:p.adoc[P]");
    }

    @Test
    public void testConvertXrefToOtherModule() {
        assertThat(AntoraMojo.convertXrefsToAntora("xref:../reference/./ch02.adoc#anchor[Ref]"))
                .isEqualTo("xref:reference:ch02.adoc#anchor[Ref]");
        assertThat(AntoraMojo.convertXrefsToAntora("xref:ch02.adoc[Ref]"))
                .isEqualTo("xref:ch02.adoc[Ref]");
    }

    @Test
    public void testLeveloffsetHasNoStrayQuote() {
        assertThat(AntoraMojo.convertForAntora(":table-caption!:\n"))
                .isEqualTo(":table-caption!:\n:leveloffset: -1\n");
    }

    @Test
    public void testFindLegacyLinks() {
        assertThat(AntoraMojo.findLegacyLinks(
                "link:../../../openam/13/admin-guide/#chap-cdsso[CDSSO] and "
                        + "link:../attachments/file.zip[file] and "
                        + "link:../../../opendj/3.5/admin-guide/[OpenDJ]"))
                .containsExactly("link:../../../openam/13/admin-guide/#chap-cdsso",
                        "link:../../../opendj/3.5/admin-guide/");
    }

    @Test
    public void testWarnLegacyLinksNamesFileAndLink() {
        AntoraMojo mojo = new AntoraMojo();
        List<String> warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {
            @Override
            public void warn(CharSequence content) {
                warnings.add(content.toString());
            }
        });
        mojo.warnLegacyLinks(new File("chap-cdsso.adoc"),
                "link:../../../openam/13/admin-guide/#chap-cdsso[CDSSO]");
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0))
                .contains("chap-cdsso.adoc", "link:../../../openam/13/admin-guide/#chap-cdsso");
    }
}
