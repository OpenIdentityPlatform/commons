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

package org.forgerock.http.util;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.http.util.Paths.*;

import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PathsTest {
    @DataProvider
    private Object[][] validHexOctets() {
        // @formatter:off
        return new Object[][] {
            { "", "" },
            { "123", "123" },
            { "%31", "1" },
            { "%31%32", "12" },
            { "%31%32%33", "123" },
            { "%31%32%33aa", "123aa" },
            { "aa%31%32%33aa", "aa123aa" },
            { "surrogate pair %f0%90%80%80", "surrogate pair \ud800\udc00" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "validHexOctets")
    public void testUrlDecode(String s, String expected) {
        assertThat(urlDecode(s)).isEqualTo(expected);
    }

    @DataProvider
    private Object[][] encodings() {
        return new Object[][] {
            { "", "" },
            { "/", "%2F" },
            { "a/path with spaces", "a%2Fpath%20with%20spaces" },
        };
    }

    @Test(dataProvider = "encodings")
    public void testUrlEncode(String s, String expected) {
        assertThat(urlEncode(s)).isEqualTo(expected);
    }

    @DataProvider
    private Object[][] invalidHexOctets() {
        // @formatter:off
        return new Object[][] {
            { "%0" },
            { "%-1" },
            { "%pp" },
            { "%ap" },
            { "%-10" },
            { "%ff%" },
            { "%ff%f" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "invalidHexOctets", expectedExceptions = IllegalArgumentException.class)
    public void testUrlDecodeForInvalidData(String s) {
        urlDecode(s);
    }

    @DataProvider
    public Object[][] pathElements() {
        return new Object[][] {
            { null, new String[] {} },
            { "", new String[] {} },
            { "/", new String[] {} },
            { "//", new String[] { "", "" } },
            { "/one/two", new String[] { "one", "two" } },
            { "one/two", new String[] { "one", "two" } },
            { "//one//two//", new String[] { "", "one", "", "two", "", "" } },
        };
    }

    @Test(dataProvider = "pathElements")
    public void testPathElements(String rawPath, String[] elements) {
        assertThat(getPathElements(rawPath)).containsExactly(elements);
    }

    @DataProvider
    public Object[][] joinElements() {
        return new Object[][] {
            { null, "" },
            { EMPTY_LIST, "" },
            { singletonList("a"), "a" },
            { asList("path/string", "space separated"), "path%2Fstring/space%20separated" },
        };
    }

    @Test(dataProvider = "joinElements")
    public void testJoin(List<String> rawPath, String expected) {
        assertThat(joinPath(rawPath)).isEqualTo(expected);
    }
}
