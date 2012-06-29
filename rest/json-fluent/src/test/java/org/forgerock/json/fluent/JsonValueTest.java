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
 * Portions Copyrighted 2011 ForgeRock AS.
 */

package org.forgerock.json.fluent;

// Java SE
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// FEST-Assert
import static org.fest.assertions.Assertions.assertThat;

// TestNG
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author László Hordós
 * @author Paul C. Bryan
 */
public class JsonValueTest {

    /** JSON value encapsulating a map. */
    private JsonValue mapValue;

    /** JSON value encapsulating a list. */
    private JsonValue listValue;

    // ----- preparation ----------

    @BeforeMethod
    public void beforeMethod() {
        mapValue = new JsonValue(new HashMap());
        listValue = new JsonValue(new ArrayList());
    }

    // ----- manipulation tests ----------

    @Test
    @SuppressWarnings("unchecked")
    public void getMapPointer() {
        Map<String, Object> m = mapValue.asMap();
        m.put("a", (m = new HashMap<String, Object>()));
        m.put("b", (m = new HashMap<String, Object>()));
        m.put("c", "d");
        assertThat(mapValue.get(new JsonPointer("/a/b/c")).asString()).isEqualTo("d");
    }

    @Test
    public void getArrayPointer() {
        listValue.put(0, "x");
        listValue.put(1, "y");
        assertThat(listValue.get(new JsonPointer("/0")).getObject()).isEqualTo("x");
        assertThat(listValue.get(new JsonPointer("/1")).getObject()).isEqualTo("y");
    }

    @Test
    public void numericIndexLeadingZeroes() {
        List<Object> list = listValue.asList();
        list.add("a");
        list.add("b");
        list.add("c");
        listValue.put(3, "d");
        assertThat(listValue.get(new JsonPointer("/0003")).asString()).isEqualTo("d");
    }

    @Test
    public void getMultiDimensionalArrayPointer() {
        mapValue.put("a", new ArrayList<Object>());
        mapValue.get("a").put(0, new ArrayList<Object>());
        mapValue.get("a").get(0).put(0, "a00");
        mapValue.get("a").get(0).put(1, "a01");
        mapValue.get("a").put(1, new ArrayList<Object>());
        mapValue.get("a").get(1).put(0, "a10");
        mapValue.get("a").get(1).put(1, "a11");
        assertThat(mapValue.get(new JsonPointer("/a/0/0")).getObject()).isEqualTo("a00");
        assertThat(mapValue.get(new JsonPointer("/a/0/1")).getObject()).isEqualTo("a01");
        assertThat(mapValue.get(new JsonPointer("/a/1/0")).getObject()).isEqualTo("a10");
        assertThat(mapValue.get(new JsonPointer("/a/1/1")).getObject()).isEqualTo("a11");
    }

    @Test
    public void putJsonPointer() throws Exception {

        List<String> listObject1 = new ArrayList<String>(3);
        listObject1.add("valueA");
        listObject1.add("valueB");
        listObject1.add("valueC");

        Map<String, Object> mapObject1 = new HashMap<String, Object>();
        mapObject1.put("keyE", "valueE");
        mapObject1.put("keyF", listObject1);
        mapObject1.put("keyG", "valueG");

        Map<String, Object> mapObject2 = new HashMap<String, Object>();
        mapObject2.put("keyH", "valueH");
        mapObject2.put("keyI", "valueI");
        mapObject2.put("keyJ", mapObject1);

        List<String> listObject2 = new ArrayList<String>(3);
        listObject2.add("valueD");
        listObject2.add("valueE");
        listObject2.add("valueF");

        JsonValue listValue = new JsonValue(listObject1);
        JsonValue mapValue = new JsonValue(new HashMap());

        mapValue.put("keyA", "valueA");
        mapValue.put("keyB", "valueB");
        mapValue.put("keyC", "valueC");
        mapValue.add("keyD", listObject2);
        mapValue.add("keyE", mapObject2);

        mapValue.put(new JsonPointer("/keyA"), "testValueA");
        assertThat(mapValue.get(new JsonPointer("/keyA")).getObject()).isEqualTo("testValueA");

        listValue.put(new JsonPointer("/1"), "testValueA");
        assertThat(listValue.get(new JsonPointer("/1")).getObject()).isEqualTo("testValueA");

        mapValue.put(new JsonPointer("/keyD/0"), "testValueD");
        assertThat(mapValue.get(new JsonPointer("/keyD/0")).getObject()).isEqualTo("testValueD");

        mapValue.put(new JsonPointer("/keyE/keyH"), "testValueH");
        assertThat(mapValue.get(new JsonPointer("/keyE/keyH")).getObject()).isEqualTo("testValueH");

        mapValue.put(new JsonPointer("/keyE/keyJ/keyF/2"), "testValueH");
        assertThat(mapValue.get(new JsonPointer("/keyE/keyJ/keyF/2")).getObject()).isEqualTo("testValueH");
    }
}
