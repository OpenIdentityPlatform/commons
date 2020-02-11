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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;

import org.testng.annotations.Test;

public class JsonPatchTest {

    private JsonValue v1;
    private JsonValue v2;
    private JsonValue diff;

    // ----- happy path unit tests ----------

    @Test
    public void removeMapItem() {
        v1 = json(object(
                field("a", "1"),
                field("b", "2")
        ));
        v2 = v1.copy();
        v2.remove("b");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(v1.isDefined("b")).isFalse();
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void addMapItem() {
        v1 = json(object(
                field("a", "b")
        ));
        v2 = v1.copy();
        v2.put("c", "d");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(v1.get("c").getObject()).isEqualTo("d");
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void addMapItem2() {
        v1 = json(object(
                field("a", array(
                        "1",
                        "2",
                        "3"
            ))
        ));

        diff = json(array(object(
                field("op", "add"),
                field("path", "/a"),
                field("value", array(
                        "x",
                        "y"
            ))
        )));

        JsonPatch.patch(v1, diff);
        assertThat(v1.get("a").asList().size()).isEqualTo(4);
    }

    @Test
    public void replaceMapItem() {
        v1 = json(object(
                field("a", "b"),
                field("c", "d")
        ));
        v2 = v1.copy();
        v2.put("a", "e");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(v1.get("a").getObject()).isEqualTo("e");
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void moveMapItem() {
        v1 = json(object(
                field("a", "b"),
                field("c", "d")
        ));
        diff = json(array(object(
                field("op", "move"),
                field("from", "/c"),
                field("path", "/e")
        )));
        JsonPatch.patch(v1, diff);
        assertThat(v1.get("e").getObject()).isEqualTo("d");
        assertThat(v1.get("c").getObject()).isNull();
    }

    @Test
    public void copyMapItem() {
        v1 = json(object(
                field("a", "b"),
                field("c", "d")
        ));
        diff = json(array(object(
                field("op", "copy"),
                field("from", "/c"),
                field("path", "/e")
        )));
        JsonPatch.patch(v1, diff);
        assertThat(v1.get("e").getObject()).isEqualTo("d");
        assertThat(v1.get("c").getObject()).isEqualTo("d");
    }

    @Test
    public void testMapItem() {
        v1 = json(object(
                field("a", "b"),
                field("c", 10)
        ));
        diff = json(
                array(
                        object(
                            field("op", "test"),
                            field("path", "/a"),
                            field("value", "b")
                ), object(
                            field("op", "test"),
                            field("path", "/c"),
                            field("value", 10)
                ))
        );
        JsonPatch.patch(v1, diff);
    }

    @Test
    public void mapNoChanges() {
        v1 = json(object(
                field("foo", "bar"),
                field("boo", "far")
        ));
        v2 = v1.copy();
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void mapDifferentSizes() {
        v1 = json(object(
                field("foo", "bar"),
                field("boo", "far")
        ));
        v2 = json(object(
                field("foo", "bar"),
                field("boo", "far"),
                field("bar", "baz")
        ));
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        // different size maps should "fail fast" when testing equality
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void mapSameSizeDifferentKeys() {
        v1 = json(object(
                field("foo", "bar"),
                field("boo", "far")
        ));
        v2 = json(object(
                field("foo", "bar"),
                field("bar", "baz")
        ));
        diff = JsonPatch.diff(v1, v2);
        // diff should contain a remove of one key-value pair and an add of the new one
        assertThat(diff.size()).isEqualTo(2);
        // same-size maps with different keys will have to be traversed when testing equality
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void listDiffNoChanges() {
        v1 = json(object(
                field("a", array(
                        "foo",
                        "bar"
            ))
        ));
        v2 = v1.copy();
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void listDiffDifferentSizes() {
        v1 = json(array("a", "b", "c"));
        v2 = v1.copy();
        v2.add("d");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void listDiffDifferentElements() {
        v1 = json(array("a", "b", "c"));
        v2 = json(array("a", "b", "d"));
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();

        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void differentTypesNotEqual() {
        v1 = json(array("a", "b", "c"));
        v2 = json(object(field("foo", "bar")));

        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();
    }

    @Test
    public void oneNullNotEqual() {
        v1 = json(null);
        v2 = json(object());

        assertThat(JsonPatch.isEqual(v1, v2)).isFalse();
        assertThat(JsonPatch.isEqual(v2, v1)).isFalse();
    }

    @Test
    public void twoNullAreEqual() {
        v1 = json(null);
        v2 = json(null);

        assertThat(JsonPatch.isEqual(v1, v2)).isTrue();
    }

    @Test
    public void testScriptedReplace() {
        v1 = json(object(
                field("a", "b")
        ));
        diff = json(array(object(
                field("op", "replace"),
                field("path", "/a"),
                field("script", "var source = content.a; var target = source + 'xformed'; target;")
        )));
        JsonPatch.patch(v1, diff, new JsonPatchJavascriptValueTransformer());
        assertThat(v1.get("a").asString()).isEqualTo("bxformed");
    }

    // ----- exception unit tests ----------

    @Test(expectedExceptions = JsonValueException.class)
    public void replaceNonExistentMapItem() {
        v1 = json(object(
                field("a", "1")
        ));
        v2 = v1.copy();
        v2.put("a", "2");
        diff = JsonPatch.diff(v1, v2);
        v1.clear();
        JsonPatch.patch(v1, diff);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void addExistentMapItem() {
        v1 = json(object());
        v2 = v1.copy();
        v2.put("a", "b");
        diff = JsonPatch.diff(v1, v2);
        v1 = v2.copy();
        JsonPatch.patch(v1, diff);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void removeNonExistentMapItem() {
        v1 = json(object(
                field("a", "1")
        ));
        v2 = v1.copy();
        v2.clear();
        diff = JsonPatch.diff(v1, v2);
        v1.clear();
        JsonPatch.patch(v1, diff);
    }
}
