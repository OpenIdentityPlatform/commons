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

import javax.servlet.ServletRequest;

/**
 * Provides a convenience layer on top of {@link AuthorizationContext} to simplify access to particular attributes in
 * the authorisation context. Usage:
 * <pre>{@code
 *     AuthorizationAttribute<Set<String>> rolesAttr = new AuthorizationAttribute<Set<String>>("roles");
 *     ...
 *     rolesAttr.set(context, Collections.singleton("someRole"));
 *     ...
 *     Set<String> roles = rolesAttr.get(context);
 * }</pre>
 * Note that due to the dynamic nature of servlet request attributes, it is not possible to make this completely type-safe.
 * Clients should be prepared for runtime {@link ClassCastException}s if an unexpected value is found in an
 * authorization context.
 *
 * @since 1.4.0
 */
public final class AuthorizationAttribute<T> {
    private final String key;

    /**
     * Constructs an authorization attribute for the given authorization context key.
     *
     * @param key the key to use for this attribute in the {@link AuthorizationContext}.
     * @throws NullPointerException if the key is null.
     */
    public AuthorizationAttribute(String key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        this.key = key;
    }

    /**
     * Gets this attribute from the given authorization context.
     *
     * @param context the context to get this attribute from.
     * @return the attribute from the context or null if not set.
     * @throws ClassCastException if an entry exists in the context for this key but has the wrong type.
     * @throws NullPointerException if the context is null.
     */
    @SuppressWarnings("unchecked")
    public T get(AuthorizationContext context) {
        return context.getAttribute(key);
    }

    /**
     * Gets this attribute from the authorization context associated with the given servlet request.
     *
     * @param request the request to get this attribute from.
     * @return the attribute associated with this request in the authorization context or null if not set.
     * @throws ClassCastException if an entry exists in the context for this key but has the wrong type or if the
     * context attribute in the request is itself of the wrong type.
     * @throws NullPointerException if the request is null.
     */
    public T get(ServletRequest request) {
        return get(AuthorizationContext.forRequest(request));
    }

    /**
     * Sets this attribute in the given authorization context to the given value.
     *
     * @param context the context to set the attribute in.
     * @param value the value to set.
     * @throws NullPointerException if the context is null.
     */
    public void set(AuthorizationContext context, T value) {
        context.setAttribute(key, value);
    }

    /**
     * Sets this attribute in the authorization context associated with the given request to the given value.
     *
     * @param request the request to set this authorization attribute for.
     * @param value the value to associate with this attribute.
     * @throws NullPointerException if the request is null.
     * @throws ClassCastException if the authorization context in the request is of the wrong type (i.e., not an
     * actual authorization context).
     */
    public void set(ServletRequest request, T value) {
        set(AuthorizationContext.forRequest(request), value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorizationAttribute)) {
            return false;
        }

        AuthorizationAttribute that = (AuthorizationAttribute) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "AuthorizationAttribute{key='" + key +"'}";
    }
}
