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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * Unit tests for JsonValue.
 */
@SuppressWarnings("javadoc")
public class JsonValueKeyAccessCheckerTest {

    private JsonValue value;

    @Test
    public void testIntValue() {
        Map<String, ?> m = Collections.singletonMap("field", 42);
        value = new JsonValue(m).recordKeyAccesses();
        assertFalse(value.get("field") instanceof JsonValueKeyAccessChecker);
    }

    @Test
    public void testStringValue() {
        Map<String, ?> m = Collections.singletonMap("field", "stringValue");
        value = new JsonValue(m).recordKeyAccesses();
        assertFalse(value.get("field") instanceof JsonValueKeyAccessChecker);
    }

    @Test
    public void testListValue() {
        Map<String, ?> m = Collections.singletonMap("field", Collections.emptyList());
        value = new JsonValue(m).recordKeyAccesses();
        assertTrue(value.get("field") instanceof JsonValueKeyAccessChecker);
    }

    @Test
    public void testMapValue() {
        Map<String, ?> m = Collections.singletonMap("field", Collections.emptyMap());
        value = new JsonValue(m).recordKeyAccesses();
        assertTrue(value.get("field") instanceof JsonValueKeyAccessChecker);
    }

    private Map<String, Object> buildMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("string", "value");
        map.put("int", 42);
        map.put("list", Arrays.asList("val1", "val2"));
        return map;
    }

    private Map<String, Object> buildSubMap() {
        final Map<String, Object> subMap = new HashMap<>();
        subMap.put("string", "subValue");
        subMap.put("int", 4242);
        subMap.put("list", Arrays.asList("subVal1", "subVal2"));
        return subMap;
    }

    @Test
    public void testThatAllKeysWereAccessed() {
        final Map<String, Object> map = buildMap();

        value = new JsonValue(map).recordKeyAccesses();
        assertFalse(value.get("string") instanceof JsonValueKeyAccessChecker);
        assertFalse(value.get("int") instanceof JsonValueKeyAccessChecker);
        assertTrue(value.get("list") instanceof JsonValueKeyAccessChecker);
        value.verifyAllKeysAccessed();
    }

    @Test
    public void testThatNotAllKeysWereAccessed() {
        final Map<String, Object> map = buildMap();

        value = new JsonValue(map).recordKeyAccesses();
        try {
            value.verifyAllKeysAccessed();
        } catch (JsonException e) {
            assertThat(e.getMessage()).isEqualTo("/: Unused keys: [int, list, string]");
        }
    }

    @Test
    public void testThatAllKeysWereAccessedIncludingSubValues() {
        final Map<String, Object> subMap = buildSubMap();
        final Map<String, Object> map = buildMap();
        map.put("map", subMap);

        value = new JsonValue(map).recordKeyAccesses();
        assertFalse(value.get("string") instanceof JsonValueKeyAccessChecker);
        assertFalse(value.get("int") instanceof JsonValueKeyAccessChecker);
        assertTrue(value.get("list") instanceof JsonValueKeyAccessChecker);
        final JsonValue subValue = value.get("map").required();
        assertTrue(subValue instanceof JsonValueKeyAccessChecker);
        assertFalse(subValue.get("string") instanceof JsonValueKeyAccessChecker);
        assertFalse(subValue.get("int") instanceof JsonValueKeyAccessChecker);
        assertTrue(subValue.get("list") instanceof JsonValueKeyAccessChecker);
        value.verifyAllKeysAccessed();
    }

    @Test
    public void testThatBasicKeysAccessedThroughIteratorAreChecked() {
        final Map<String, Object> map = new HashMap<>();
        map.put("string", "value");
        map.put("int", 42);

        value = new JsonValue(map).recordKeyAccesses();
        for (JsonValue val : value) {
            assertFalse(val instanceof JsonValueKeyAccessChecker);
        }
        value.verifyAllKeysAccessed();
    }

    @Test
    public void testThatSpecialKeysAccessedThroughIteratorAreChecked() {
        final Map<String, Object> map = new HashMap<>();
        map.put("list", Collections.emptyList());
        map.put("map", Collections.emptyMap());

        value = new JsonValue(map).recordKeyAccesses();
        for (JsonValue val : value) {
            assertTrue(val instanceof JsonValueKeyAccessChecker);
        }
        value.verifyAllKeysAccessed();
    }

    @Test
    public void testThatNotAllKeysWereAccessedInSubValues() {
        final Map<String, Object> subMap = buildSubMap();
        final Map<String, Object> map = buildMap();
        map.put("map", subMap);

        value = new JsonValue(map).recordKeyAccesses();
        assertFalse(value.get("string") instanceof JsonValueKeyAccessChecker);
        assertFalse(value.get("int") instanceof JsonValueKeyAccessChecker);
        assertTrue(value.get("list") instanceof JsonValueKeyAccessChecker);
        assertTrue(value.get("map") instanceof JsonValueKeyAccessChecker);
        try {
            value.verifyAllKeysAccessed();
        } catch (JsonException e) {
            assertThat(e.getMessage()).isEqualTo("/map: Unused keys: [int, list, string]");
        }
    }

    @Test
    public void testThatWhenParentKeysAreReportedSubKeysAreNot() {
        final Map<String, Object> subMap = buildSubMap();
        final Map<String, Object> map = buildMap();
        map.put("map", subMap);

        value = new JsonValue(map).recordKeyAccesses();
        try {
            value.verifyAllKeysAccessed();
        } catch (JsonException e) {
            assertThat(e.getMessage()).isEqualTo("/: Unused keys: [int, list, map, string]");
        }
    }

    @Test
    public void testThatNoKeysWereAccessedReportsOrderedJsonPointers() {
        final Map<String, Object> subMap = buildSubMap();
        final Map<String, Object> map = buildMap();
        map.put("map", subMap);

        value = new JsonValue(map).recordKeyAccesses();
        value.get("map");
        try {
            value.verifyAllKeysAccessed();
        } catch (JsonException e) {
            assertThat(e.getMessage()).isEqualTo("/: Unused keys: [int, list, string]" + "\n"
                            + "/map: Unused keys: [int, list, string]");
        }
    }

}
