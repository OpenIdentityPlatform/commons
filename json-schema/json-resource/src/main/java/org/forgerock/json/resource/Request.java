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

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * Common attributes of all JSON resource requests.
 */
public interface Request {

    // TODO: many of these fields are not used by action requests. Perhaps they
    // should be pushed down? For example, a bulk update operation would not use
    // any of these parameters.

    // TODO: include support for something similar to LDAP controls?

    /**
     * The name of the field which contains the fields in the JSON
     * representation.
     */
    public static final String FIELD_FIELDS = "fields";

    /**
     * The name of the field which contains the resource name in the JSON
     * representation.
     */
    public static final String FIELD_RESOURCE_NAME = "resourceName";

    /**
     * The name of the field which contains the additional query parameters in
     * the JSON representation.
     */
    public static final String FIELD_ADDITIONAL_PARAMETERS = "additionalParameters";

    /**
     * Applies a {@code RequestVisitor} to this {@code Request}.
     *
     * @param <R>
     *            The return type of the visitor's methods.
     * @param <P>
     *            The type of the additional parameters to the visitor's
     *            methods.
     * @param v
     *            The request visitor.
     * @param p
     *            Optional additional visitor parameter.
     * @return A result as specified by the visitor.
     */
    <R, P> R accept(final RequestVisitor<R, P> v, final P p);

    /**
     * Adds one or more fields which should be included with each JSON resource
     * returned by this request.
     *
     * @param fields
     *            The fields which should be included with each JSON resource
     *            returned by this request.
     * @return This request.
     * @throws UnsupportedOperationException
     *             If this request does not permit changes to the fields.
     */
    Request addField(JsonPointer... fields);

    /**
     * Adds one or more fields which should be included with each JSON resource
     * returned by this request.
     *
     * @param fields
     *            The fields which should be included with each JSON resource
     *            returned by this request.
     * @return This request.
     * @throws IllegalArgumentException
     *             If one or more of the provided field identifiers could not be
     *             parsed as a JSON pointer.
     * @throws UnsupportedOperationException
     *             If this request does not permit changes to the fields.
     */
    Request addField(String... fields);

    /**
     * Returns the list of fields which should be included with each JSON
     * resource returned by this request. The returned list may be modified if
     * permitted by this query request. An empty list indicates that all fields
     * should be included.
     * <p>
     * <b>NOTE:</b> field filtering alters the structure of a JSON resource and
     * MUST only be performed once while processing a request. It is therefore
     * the responsibility of front-end implementations (e.g. HTTP listeners,
     * Servlets, etc) to perform field filtering. Request handler and resource
     * provider implementations SHOULD NOT filter fields, but MAY choose to
     * optimise their processing in order to return a resource containing only
     * the fields targeted by the field filters.
     *
     * @return The list of fields which should be included with each JSON
     *         resource returned by this request (never {@code null}).
     */
    List<JsonPointer> getFields();

    /**
     * Returns the type of this request.
     *
     * @return The type of this request.
     */
    RequestType getRequestType();

    /**
     * Returns the non-{@code null} name of the JSON resource to which this
     * request should be targeted. The resource name is relative and never
     * begins or ends with a forward slash, but may be empty.
     * <p>
     * <b>NOTE</b>: for resource provider implementations the resource name is
     * relative to the current resource being accessed. See the description of
     * {@link org.forgerock.http.RouterContext} for more information.
     *
     * @return The non-{@code null} name of the JSON resource to which this
     *         request should be targeted, which may be the empty string.
     */
    String getResourceName();

    /**
     * Returns the non-{@code null} name of the JSON resource to which this
     * request should be targeted. The resource name is relative and never
     * begins or ends with a forward slash, but may be empty.
     * <p>
     * <b>NOTE</b>: for resource provider implementations the resource name is
     * relative to the current resource being accessed. See the description of
     * {@link org.forgerock.http.RouterContext} for more information.
     *
     * @return The non-{@code null} name of the JSON resource to which this
     *         request should be targeted, which may be the empty string.
     */
    ResourceName getResourceNameObject();

    /**
     * Returns the additional parameters which should be used to control the
     * behavior of this action request. The returned map may be modified if
     * permitted by this action request.
     *
     * @return The additional parameters which should be used to control the
     *         behavior of this action request (never {@code null}).
     */
    Map<String, String> getAdditionalParameters();

    /**
     * Returns the additional parameter which should be used to control the behavior
     * of this action request.
     *
     * @param name
     *            The name of the additional parameter.
     * @return The additional parameter which should be used to control the
     *         behavior of this action request
     */
    String getAdditionalParameter(String name);

    /**
     * Sets the non-{@code null} name of the JSON resource to which this request
     * should be targeted. The resource name is relative and never begins or
     * ends with a forward slash, but may be empty.
     * <p>
     * <b>NOTE</b>: for resource provider implementations the resource name is
     * relative to the current resource being accessed. See the description of
     * {@link org.forgerock.http.RouterContext} for more information.
     *
     * @param name
     *            The non-{@code null} name of the JSON resource to which this
     *            request should be targeted, which may be the empty string.
     *            The name should be URL-encoded.
     * @return This request.
     * @throws UnsupportedOperationException
     *             If this request does not permit changes to the JSON resource
     *             name.
     */
    Request setResourceName(String name);

    /**
     * Sets the non-{@code null} name of the JSON resource to which this request
     * should be targeted. The resource name is relative and never begins or
     * ends with a forward slash, but may be empty.
     * <p>
     * <b>NOTE</b>: for resource provider implementations the resource name is
     * relative to the current resource being accessed. See the description of
     * {@link org.forgerock.http.RouterContext} for more information.
     *
     * @param name
     *            The non-{@code null} name of the JSON resource to which this
     *            request should be targeted, which may be the empty string.
     * @return This request.
     * @throws UnsupportedOperationException
     *             If this request does not permit changes to the JSON resource
     *             name.
     */
    Request setResourceName(ResourceName name);

    /**
     * Sets an additional parameter which should be used to control the behavior
     * of this action request.
     *
     * @param name
     *            The name of the additional parameter.
     * @param value
     *            The additional parameter's value.
     * @return This action request.
     * @throws BadRequestException
     *             If this request does not permit the additional parameter to be set.
     * @throws UnsupportedOperationException
     *             If this request does not permit changes to the
     *             additional parameters.
     */
    Request setAdditionalParameter(String name, String value) throws BadRequestException;

    /**
     * Return a JsonValue representation of this request.
     *
     * @return this request as a JsonValue
     */
    JsonValue toJsonValue();
}
