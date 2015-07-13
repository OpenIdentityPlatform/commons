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

package org.forgerock.audit.util;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Contains some JsonValue Utility methods.
 */
public final class JsonValueUtils {

    private JsonValueUtils() {

    }

    /**
     * Expands a Json Object Map, where the keys of the map are the {@link JsonPointer JsonPointer }s
     * and the values are the value the {@link JsonPointer JsonPointer } resolves to.
     *
     * For Example, the following key-value pairs
     * <pre>
     *      /object/array/0 , "test"
     *      /object/array/1 , "test1"
     *      /string         , "stringVal"
     *      /boolean        , false
     *      /number         , 1
     *      /array/0        , "value1"
     *      /array/1        , "value2"
     * </pre>
     *
     * will produce the following json object
     * <pre>
     *     {
     *         "object" : {
     *             "array" : ["test", "test1"]
     *         },
     *         "string" : "stringVal",
     *         "boolean" : false,
     *         "number" : 1,
     *         "array" : ["value1", "value2"]
     *     }
     * </pre>
     * @param object the Json Object Map containing the {@link JsonPointer JsonPointer }s and values
     * @return the {@link JsonValue JsonValue } expanded from the object map
     */
    public static JsonValue expand(final Map<String, Object> object) {
        //sort the objects so the array objects are in order
        final Map<String, Object> sortedObjects = new TreeMap<>(object);
        return buildObject(sortedObjects);
    }

    /**
     *
     * Flattens a {@link JsonValue JsonValue } to a Map, where the keys of the Map are {@link JsonPointer JsonPointer }s
     * and the values are the value the {@link JsonPointer JsonPointer }s resolve to.
     *
     * For Example, the following JsonValue
     *
     * <pre>
     *     {
     *         "object" : {
     *             "array" : ["test", "test1"]
     *         },
     *         "string" : "stringVal",
     *         "boolean" : false,
     *         "number" : 1,
     *         "array" : ["value1", "value2"]
     *     }
     * </pre>
     *
     * will produce the following Map key-value pairs
     *
     *  <pre>
     *      /object/array/0 , "test"
     *      /object/array/1 , "test1"
     *      /string         , "stringVal"
     *      /boolean        , false
     *      /number         , 1
     *      /array/0        , "value1"
     *      /array/1        , "value2"
     * </pre>
     * @param jsonValue the {@link JsonValue JsonValue } object to flatten
     * @return a Map representing the flattened {@link JsonValue JsonValue } object
     */
    public static Map<String, Object> flatten(final JsonValue jsonValue) {
        Map<String, Object> flatObject = new LinkedHashMap<>();
        flatten(new JsonPointer(), jsonValue, flatObject);
        return flatObject;
    }

    private static JsonValue buildObject(Map<String, Object> objectSet) {
        final JsonValue jsonValue = new JsonValue(new LinkedHashMap<>());
        for (Map.Entry<String, Object> entry : objectSet.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (jsonValue.get(new JsonPointer(key)) != null
                    && !jsonValue.get(new JsonPointer(key)).isNull()) {
                //only build a sub json object for one prefix value
                continue;
            }
            final JsonPointer jsonPointer = new JsonPointer(key);
            int numberOfIndexTokens = getIndexTokens(jsonPointer);
            if (numberOfIndexTokens > 1) {
                //more than one json array must build the sub json object
                int firstIndexTokenPos = getNextIndexToken(jsonPointer, 0);
                final JsonPointer prefix = subJsonPointer(jsonPointer, 0, firstIndexTokenPos + 1);
                jsonValue.putPermissive(
                        replaceLastIndexToken(prefix),
                        buildObject(findObjectsThatMatchPrefix(prefix, objectSet)).getObject()
                );
            } else {
                jsonValue.putPermissive(replaceLastIndexToken(jsonPointer), value);
            }
        }
        return jsonValue;
    }

    private static Map<String, Object> findObjectsThatMatchPrefix(
            final JsonPointer prefix,
            Map<String, Object> objectSet) {
        Map<String, Object> matchingObjects = new LinkedHashMap<>();
        for (final String key : objectSet.keySet()) {
            if (key.startsWith(prefix.toString())) {
                matchingObjects.put(key.substring(prefix.toString().length(), key.length()), objectSet.get(key));
            }
        }
        return matchingObjects;
    }

    private static boolean isIndexToken(final String token) {
        if (token.isEmpty()) {
            return false;
        } else {
            for (int i = 0; i < token.length(); i++) {
                final char c = token.charAt(i);
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static JsonPointer replaceLastIndexToken(final JsonPointer jsonPointer) {
        final String[] jsonPointerTokens = jsonPointer.toArray();
        if (getNextIndexToken(jsonPointer, jsonPointer.size() - 1) != -1) {
            jsonPointerTokens[jsonPointerTokens.length - 1] = "-";
        }
        return new JsonPointer(jsonPointerTokens);
    }

    private static int getNextIndexToken(final JsonPointer jsonPointer, int start) {
        final String[] jsonPointerTokens = jsonPointer.toArray();
        for (int i = start; i < jsonPointerTokens.length; i++) {
            if (isIndexToken(jsonPointerTokens[i])) {
                return i;
            }
        }
        return -1;
    }

    private static JsonPointer subJsonPointer(final JsonPointer jsonPointer, final int start, final int end) {
        final String[] jsonPointerTokens = jsonPointer.toArray();
        final List<String> newJsonPointerTokens = new ArrayList<>();
        for (int i = start; i < end; i++) {
            newJsonPointerTokens.add(jsonPointerTokens[i]);
        }
        return new JsonPointer(newJsonPointerTokens.toArray(new String[newJsonPointerTokens.size()]));
    }

    private static int getIndexTokens(final JsonPointer jsonPointer) {
        int numberOfIndexTokens = 0;
        final String[] jsonPointerTokens = jsonPointer.toArray();
        for (int i = 0; i < jsonPointerTokens.length; i++) {
            if (isIndexToken(jsonPointerTokens[i])) {
                numberOfIndexTokens++;
            }
        }
        return numberOfIndexTokens;
    }

    private static void flatten(
            final JsonPointer pointer,
            final JsonValue jsonValue,
            final Map<String, Object> flatObject) {
        final Set<String> jsonValueKeys = jsonValue.get(pointer).keys();
        for (final String key : jsonValueKeys) {
            final JsonPointer keyPointer = concatJsonPointer(pointer, key);
            final JsonValue temp = jsonValue.get(keyPointer);
            if (temp.isMap() || temp.isList()) {
                flatten(keyPointer, jsonValue, flatObject);
            } else {
                flatObject.put(
                        keyPointer.toString(),
                        jsonValue.get(keyPointer).getObject());
            }
        }
        return;
    }

    private static JsonPointer concatJsonPointer(final JsonPointer pointer, final String key) {
        final String[] pointerTokens = pointer.toArray();
        final String[] newPointerTokens = Arrays.copyOf(pointerTokens, pointerTokens.length + 1);
        newPointerTokens[pointerTokens.length] = key;
        return new JsonPointer(newPointerTokens);

    }

    /**
     * A generic JsonValue Query Filter Visitor.
     */
    public static final QueryFilterVisitor<Boolean, JsonValue> JSONVALUE_FILTER_VISITOR =
            new QueryFilterVisitor<Boolean, JsonValue>() {
        @Override
        public Boolean visitAndFilter(final JsonValue p, final List<QueryFilter> subFilters) {
            for (final QueryFilter subFilter : subFilters) {
                if (!subFilter.accept(this, p)) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }

        @Override
        public Boolean visitBooleanLiteralFilter(final JsonValue p, final boolean value) {
            return value;
        }

        @Override
        public Boolean visitContainsFilter(final JsonValue p, final JsonPointer field,
                                           final Object valueAssertion) {
            for (final Object value : getValues(p, field)) {
                if (isCompatible(valueAssertion, value)) {
                    if (valueAssertion instanceof String) {
                        final String s1 = ((String) valueAssertion).toLowerCase(Locale.ENGLISH);
                        final String s2 = ((String) value).toLowerCase(Locale.ENGLISH);
                        if (s2.contains(s1)) {
                            return Boolean.TRUE;
                        }
                    } else {
                        // Use equality matching for numbers and booleans.
                        if (compareValues(valueAssertion, value) == 0) {
                            return Boolean.TRUE;
                        }
                    }
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitEqualsFilter(final JsonValue p, final JsonPointer field,
                                         final Object valueAssertion) {
            Boolean result = Boolean.TRUE;
            for (final Object value : getValues(p, field)) {
                if (!isCompatible(valueAssertion, value) || compareValues(valueAssertion, value) != 0) {
                    result = Boolean.FALSE;
                }
            }
            return result;
        }

        @Override
        public Boolean visitExtendedMatchFilter(final JsonValue p, final JsonPointer field,
                                                final String matchingRuleId, final Object valueAssertion) {
            // Extended filters are not supported
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitGreaterThanFilter(final JsonValue p, final JsonPointer field,
                                              final Object valueAssertion) {
            for (final Object value : getValues(p, field)) {
                if (isCompatible(valueAssertion, value) && compareValues(valueAssertion, value) < 0) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitGreaterThanOrEqualToFilter(final JsonValue p, final JsonPointer field,
                                                       final Object valueAssertion) {
            for (final Object value : getValues(p, field)) {
                if (isCompatible(valueAssertion, value) && compareValues(valueAssertion, value) <= 0) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitLessThanFilter(final JsonValue p, final JsonPointer field,
                                           final Object valueAssertion) {
            for (final Object value : getValues(p, field)) {
                if (isCompatible(valueAssertion, value) && compareValues(valueAssertion, value) > 0) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitLessThanOrEqualToFilter(final JsonValue p, final JsonPointer field,
                                                    final Object valueAssertion) {
            for (final Object value : getValues(p, field)) {
                if (isCompatible(valueAssertion, value) && compareValues(valueAssertion, value) >= 0) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitNotFilter(final JsonValue p, final QueryFilter subFilter) {
            return !subFilter.accept(this, p);
        }

        @Override
        public Boolean visitOrFilter(final JsonValue p, final List<QueryFilter> subFilters) {
            for (final QueryFilter subFilter : subFilters) {
                if (subFilter.accept(this, p)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitPresentFilter(final JsonValue p, final JsonPointer field) {
            final JsonValue value = p.get(field);
            return value != null;
        }

        @Override
        public Boolean visitStartsWithFilter(final JsonValue p, final JsonPointer field,
                                             final Object valueAssertion) {
            for (final Object value : getValues(p, field)) {
                if (isCompatible(valueAssertion, value)) {
                    if (valueAssertion instanceof String) {
                        final String s1 = ((String) valueAssertion).toLowerCase(Locale.ENGLISH);
                        final String s2 = ((String) value).toLowerCase(Locale.ENGLISH);
                        if (s2.startsWith(s1)) {
                            return Boolean.TRUE;
                        }
                    } else {
                        // Use equality matching for numbers and booleans.
                        if (compareValues(valueAssertion, value) == 0) {
                            return Boolean.TRUE;
                        }
                    }
                }
            }
            return Boolean.FALSE;
        }

        private List<Object> getValues(final JsonValue resource, final JsonPointer field) {
            final JsonValue value = resource.get(field);
            if (value == null) {
                return Collections.emptyList();
            } else if (value.isList()) {
                return value.asList();
            } else {
                return Collections.singletonList(value.getObject());
            }
        }

        private int compareValues(final Object v1, final Object v2) {
            if (v1 instanceof String && v2 instanceof String) {
                final String s1 = (String) v1;
                final String s2 = (String) v2;
                return s1.compareToIgnoreCase(s2);
            } else if (v1 instanceof Number && v2 instanceof Number) {
                final Double n1 = ((Number) v1).doubleValue();
                final Double n2 = ((Number) v2).doubleValue();
                return n1.compareTo(n2);
            } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
                final Boolean b1 = (Boolean) v1;
                final Boolean b2 = (Boolean) v2;
                return b1.compareTo(b2);
            } else {
                // Different types: we need to ensure predictable ordering,
                // so use class name as secondary key.
                return v1.getClass().getName().compareTo(v2.getClass().getName());
            }
        }

        private boolean isCompatible(final Object v1, final Object v2) {
            return (v1 instanceof String && v2 instanceof String)
                    || (v1 instanceof Number && v2 instanceof Number)
                    || (v1 instanceof Boolean && v2 instanceof Boolean);
        }

    };
}
