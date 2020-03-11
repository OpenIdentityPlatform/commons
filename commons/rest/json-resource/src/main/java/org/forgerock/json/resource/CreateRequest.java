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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.List;
import java.util.Map;

import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A request to create a new JSON resource.
 */
public interface CreateRequest extends Request {

    /**
     * The name of the action which is reserved for performing "create" operations.
     */
    String ACTION_ID_CREATE = CreateRequest.ACTION_ID_CREATE;

    /**
     * The name of the field which contains the resource content in the JSON representation.
     */
    String FIELD_CONTENT = "content";

    /**
     * The name of the field which contains the new resource ID in the JSON representation.
     */
    String FIELD_NEW_RESOURCE_ID = "newResourceId";


    @Override
    <R, P> R accept(final RequestVisitor<R, P> v, final P p);


    @Override
    CreateRequest addField(JsonPointer... fields);


    @Override
    CreateRequest addField(String... fields);


    @Override
    String getAdditionalParameter(String name);


    @Override
    Map<String, String> getAdditionalParameters();

    /**
     * Returns the content of the JSON resource to be created.
     *
     * @return The content of the JSON resource to be created.
     */
    JsonValue getContent();


    @Override
    List<JsonPointer> getFields();

    /**
     * Returns the client provided ID of the resource to be created. The new resource ID will be appended to the
     * resource path in order to obtain the full path of the new resource.
     * <p>
     * The new resource ID is optional and should be used in cases where the client wishes to determine the path of the
     * resource to be created. If the new resource ID is not provided then the server will be responsible for generating
     * the ID of the new resource.
     *
     * @return The client provided ID of the resource to be created, or {@code null} if the server should be responsible
     * for generating the resource ID.
     * @see #getResourcePath()
     */
    String getNewResourceId();


    @Override
    PreferredLocales getPreferredLocales();


    @Override
    RequestType getRequestType();

    /**
     * Returns the path of the JSON resource container beneath which the new resource should be created.
     * <p>
     * The path of the newly created resource will be the concatenation of the resource path and either the client
     * provided resource ID, if provided, or a server generated resource ID.
     *
     * @return The path of the JSON resource container beneath which the new resource should be created.
     * @see #getNewResourceId()
     */
    @Override
    String getResourcePath();


    @Override
    ResourcePath getResourcePathObject();


    @Override
    Version getResourceVersion();


    @Override
    CreateRequest setAdditionalParameter(String name, String value) throws BadRequestException;

    /**
     * Sets the content of the JSON resource to be created.
     *
     * @param content
     *         The content of the JSON resource to be created.
     * @return This create request.
     * @throws UnsupportedOperationException
     *         If this create request does not permit changes to the content.
     */
    CreateRequest setContent(JsonValue content);

    /**
     * Sets the client provided ID of the resource to be created. The new resource ID will be appended to the resource
     * path in order to obtain the full path of the new resource.
     * <p>
     * The new resource ID is optional and should be used in cases where the client wishes to determine the path of the
     * resource to be created. If the new resource ID is not provided then the server will be responsible for generating
     * the ID of the new resource.
     *
     * @param id
     *         The client provided ID of the resource to be created, or {@code null} if the server should be responsible
     *         for generating the resource ID.
     * @return This create request.
     * @throws UnsupportedOperationException
     *         If this create request does not permit changes to the new resource ID.
     * @see #setResourcePath(String)
     */
    CreateRequest setNewResourceId(String id);


    @Override
    CreateRequest setPreferredLocales(PreferredLocales preferredLocales);

    /**
     * Sets the path of the JSON resource container beneath which the new resource should be created.
     * <p>
     * The path of the newly created resource will be the concatenation of the resource path and either the client
     * provided resource ID, if provided, or a server generated resource ID.
     *
     * @param path
     *         The path of the JSON resource container beneath which the new resource should be created.
     * @return This create request.
     * @throws UnsupportedOperationException
     *         If this create request does not permit changes to the resource path.
     * @see #setNewResourceId(String)
     */
    @Override
    CreateRequest setResourcePath(String path);


    @Override
    CreateRequest setResourcePath(ResourcePath path);


    @Override
    CreateRequest setResourceVersion(Version resourceVersion);


    @Override
    JsonValue toJsonValue();
}
