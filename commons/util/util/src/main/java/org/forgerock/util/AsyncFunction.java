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

import org.forgerock.util.promise.Promise;

/**
 * An asynchronous {@link Function} which returns a result at some point in the
 * future.
 * <p>
 * <b>Exception handling</b>: implementations may fail immediately if an error
 * is encountered before asynchronous processing begins (e.g. if the function
 * parameter is invalid). Implementations which do not throw any exceptions
 * should declare that they throw an exception of type
 * {@link org.forgerock.util.promise.NeverThrowsException}.
 * <p>
 * Example usage:
 *
 * <pre>
 * public class IsPossiblePrime implements AsyncFunction&lt;String, Boolean, IllegalArgumentException&gt; {
 *     // Executor for asynchronously computing primeness.
 *     private ExecutorService executor = Executors.newCachedThreadPool();
 *
 *     public Promise&lt;Boolean, IllegalArgumentException&gt; apply(String value)
 *             throws IllegalArgumentException {
 *         // Create a promise which will hold the asynchronous result.
 *         final PromiseImpl&lt;Boolean, IllegalArgumentException&gt; promise = PromiseImpl.create();
 *
 *         // Parse the parameter now and potentially immediately throw an
 *         // exception. Parsing could be deferred to the executor in which
 *         // case the exception should be trapped and promise.handleException()
 *         // invoked.
 *         final BigInteger possiblePrime = new BigInteger(value);
 *
 *         // Use an executor to asynchronously determine if the parameter is a
 *         // prime number.
 *         executor.execute(new Runnable() {
 *             &#064;Override
 *             public void run() {
 *                 // Set the promised result.
 *                 promise.handleResult(possiblePrime.isProbablePrime(1000));
 *             }
 *         });
 *         return promise;
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
 * @see Function
 * @see org.forgerock.util.promise.NeverThrowsException
 */
// @FunctionalInterface
public interface AsyncFunction<VIN, VOUT, E extends Exception> extends
        Function<VIN, Promise<? extends VOUT, ? extends E>, E> {

    /**
     * Asynchronously applies this function to the input parameter {@code value}
     * and returns a {@link Promise} for the result.
     *
     * @param value
     *            The input parameter.
     * @return The {@link Promise} representing the result of applying this
     *         function to {@code value}.
     * @throws E
     *             If this function cannot be applied to {@code value}.
     */
    @Override
    Promise<? extends VOUT, ? extends E> apply(VIN value) throws E;

}
