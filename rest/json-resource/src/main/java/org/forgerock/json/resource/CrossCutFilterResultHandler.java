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

package org.forgerock.json.resource;

import org.forgerock.resource.core.ServerContext;

/**
 * A call-back which must be invoked by cross-cutting filters once a request has
 * been filtered.
 *
 * @param <C>
 *            The type of filter state to be maintained between request
 *            notification and response notification. Use {@code Void} if the
 *            filter is stateless and no filtering state information is
 *            required.
 * @param <R>
 *            The type of result to be returned if processing of the request is
 *            to stopped and a result returned immediately.
 */
public interface CrossCutFilterResultHandler<C, R> {
    /**
     * Indicates that the request should continue to be processed by the
     * remained of the filter chain.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state to be included when filtering the response.
     */
    void handleContinue(ServerContext context, C state);

    /**
     * Indicates that processing of the request should stop and an error
     * response returned to the client.
     *
     * @param error
     *            The error response to be returned to the client.
     */
    void handleError(ResourceException error);

    /**
     * Indicates that processing of the request should stop and a result
     * returned to the client.
     *
     * @param result
     *            The result to be returned to the client.
     */
    void handleResult(R result);
}
