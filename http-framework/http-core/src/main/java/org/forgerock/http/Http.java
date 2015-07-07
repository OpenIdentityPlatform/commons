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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http;

import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

/**
 * Utility methods for creating common types of handler and filters.
 */
public final class Http {

    /**
     * Returns a {@link Promise} representing the {@code Response} for an
     * asynchronous {@code Request} which has already completed. Attempts to get
     * the {@code Response} will immediately return without blocking, and any
     * listeners registered against the returned promise will be immediately
     * invoked in the same thread as the caller.
     *
     * @param response
     *            The {@code Response}.
     * @return A {@link Promise} representing the {@code Response} for an
     *         asynchronous {@code Request} which has already completed.
     */
    public static Promise<Response, NeverThrowsException> newResponsePromise(Response response) {
        return Response.newResponsePromise(response);
    }

    /**
     * Creates a new pending {@link Promise} implementation representing the
     * {@code Response} for an asynchronous {@code Request}. The returned
     * {@link PromiseImpl} must be completed once the {@code Response} is
     * received by invoking the {@link PromiseImpl#handleResult(Object)
     * handleResult} method.
     *
     * @return A new pending {@link Promise} implementation representing the
     *         {@code Response} for an asynchronous {@code Request}.
     */
    public static PromiseImpl<Response, NeverThrowsException> newResponsePromiseImpl() {
        return Response.newResponsePromiseImpl();
    }


    private Http() {
        // Prevent instantiation.
    }
}
