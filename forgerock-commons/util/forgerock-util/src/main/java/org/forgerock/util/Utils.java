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
package org.forgerock.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Common utility methods.
 */
public final class Utils {

    /**
     * Closes the provided resources ignoring any errors which occurred.
     *
     * @param resources
     *            The resources to be closed, which may be {@code null}.
     */
    public static void closeSilently(final Closeable... resources) {
        if (resources == null) {
            return;
        }
        closeSilently(Arrays.asList(resources));
    }

    /**
     * Closes the provided resources ignoring any errors which occurred.
     *
     * @param resources
     *            The resources to be closed, which may be {@code null}.
     */
    public static void closeSilently(final Iterable<? extends Closeable> resources) {
        if (resources == null) {
            return;
        }
        for (final Closeable r : resources) {
            try {
                if (r != null) {
                    r.close();
                }
            } catch (final IOException ignored) {
                // Ignore.
            }
        }
    }

    /**
     * Returns a string whose content is the string representation of the
     * provided objects concatenated together using the provided separator.
     *
     * @param separator
     *            The separator string.
     * @param values
     *            The objects to be joined.
     * @return A string whose content is the string representation of the
     *         provided objects concatenated together using the provided
     *         separator.
     * @throws NullPointerException
     *             If {@code values} or {@code separator} were {@code null}.
     */
    public static String joinAsString(final String separator, final Object... values) {
        return joinAsString(separator, Arrays.asList(values));
    }

    /**
     * Returns a string whose content is the string representation of the
     * objects contained in the provided iterable concatenated together using
     * the provided separator.
     *
     * @param separator
     *            The separator string.
     * @param values
     *            The iterable whose elements are to be joined.
     * @return A string whose content is the string representation of the
     *         objects contained in the provided iterable concatenated
     *         together using the provided separator.
     * @throws NullPointerException
     *             If {@code separator} or {@code values} were {@code null}.
     */
    public static String joinAsString(final String separator, final Iterable<?> values) {
        Reject.ifNull(separator);
        Reject.ifNull(values);

        final StringBuilder builder = new StringBuilder();
        joinAsString(builder, separator, values);
        return builder.toString();
    }

    /**
     * Appends into the provided {@link StringBuilder} the string representation
     * of the provided objects concatenated together using the provided separator.
     *
     * @param builder
     *            The String builder where to append.
     * @param separator
     *            The separator string.
     * @param values
     *            The objects to be joined.
     * @throws NullPointerException
     *             If {@code builder}, {@code separator} or {@code values} were {@code null}.
     */
    public static void joinAsString(final StringBuilder builder, final String separator, final Object... values) {
        joinAsString(builder, separator, Arrays.asList(values));
    }

    /**
     * Appends into the provided {@link StringBuilder} the string representation
     * of the objects contained in the provided iterable concatenated together
     * using the provided separator.
     *
     * @param builder
     *            The String builder where to append.
     * @param separator
     *            The separator string.
     * @param values
     *            The iterable whose elements are to be joined.
     * @throws NullPointerException
     *             If {@code builder}, {@code separator} or {@code values} were {@code null}.
     */
    public static void joinAsString(final StringBuilder builder, final String separator, final Iterable<?> values) {
        Reject.ifNull(builder);
        Reject.ifNull(separator);
        Reject.ifNull(values);

        final Iterator<?> iterator = values.iterator();
        if (iterator.hasNext()) {
            builder.append(iterator.next());

            while (iterator.hasNext()) {
                builder.append(separator);
                builder.append(iterator.next());
            }
        }
    }

    /**
     * Creates a new thread factory which will create threads using the
     * specified thread group, naming template, and daemon status.
     *
     * @param group
     *            The thread group, which may be {@code null}.
     * @param nameTemplate
     *            The thread name format string which may contain a "%d" format
     *            option which will be substituted with the thread count.
     * @param isDaemon
     *            Indicates whether or not threads should be daemon threads.
     * @return The new thread factory.
     */
    public static ThreadFactory newThreadFactory(final ThreadGroup group,
            final String nameTemplate, final boolean isDaemon) {
        return new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(final Runnable r) {
                final String name = String.format(nameTemplate, count.getAndIncrement());
                final Thread t = new Thread(group, r, name);
                t.setDaemon(isDaemon);
                return t;
            }
        };
    }

    /**
     * Returns the string value as an enum constant of the specified enum
     * type. The string value and enum constants are compared, ignoring case
     * considerations. If the string value is {@code null}, this method returns
     * {@code null}.
     *
     * @param <T>
     *            the enum type sub-class.
     * @param value
     *            the string value
     * @param type
     *            the enum type to match constants with the value.
     * @return the enum constant represented by the string value.
     * @throws IllegalArgumentException
     *             if {@code type} does not represent an enum type,
     *             of if {@code value} does not match one of the enum constants
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     */
    public static <T extends Enum<T>> T asEnum(final String value, final Class<T> type) {
        if (value == null) {
            return null;
        }
        final T[] constants = type.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("Type is not an enum class");
        }
        for (final T constant : constants) {
            if (value.equalsIgnoreCase(constant.toString())) {
                return constant;
            }
        }
        final StringBuilder sb = new StringBuilder("Expecting String containing one of: ");
        sb.append(joinAsString(" ", (Object[]) constants));
        throw new IllegalArgumentException(sb.toString());
    }

    /**
     * Check to see if the provided String is {@code null} or empty.
     * @param value The value to check.
     * @return {@code true} if the value is either {@code null} or is empty.
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private Utils() {
        // Prevent instantiation.
    }
}
