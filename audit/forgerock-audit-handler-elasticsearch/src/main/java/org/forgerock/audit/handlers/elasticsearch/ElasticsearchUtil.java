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
package org.forgerock.audit.handlers.elasticsearch;

import static org.forgerock.http.util.Json.readJson;
import static org.forgerock.json.JsonValue.json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.json.JsonValue;
import org.forgerock.util.annotations.VisibleForTesting;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for working with Elasticsearch.
 */
class ElasticsearchUtil {

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
    public static JsonValue normalizeJson(final JsonValue value) throws IOException {
        if (value != null) {
            if (value.get(NORMALIZED_FIELD).isNotNull()) {
                throw new IllegalStateException(NORMALIZED_FIELD + " is a reserved JsonValue field");
            }
            final Map<String, Object> normalized = new LinkedHashMap<>(MAX_FIELD_COUNT);
            final JsonValue result = replaceKeyPeriodsWithUnderscores(value, normalized);
            if (!normalized.isEmpty()) {
                // add metadata for de-normalization
                result.put(NORMALIZED_FIELD, normalized);
            }
            return result;
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
            final JsonValue nomalized = value.get(NORMALIZED_FIELD);
            if (nomalized.isNotNull()) {
                value.remove(NORMALIZED_FIELD);
            } else {
                // nothing needs to be de-normalized, because there is no de-normalization metadata
                return value;
            }
            return restoreKeyPeriods(value, nomalized);
        }
        return null;
    }

    /**
     * Replaces all period-characters in JSON keys with underscore-characters
     * [<a href="https://discuss.elastic.co/t/field-name-cannot-contain/33251/29">ref</a>]. If normalization is
     * required, the {@code fieldNames} field will be added to the {@code normalized} metadata.
     *
     * @param value JSON input
     * @param normalized De-normalization metadata, which this method may add to
     * @return Resulting JSON
     * @throws IOException If unable to parse the json.
     */
    @VisibleForTesting
    protected static JsonValue replaceKeyPeriodsWithUnderscores(final JsonValue value,
            final Map<String, Object> normalized) throws IOException {
        final String s = value.toString();
        final Matcher m = JSON_KEY_WITH_PERIOD_CHAR_PATTERN.matcher(s);
        if (m.find()) {
            // fieldNames contains metadata for de-normalization
            final Map<String, Object> fieldNames = new LinkedHashMap<>(2);
            normalized.put(FIELD_NAMES_FIELD, fieldNames);

            final int n = s.length();
            final StringBuilder builder = new StringBuilder(n);
            if (m.start() != 0) {
                builder.append(s.substring(0, m.start()));
            }

            String fieldName = m.group(1);
            fieldNames.put(replaceAllPeriodsWithUnderscores(fieldName), fieldName);

            builder.append(replaceAllPeriodsWithUnderscores(s.substring(m.start(), m.end())));
            int index = m.end();
            while (index != n && m.find(index)) {
                if (index != m.start()) {
                    builder.append(s.substring(index, m.start()));
                }

                fieldName = m.group(1);
                fieldNames.put(replaceAllPeriodsWithUnderscores(fieldName), fieldName);

                builder.append(replaceAllPeriodsWithUnderscores(s.substring(m.start(), m.end())));
                index = m.end();
            }
            if (index != n) {
                builder.append(s.substring(index));
            }
            return json(readJson(builder.toString()));
        }
        // no normalization required
        return value;
    }

    @VisibleForTesting
    protected static JsonValue restoreKeyPeriods(final JsonValue value, final JsonValue nomalized) throws IOException {
        final JsonValue fieldNames = nomalized.get(FIELD_NAMES_FIELD);
        if (fieldNames.isNotNull()) {
            final String s = value.toString();
            final Matcher m = JSON_KEY_WITH_UNDERSCORE_CHAR_PATTERN.matcher(s);
            if (m.find()) {
                final int n = s.length();
                final StringBuilder builder = new StringBuilder(n);
                if (m.start() != 0) {
                    builder.append(s.substring(0, m.start()));
                }
                builder.append(replace(s.substring(m.start(), m.end()), m.group(1), fieldNames));
                int index = m.end();
                while (index != n && m.find(index)) {
                    if (index != m.start()) {
                        builder.append(s.substring(index, m.start()));
                    }
                    builder.append(replace(s.substring(m.start(), m.end()), m.group(1), fieldNames));
                    index = m.end();
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
     * @return Result
     */
    private static String replaceAllPeriodsWithUnderscores(final String s) {
        return PERIOD_CHAR_PATTERN.matcher(s).replaceAll("_");
    }

    /**
     * Replaces all instances of a key ({@literal String}) with the corresponding value ({@literal String}). If
     * a key-value mapping is not found, then the original input is returned.
     *
     * @param s Input
     * @param fieldName Key
     * @param fieldNames Map of key-values ({@literal String})
     * @return Result
     */
    private static String replace(final String s, final String fieldName, final JsonValue fieldNames) {
        final JsonValue value = fieldNames.get(fieldName);
        return value.isNull() ? s : s.replace(fieldName, fieldNames.get(fieldName).asString());
    }
}
