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
 * Copyright 2014 ForgeRock AS. 
 */

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonValue;

/**
 * A {@link ServerContext} from an internal source.
 */
public class InternalServerContext extends ServerContext implements ClientContext {
    /** the client-friendly name of this context */
    private static final String CONTEXT_NAME = "internal-server";

    /**
     * {@inheritDoc}
     */
    public InternalServerContext(Context parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public InternalServerContext(JsonValue savedContext, PersistenceConfig config) throws ResourceException {
        super(savedContext, config);
    }

    /**
     * Get this Context's name
     *
     * @return this object's name
     */
    public String getContextName() {
        return CONTEXT_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExternal() {
        return false;
    }
}
