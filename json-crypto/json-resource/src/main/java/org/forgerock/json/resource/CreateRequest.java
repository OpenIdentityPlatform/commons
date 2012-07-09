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

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * A request to create a new JSON resource.
 */
public interface CreateRequest extends Request {

    /**
     * {@inheritDoc}
     */
    @Override
    CreateRequest addFieldFilter(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    CreateRequest addFieldFilter(String... fields);

    /**
     * Returns the content of the JSON resource to be created.
     *
     * @return The content of the JSON resource to be created.
     */
    JsonValue getContent();

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
     * <p>
     * The resource ID may be {@code null} indicating that the resource provider
     * should generate a resource ID on behalf of the client.
     */
    @Override
    String getResourceId();

    /**
     * Sets the content of the JSON resource to be created.
     *
     * @param content
     *            The content of the JSON resource to be created.
     * @return This create request.
     * @throws UnsupportedOperationException
     *             If this create request does not permit changes to the
     *             content.
     */
    CreateRequest setContent(JsonValue content);

    /**
     * {@inheritDoc}
     */
    @Override
    CreateRequest setComponent(String path);

    /**
     * {@inheritDoc}
     * <p>
     * The resource ID may be {@code null} indicating that the resource provider
     * should generate a resource ID on behalf of the client.
     */
    @Override
    CreateRequest setResourceId(String id);
}
