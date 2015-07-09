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
 * Copyright 2014 ForgeRock Inc.
 */
package org.forgerock.util;


/**
 * A synchronous function which returns a result immediately.
 * <p>
 * <b>Exception handling</b>: implementations which do not throw any exceptions
 * should declare that they throw an exception of type
 * {@link org.forgerock.util.promise.NeverThrowsException}.
 * <p>
 * Example usage:
 *
 * <pre>
 * public class IsPossiblePrime implements Function&lt;String, Boolean, IllegalArgumentException&gt; {
 *     public Boolean apply(String value) throws IllegalArgumentException {
 *         // Parse the parameter now and potentially immediately throw an
 *         // exception.
 *         final BigInteger possiblePrime = new BigInteger(value);
 *
 *         // Determine if the parameter is a prime number.
 *         return possiblePrime.isProbablePrime(1000);
 *     }
 * }
 * </pre>
 *
 * @param <VIN>
 *            The type of the function parameter, or {@link Void} if the
 *            function does not expect a parameter.
 * @param <VOUT>
 *            The type of the function result, or {@link Void} if the function
 *            does not return anything (i.e. it only has side-effects).
 * @param <E>
 *            The type of the exception thrown by the function, or
 *            {@link org.forgerock.util.promise.NeverThrowsException} if no exception is thrown by the
 *            function.
 * @see AsyncFunction
 * @see org.forgerock.util.promise.NeverThrowsException
 */
// @FunctionalInterface
public interface Function<VIN, VOUT, E extends Exception> {
    /**
     * Applies this function to the input parameter {@code value} and returns
     * the result.
     *
     * @param value
     *            The input parameter.
     * @return The result of applying this function to {@code value}.
     * @throws E
     *             If this function cannot be applied to {@code value}.
     */
    VOUT apply(VIN value) throws E;
}
