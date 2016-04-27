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
 * <p>Note that due to the dynamic nature of request attributes, it is not possible to make this completely
 * type-safe.</p>
 *
 * <p>Clients should be prepared for runtime {@link ClassCastException}s if an unexpected value is found in an
 * authorization context.</p>
 *
 * @param <T> The type of the attribute.
 * @since 1.4.0
 */
public final class AuthorizationAttribute<T> {

    private final String key;

    /**
     * Constructs an authorization attribute for the given authorization context key.
     *
     * @param key The key to use for this attribute in the {@link AuthorizationContext}.
     * @throws NullPointerException If the key is null.
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
     * @param context The context to get this attribute from.
     * @return The attribute from the context or null if not set.
     * @throws ClassCastException If an entry exists in the context for this key but has the wrong type.
     * @throws NullPointerException If the context is null.
     */
    @SuppressWarnings("unchecked")
    public T get(AuthorizationContext context) {
        return context.getAttribute(key);
    }

    /**
     * Sets this attribute in the given authorization context to the given value.
     *
     * @param context The context to set the attribute in.
     * @param value The value to set.
     * @throws NullPointerException If the context is null.
     */
    public void set(AuthorizationContext context, T value) {
        context.setAttribute(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "AuthorizationAttribute{key='" + key + "'}";
    }
}
