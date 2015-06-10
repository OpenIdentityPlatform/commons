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

package org.forgerock.authz.filter.servlet.api;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context to use for HTTP servlet authorization requests. Authorization modules can add additional information to the
 * context, which will be passed on to further authorization filters and the protected resource via request attributes.
 * Provides simple dynamic typing for getting and setting attribute values on the context.
 *
 * @since 1.4.0
 */
public final class HttpAuthorizationContext extends AuthorizationContext {

    /**
     * <p>Key under which an authorization context will be stored in the {@link javax.servlet.ServletRequest} of a
     * request. The raw {@code Map<String, Object>} will be stored in the request to avoid downstream clients requiring
     * a dependency on this class.</p>
     *
     * <p>Note: this value should match the CREST
     * {@code org.forgerock.json.resource.servlet.SecurityContextFactory.ATTRIBUTE_AUTHZID}.</p>
     */
    public static final String ATTRIBUTE_AUTHORIZATION_CONTEXT = "org.forgerock.authentication.context";

    private static final Logger logger = LoggerFactory.getLogger(HttpAuthorizationContext.class);

    /**
     * Creates a new authorization context using the given attribute map as a backing store.
     *
     * @param attributes The initial attributes to copy into the new context.
     */
    private HttpAuthorizationContext(Map<String, Object> attributes) {
        super(attributes);
    }

    /**
     * Gets the authorization context associated with the given request, creating one if it is not already present and
     * associating it with the request.
     *
     * @param request The request to get the associated authorization context for.
     * @return The associated authorization context.
     * @throws NullPointerException If the request is {@code null}.
     */
    public static HttpAuthorizationContext forRequest(ServletRequest request) {
        Map<String, Object> contextMap = asMap(request.getAttribute(ATTRIBUTE_AUTHORIZATION_CONTEXT));
        if (contextMap == null) {
            contextMap = new LinkedHashMap<>();
            request.setAttribute(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);
        }
        return new HttpAuthorizationContext(contextMap);
    }

    /**
     * Tries to cast an object to a {@code Map<String, Object>}, returning {@code null} if not possible.
     *
     * @param object The object to convert.
     * @return The object cast to a map, or {@code null} if either the input is {@code null} or is not of the correct
     * type.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object object) {
        try {
            return (Map<String, Object>) object;
        } catch (ClassCastException ex) {
            logger.error("Invalid object found in authorization context attribute", ex);
            return null;
        }
    }
}
