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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.json.JsonValue;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Utilities for working with Elasticsearch.
 */
class ElasticsearchUtil {

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
     *     [{,]\s*"(?:(?=[^"]*[.][^"]*)[^"]+)"\s*:
     * </pre>
     */
    private final static Pattern JSON_KEY_WITH_PERIOD_CHAR_PATTERN =
            Pattern.compile("[{,]\\s*\"(?:(?=[^\"]*[.][^\"]*)[^\"]+)\"\\s*:");

    private final static Pattern PERIOD_CHAR_PATTERN = Pattern.compile("[.]");

    /**
     * Normalizes JSON to conform to Elasticsearch data-format restrictions. The following normalizations are performed,
     * <ul>
     * <li>Periods in JSON keys are converted to underscore characters</li>
     * </ul>
     *
     * @param value JSON value
     * @return Normalized JSON as a {@code String}
     */
    public static String normalizeJson(final JsonValue value) {
        if (value != null) {
            return replaceKeyPeriodsWithUnderscores(value.toString());
        }
        return null;
    }

    /**
     * Replaces all period-characters in JSON keys with underscore-characters
     * [<a href="https://discuss.elastic.co/t/field-name-cannot-contain/33251/29">ref</a>].
     *
     * @param s JSON input
     * @return Resulting JSON
     */
    @VisibleForTesting
    static String replaceKeyPeriodsWithUnderscores(final String s) {
        final Matcher m = JSON_KEY_WITH_PERIOD_CHAR_PATTERN.matcher(s);
        if (m.find()) {
            final int n = s.length();
            final StringBuilder builder = new StringBuilder(n);
            if (m.start() != 0) {
                builder.append(s.substring(0, m.start()));
            }
            builder.append(replaceAllPeriodsWithUnderscores(s.substring(m.start(), m.end())));
            int index = m.end();
            while (index != n && m.find(index)) {
                if (index != m.start()) {
                    builder.append(s.substring(index, m.start()));
                }
                builder.append(replaceAllPeriodsWithUnderscores(s.substring(m.start(), m.end())));
                index = m.end();
            }
            if (index != n) {
                builder.append(s.substring(index));
            }
            return builder.toString();
        }
        // no normalization required
        return s;
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
}
