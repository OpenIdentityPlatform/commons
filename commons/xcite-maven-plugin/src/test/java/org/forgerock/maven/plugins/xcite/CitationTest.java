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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.maven.plugins.xcite;

import static org.assertj.core.api.Assertions.*;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CitationTest {

    @Test
    public void valueOfAndToString() {
        String valid = "[/path/to/script.sh:# start:# end]";
        Citation citation = Citation.valueOf(valid);
        assertThat(citation.toString()).isEqualTo(valid);
    }

    @Test
    public void missingBracketsFail() {
        String invalid = "Gobble dee gook";
        assertThat(Citation.valueOf(invalid)).isNull();
    }

    @Test
    public void missingPathFail() {
        String invalid = "[:Gobble dee gook]";
        assertThat(Citation.valueOf(invalid)).isNull();
    }

    @Test
    public void extraMarkerFail() {
        String invalid = "[/test:start:end:middle]";
        assertThat(Citation.valueOf(invalid)).isNull();
    }

    @Test
    public void nullStartDelimiter() {
        String nullStart = "[/test:]";    // Technically wrong, but tolerable.
        Citation citation = Citation.valueOf(nullStart);
        assertThat(citation.toString()).isEqualTo("[/test]");
    }

    @Test
    public void successiveDelimitersFail() {
        String invalid = "[/test::]";
        assertThat(Citation.valueOf(invalid)).isNull();
    }

    @Test
    public void differentDelimiter() {
        String valid = "[/path/to/script.sh%# start%# end]";
        Citation citation = Citation.valueOf(valid);
        assertThat(citation.toString()).isEqualTo(valid);
    }

    @Test
    public void pathConstructor() {
        Citation citation = new Citation("/test");
        assertThat(citation.toString()).isEqualTo("[/test]");
    }

    @Test
    public void pathDelimiterAndStartConstructor() {
        Citation citation = new Citation("/test", '%', "marker");
        assertThat(citation.toString()).isEqualTo("[/test%marker%marker]");
    }

    @Test
    public void checkWindowsAbsPath() {
        Citation citation = Citation.valueOf("[C:\\test%start%end]", "%");
        assertThat(citation.getPath()).isEqualTo("C:\\test");
    }
}
