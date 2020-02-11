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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;


/**
 * A visitor of {@code Request}s, in the style of the visitor design pattern.
 * <p>
 * Classes implementing this interface can perform actions based on the type of
 * a request in a type-safe manner. When a visitor is passed to a request's
 * accept method, the corresponding visit method associated with the type of the
 * request is invoked.
 *
 * @param <R>
 *            The return type of this visitor's methods. Use
 *            {@link java.lang.Void} for visitors that do not need to return
 *            results.
 * @param <P>
 *            The type of the additional parameter to this visitor's methods.
 *            Use {@link java.lang.Void} for visitors that do not need an
 *            additional parameter.
 */
public interface RequestVisitor<R, P> {

    /**
     * Visits an action request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The action request.
     * @return Returns a visitor specified result.
     */
    R visitActionRequest(P p, ActionRequest request);

    /**
     * Visits a create request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The create request.
     * @return Returns a visitor specified result.
     */
    R visitCreateRequest(P p, CreateRequest request);

    /**
     * Visits a delete request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The delete request.
     * @return Returns a visitor specified result.
     */
    R visitDeleteRequest(P p, DeleteRequest request);

    /**
     * Visits a patch request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The patch request.
     * @return Returns a visitor specified result.
     */
    R visitPatchRequest(P p, PatchRequest request);

    /**
     * Visits a query request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The query request.
     * @return Returns a visitor specified result.
     */
    R visitQueryRequest(P p, QueryRequest request);

    /**
     * Visits a read request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The read request.
     * @return Returns a visitor specified result.
     */
    R visitReadRequest(P p, ReadRequest request);

    /**
     * Visits an update request.
     *
     * @param p
     *            A visitor specified parameter.
     * @param request
     *            The update request.
     * @return Returns a visitor specified result.
     */
    R visitUpdateRequest(P p, UpdateRequest request);

}
