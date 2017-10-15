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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.caf.authn;

import static org.forgerock.json.JsonValue.*;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.forgerock.json.JsonPointer;
import org.forgerock.util.test.assertj.Conditions;
import org.hamcrest.Matcher;

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
    static Map<JsonPointer, Condition<?>> exceptionMatcher(int code) {
        Map<JsonPointer, Condition<?>> matchers = new HashMap<>();
        matchers.put(new JsonPointer("data"), NULL_CONDITION);
        matchers.put(new JsonPointer("principal"), NULL_CONDITION);
        matchers.put(new JsonPointer("context"), NULL_CONDITION);
        matchers.put(new JsonPointer("code"), Conditions.equalTo(code));
        matchers.put(new JsonPointer("message"), NOT_NULL_CONDITION);
        return matchers;
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying an exception response body.
     *
     * @param code The expected code in the response body.
     * @param messageMatcher The {@code Matcher} to use to verify the "message".
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<JsonPointer, Condition<?>> exceptionMatcher(int code, Matcher<?> messageMatcher) {
        Map<JsonPointer, Condition<?>> matchers = new HashMap<>();
        matchers.put(new JsonPointer("data"), NULL_CONDITION);
        matchers.put(new JsonPointer("principal"), NULL_CONDITION);
        matchers.put(new JsonPointer("context"), NULL_CONDITION);
        matchers.put(new JsonPointer("code"), Conditions.equalTo(code));
        matchers.put(new JsonPointer("message"), new MatcherCondition<>(messageMatcher));
        return matchers;
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying a resource response body.
     *
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<JsonPointer, Condition<?>> resourceMatcher() {
        Map<JsonPointer, Condition<?>> matchers = new HashMap<>();
        matchers.put(new JsonPointer("data"), Conditions.equalTo("RESOURCE_DATA"));
        matchers.put(new JsonPointer("principal"), NULL_CONDITION);
        matchers.put(new JsonPointer("context"), NULL_CONDITION);
        matchers.put(new JsonPointer("code"), NULL_CONDITION);
        matchers.put(new JsonPointer("message"), NULL_CONDITION);
        return matchers;
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying a resource response body.
     *
     * @param principal The expected principal
     * @param contextEntries The expected content entries.
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map<JsonPointer, Condition<?>> resourceMatcher(String principal, String... contextEntries) {
        Map<JsonPointer, Condition<?>> object = new HashMap<>();
        object.put(new JsonPointer("data"), Conditions.equalTo("RESOURCE_DATA"));
        object.put(new JsonPointer("principal"), Conditions.equalTo(principal));
        object.put(new JsonPointer("code"), NULL_CONDITION);
        object.put(new JsonPointer("message"), NULL_CONDITION);

        for (String contextEntry : contextEntries) {
            object.put(new JsonPointer("context/" + contextEntry), Conditions.equalTo(true));
        }

        return object;
    }

    /**
     * Returns a {@code Map} of {@code Matcher}s for verifying an empty response body.
     *
     * @return A {@code Map} of {@code Matcher}s.
     */
    @SuppressWarnings("unchecked")
    static Map noData() {
        return (Map) object(
                field(null, Conditions.equalTo(""))
        );
    }

    private static final Condition<Object> NULL_CONDITION = new Condition<Object>() {
        @Override
        public boolean matches(Object o) {
            return o == null;
        }
    };

    private static final Condition<Object> NOT_NULL_CONDITION = new Condition<Object>() {
        @Override
        public boolean matches(Object o) {
            return o != null;
        }
    };

    private static class MatcherCondition<T> extends Condition<T> {

        private final Matcher<T> matcher;

        MatcherCondition(Matcher<T> matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(T t) {
            return matcher.matches(t);
        }
    }
}
