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
 * Copyright 2015 ForgeRock AS.
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
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("javadoc")
public class ProfilerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Map<String, String> inclusions;
    File includesDir;

    private Map<String, String> exclusions;
    File excludesDir;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        inclusions = new HashMap<>();
        inclusions.put("os", "linux unix");
        inclusions.put("condition", "release");
        File includes = new File(getClass().getResource("/unit/utils/profile/chapter-includes.xml").toURI());
        includesDir = new File(folder.getRoot(), "includes");
        if (!includesDir.mkdir()) {
            throw new IOException("Failed to create " + includesDir.getPath());
        }
        FileUtils.copyFileToDirectory(includes, includesDir);

        exclusions = new HashMap<>();
        exclusions.put("os", "linux unix");
        exclusions.put("condition", "draft");
        File excludes = new File(getClass().getResource("/unit/utils/profile/chapter-excludes.xml").toURI());
        excludesDir = new File(folder.getRoot(), "excludes");
        if (!excludesDir.mkdir()) {
            throw new IOException("Failed to create " + excludesDir.getPath());
        }
        FileUtils.copyFileToDirectory(excludes, excludesDir);
    }

    @Test
    public void shouldIncludeOsAsLinuxUnixAndConditionAsRelease() throws IOException {
        new Profiler(inclusions, null).applyProfiles(includesDir);

        File out = new File(includesDir, "chapter-includes.xml");
        String expectedXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<chapter>\n"
                        + " <title>Chapter</title>\n"
                        + " <para condition=\"release\">Release</para>\n"
                        + " <para os=\"linux\">Linux</para>\n"
                        + " <para os=\"unix\">UNIX</para>\n"
                        + "</chapter>";
        assertThat(contentOf(out)).isXmlEqualTo(expectedXml);
    }

    @Test
    public void shouldExcludeOsAsLinuxUnixAndConditionAsDraft() throws IOException {
        new Profiler(null, exclusions).applyProfiles(excludesDir);

        File out = new File(excludesDir, "chapter-excludes.xml");
        String expectedXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<chapter>\n"
                        + " <title>Chapter</title>\n"
                        + " <para condition=\"release\">Release</para>\n"
                        + " <para os=\"windows\">Windows</para>\n"
                        + "</chapter>";
        assertThat(contentOf(out)).isXmlEqualTo(expectedXml);
    }

    @After
    public void tearDown() {
        folder.delete();
    }
}
