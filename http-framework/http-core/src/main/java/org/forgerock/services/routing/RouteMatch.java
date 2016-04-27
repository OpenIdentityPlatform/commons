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
 */

package org.forgerock.services.routing;

import org.forgerock.services.context.Context;

/**
 * Contains the result of routing to a particular route.
 */
public interface RouteMatch {

    /**
     * Determines whether this route match is better than the given rout match.
     *
     * <p>Implementation should check that the {@code RouteMatch} can be
     * compared to this instance.</p>
     *
     * @param result The other route match to compare to.
     * @return {@code true} if this route match is the better, {@code false} otherwise.
     * @throws IncomparableRouteMatchException If the provided {@code RouteMatch}
     * could not be compared this {@code RouteMatch} instance.
     */
    boolean isBetterMatchThan(RouteMatch result) throws IncomparableRouteMatchException;

    /**
     * Decorates the given context with any routing information for the route.
     *
     * @param context The original context to decorate.
     * @return The decorated context.
     */
    Context decorateContext(Context context);
}
