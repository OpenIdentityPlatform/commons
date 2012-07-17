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

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A strategy for routing requests to resource providers.
 * <p>
 * Routing strategies must be thread safe and, in particular, support
 * registration and deregistration of resource providers while the router is
 * handling requests.
 */
public interface RoutingStrategy {

    /**
     * Determines which resource provider should handle the provided request,
     * throwing a {@code ResourceException} if the request cannot be routed.
     * <p>
     * Implementations may add attributes to the provided context, such as
     * resolved URI template variables.
     *
     * @param context
     *            The request context.
     * @param request
     *            The request to be routed.
     * @return The routing result which will be used to obtain the resource
     *         provider (never {@code null}).
     * @throws ResourceException
     *             If the request cannot be routed. This will usually be a
     *             {@code NotFoundException}.
     */
    RoutingResult routeRequest(Context context, Request request) throws ResourceException;

}
