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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource.provider;

/**
 * A routing request result returned by a call to
 * {@link RoutingStrategy#routeRequest}.
 */
public final class RoutingResult {
    /*
     * This class is required in order to constrain the types of provider
     * returned by a routing strategy. Without it we lose type safety since a
     * strategy could return any type of object.
     */

    private final Object resourceProvider;

    /**
     * Creates a new routing result whose value is a collection resource
     * provider.
     *
     * @param provider
     *            The collection resource provider.
     */
    public RoutingResult(final CollectionResourceProvider provider) {
        this.resourceProvider = provider;
    }

    /**
     * Creates a new routing result whose value is a singleton resource
     * provider.
     *
     * @param provider
     *            The singleton resource provider.
     */
    public RoutingResult(final SingletonResourceProvider provider) {
        this.resourceProvider = provider;
    }

    /**
     * Returns the collection resource provider associated with this routing
     * result.
     *
     * @return The collection resource provider associated with this routing
     *         result.
     * @throws ClassCastException
     *             If this routing result represents a singleton resource
     *             provider.
     */
    public CollectionResourceProvider asCollection() {
        return (CollectionResourceProvider) resourceProvider;
    }

    /**
     * Returns the singleton resource provider associated with this routing
     * result.
     *
     * @return The singleton resource provider associated with this routing
     *         result.
     * @throws ClassCastException
     *             If this routing result represents a collection resource
     *             provider.
     */
    public SingletonResourceProvider asSingleton() {
        return (SingletonResourceProvider) resourceProvider;
    }

    /**
     * Returns {@code true} if this result represents a collection resource
     * provider.
     *
     * @return {@code true} if this result represents a collection resource
     *         provider.
     */
    public boolean isCollection() {
        return resourceProvider instanceof CollectionResourceProvider;
    }

    /**
     * Returns {@code true} if this result represents a singleton resource
     * provider.
     *
     * @return {@code true} if this result represents a singleton resource
     *         provider.
     */
    public boolean isSingleton() {
        return resourceProvider instanceof SingletonResourceProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return resourceProvider.toString();
    }

}
