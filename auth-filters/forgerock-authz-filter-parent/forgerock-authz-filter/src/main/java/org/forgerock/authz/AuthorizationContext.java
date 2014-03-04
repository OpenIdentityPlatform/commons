/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.authz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context to use for authorization requests. Authz modules can add additional information to the context, which will be
 * passed on to further authz filters and the protected resource via request attributes. Provides simple dynamic
 * typing for getting and setting attribute values on the context.
 *
 * @since 1.4.0
 */
public class AuthorizationContext {
    /**
     * Key under which an authorization context will be stored in the {@link javax.servlet.ServletRequest} of a request.
     * The raw {@code Map<String, Object>} will be stored in the request to avoid downstream clients requiring a
     * dependency on this class.
     * <p/>
     * Note: this value should match the CREST
     * {@code org.forgerock.json.resource.servlet.SecurityContextFactory.ATTRIBUTE_AUTHZID}.
     */
    public static final String ATTRIBUTE_AUTHORIZATION_CONTEXT = "org.forgerock.authentication.context";

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationContext.class);

    private final Map<String, Object> attributes;

    /**
     * Constructs a blank authorization context.
     */
    public AuthorizationContext() {
        this(new LinkedHashMap<String, Object>());
    }

    /**
     * Creates a new authorization context using the given attribute map as a backing store.
     *
     * @param attributes the initial attributes to copy into the new context.
     */
    private AuthorizationContext(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets the authorization context associated with the given request, creating one if it is not already present and
     * associating it with the request.
     *
     * @param request the request to get the associated authorization context for.
     * @return the associated authorization context.
     * @throws NullPointerException if the request is null.
     */
    public static AuthorizationContext forRequest(ServletRequest request) {
        Map<String, Object> contextMap = asMap(request.getAttribute(ATTRIBUTE_AUTHORIZATION_CONTEXT));
        if (contextMap == null) {
            contextMap = new LinkedHashMap<String, Object>();
            request.setAttribute(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);
        }
        return new AuthorizationContext(contextMap);
    }

    /**
     * Tries to cast an object to a {@code Map<String, Object>}, returning null if not possible.
     *
     * @param object the object to convert.
     * @return the object cast to a map, or null if either the input is null or is not of the correct type.
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

    /**
     * Gets an attribute from the authorization context, returning null if it does not exist.
     *
     * @param key the key of the attribute.
     * @param <T> the result type.
     * @return the attribute value, or null if it does not exist.
     * @throws NullPointerException if the key is null.
     * @throws ClassCastException if the attribute exists but is not of the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        return (T) attributes.get(key);
    }

    /**
     * Sets an attribute in the shared context.
     * @param key the key to set.
     * @param value the value to associate with the key.
     * @throws NullPointerException if the key or value is null.
     * @return this context to allow method chaining.
     */
    public AuthorizationContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Gets a copy of all attributes set in this authorization context. Changes made to the returned map will not be
     * reflected in the authorization context.
     *
     * @return a copy of the attributes in this context.
     */
    public Map<String, Object> getAttributes() {
        return new LinkedHashMap<String, Object>(attributes);
    }

    @Override
    public boolean equals(Object that) {
        return this == that
            || (that instanceof AuthorizationContext && this.attributes.equals(((AuthorizationContext) that).attributes));
    }

    @Override
    public int hashCode() {
        return attributes.hashCode();
    }
}
