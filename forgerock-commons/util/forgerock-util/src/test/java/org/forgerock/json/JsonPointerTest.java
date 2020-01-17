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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2010–2011 ApexIdentity Inc. All rights reserved.
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package org.forgerock.json;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test JsonPointer.
 */
@SuppressWarnings("javadoc")
public class JsonPointerTest {

    // ----- parsing unit tests ----------

    @Test
    public void identicalPathEquality() {
        JsonPointer p1 = new JsonPointer("/a/b/c");
        JsonPointer p2 = new JsonPointer("/a/b/c");
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void differentPathInequality() {
        JsonPointer p1 = new JsonPointer("/a/b/c");
        JsonPointer p2 = new JsonPointer("/d/e/f");
        assertThat((Object) p1).isNotEqualTo(p2);
    }

    @Test
    public void simpleEscape() throws JsonException {
        JsonPointer p1 = new JsonPointer("/a/%65%73%63%61%70%65");
        JsonPointer p2 = new JsonPointer("/a/escape");
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void parseVsStringRootEquality1() {
        JsonPointer p1 = new JsonPointer("");
        JsonPointer p2 = new JsonPointer();
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void parseVsStringRootEqualityTrailingSlash() {
        JsonPointer p1 = new JsonPointer("/");
        JsonPointer p2 = new JsonPointer();
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void parseVsStringChildEquality() {
        JsonPointer p1 = new JsonPointer("/a/b/c");
        JsonPointer p2 = new JsonPointer().child("a").child("b").child("c");
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void parseVsStringChildEqualityTrailingSlash() {
        JsonPointer p1 = new JsonPointer("/a/b/c/");
        JsonPointer p2 = new JsonPointer().child("a").child("b").child("c");
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void parseVsIntegerChildEquality() {
        JsonPointer p1 = new JsonPointer("/1/2");
        JsonPointer p2 = new JsonPointer().child(1).child(2);
        assertThat((Object) p1).isEqualTo(p2);
    }

    @Test
    public void slashEncoded() {
        JsonPointer p1 = new JsonPointer().child("a/b").child("c");
        assertThat(p1.toString()).isEqualTo("/a%2Fb/c");
    }

    @Test
    public void relativePointer0() {
        JsonPointer p = new JsonPointer("/");
        assertThat((Object) p.relativePointer()).isEqualTo(new JsonPointer("/"));
    }

    @Test
    public void relativePointer1() {
        JsonPointer p = new JsonPointer("/a");
        assertThat((Object) p.relativePointer()).isEqualTo(new JsonPointer("/"));
    }

    @Test
    public void relativePointer2() {
        JsonPointer p = new JsonPointer("/a/b");
        assertThat((Object) p.relativePointer()).isEqualTo(new JsonPointer("/b"));
    }

    @Test
    public void relativePointer3() {
        JsonPointer p = new JsonPointer("/a/b/c");
        assertThat((Object) p.relativePointer()).isEqualTo(new JsonPointer("/b/c"));
    }

    @Test
    public void relativePointerOffset0() {
        JsonPointer p = new JsonPointer("/");
        assertThat((Object) p.relativePointer(0)).isSameAs(p);
    }

    @Test
    public void relativePointerOffset10() {
        JsonPointer p = new JsonPointer("/a");
        assertThat((Object) p.relativePointer(0)).isEqualTo(new JsonPointer("/"));
    }

    @Test
    public void relativePointerOffset11() {
        JsonPointer p = new JsonPointer("/a");
        assertThat((Object) p.relativePointer(1)).isSameAs(p);
    }

    @Test
    public void relativePointerOffset20() {
        JsonPointer p = new JsonPointer("/a/b");
        assertThat((Object) p.relativePointer(0)).isEqualTo(new JsonPointer("/"));
    }

    @Test
    public void relativePointerOffset21() {
        JsonPointer p = new JsonPointer("/a/b");
        assertThat((Object) p.relativePointer(1)).isEqualTo(new JsonPointer("/b"));
    }

    @Test
    public void relativePointerOffset22() {
        JsonPointer p = new JsonPointer("/a/b");
        assertThat((Object) p.relativePointer(2)).isSameAs(p);
    }

    @Test
    public void relativePointerOffset30() {
        JsonPointer p = new JsonPointer("/a/b/c");
        assertThat((Object) p.relativePointer(0)).isEqualTo(new JsonPointer("/"));
    }

    @Test
    public void relativePointerOffset31() {
        JsonPointer p = new JsonPointer("/a/b/c");
        assertThat((Object) p.relativePointer(1)).isEqualTo(new JsonPointer("/c"));
    }

    @Test
    public void relativePointerOffset32() {
        JsonPointer p = new JsonPointer("/a/b/c");
        assertThat((Object) p.relativePointer(2)).isEqualTo(new JsonPointer("/b/c"));
    }

    @Test
    public void relativePointerOffset33() {
        JsonPointer p = new JsonPointer("/a/b/c");
        assertThat((Object) p.relativePointer(3)).isSameAs(p);
    }

    @DataProvider(name = "validJsonPointers")
    public Object[][] getValidJsonPointers() {
        return new Object[][] {
            { "/" }, { "/a" }, { "/a/b" }, { "/a/b/c" },
            { "/a/b/c/%2F" }, { "/a/b/c/%2f" },
        };
    }

    @Test(dataProvider = "validJsonPointers")
    public void toString(final String pointer) {
        JsonPointer p = new JsonPointer(pointer);
    // with URI encoded data, "%2f" and "%2F" are equivalent
        assertThat(p.toString()).isEqualTo(pointer.replace("%2f", "%2F"));
    }

    // ----- exception unit tests ----------

    @Test(expectedExceptions = JsonException.class)
    public void uriSyntaxException() throws JsonException {
        new JsonPointer("%%%");
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void relativePathOffsetNegative() {
        new JsonPointer("/a/b/c").relativePointer(-1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void relativePathOffsetTooLarge() {
        new JsonPointer("/a/b/c").relativePointer(4);
    }
}
