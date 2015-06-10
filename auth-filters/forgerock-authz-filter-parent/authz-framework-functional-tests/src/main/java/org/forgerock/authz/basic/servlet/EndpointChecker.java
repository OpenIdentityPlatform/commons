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

package org.forgerock.authz.basic.servlet;

import java.util.HashSet;
import java.util.Set;

/**
 * Simply checks if the endpoint is "allowed" or not.
 *
 * @since 1.5.0
 */
public class EndpointChecker {

    private final Set<String> allowedEndpoints = new HashSet<>();

    /**
     * Creates a new {@code EndpointChecker} instance, which only allows requests to the {@code users} endpoint.
     */
    public EndpointChecker() {
        allowedEndpoints.add("users");
    }

    /**
     * Checks to see if access to the specified endpoint is allowed.
     *
     * @param endpoint The requested  endpoint.
     * @return {@code true} if access to the endpoint is allowed.
     */
    public boolean check(String endpoint) {
        return allowedEndpoints.contains(endpoint);
    }
}
