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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.audit.util;

import static org.forgerock.http.util.Json.readJson;
import static org.forgerock.json.JsonValue.json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.annotations.VisibleForTesting;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for working with Elasticsearch.
 */
public final class ElasticsearchUtil {

    /**
     * Jackson {@link ObjectMapper} for working with JSON.
     */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * JSON field-name of metadata to assist in de-normalization.
     */
    @VisibleForTesting
    protected static final String NORMALIZED_FIELD = "_normalized";

    /**
     * JSON field-name of normalized field-names.
     */
    @VisibleForTesting
    protected static final String FIELD_NAMES_FIELD = "fieldNames";

    private static final String NORMALIZED_FIELD_JSON_PREFIX = ",\"" + NORMALIZED_FIELD + "\":";

    /**
     * Number of normalization metadata fields that this class might add to the {@link #NORMALIZED_FIELD JSON object.
     */
    private static final int MAX_FIELD_COUNT = 1;

    private ElasticsearchUtil() {
        // hidden
    }

    /**
     * Regex pattern that matches JSON keys that contain at least one period.  This is useful, because Elasticsearch
     * does not allow period characters in JSON keys
     * [<a href="https://discuss.elastic.co/t/field-name-cannot-contain/33251/29">ref</a>].
     * <p/>
     * The following regex matches anything after an open-curly-brace, or comma, that is within double-quotes and
     * followed by a semi-colon. Furthermore, positive-lookahead requires at least one period-character within the
     * double-quotes.
     * <pre>
     *     [{,]\s*"((?=[^"]*[.][^"]*)[^"]+)"\s*:
     * </pre>
     */
    private final static Pattern JSON_KEY_WITH_PERIOD_CHAR_PATTERN =
            Pattern.compile("[{,]\\s*\"((?=[^\"]*[.][^\"]*)[^\"]+)\"\\s*:");

    /**
     * Regex pattern identical to {@link #JSON_KEY_WITH_PERIOD_CHAR_PATTERN}, except it searches for JSON keys that
     * contain underscore characters. This is used to facilitate reversing normalization.
     */
    private final static Pattern JSON_KEY_WITH_UNDERSCORE_CHAR_PATTERN =
            Pattern.compile("[{,]\\s*\"((?=[^\"]*[_][^\"]*)[^\"]+)\"\\s*:");

    /**
     * Regex pattern to find period characters.
     */
    private final static Pattern PERIOD_CHAR_PATTERN = Pattern.compile("[.]");

    /**
     * Normalizes JSON to conform to Elasticsearch data-format restrictions. The following normalizations are performed,
     * <ul>
     * <li>Periods in JSON fields (keys) are converted to underscore characters</li>
     * </ul>
     * The following metadata, for example, is added to the Normalized JSON, to facilitate de-normalization,
     * <pre>
     *     "_normalized" : {
     *         "fieldNames" : {
     *             "key_1" : "key.1",
     *             "key_2" : "key.2"
     *         }
     *     }
     * </pre>
     *
     * @param value JSON value
     * @return Resulting JSON, with {@code _normalized} field if any normalization was necessary
     * @throws IOException If unable to parse the json.
     */
    public static String normalizeJson(final JsonValue value) throws IOException {
        if (value != null) {
            if (value.get(NORMALIZED_FIELD).isNotNull()) {
                throw new IllegalStateException(NORMALIZED_FIELD + " is a reserved JsonValue field");
            }
            final String json = OBJECT_MAPPER.writeValueAsString(value.getObject());
            return replaceKeyPeriodsWithUnderscores(json);
        }
        return null;
    }

    /**
     * De-normalizes JSON that was previously normalized by  {@link #normalizeJson(JsonValue)}.
     *
     * @param value JSON value
     * @return Original, de-normalized JSON
     * @throws IOException If unable to parse the json.
     */
    public static JsonValue denormalizeJson(final JsonValue value) throws IOException {
        if (value != null) {
            final JsonValue normalized = value.get(NORMALIZED_FIELD);
            if (normalized.isNotNull()) {
                value.remove(NORMALIZED_FIELD);
            } else {
                // nothing needs to be de-normalized, because there is no de-normalization metadata
                return value;
            }
            return restoreKeyPeriods(value, normalized);
        }
        return null;
    }

    /**
     * Replaces all period-characters in JSON keys with underscore-characters
     * [<a href="https://discuss.elastic.co/t/field-name-cannot-contain/33251/29">ref</a>]. If normalization is
     * required, the {@code fieldNames} field will be added to the {@code normalized} metadata.
     *
     * @param json JSON {@code String} input
     * @return Resulting JSON {@code String}
     * @throws IOException If unable to parse the json.
     */
    @VisibleForTesting
    protected static String replaceKeyPeriodsWithUnderscores(final String json)
            throws IOException {
        final Matcher m = JSON_KEY_WITH_PERIOD_CHAR_PATTERN.matcher(json);
        if (m.find()) {
            // fieldNames contains metadata for de-normalization
            final Map<String, Object> normalized = new LinkedHashMap<>(MAX_FIELD_COUNT);
            final Map<String, Object> fieldNames = new LinkedHashMap<>(2);
            normalized.put(FIELD_NAMES_FIELD, fieldNames);

            final int n = json.length();

            // allocate enough capacity to prevent resizing
            final StringBuilder builder = new StringBuilder(n + NORMALIZED_FIELD_JSON_PREFIX.length() + 128);
            builder.append(json);

            String originalFieldName = m.group(1);
            String normalizedFieldName = replaceAllPeriodsWithUnderscores(builder, m.start(1), m.end(1));
            fieldNames.put(normalizedFieldName, originalFieldName);

            int index = m.end();
            while (index != n && m.find(index)) {
                originalFieldName = m.group(1);
                normalizedFieldName = replaceAllPeriodsWithUnderscores(builder, m.start(1), m.end(1));
                fieldNames.put(normalizedFieldName, originalFieldName);

                index = m.end();
            }

            // remove last curly-brace, so that we can append
            builder.setLength(n - 1);

            // add JSON metadata to end
            builder.append(NORMALIZED_FIELD_JSON_PREFIX)
                    .append(OBJECT_MAPPER.writeValueAsString(normalized))
                    .append('}');

            return builder.toString();
        }
        // no normalization required
        return json;
    }

    /**
     * Reverses the normalization steps preformed by {@link #replaceKeyPeriodsWithUnderscores(String)}.
     *
     * @param value JSON input
     * @param normalized De-normalization metadata, which this method may add to
     * @return Resulting JSON
     * @throws IOException If unable to parse the json.
     */
    @VisibleForTesting
    protected static JsonValue restoreKeyPeriods(final JsonValue value, final JsonValue normalized) throws IOException {
        final JsonValue fieldNames = normalized.get(FIELD_NAMES_FIELD);
        if (fieldNames.isNotNull() && !fieldNames.asMap().isEmpty()) {
            final String s = OBJECT_MAPPER.writeValueAsString(value.getObject());
            final Matcher m = JSON_KEY_WITH_UNDERSCORE_CHAR_PATTERN.matcher(s);
            if (m.find()) {
                final int n = s.length();
                final StringBuilder builder = new StringBuilder(n);
                if (m.start(1) != 0) {
                    builder.append(s.substring(0, m.start(1)));
                }
                builder.append(replace(m.group(1), fieldNames));
                int index = m.end(1);
                while (index != n && m.find(index)) {
                    if (index != m.start(1)) {
                        builder.append(s.substring(index, m.start(1)));
                    }
                    builder.append(replace(m.group(1), fieldNames));
                    index = m.end(1);
                }
                if (index != n) {
                    builder.append(s.substring(index));
                }
                return json(readJson(builder.toString()));
            }
        }
        // no normalization required
        return value;
    }

    /**
     * Replaces all period-characters with underscore-characters.
     *
     * @param s Input
     * @param start Start index (inclusive)
     * @param end End index (exclusive)
     * @return Result
     */
    private static String replaceAllPeriodsWithUnderscores(final StringBuilder s, final int start, final int end) {
        for (int i = start; i < end; ++i) {
            if (s.charAt(i) == '.') {
                s.setCharAt(i, '_');
            }
        }
        return s.substring(start, end);
    }

    /**
     * Finds replacement for a given key ({@literal fieldName}) within {@literal fieldNames} map. If a key-value mapping
     * is not found, then the original input is returned.
     *
     * @param fieldName Key
     * @param fieldNames Map of key-values ({@literal String})
     * @return Result
     */
    private static String replace(final String fieldName, final JsonValue fieldNames) {
        final JsonValue value = fieldNames.get(fieldName);
        return value.isNull() ? fieldName : value.asString();
    }

    /**
     * Replaces periods in {@link JsonPointer} keys with underscore.
     *
     * @param ptr The {@link JsonPointer} to normalize.
     * @return A normalized {@link JsonPointer}.
     */
    public static JsonPointer normalizeJsonPointer(final JsonPointer ptr) {
        if (ptr != null) {
            final String jsonPointer = ptr.toString();
            final Matcher matcher = PERIOD_CHAR_PATTERN.matcher(jsonPointer);
            if (matcher.find()) {
                return new JsonPointer(matcher.replaceAll("_"));
            }
        }
        return ptr;
    }

    /**
     * Renames a field within the given {@link JsonValue}.
     *
     * @param jsonValue {@link JsonValue} to have a top-level field renamed
     * @param oldKey Old field name
     * @param newKey New field name (field must <b>not</b> already exist)
     * @return {@code true} if field was found and renamed, and {@code false} otherwise
     */
    public static boolean renameField(final JsonValue jsonValue, final String oldKey, final String newKey) {
        if (jsonValue.isMap()) {
            final Map<String, Object> map = jsonValue.asMap();
            final Object value = map.remove(oldKey);
            if (value != null) {
                if (map.put(newKey, value) != null) {
                    // newKey already existed, so reverse the change and throw Exception
                    renameField(jsonValue, newKey, oldKey);
                    throw new IllegalStateException("Cannot overwrite existing field: " + newKey);
                }
                return true;
            }
        }
        return false;
    }
}
