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

/**
 * A input parameter-validating utility class using fluent invocation:
 *
 * <pre>
 * public int divide(int dividend, int divisor) {
 *     Reject.ifTrue(divisor == 0, &quot;Division by zero not supported&quot;);
 *     return dividend / divisor;
 * }
 * </pre>
 *
 * The example above will cause an {@code IllegalArgumentException} to be thrown
 * with the message given.
 * <p>
 * Another use case is validating constructor parameters:
 *
 * <pre>
 * public TokenManager(final TokenFactory factory) {
 *     Reject.ifNull(factory, &quot;Cannot instantiate TokenManager with null TokenFactory&quot;);
 * }
 * </pre>
 *
 * Sometimes, constructor parameters are passed to ancestor constructors which
 * must be called first--thus, the {@code checkNotNull} syntax is available:
 *
 * <pre>
 *     import static org.forgerock.util.Reject.checkNotNull;
 *
 *     public TokenManager(final TokenFactory factory) {
 *         super(checkNotNull(factory));
 *     }
 * </pre>
 *
 * Note that the methods herein throw generic RuntimeExceptions as opposed to
 * custom, application-specific error Exceptions. This class is intended for
 * wide use among multiple projects whose Exception frameworks may differ. The
 * implementer is encouraged to catch the generic exceptions thrown by this
 * class and rethrow exceptions appropriate to the target application.
 */
public final class Reject {

    /**
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is
     * null, returns the object otherwise.
     *
     * @param <T>
     *            The type of object to test.
     * @param object
     *            the object to test
     * @return the object
     * @throws NullPointerException
     *             if {@code object} is null
     */
    public static <T> T checkNotNull(final T object) {
        return checkNotNull(object, null);
    }

    /**
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is
     * null, returns the object otherwise.
     *
     * @param <T>
     *            The type of object to test.
     * @param object
     *            the object to test
     * @param message
     *            a custom exception message to use
     * @return the object
     * @throws NullPointerException
     *             if {@code object} is null
     */
    public static <T> T checkNotNull(final T object, final String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }

    /**
     * Throws an {@code IllegalArgumentException} if the <tt>condition</tt>
     * parameter is false.
     *
     * @param condition
     *            the condition to test
     * @throws IllegalArgumentException
     *             if {@code condition} is false
     */
    public static void ifFalse(final boolean condition) {
        ifFalse(condition, "Expected condition was true, found false");
    }

    /**
     * Throws an {@code IllegalArgumentException} with a custom {@code message}
     * if the <tt>condition</tt> parameter is false.
     *
     * @param condition
     *            the condition to test
     * @param message
     *            a custom exception message to use
     * @throws IllegalArgumentException
     *             if {@code condition} is false
     */
    public static void ifFalse(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Alias for {@code checkNotNull} to be used in fluent {@code Reject.ifNull}
     * syntax. Throws a {@code NullPointerException} if the <tt>object</tt>
     * parameter is null.
     *
     * @param object
     *            the object to test
     * @throws NullPointerException
     *             if {@code object} is null
     */
    public static void ifNull(final Object object) {
        ifNull(object, null);
    }

    /**
     * Throws a {@code NullPointerException} if any of the provided arguments
     * are {@code null}.
     *
     * @param <T>
     *            The type of object to test.
     * @param objects
     *            The objects to test.
     * @throws NullPointerException
     *             If any of the provided arguments are {@code null}.
     */
    @SafeVarargs
    public static <T> void ifNull(final T... objects) {
        /*
         * This method is generic in order to play better with varargs.
         * Otherwise invoking this method with an array of Strings will be
         * flagged with a warning because of the potential ambiguity. See
         * org.forgerock.util.RejectTest.ifNullVarArgsStrings().
         */
        for (final Object o : objects) {
            if (o == null) {
                throw new NullPointerException();
            }
        }
    }

    /**
     * Alias for {@code checkNotNull} to be used in fluent {@code Reject.ifNull}
     * syntax. Throws a {@code NullPointerException} if the <tt>object</tt>
     * parameter is null.
     *
     * @param object
     *            the object to test
     * @param message
     *            a custom exception message to use
     * @throws NullPointerException
     *             if {@code object} is null
     */
    public static void ifNull(final Object object, final String message) {
        checkNotNull(object, message);
    }

    /**
     * Throws an {@code IllegalArgumentException} if the <tt>condition</tt>
     * parameter is true.
     *
     * @param condition
     *            the condition to test
     * @throws IllegalArgumentException
     *             if {@code condition} is true
     */
    public static void ifTrue(final boolean condition) {
        ifTrue(condition, "Expected condition was false, found true");
    }

    /**
     * Throws an {@code IllegalArgumentException} with a custom {@code message}
     * if the <tt>condition</tt> parameter is true.
     *
     * @param condition
     *            the condition to test
     * @param message
     *            a custom exception message to use
     * @throws IllegalArgumentException
     *             if {@code condition} is true
     */
    public static void ifTrue(final boolean condition, final String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws an {@code IllegalStateException} with a custom {@code message}
     * if the <tt>condition</tt> parameter is true.
     *
     * @param condition
     *            the condition to test
     * @param message
     *            a custom exception message to use
     * @throws IllegalStateException
     *             if {@code condition} is true
     */
    public static void rejectStateIfTrue(final boolean condition, final String message) {
        if (condition) {
            throw new IllegalStateException(message);
        }
    }

    // Prevent instantiation
    private Reject() {
        // nothing to do
    }

}
