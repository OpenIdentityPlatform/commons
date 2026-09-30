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
 * Copyright 2024 3A Systems LLC.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void testConvertXrefWithCurrentDirPrefix() {
        assertThat(AntoraMojo.convertXrefsToAntora(
                "see xref:./chap-jee-agent-config.adoc#configure-j2ee-policy-agent[Configure]"))
                .isEqualTo("see xref:chap-jee-agent-config.adoc#configure-j2ee-policy-agent[Configure]");
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
}
