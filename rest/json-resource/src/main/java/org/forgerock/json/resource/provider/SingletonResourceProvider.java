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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource.provider;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;

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
 */
public interface SingletonResourceProvider {

    /**
     * Performs the provided
     * {@link RequestHandler#handleAction(Context, ActionRequest, ResultHandler)
     * action} against the single resource instance.
     *
     * @param context
     *            The request context.
     * @param request
     *            The action request. The resource ID will be {@code null}.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleAction(Context, ActionRequest, ResultHandler)
     */
    void actionInstance(Context context, ActionRequest request, ResultHandler<JsonValue> handler);

    /**
     * {@link RequestHandler#handlePatch(Context, PatchRequest, ResultHandler)
     * Patches} the single resource instance.
     *
     * @param context
     *            The request context.
     * @param request
     *            The patch request. The resource ID will be {@code null}.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handlePatch(Context, PatchRequest, ResultHandler)
     */
    void patchInstance(Context context, PatchRequest request, ResultHandler<Resource> handler);

    /**
     * {@link RequestHandler#handleRead(Context, ReadRequest, ResultHandler) Reads}
     * the single resource instance.
     *
     * @param context
     *            The request context.
     * @param request
     *            The read request. The resource ID will be {@code null}.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleRead(Context, ReadRequest, ResultHandler)
     */
    void readInstance(Context context, ReadRequest request, ResultHandler<Resource> handler);

    /**
     * {@link RequestHandler#handleUpdate(Context, UpdateRequest, ResultHandler)
     * Updates} the single resource instance.
     *
     * @param context
     *            The request context.
     * @param request
     *            The update request. The resource ID will be {@code null}.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleUpdate(Context, UpdateRequest, ResultHandler)
     */
    void updateInstance(Context context, UpdateRequest request, ResultHandler<Resource> handler);

}
