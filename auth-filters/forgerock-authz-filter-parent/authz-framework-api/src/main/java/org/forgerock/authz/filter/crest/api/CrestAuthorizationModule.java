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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.crest.api;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * <p>A {@code CrestAuthorizationModule} authorizes client REST requests asynchronously.</p>
 *
 * <p>A module implementation should assume it may be used to authorize different requests from different clients. A
 * module should also assume it may be used concurrently by multiple callers. It is the module implementation's
 * responsibility to properly save and restore state as necessary. A module that does not need to do so may remain
 * completely stateless.</p>
 *
 * @since 1.5.0
 */
public interface CrestAuthorizationModule {

    /**
     * Returns the name of the authorization module.
     *
     * @return The module name.
     */
    String getName();

    /**
     * <p>Authorizes a received REST create request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link CreateRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context, CreateRequest request);

    /**
     * <p>Authorizes a received REST read request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link ReadRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request);

    /**
     * <p>Authorizes a received REST update request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link UpdateRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context, UpdateRequest request);

    /**
     * <p>Authorizes a received REST delete request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link DeleteRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizeDelete(Context context, DeleteRequest request);

    /**
     * <p>Authorizes a received REST patch request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link PatchRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest request);

    /**
     * <p>Authorizes a received REST action request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link ActionRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request);

    /**
     * <p>Authorizes a received REST query request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link ResourceException}</p>
     *
     * @param context The {@link Context} representing the context of the request.
     * @param request The {@link QueryRequest} to authorize.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code ResourceException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest request);
}
