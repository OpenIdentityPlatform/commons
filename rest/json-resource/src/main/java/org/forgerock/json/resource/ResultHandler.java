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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;


/**
 * A completion handler for consuming the result of an asynchronous operation or
 * connection attempts.
 * <p>
 * A result completion handler may be specified when performing asynchronous
 * requests using a {@link Connection} object or when connecting asynchronously
 * to a JSON resource provider using a {@link ConnectionFactory}. The
 * {@link #handleResult} method is invoked when the request or connection
 * attempt completes successfully. The {@link #handleError} method is invoked if
 * the request or connection attempt fails.
 * <p>
 * Implementations of these methods should complete in a timely manner so as to
 * avoid keeping the invoking thread from dispatching to other completion
 * handlers.
 *
 * @param <V>
 *            The type of result handled by this result handler.
 */
public interface ResultHandler<V> {

    /**
     * Invoked when the asynchronous request has failed.
     *
     * @param error
     *            The resource exception indicating why the asynchronous request
     *            has failed.
     */
    void handleError(ResourceException error);

    /**
     * Invoked when the asynchronous request has completed successfully.
     *
     * @param result
     *            The result of the asynchronous request.
     */
    void handleResult(V result);
}
