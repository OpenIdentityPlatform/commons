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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.util;

/**
 * <p>
 * A input parameter-validating utility class using fluent invocation:
 * <pre>
 *     public int divide(int dividend, int divisor) {
 *         Reject.ifTrue(divisor == 0, "Division by zero not supported");
 *         return dividend / divisor;
 *     }
 * </pre>
 * will cause an {@code IllegalArgumentException} to be thrown with the message given.
 * <p>
 * Another use case is validating constructor parameters:
 * <pre>
 *     public TokenManager(final TokenFactory factory) {
 *         Reject.ifNull(factory, "Cannot instantiate TokenManager with null TokenFactory");
 *     }
 * </pre>
 * Sometimes, constructor parameters are passed to ancestor constructors which must be
 * called first--thus, the {@code checkNotNull} syntax is available:
 * <pre>
 *     import static org.forgerock.util.Reject.checkNotNull;
 *
 *     public TokenManager(final TokenFactory factory) {
 *         super(checkNotNull(factory));
 *     }
 * </pre>
 * </p>
 * <p>
 * Note that the methods herein throw generic RuntimeExceptions as opposed to custom,
 * application-specific error Exceptions.  This class is intended for wide use among
 * multiple projects whose Exception frameworks may differ.  The implementer is encouraged
 * to catch the generic exceptions thrown by this class and rethrow exceptions appropriate
 * to the target application.
 * </p>
 */
public class Reject {

    // Prevent instantiation
    private Reject() {
        // nothing to do
    }

    /**
     * Alias for {@code checkNotNull} to be used in fluent {@code Reject.ifNull} syntax.
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is null.
     *
     * @param object the object to test
     * @throws NullPointerException if {@code object} is null
     */
    public static void ifNull(final Object object) {
        ifNull(object, null);
    }

    /**
     * Alias for {@code checkNotNull} to be used in fluent {@code Reject.ifNull} syntax.
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is null.
     *
     * @param object the object to test
     * @param message a custom exception message to use
     * @throws NullPointerException if {@code object} is null
     */
    public static void ifNull(final Object object, final String message) {
        checkNotNull(object, message);
    }

    /**
     * Alias for {@code checkTrue} to be used in fluent {@code Reject.ifFalse} syntax.
     * Throws an {@code IllegalArgumentException} if the <tt>value</tt> parameter is false.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if {@code value} is false
     */
    public static void ifFalse(final boolean value) {
        checkTrue(value);
    }

    /**
     * Alias for {@code checkFalse} to be used in fluent {@code Reject.ifTrue} syntax.
     * Throws an {@code IllegalArgumentException} if the <tt>value</tt> parameter is true.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if {@code value} is true
     */
    public static void ifTrue(final boolean value) {
        checkFalse(value);
    }

    /**
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is null,
     * returns the object otherwise.
     *
     * @param object the object to test
     * @return the object
     * @throws NullPointerException if {@code object} is null
     */
    public static <T> T checkNotNull(final T object) {
        return checkNotNull(object, null);
    }

    /**
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is null,
     * returns the object otherwise.
     *
     * @param object the object to test
     * @param message a custom exception message to use
     * @return the object
     * @throws NullPointerException if {@code object} is null
     */
    public static <T> T checkNotNull(final T object, final String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }

    /**
     * Tests that the {@code value} parameter is false.
     * Throws an @{code IllegalArgumentException} with a custom {@code message} if not.
     *
     * @param value the value to test
     * @param message a custom exception message to use
     * @throws IllegalArgumentException if {@code value} is true
     */
    public static boolean checkFalse(final boolean value, String message) {
        if (value) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Tests that the {@code value} parameter is true.
     * Throws an @{code IllegalArgumentException} with a custom {@code message} if not.
     *
     * @param value the value to test
     * @param message a custom exception message to use
     * @throws IllegalArgumentException if {@code value} is false
     */
    public static boolean checkTrue(final boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Tests that the {@code value} parameter is false.
     * Throws an @{code IllegalArgumentException} with a default message if not.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if {@code value} is false
     */
    public static boolean checkFalse(final boolean value) {
        return checkFalse(value, "Expected value was false, found true");
    }

    /**
     * Tests that the {@code value} parameter is true.
     * Throws an @{code IllegalArgumentException} with a default message if not.
     *
     * @param value the value to test
     * @throws IllegalArgumentException if {@code value} is false
     */
    public static boolean checkTrue(final boolean value) {
        return checkTrue(value, "Expected value was true, found false");
    }

}
