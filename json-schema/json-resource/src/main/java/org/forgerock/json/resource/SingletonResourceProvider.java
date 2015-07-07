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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;

/**
 * An implementation interface for resource providers which exposes a single
 * permanent resource instance. A singleton resource may support the following
 * operations:
 * <ul>
 * <li>action
 * <li>patch
 * <li>read
 * <li>update
 * </ul>
 * More specifically, a singleton resource cannot be created, deleted, or
 * queried and may only support a limited sub-set of actions.
 * <p>
 * <b>NOTE:</b> field filtering alters the structure of a JSON resource and MUST
 * only be performed once while processing a request. It is therefore the
 * responsibility of front-end implementations (e.g. HTTP listeners, Servlets,
 * etc) to perform field filtering. Request handler and resource provider
 * implementations SHOULD NOT filter fields, but MAY choose to optimise their
 * processing in order to return a resource containing only the fields targeted
 * by the field filters.
 */
public interface SingletonResourceProvider {

    /**
     * Performs the provided
     * {@link RequestHandler#handleAction(ServerContext, ActionRequest)
     * action} against the single resource instance.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleAction(ServerContext, ActionRequest)
     */
    Promise<JsonValue, ResourceException> actionInstance(ServerContext context, ActionRequest request);

    /**
     * {@link RequestHandler#handlePatch(ServerContext, PatchRequest)
     * Patches} the single resource instance.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The patch request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handlePatch(ServerContext, PatchRequest)
     */
    Promise<Resource, ResourceException> patchInstance(ServerContext context, PatchRequest request);

    /**
     * {@link RequestHandler#handleRead(ServerContext, ReadRequest)
     * Reads} the single resource instance.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The read request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleRead(ServerContext, ReadRequest)
     */
    Promise<Resource, ResourceException> readInstance(ServerContext context, ReadRequest request);

    /**
     * {@link RequestHandler#handleUpdate(ServerContext, UpdateRequest)
     * Updates} the single resource instance.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The update request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleUpdate(ServerContext, UpdateRequest)
     */
    Promise<Resource, ResourceException> updateInstance(ServerContext context, UpdateRequest request);
}
