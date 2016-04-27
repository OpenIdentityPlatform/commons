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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.util.test.assertj;

import org.assertj.core.api.Condition;

/**
 * {@code Condition} implementations useful for assertions.
 * @see org.forgerock.json.test.assertj.AssertJJsonValueAssert.AbstractJsonValueAssert#stringIs(java.lang.String,
 * org.assertj.core.api.Condition)
 */
public final class Conditions {

    private Conditions() {
        // Prevent from instantiating
    }

    /**
     * A condition for equality testing.
     * @param expected The value expected.
     * @param <T> The type expected.
     * @return The {@code Condition}.
     */
    public static <T> Condition<T> equalTo(T expected) {
        EqualToCondition<T> condition = new EqualToCondition<>();
        condition.expected = expected;
        return condition;
    }

    private static class EqualToCondition<T> extends Condition<T> {

        private T expected;

        @Override
        public boolean matches(T value) {
            return expected == null ? value == null : expected.equals(value);
        }
    }
}
