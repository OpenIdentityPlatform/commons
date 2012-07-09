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

/**
 * A request to read a single identified JSON resource.
 */
public interface ReadRequest extends Request {

    /**
     * {@inheritDoc}
     */
    @Override
    ReadRequest addFieldFilter(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    ReadRequest addFieldFilter(String... fields);

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
     * {@inheritDoc}
     */
    @Override
    ReadRequest setComponent(String path);

    /**
     * {@inheritDoc}
     */
    @Override
    ReadRequest setResourceId(String id);
}
