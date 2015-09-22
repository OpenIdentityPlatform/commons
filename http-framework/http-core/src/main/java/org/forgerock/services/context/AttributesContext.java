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
 * Copyright 2015 ForgeRock AS.
 *
 */

package org.forgerock.services.context;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.session.SessionContext;
import org.forgerock.json.JsonValue;

/**
 * An {@code AttributesContext} is a mechanism for transferring transient state between components when processing a
 * single request. For example, a filter may store information about the end-user in the {@code AttributeContext} which
 * can then be accessed in subsequent filters and handlers in order to perform access control decisions, routing
 * decisions, etc.
 * <p>
 * The {@code AttributesContext} has the same life-cycle as the request with which it is associated. Specifically, any
 * attributes stored when processing one request will not be accessible when processing a subsequent request, even if it
 * is from the same logical client.
 * <p>
 * Use a {@link SessionContext SessionContext} for maintaining state between successive requests
 * from the same logical client.
 */
public final class AttributesContext extends AbstractContext {

    /**
     * Attributes associated with the current request. This field is not serialized in the {@link JsonValue}
     * representation of this context.
     */
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * Constructs a new {@code AttributesContext}.
     *
     * @param parent
     *         The parent {@code Context}.
     */
    public AttributesContext(Context parent) {
        super(parent, "attributes");
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *         The JSON representation from which this context's attributes should be parsed.
     * @param classLoader
     *         The ClassLoader which can properly resolve the persisted class-name.
     */
    public AttributesContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
    }

    /**
     * Returns the attributes associated with the current request.
     *
     * @return The attributes associated with the current request.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
