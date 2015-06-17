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

package org.forgerock.json.fluent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.testng.Assert.fail;

import org.forgerock.util.Function;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        mapValue = new JsonValue(new HashMap<>());
        listValue = new JsonValue(new ArrayList<>());
    }

    // ----- basics ----------

    @Test
    public void shouldFieldBeAddedToJsonObject() {
        final JsonValue jv = json(object(field("uid", "bjensen"),
                                         field("age", 30),
                                         field("nullField", null)));
        assertThat(jv.get("uid").asString()).isEqualTo("bjensen");
        assertThat(jv.get("age").asInteger()).isEqualTo(30);
        assertThat(jv.isDefined("nullField")).isTrue();
    }

    @Test
    public void shouldFieldIfNotNullDoNotBeAddedToJsonObject() {
        final JsonValue jv = json(object(field("uid", "bjensen"),
                                         field("age", 30),
                                         fieldIfNotNull("nullField", null)));
        assertThat(jv.get("uid").asString()).isEqualTo("bjensen");
        assertThat(jv.get("age").asInteger()).isEqualTo(30);
        assertThat(jv.isDefined("nullField")).isFalse();
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
        assertThat(value.get(ptr("/contactDetails")).asMap()).hasSize(2).contains(
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
    public void testAddToEndOfSet() {
        final JsonValue value = json(set()).add("one").add("two").add("three");
        assertThat(value.asSet()).containsOnly("one", "two", "three");
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAddOnNonCollection() {
        json(object(field("hello", "world"))).add("one");
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
        m.put("a", (m = new HashMap<>()));
        m.put("b", (m = new HashMap<>()));
        m.put("c", "d");
        assertThat(mapValue.get(new JsonPointer("/a/b/c")).asString()).isEqualTo("d");
    }

    @Test
    public void testGetMultiDimensionalArrayPointer() {
        mapValue.put("a", new ArrayList<>());
        mapValue.get("a").put(0, new ArrayList<>());
        mapValue.get("a").get(0).put(0, "a00");
        mapValue.get("a").get(0).put(1, "a01");
        mapValue.get("a").put(1, new ArrayList<>());
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
        final JsonValue v1 = new JsonValue(new HashMap<>());
        v1.put("key1", "value1");
        v1.put("key2", "value2");

        // Reverse order, but should be equivalent.
        final JsonValue v2 = new JsonValue(new HashMap<>());
        v2.put("key2", "value2");
        v2.put("key1", "value1");
        assertThat(v1.getObject().hashCode()).isEqualTo(v2.getObject().hashCode());

        // Now add a sub-object.
        final Map<String, Object> o1 = new HashMap<>();
        o1.put("skey1", "svalue1");
        o1.put("skey2", "svalue2");
        v1.add("object", o1);
        assertThat(v1.getObject().hashCode()).isNotEqualTo(v2.getObject().hashCode());

        final Map<String, Object> o2 = new HashMap<>();
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

        final List<String> listObject1 = new ArrayList<>(3);
        listObject1.add("valueA");
        listObject1.add("valueB");
        listObject1.add("valueC");

        final Map<String, Object> mapObject1 = new HashMap<>();
        mapObject1.put("keyE", "valueE");
        mapObject1.put("keyF", listObject1);
        mapObject1.put("keyG", "valueG");

        final Map<String, Object> mapObject2 = new HashMap<>();
        mapObject2.put("keyH", "valueH");
        mapObject2.put("keyI", "valueI");
        mapObject2.put("keyJ", mapObject1);

        final List<String> listObject2 = new ArrayList<>(3);
        listObject2.add("valueD");
        listObject2.add("valueE");
        listObject2.add("valueF");

        final JsonValue listValue = new JsonValue(listObject1);
        final JsonValue mapValue = new JsonValue(new HashMap<>());

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
        assertThat(value.get(ptr("/contactDetails")).asMap()).hasSize(2).contains(
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

    @Test
    public void testAsCollectionOfUnTyped() {
        // test List as Collection
        Collection<Integer> list = json(array(2, 3, 5, 8)).asCollection(Integer.class);
        assertThat(list.size()).isEqualTo(4);
        assertThat(list).containsOnly(2, 3, 5, 8);

        // test Set as Collection
        Collection<Integer> set = json(set(2, 3, 5, 8)).asCollection(Integer.class);
        assertThat(set.size()).isEqualTo(4);
        assertThat(set).containsOnly(2, 3, 5, 8);
    }

    @Test
    public void testAsCollectionOfType() {
        // test List as Collection
        Collection<Integer> list = json(array(2, 3, 5, 8)).asCollection(Integer.class);
        assertThat(list.size()).isEqualTo(4);
        assertThat(list).containsOnly(2, 3, 5, 8);

        // test Set as Collection
        Collection<Integer> set = json(set(2, 3, 5, 8)).asCollection(Integer.class);
        assertThat(set.size()).isEqualTo(4);
        assertThat(set).containsOnly(2, 3, 5, 8);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsCollectionOfBadType() {
        json(array(2, 3, 5, 8)).asCollection(String.class);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsCollectionOfBadElementType() {
        json(array(2, 3, "5", 8)).asCollection(Integer.class);
    }

    @Test
    public void testAsListOfType() {
        List<Integer> list = json(array(2, 3, 5, 8)).asList(Integer.class);
        assertThat(list.size()).isEqualTo(4);
        assertThat(list.get(0)).isEqualTo(2);
        assertThat(list.get(1)).isEqualTo(3);
        assertThat(list.get(2)).isEqualTo(5);
        assertThat(list.get(3)).isEqualTo(8);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsListOfBadType() {
        json(array(2, 3, 5, 8)).asList(String.class);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsListOfBadElementType() {
        json(array(2, 3, "5", 8)).asList(Integer.class);
    }

    private static final Function<JsonValue, Integer, Exception> AS_INTEGER =
            new Function<JsonValue, Integer, Exception>() {
        @Override
        public Integer apply(JsonValue jsonValue) throws Exception {
            if (jsonValue.isString()) {
                return Integer.valueOf(jsonValue.asString());
            }
            throw new JsonValueException(jsonValue, "jsonValue value " + jsonValue.getObject() + " is not an integer");
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

    @Test
    public void testAsSetOfType() {
        Set<Integer> set = json(set(2, 3, 5, 8)).asSet(Integer.class);
        assertThat(set.size()).isEqualTo(4);
        assertThat(set).containsOnly(2, 3, 5, 8);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsSetOfBadType() {
        json(set(2, 3, 5, 8)).asSet(String.class);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsSetOfBadElementType() {
        json(set(2, 3, "5", 8)).asSet(Integer.class);
    }

    @Test
    public void testAsSetTransformFunction() throws Exception {
        final JsonValue value = json(set("2", "3", "5", "8"));
        final Set<Integer> set = value.asSet(AS_INTEGER);
        assertThat(set.size()).isEqualTo(4);
        assertThat(set).containsOnly(2, 3, 5, 8);
    }

    @Test(expectedExceptions = Exception.class)
    public void testAsSetTransformFunctionBadType() throws Exception {
        final JsonValue badValue = json(set("a", "b", "c"));
        badValue.asSet(AS_INTEGER);
    }

    @Test
    public void testAsMapOf() {
        Map<String, Object> m = mapValue.asMap();
        m.put("a", "aString");
        m.put("b", "bString");
        m.put("c", "cString");
        Map<?, ?> stringMap = mapValue.asMap(String.class);
        assertThat(stringMap.get("a") instanceof String).isTrue();
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsMapOfBad() {
        Map<String, Object> m = mapValue.asMap();
        m.put("a", "aString");
        m.put("b", true);
        m.put("c", 4);
        Map<?, ?> stringMap = mapValue.asMap(String.class);
        assertThat(stringMap.get("a") instanceof String).isTrue();
    }

    @Test
    public void testAsMapOfList() {
        listValue.add("String");
        Map<String, Object> m = mapValue.asMap();
        m.put("a", listValue.getObject());
        Map<String, ?> stringListMap = mapValue.asMapOfList(String.class);
        assertThat(stringListMap.get("a") instanceof List).isTrue();
        assertThat(((List<?>) stringListMap.get("a")).get(0)).isEqualTo("String");
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsMapOfListNonList() {
        Map<String, Object> m = mapValue.asMap();
        m.put("a", true);
        mapValue.asMapOfList(String.class);
    }

    @Test(expectedExceptions = JsonValueException.class)
    public void testAsMapOfListBadElement() {
        listValue.add(true);
        listValue.add(false);
        Map<String, Object> m = mapValue.asMap();
        m.put("a", listValue.getObject());
        mapValue.asMapOfList(String.class);
    }

    @Test
    public void testToStringOfList() {
        final JsonValue value = json(array()).add("one").add("two").add("three");
        assertThat(value.toString()).matches("\\[\\s*\"one\"\\s*,\\s*\"two\"\\s*,\\s*\"three\"\\s*\\]");
    }

    @Test
    public void testToStringOfSet() {
        final JsonValue value = json(set()).add("one").add("two").add("three").add("four").add("five");
        String s = value.toString();
        // do our best to test containment and presence of values since sets are unordered
        assertThat(s).startsWith("[");
        assertThat(s).endsWith("]");
        assertThat(s.substring(1, s.length() - 1)).doesNotContain("[");
        assertThat(s.substring(1, s.length() - 1)).doesNotContain("]");
        assertThat(s.contains("\"one\""));
        assertThat(s.contains("\"two\""));
        assertThat(s.contains("\"three\""));
        assertThat(s.contains("\"four\""));
        assertThat(s.contains("\"five\""));
    }

    @Test
    public void toStringShouldEscapeSpecialCharacters() {
        final JsonValue value =
                json(object(field("a \"silly\" key", "value containing a \\ and a \" and "
                        + "some controls \b\f\n\r\t\u0000\u001f\u007f\u009f")));
        assertThat(value.toString()).isEqualTo(
                "{ \"a \\\"silly\\\" key\": \"value containing a \\\\ and a \\\" and "
                        + "some controls \\b\\f\\n\\r\\t\\u0000\\u001F\\u007F\\u009F\" }");
    }

    @Test
    public void testCoerceListToSet() {
        final JsonValue value = json(array()).add("2").add("3").add("5").add("2");
        assertThat(value.isList()).isTrue();
        assertThat(value.isSet()).isFalse();
        assertThat(value.asSet().size()).isEqualTo(3); // Set has no duplicates
        assertThat(value.asSet()).containsOnly("2", "3", "5");
    }

    @Test
    public void testCoerceSetToList() {
        final JsonValue value = json(set()).add("2").add("3").add("5").add("8");
        assertThat(value.isList()).isFalse();
        assertThat(value.isSet()).isTrue();
        assertThat(value.asList().size()).isEqualTo(4);
        assertThat(value.asList()).containsOnly("2", "3", "5", "8");
    }

    @DataProvider
    private Object[][] shouldIterateChildElementsInOrderData() {
        // @formatter:off
        return new Object[][] {
            { field("key-1", "value-1"), field("key-2", "value-2") },
            { field("key-2", "value-2"), field("key-1", "value-1") }
        };
        // @formatter:on
    }

    @Test(dataProvider = "shouldIterateChildElementsInOrderData")
    public void shouldIterateChildElementsInOrder(Map.Entry<String, Object> field1,
            Map.Entry<String, Object> field2) throws Exception {
        JsonValue iterable = json(object(field1, field2));
        Iterator<JsonValue> iterator = iterable.iterator();
        assertThat(iterator.next().asString()).isEqualTo(field1.getValue().toString());
        assertThat(iterator.next().asString()).isEqualTo(field2.getValue().toString());
        assertThat(iterator.hasNext()).isFalse();
    }

    private JsonPointer ptr(final String pointer) {
        return new JsonPointer(pointer);
    }
}
