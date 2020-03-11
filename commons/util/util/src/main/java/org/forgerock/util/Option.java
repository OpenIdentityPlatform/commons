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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.util;

/**
 * A configuration option whose value can be stored in a set of {@link Options}.
 * Refer to the appropriate class for the list of supported options.
 *
 * @param <T>
 *            The type of value associated with the option.
 */
public final class Option<T> {
    /**
     * Defines an option with the provided type and default value.
     *
     * @param <T>
     *            The type of value associated with the option.
     * @param type
     *            The type of value associated with the option.
     * @param defaultValue
     *            The default value for the option.
     * @return An option with the provided type and default value.
     */
    public static <T> Option<T> of(final Class<T> type, final T defaultValue) {
        return new Option<>(type, defaultValue);
    }

    /**
     * Defines a boolean option with the provided default value.
     *
     * @param <T>
     *            The type of value associated with the option.
     * @param defaultValue
     *            The default value for the option.
     * @return A boolean option with the provided default value.
     */
    @SuppressWarnings("unchecked")
    public static <T> Option<T> withDefault(final T defaultValue) {
        return new Option<>((Class<T>) defaultValue.getClass(), defaultValue);
    }

    private final T defaultValue;
    private final Class<T> type;

    private Option(final Class<T> type, final T defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    T getValue(final Object value) {
        return value != null ? type.cast(value) : defaultValue;
    }
}
