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

package org.forgerock.authz.filter.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context to use for authorization requests. Authorization modules can add additional information to the context, which
 * will be passed on to further authorization filters and the protected resource via request attributes. Provides simple
 * dynamic typing for getting and setting attribute values on the context.
 *
 * @since 1.4.0
 */
public class AuthorizationContext {

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
     * @param attributes The initial attributes to copy into the new context.
     */
    public AuthorizationContext(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets an attribute from the authorization context, returning null if it does not exist.
     *
     * @param key The key of the attribute.
     * @param <T> The result type.
     * @return The attribute value, or null if it does not exist.
     * @throws NullPointerException If the key is null.
     * @throws ClassCastException If the attribute exists but is not of the expected type.
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
     *
     * @param key The key to set.
     * @param value The value to associate with the key.
     * @throws NullPointerException If the key or value is null.
     * @return This context to allow method chaining.
     */
    public AuthorizationContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Gets a copy of all attributes set in this authorization context. Changes made to the returned map will not be
     * reflected in the authorization context.
     *
     * @return A copy of the attributes in this context.
     */
    public Map<String, Object> getAttributes() {
        return new LinkedHashMap<>(attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object that) {
        return this == that
            || (that instanceof AuthorizationContext
                && this.attributes.equals(((AuthorizationContext) that).attributes));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return attributes.hashCode();
    }
}
