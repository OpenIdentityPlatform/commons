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
 * Portions Copyrighted 2011-2013 ForgeRock AS.
 */

package org.forgerock.json.fluent;

// Java SE
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.fest.assertions.MapAssert.entry;
import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.util.promise.Function;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for JsonValue.
 */
@SuppressWarnings("javadoc")
public class JsonValueTest {

    /** JSON value encapsulating a list. */
    private JsonValue listValue;

    /** JSON value encapsulating a map. */
    private JsonValue mapValue;

    // ----- preparation ----------

    @BeforeMethod
    public void beforeMethod() {
        mapValue = new JsonValue(new HashMap<String, Object>());
        listValue = new JsonValue(new ArrayList<Object>());
    }

    // ----- manipulation tests ----------

    @Test
    public void testAddPermissiveJsonPointer() {
        final JsonValue value =
                json(object(field("uid", "bjensen"), field("roles", array("sales", "marketing"))));

        // New field.
        value.addPermissive(ptr("/email"), "bjensen@example.com");

        // Lazy create object.
        value.addPermissive(ptr("/contactDetails/mobile"), "+33 61234567");
        value.addPermissive(ptr("/contactDetails/fixed"), "+33 47654321");

        // Replace field (already exists).
        try {
            value.addPermissive(ptr("/uid"), "trigden");
            fail("Permissive add succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        // Insert array element.
        value.addPermissive(ptr("/roles/0"), "hr");

        // Lazy create array.
        value.addPermissive(ptr("/groups/-"), object(field("id", "managers-hr"), field(
                "displayName", "Human Resources Manager")));

        // Index out of range.
        try {
            value.addPermissive(ptr("/roles/10"), "it");
            fail("Permissive indexed put succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        // Leaf index into missing array.
        try {
            value.addPermissive(ptr("/missing/10"), "dummy");
            fail("Permissive indexed put succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        // Parent index into missing array.
        try {
            value.addPermissive(ptr("/groups/10/id"), "dummy");
            fail("Permissive indexed put succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        assertThat(value.get(ptr("/uid")).asString()).isEqualTo("bjensen");
        assertThat(value.get(ptr("/email")).asString()).isEqualTo("bjensen@example.com");
        assertThat(value.get(ptr("/contactDetails")).asMap()).hasSize(2).includes(
                entry("mobile", "+33 61234567"), entry("fixed", "+33 47654321"));
        assertThat(value.get(ptr("/roles")).asList()).containsExactly("hr", "sales", "marketing");
        assertThat(value.get(ptr("/groups")).asList())
                .containsExactly(
                        object(field("id", "managers-hr"), field("displayName",
                                "Human Resources Manager")));
    }

    @Test
    public void testAddToEndOfList() {
        final JsonValue value = json(array()).add("one").add("two").add("three");
        assertThat(value.asList()).containsExactly("one", "two", "three");
    }

    @Test
    public void testGetArrayPointer() {
        listValue.put(0, "x");
        listValue.put(1, "y");
        assertThat(listValue.get(new JsonPointer("/0")).getObject()).isEqualTo("x");
        assertThat(listValue.get(new JsonPointer("/1")).getObject()).isEqualTo("y");
        assertThat(listValue.get(new JsonPointer("/-"))).isNull();
    }

    @Test
    public void testGetMapPointer() {
        Map<String, Object> m = mapValue.asMap();
        m.put("a", (m = new HashMap<String, Object>()));
        m.put("b", (m = new HashMap<String, Object>()));
        m.put("c", "d");
        assertThat(mapValue.get(new JsonPointer("/a/b/c")).asString()).isEqualTo("d");
    }

    @Test
    public void testGetMultiDimensionalArrayPointer() {
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

    /**
     * Check {@link JsonValue#getObject()} hash code stability - see CREST-52.
     */
    @Test
    public void testHashCodeStability() {
        final JsonValue v1 = new JsonValue(new HashMap<String, Object>());
        v1.put("key1", "value1");
        v1.put("key2", "value2");

        // Reverse order, but should be equivalent.
        final JsonValue v2 = new JsonValue(new HashMap<String, Object>());
        v2.put("key2", "value2");
        v2.put("key1", "value1");
        assertThat(v1.getObject().hashCode()).isEqualTo(v2.getObject().hashCode());

        // Now add a sub-object.
        final Map<String, Object> o1 = new HashMap<String, Object>();
        o1.put("skey1", "svalue1");
        o1.put("skey2", "svalue2");
        v1.add("object", o1);
        assertThat(v1.getObject().hashCode()).isNotEqualTo(v2.getObject().hashCode());

        final Map<String, Object> o2 = new HashMap<String, Object>();
        o2.put("skey2", "svalue2");
        o2.put("skey1", "svalue1");
        v2.add("object", o2);
        assertThat(v1.getObject().hashCode()).isEqualTo(v2.getObject().hashCode());

        // Now add a sub-array.
        final List<Object> a1 = Arrays.<Object> asList("one", "two", "three");
        v1.add("array", a1);
        assertThat(v1.getObject().hashCode()).isNotEqualTo(v2.getObject().hashCode());

        final List<Object> a2 = Arrays.<Object> asList("one", "two", "three");
        v2.add("array", a2);
        assertThat(v1.getObject().hashCode()).isEqualTo(v2.getObject().hashCode());

        // Arrays are ordered, so mutations should change the hash code.
        Collections.reverse(a2);
        assertThat(v1.getObject().hashCode()).isNotEqualTo(v2.getObject().hashCode());

        // There and back again.
        Collections.reverse(a2);
        assertThat(v1.getObject().hashCode()).isEqualTo(v2.getObject().hashCode());
    }

    @Test
    public void testNumericIndexLeadingZeroes() {
        final List<Object> list = listValue.asList();
        list.add("a");
        list.add("b");
        list.add("c");
        listValue.put(3, "d");
        assertThat(listValue.get(new JsonPointer("/0003")).asString()).isEqualTo("d");
    }

    @Test
    public void testPutJsonPointer() throws Exception {

        final List<String> listObject1 = new ArrayList<String>(3);
        listObject1.add("valueA");
        listObject1.add("valueB");
        listObject1.add("valueC");

        final Map<String, Object> mapObject1 = new HashMap<String, Object>();
        mapObject1.put("keyE", "valueE");
        mapObject1.put("keyF", listObject1);
        mapObject1.put("keyG", "valueG");

        final Map<String, Object> mapObject2 = new HashMap<String, Object>();
        mapObject2.put("keyH", "valueH");
        mapObject2.put("keyI", "valueI");
        mapObject2.put("keyJ", mapObject1);

        final List<String> listObject2 = new ArrayList<String>(3);
        listObject2.add("valueD");
        listObject2.add("valueE");
        listObject2.add("valueF");

        final JsonValue listValue = new JsonValue(listObject1);
        final JsonValue mapValue = new JsonValue(new HashMap<String, Object>());

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
        assertThat(mapValue.get(new JsonPointer("/keyE/keyJ/keyF/2")).getObject()).isEqualTo(
                "testValueH");

        mapValue.put(new JsonPointer("/keyE/keyJ/keyF/-"), "testValueI");
        assertThat(mapValue.get(new JsonPointer("/keyE/keyJ/keyF/3")).getObject()).isEqualTo(
                "testValueI");
    }

    @Test
    public void testPutPermissiveJsonPointer() {
        final JsonValue value =
                json(object(field("uid", "bjensen"), field("roles", array("sales", "marketing"))));

        // New field.
        value.putPermissive(ptr("/email"), "trigden@example.com");

        // Lazy create object.
        value.putPermissive(ptr("/contactDetails/mobile"), "+33 61234567");
        value.putPermissive(ptr("/contactDetails/fixed"), "+33 47654321");

        // Replace field.
        value.putPermissive(ptr("/uid"), "trigden");

        // Replace array element.
        value.putPermissive(ptr("/roles/0"), "hr");

        // Lazy create array.
        value.putPermissive(ptr("/groups/-"), object(field("id", "managers-hr"), field(
                "displayName", "Human Resources Manager")));

        // Index out of range.
        try {
            value.putPermissive(ptr("/roles/10"), "it");
            fail("Permissive indexed put succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        // Leaf index into missing array.
        try {
            value.putPermissive(ptr("/missing/10"), "dummy");
            fail("Permissive indexed put succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        // Parent index into missing array.
        try {
            value.putPermissive(ptr("/groups/10/id"), "dummy");
            fail("Permissive indexed put succeeded unexpectedly");
        } catch (final JsonValueException ignored) {
            // Ignore.
        }

        assertThat(value.get(ptr("/uid")).asString()).isEqualTo("trigden");
        assertThat(value.get(ptr("/email")).asString()).isEqualTo("trigden@example.com");
        assertThat(value.get(ptr("/contactDetails")).asMap()).hasSize(2).includes(
                entry("mobile", "+33 61234567"), entry("fixed", "+33 47654321"));
        assertThat(value.get(ptr("/roles")).asList()).containsExactly("hr", "marketing");
        assertThat(value.get(ptr("/groups")).asList())
                .containsExactly(
                        object(field("id", "managers-hr"), field("displayName",
                                "Human Resources Manager")));
    }

    @Test
    public void testRemoveJsonPointer() {
        final JsonValue value =
                json(object(field("uid", "bjensen"), field("roles", array("hr", "sales",
                        "marketing"))));
        value.remove(ptr("/missing/token"));
        value.remove(ptr("/roles/1"));
        assertThat(value.get(ptr("/roles")).asList()).containsExactly("hr", "marketing");
        value.remove(ptr("/roles"));
        assertThat(value.isDefined("/roles")).isFalse();
        value.remove(ptr("/uid"));
        assertThat(value.isDefined("/uid")).isFalse();
    }

    @Test
    public void testAsURL() throws Exception {
        URL url = new URL("http://java.sun.com/index.html");
        JsonValue value = json("http://java.sun.com/index.html");
        assertThat(value.asURL()).isEqualTo(url);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsURLBad() throws Exception {
        json("asdf://nowhere").asURL();
    }

    private static final Function AS_INTEGER = new Function<JsonValue, Integer, Exception>() {
        @Override
        public Integer apply(JsonValue jsonValue) throws Exception {
            if (jsonValue.isString()) {
                return Integer.valueOf(jsonValue.asString());
            }
            throw new Exception("jsonValue value " + jsonValue.getObject() + " is not an integer");
        }
    };

    @Test
    public void testAsListTransformFunction() throws Exception {
        final JsonValue value = json(array("2", "3", "5", "8"));
        final List<Integer> list = value.asList(AS_INTEGER);
        assertThat(list.size()).isEqualTo(4);
        assertThat(list.get(0)).isEqualTo(2);
        assertThat(list.get(1)).isEqualTo(3);
        assertThat(list.get(2)).isEqualTo(5);
        assertThat(list.get(3)).isEqualTo(8);
    }

    @Test(expectedExceptions = Exception.class)
    public void testAsListTransformFunctionBadType() throws Exception {
        final JsonValue badValue = json(array("a", "b", "c"));
        badValue.asList(AS_INTEGER);
    }

    private JsonPointer ptr(final String pointer) {
        return new JsonPointer(pointer);
    }
}
