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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class AsciidocToPdfMojoTest {

    private static final String SITE = "https://doc.openidentityplatform.org";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static String convert(String adoc) {
        return AsciidocToPdfMojo.convertXrefsForPdf(adoc, SITE, "openidm", "getting-started");
    }

    @Test
    public void testCrossComponentXref() {
        assertThat(convert("see xref:opendj:install-guide:index.adoc[Install Guide]."))
                .isEqualTo("see link:" + SITE + "/opendj/install-guide/index.html[Install Guide].");
        assertThat(convert("xref:opendj:admin-guide:chap-replication.adoc#read-ecl-as-regular-user[ECL]"))
                .isEqualTo("link:" + SITE
                        + "/opendj/admin-guide/chap-replication.html#read-ecl-as-regular-user[ECL]");
    }

    @Test
    public void testCrossModuleXrefInSameComponent() {
        assertThat(convert("xref:install-guide:chap-install.adoc#x[Install]"))
                .isEqualTo("link:" + SITE + "/openidm/install-guide/chap-install.html#x[Install]");
    }

    @Test
    public void testRootModuleHasNoModuleSegment() {
        assertThat(convert("xref:opendj::index.adoc[OpenDJ] xref:openam:ROOT:index.adoc[OpenAM]"))
                .isEqualTo("link:" + SITE + "/opendj/index.html[OpenDJ] link:" + SITE + "/openam/index.html[OpenAM]");
    }

    @Test
    public void testCrossGuideRelativeXref() {
        assertThat(convert("xref:../connectors-guide/chap-ldap.adoc#ldap-connector[LDAP]"))
                .isEqualTo("link:" + SITE + "/openidm/connectors-guide/chap-ldap.html#ldap-connector[LDAP]");
        assertThat(convert("xref:../integrators-guide/index.adoc[Integrator's Guide]"))
                .isEqualTo("link:" + SITE + "/openidm/integrators-guide/index.html[Integrator's Guide]");
        assertThat(convert("xref:../reference/./ch02.adoc#anchor[Ref]"))
                .isEqualTo("link:" + SITE + "/openidm/reference/ch02.html#anchor[Ref]");
    }

    @Test
    public void testRelativeXrefIntoCurrentGuideStaysInternal() {
        assertThat(convert("xref:../getting-started/chap-overview.adoc#a[Overview]"))
                .isEqualTo("xref:chap-overview.adoc#a[Overview]");
    }

    @Test
    public void testSameGuideXrefsAreUnchanged() {
        String adoc = "xref:#anchor[A] xref:chap-x.adoc#anchor[B] xref:./chap-y.adoc[C] "
                + "xref:ROOT:attachment$file.zip[D]";
        assertThat(convert(adoc)).isEqualTo(adoc);
    }

    @Test
    public void testSiteUrlTrailingSlashIsIgnored() {
        assertThat(AsciidocToPdfMojo.convertXrefsForPdf("xref:../a/b.adoc[B]", SITE + "/", "openidm", "c"))
                .isEqualTo("link:" + SITE + "/openidm/a/b.html[B]");
    }

    @Test
    public void testPreparePdfSourceRewritesCopyOnly() throws Exception {
        File source = tmp.newFolder("source");
        File guide = new File(source, "getting-started");
        File chapter = new File(guide, "chap-where-to-go.adoc");
        String original = "xref:opendj:install-guide:index.adoc[OpenDJ] xref:#local[Local]\n";
        FileUtils.writeStringToFile(chapter, original, StandardCharsets.UTF_8);
        File partial = new File(source, "partials/note.adoc");
        FileUtils.writeStringToFile(partial, "xref:../getting-started/chap-x.adoc[X]\n", StandardCharsets.UTF_8);
        File image = new File(source, "images/logo.png");
        FileUtils.writeByteArrayToFile(image, new byte[] {1, 2, 3});

        File target = new File(tmp.getRoot(), "pdf-source");
        AsciidocToPdfMojo.preparePdfSource(source, target, SITE, "openidm");

        assertThat(FileUtils.readFileToString(chapter, StandardCharsets.UTF_8)).isEqualTo(original);
        assertThat(new File(target, "getting-started/chap-where-to-go.adoc"))
                .hasContent("link:" + SITE + "/opendj/install-guide/index.html[OpenDJ] xref:#local[Local]");
        assertThat(new File(target, "partials/note.adoc"))
                .hasContent("link:" + SITE + "/openidm/getting-started/chap-x.html[X]");
        assertThat(new File(target, "images/logo.png")).hasBinaryContent(new byte[] {1, 2, 3});
    }
}
