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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn;

import org.hamcrest.Matcher;

import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Utility class which contains methods for creating a {@code Map} of {@code Matcher}s that will be used to verify the
 * contents of the response body.
 *
 * @since 1.5.0
 */
final class BodyMatcher {

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying an exception response body.
     *
     * @param code The expected code in the response body.
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Matcher<?>> exceptionMatcher(int code) {
        return (Map<String, Matcher<?>>) object(
                field("code", is(code)),
                field("message", notNullValue()),
                field("data", nullValue()),
                field("principal", nullValue()),
                field("context", nullValue())
        );
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying an exception response body.
     *
     * @param code The expected code in the response body.
     * @param messageMatcher The {@code Matcher} to use to verify the "message".
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Matcher<?>> exceptionMatcher(int code, Matcher<?> messageMatcher) {
        return (Map<String, Matcher<?>>) object(
                field("code", is(code)),
                field("message", messageMatcher),
                field("data", nullValue()),
                field("principal", nullValue()),
                field("context", nullValue())
        );
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying a resource response body.
     *
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Matcher<?>> resourceMatcher() {
        return (Map<String, Matcher<?>>) object(
                field("data", is("RESOURCE_DATA")),
                field("principal", nullValue()),
                field("context", nullValue()),
                field("code", nullValue()),
                field("message", nullValue())
        );
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying a resource response body.
     *
     * @param principal The expected principal
     * @param contextEntries The expected content entries.
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Matcher<?>> resourceMatcher(String principal, String... contextEntries) {
        Map<String, Matcher<?>> object = (Map<String, Matcher<?>>) object(
                field("data", is("RESOURCE_DATA")),
                field("principal", is(principal)),
                field("code", nullValue()),
                field("message", nullValue())
        );

        for (String contextEntry : contextEntries) {
            object.put("context." + contextEntry, is(true));
        }

        return object;
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying an empty response body.
     *
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Matcher<?>> noData() {
        return (Map<String, Matcher<?>>) object(
                field(null, is(""))
        );
    }
}
