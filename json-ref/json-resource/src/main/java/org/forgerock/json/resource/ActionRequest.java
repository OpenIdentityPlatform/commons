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
 * Copyright 2012-2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * An implementation specific action, or operation, upon a JSON resource.
 */
public interface ActionRequest extends Request {
    /**
     * The name of the field which contains the action ID in the JSON
     * representation.
     */
    public static final String FIELD_ACTION = "action";

    /**
     * The name of the field which contains the additional action parameters in
     * the JSON representation.
     */
    public static final String FIELD_ADDITIONAL_PARAMETERS = "additionalParameters";

    /**
     * The name of the field which contains the action content in the JSON
     * representation.
     */
    public static final String FIELD_CONTENT = "content";

    /**
     * The name of the action which is reserved for performing "create"
     * operations.
     */
    public static final String ACTION_ID_CREATE = "create";

    /**
     * {@inheritDoc}
     */
    <R, P> R accept(RequestVisitor<R, P> v, P p);

    /**
     * {@inheritDoc}
     */
    @Override
    ActionRequest addField(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    ActionRequest addField(String... fields);

    /**
     * Returns the ID of the action to be performed by this action request.
     *
     * @return The ID of the action to be performed by this action request.
     */
    String getAction();

    /**
     * Returns the additional parameters which should be used to control the
     * behavior of this action request. The returned map may be modified if
     * permitted by this action request.
     *
     * @return The additional parameters which should be used to control the
     *         behavior of this action request (never {@code null}).
     * @deprecated Use {@link ActionRequest#getAdditionalParameters} instead.
     */
    @Deprecated
    Map<String, String> getAdditionalActionParameters();

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
     * Returns the content of this action request. The structure of the content
     * is defined by the action.
     *
     * @return The content of this action request.
     */
    JsonValue getContent();

    /**
     * {@inheritDoc}
     */
    @Override
    List<JsonPointer> getFields();

    /**
     * {@inheritDoc}
     */
    @Override
    RequestType getRequestType();

    /**
     * {@inheritDoc}
     */
    @Override
    String getResourceName();

    /**
     * {@inheritDoc}
     */
    @Override
    ResourceName getResourceNameObject();

    /**
     * Sets the ID of the action to be performed by this action request.
     *
     * @param id
     *            The ID of the action to be performed by this action request.
     * @return This action request.
     * @throws UnsupportedOperationException
     *             If this action request does not permit changes to the action
     *             ID.
     */
    ActionRequest setAction(String id);

    /**
     * Sets an additional parameter which should be used to control the behavior
     * of this action request.
     *
     * @param name
     *            The name of the additional parameter.
     * @param value
     *            The additional parameter's value.
     * @return This action request.
     * @throws UnsupportedOperationException
     *             If this action request does not permit changes to the
     *             additional parameters.
     * @deprecated Use {ActionRequest#setAdditionalParameter} instead.
     */
    @Deprecated
    ActionRequest setAdditionalActionParameter(String name, String value);

    /**
     * Sets an additional parameter which should be used to control the behavior
     * of this action request.
     *
     * @param name
     *            The name of the additional parameter.
     * @param value
     *            The additional parameter's value.
     * @return This action request.
     * @throws UnsupportedOperationException
     *             If this action request does not permit changes to the
     *             additional parameters.
     */
    ActionRequest setAdditionalParameter(String name, String value);
    /**
     * Sets the content of this action request. The structure of the content is
     * defined by the action.
     *
     * @param content
     *            The content of this action request.
     * @return This action request.
     * @throws UnsupportedOperationException
     *             If this action request does not permit changes to the
     *             content.
     */
    ActionRequest setContent(JsonValue content);

    /**
     * {@inheritDoc}
     */
    @Override
    Request setResourceName(ResourceName name);

    /**
     * {@inheritDoc}
     */
    @Override
    ActionRequest setResourceName(String name);
}
