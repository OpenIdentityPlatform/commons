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
     * {@inheritDoc}
     */
    @Override
    ActionRequest addFieldFilter(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    ActionRequest addFieldFilter(String... fields);

    /**
     * Returns the identifier of the type of operation to be performed by this
     * action request.
     *
     * @return The identifier of the type of operation to be performed by this
     *         action request.
     */
    String getActionId();

    /**
     * Returns the additional parameters which should be used to control the
     * behavior of this action request. The returned map may be modified if
     * permitted by this action request.
     *
     * @return The additional parameters which should be used to control the
     *         behavior of this action request (never {@code null}).
     */
    Map<String, String> getAdditionalActionParameters();

    /**
     * {@inheritDoc}
     */
    @Override
    String getComponent();

    /**
     * {@inheritDoc}
     */
    @Override
    List<JsonPointer> getFieldFilters();

    /**
     * {@inheritDoc}
     */
    @Override
    String getResourceId();

    /**
     * Returns the content of this action request. The structure of the content
     * is defined by the action.
     *
     * @return The content of this action request.
     */
    JsonValue getContent();

    /**
     * Sets the identifier of the type of operation to be performed by this
     * action request.
     *
     * @param id
     *            The identifier of the type of operation to be performed by
     *            this action request.
     * @return This action request.
     * @throws UnsupportedOperationException
     *             If this action request does not permit changes to the action
     *             ID.
     */
    ActionRequest setActionId(String id);

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
    ActionRequest setAdditionalActionParameter(String name, String value);

    /**
     * {@inheritDoc}
     */
    @Override
    ActionRequest setComponent(String path);

    /**
     * {@inheritDoc}
     */
    @Override
    ActionRequest setResourceId(String id);

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
}
