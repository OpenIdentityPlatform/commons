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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.patch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JsonPatchTest {

    /** JSON value encapsulating a map. */
    private JsonValue mapValue;

    private JsonValue v1;

    private JsonValue v2;

    private JsonValue diff;

    // ----- preparation ----------

    @BeforeMethod
    public void beforeMethod() {
        mapValue = new JsonValue(new HashMap<>());
        v1 = null;
        v2 = null;
    }

    // ----- happy path unit tests ----------

    @Test
    public void removeMapItem() {
        v1 = mapValue;
        v1.put("a", "1");
        v1.put("b", "2");
        v2 = v1.copy();
        v2.remove("b");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
        assertThat(v1.isDefined("b")).isFalse();
    }

    @Test
    public void addMapItem() {
        v1 = mapValue;
        v1.put("a", "b");
        v2 = v1.copy();
        v2.put("c", "d");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        JsonPatch.patch(v1, diff);
        assertThat(v1.get("c").getObject()).isEqualTo("d");
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
    }

    @Test
    public void replaceMapItem() {
        v1 = mapValue;
        v1.put("a", "b");
        v1.put("c", "d");
        v2 = v1.copy();
        v2.put("a", "e");
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(1);
        JsonPatch.patch(v1, diff);
        assertThat(v1.get("a").getObject()).isEqualTo("e");
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
    }

    @Test
    public void mapDiffNoChanges() {
        v1 = mapValue;
        v1.put("foo", "bar");
        v1.put("boo", "far");
        v2 = v1.copy();
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(0);
        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
    }

    @Test
    public void listDiffNoChanges() {
        v1 = mapValue;
        v1.put("a", new ArrayList<>());
        v1.get("a").put(0, "foo");
        v1.get("a").put(1, "bar");
        v2 = v1.copy();
        diff = JsonPatch.diff(v1, v2);
        assertThat(diff.size()).isEqualTo(0);
        JsonPatch.patch(v1, diff);
        assertThat(JsonPatch.diff(v1, v2).size()).isEqualTo(0);
    }

    // ----- exception unit tests ----------

    @Test(expectedExceptions=JsonValueException.class)
    public void replaceNonExistentMapItem() {
        v1 = mapValue;
        v1.put("a", "1");
        v2 = v1.copy();
        v2.put("a", "2");
        diff = JsonPatch.diff(v1, v2);
        v1.clear();
        JsonPatch.patch(v1, diff);
    }

    @Test(expectedExceptions=JsonValueException.class)
    public void addExistentMapItem() {
        v1 = mapValue;
        v2 = v1.copy();
        v2.put("a", "b");
        diff = JsonPatch.diff(v1, v2);
        v1 = v2.copy();
        JsonPatch.patch(v1, diff);
    }

    @Test(expectedExceptions=JsonValueException.class)
    public void removeNonExistentMapItem() {
        v1 = mapValue;
        v1.put("a", "1");
        v2 = v1.copy();
        v2.clear();
        diff = JsonPatch.diff(v1, v2);
        v1.clear();
        JsonPatch.patch(v1, diff);
    }
}
