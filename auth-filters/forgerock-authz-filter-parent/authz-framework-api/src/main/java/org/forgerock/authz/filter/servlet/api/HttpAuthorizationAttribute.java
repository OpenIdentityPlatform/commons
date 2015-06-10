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

import org.forgerock.authz.filter.api.AuthorizationAttribute;
import org.forgerock.authz.filter.api.AuthorizationContext;

import javax.servlet.ServletRequest;

/**
 * <p>Provides a convenience layer on top of {@link AuthorizationContext} to simplify access to particular attributes in
 * the authorisation context. Usage:
 * <pre>{@code
 *     AuthorizationAttribute&lt;Set&lt;String&gt;&gt; rolesAttr = new AuthorizationAttribute&lt;&gt;("roles");
 *     ...
 *     rolesAttr.set(context, Collections.singleton("someRole"));
 *     ...
 *     Set<String> roles = rolesAttr.get(context);
 * }</pre></p>
 *
 * <p>Note that due to the dynamic nature of servlet request attributes, it is not possible to make this completely
 * type-safe.</p>
 *
 * <p>Clients should be prepared for runtime {@link ClassCastException}s if an unexpected value is found in an
 * authorization context.</p>
 *
 * @param <T> The type of the attribute.
 * @since 1.4.0
 */
public final class HttpAuthorizationAttribute<T> {

    private final AuthorizationAttribute<T> attribute;

    /**
     * Constructs an authorization attribute for the given authorization context key.
     *
     * @param key The key to use for this attribute in the {@link AuthorizationContext}.
     * @throws NullPointerException If the key is null.
     */
    public HttpAuthorizationAttribute(String key) {
        this.attribute = new AuthorizationAttribute<>(key);
    }

    /**
     * Gets this attribute from the given authorization context.
     *
     * @param context The context to get this attribute from.
     * @return The attribute from the context or null if not set.
     * @throws ClassCastException If an entry exists in the context for this key but has the wrong type.
     * @throws NullPointerException If the context is null.
     */
    @SuppressWarnings("unchecked")
    public T get(AuthorizationContext context) {
        return attribute.get(context);
    }

    /**
     * Gets this attribute from the authorization context associated with the given servlet request.
     *
     * @param request The request to get this attribute from.
     * @return The attribute associated with this request in the authorization context or null if not set.
     * @throws ClassCastException If an entry exists in the context for this key but has the wrong type or if the
     * context attribute in the request is itself of the wrong type.
     * @throws NullPointerException If the request is null.
     */
    public T get(ServletRequest request) {
        return get(HttpAuthorizationContext.forRequest(request));
    }

    /**
     * Sets this attribute in the given authorization context to the given value.
     *
     * @param context The context to set the attribute in.
     * @param value The value to set.
     * @throws NullPointerException If the context is null.
     */
    public void set(AuthorizationContext context, T value) {
        attribute.set(context, value);
    }

    /**
     * Sets this attribute in the authorization context associated with the given request to the given value.
     *
     * @param request The request to set this authorization attribute for.
     * @param value The value to associate with this attribute.
     * @throws NullPointerException If the request is null.
     * @throws ClassCastException If the authorization context in the request is of the wrong type (i.e., not an actual
     * authorization context).
     */
    public void set(ServletRequest request, T value) {
        set(HttpAuthorizationContext.forRequest(request), value);
    }
}
