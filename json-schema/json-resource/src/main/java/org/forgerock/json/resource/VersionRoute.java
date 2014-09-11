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

/**
 * An opaque handle for a route which has been registered in a {@link VersionRouter router}.
 *
 * @see VersionRouter
 * @since 2.4.0
 */
public final class VersionRoute<T> implements Route {

    private final Version version;
    private final T handler;

    /**
     * Constructs a new VersionRoute.
     *
     * @param version The version of the route.
     * @param handler The request handler for the route.
     */
    public VersionRoute(Version version, T handler) {
        this.version = version;
        this.handler = handler;
    }

    /**
     * Gets the routes version.
     *
     * @return The version.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Gets the routes request handler.
     *
     * @return The request handler.
     */
    public T getRequestHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VersionRoute<?> route = (VersionRoute<?>) o;
        return handler.equals(route.handler) && version.equals(route.version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + handler.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "{" + version + " -> " + handler + "}";
    }
}
