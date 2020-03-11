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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

/**
 * {@link QueryResourceHandler} that searches for a specific identifier value.
 */
public class IdentifierQueryResourceHandler implements QueryResourceHandler {
    private final String id;
    private ResourceResponse resourceResponse;

    /**
     * Creates a new {@link QueryResourceHandler} for the given identifier.
     *
     * @param id Identifier to query for
     */
    public IdentifierQueryResourceHandler(final String id) {
        this.id = checkNotNull(id);
    }

    @Override
    public boolean handleResource(final ResourceResponse resource) {
        if (id.equals(resource.getId())) {
            resourceResponse = resource;
            return false;
        }
        return true;
    }

    /**
     * Gets the identifier being queried for.
     *
     * @return Identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the {@link ResourceResponse} query result.
     *
     * @return {@link ResourceResponse} or {@code null} if not yet found
     */
    public ResourceResponse getResourceResponse() {
        return resourceResponse;
    }
}
