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

package org.forgerock.util;

import static org.forgerock.util.Reject.checkNotNull;

import java.io.Closeable;

/**
 * {@link Function} that silently closes an input-parameter after a delegate-function's {@link Function#apply(Object)}
 * is invoked. The static {@link #closeSilently(Function)} method is provided for convenience.
 *
 * @param <VIN>
 *            The type of the function input-parameter, which implements {@code Closeable}.
 * @param <VOUT>
 *            The type of the function result, or {@link Void} if the function
 *            does not return anything (i.e. it only has side-effects).
 * @param <E>
 *            The type of the exception thrown by the function, or
 *            {@link org.forgerock.util.promise.NeverThrowsException} if no exception is thrown by the
 *            function.
 */
public class CloseSilentlyFunction<VIN extends Closeable, VOUT, E extends Exception> implements Function<VIN, VOUT, E> {

    private final Function<VIN, VOUT, E> delegate;

    /**
     * Creates a new {@code CloseSilentlyFunction} instance.
     *
     * @param delegate Delegate function.
     */
    public CloseSilentlyFunction(final Function<VIN, VOUT, E> delegate) {
        this.delegate = checkNotNull(delegate);
    }

    /**
     * Invokes the delegate function's {@link Function#apply(Object)} with the input parameter {@code value}, closes it,
     * and returns the result.
     *
     * @param value {@code Closeable} input parameter.
     * @return The result of applying delegate function to {@code value}.
     * @throws E Propagates {@code Exception} thrown by delegate {@link Function}.
     */
    @Override
    public VOUT apply(final VIN value) throws E {
        try {
            return delegate.apply(value);
        } finally {
            Utils.closeSilently(value);
        }
    }

    /**
     * Wraps a delegate function in a {@code CloseSilentlyFunction}.
     *
     * @param delegate Delegate function.
     * @param <IN>
     *          The type of the function input-parameter, which implements {@code Closeable}.
     * @param <OUT>
     *          The type of the function result, or {@link Void} if the function does not return anything
     *          (i.e. it only has side-effects).
     * @param <EX> The type of the exception thrown by the function, or
     *            {@link org.forgerock.util.promise.NeverThrowsException} if no exception is thrown by the function.
     * @return New {@code CloseSilentlyFunction} instance.
     */
    public static <IN extends Closeable, OUT, EX extends Exception> Function<IN, OUT, EX> closeSilently(
            final Function<IN, OUT, EX> delegate) {
        return new CloseSilentlyFunction<>(delegate);
    }

}
