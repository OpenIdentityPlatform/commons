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
package org.forgerock.util;

import static java.util.Collections.unmodifiableMap;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A set of options which can be used for customizing the behavior of HTTP
 * clients and servers. Refer to the appropriate class for the list of supported
 * options.
 */
public final class Options {
    private static final Options DEFAULT = unmodifiableCopyOf(defaultOptions());

    /**
     * Returns a copy of the provided set of options.
     *
     * @param options
     *            The options to be copied.
     * @return A copy of the provided set of options.
     */
    public static Options copyOf(final Options options) {
        return new Options(new IdentityHashMap<>(options.map));
    }

    /**
     * Returns a new set of options with default settings.
     *
     * @return A new set of options with default settings.
     */
    public static Options defaultOptions() {
        return new Options(new IdentityHashMap<Option<?>, Object>());
    }

    /**
     * Returns an unmodifiable copy of the provided set of options.
     *
     * @param options
     *            The options to be copied.
     * @return An unmodifiable copy of the provided set of options.
     */
    public static Options unmodifiableCopyOf(final Options options) {
        return new Options(unmodifiableMap(new IdentityHashMap<>(options.map)));
    }

    /**
     * Returns an unmodifiable set of options with default settings.
     *
     * @return An unmodifiable set of options with default settings.
     */
    public static Options unmodifiableDefaultOptions() {
        return DEFAULT;
    }

    private final Map<Option<?>, Object> map;

    private Options(final Map<Option<?>, Object> optionsMap) {
        this.map = optionsMap;
    }

    /**
     * Returns the value associated with the provided option, or its default
     * value if the option has not been configured.
     *
     * @param <T>
     *            The option type.
     * @param option
     *            The option whose associated value is to be returned.
     * @return The value associated with the provided option, or its default
     *         value if the option has not been configured.
     */
    public <T> T get(final Option<T> option) {
        return option.getValue(map.get(option));
    }

    /**
     * Resets an option to its default behavior.
     *
     * @param <T>
     *            The option type.
     * @param option
     *            The option whose value is to be reset.
     * @return This set of options.
     * @throws UnsupportedOperationException
     *             If this set of options is unmodifiable.
     */
    public <T> Options reset(final Option<T> option) {
        map.remove(option);
        return this;
    }

    /**
     * Sets an option to the provided value. If this set of options previously
     * contained a mapping for the option, the old value is replaced by the
     * specified value.
     *
     * @param <T>
     *            The option type.
     * @param option
     *            The option whose value is to be set.
     * @param value
     *            The option value.
     * @return This set of options.
     * @throws UnsupportedOperationException
     *             If this set of options is unmodifiable.
     */
    public <T> Options set(final Option<T> option, final T value) {
        map.put(option, value);
        return this;
    }

}
