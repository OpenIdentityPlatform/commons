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

package org.forgerock.util.time;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Fail;

/**
 * Provides {@link #assertThat(Duration)} for unit testing {@link Duration}.
 */
public class DurationAssert extends AbstractAssert<DurationAssert, Duration> {

    /**
     * Hidden constructor.
     *
     * @param actual the actual value.
     */
    private DurationAssert(final Duration actual) {
        super(actual, DurationAssert.class);
    }

    /**
     * Asserts {@link Duration} equality on all of its instance fields.
     *
     * @param n number value.
     * @param unit unit of time.
     * @return the assertion object.
     */
    public DurationAssert isEqualTo(final long n, final TimeUnit unit) {
        isNotNull();
        if (actual.getValue() != n) {
            Fail.fail(String.format("Duration value does not match: was:%d expected:%d", actual.getValue(), n));
        }
        if (!actual.getUnit().equals(unit)) {
            Fail.fail(String.format("Duration TimeUnit does not match: was:%s expected:%s",
                    actual.getUnit().name(), unit.name()));
        }
        return this;
    }

    /**
     * Asserts that {@link Duration} is unlimited.
     *
     * @return the assertion object.
     */
    public DurationAssert isUnlimited() {
        isNotNull();
        if (!actual.isUnlimited()) {
            Fail.fail(String.format("Duration is not unlimited current values: %d %s",
                    actual.getValue(), actual.getUnit()));
        }
        return this;
    }

    /**
     * Asserts that {@link Duration} is zero.
     *
     * @return the assertion object.
     */
    public DurationAssert isZero() {
        isNotNull();
        if (!actual.isZero()) {
            Fail.fail(String.format("Duration is not zero-length: %d %s", actual.getValue(), actual.getUnit()));
        }
        return this;
    }

    /**
     * Creates a new instance of <code>{@link DurationAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static DurationAssert assertThat(final Duration actual) {
        return new DurationAssert(actual);
    }
}
