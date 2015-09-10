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

package org.forgerock.authz.basic.http;

import static org.forgerock.json.JsonValue.*;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.http.api.HttpAuthorizationModule;
import org.forgerock.http.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * A {@code HttpAuthorizationModule} that only allows access to the {@code users} endpoint.
 *
 * @since 1.5..0
 */
public class HttpEndpointCheckerAuthorizationModule implements HttpAuthorizationModule {

    @Override
    public String getName() {
        return "EndpointChecker";
    }

    private final EndpointChecker endpointChecker;

    /**
     * Creates a new {@code HttpEndpointCheckerAuthorizationModule} instance.
     *
     * @param endpointChecker An {@code EndpointChecker} instance.
     */
    public HttpEndpointCheckerAuthorizationModule(EndpointChecker endpointChecker) {
        this.endpointChecker = endpointChecker;
    }

    /**
     * Authorizes all requests to the {@code users} endpoint. All others are forbidden.
     *
     * @param req {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, AuthorizationException> authorize(Context context, Request req,
            AuthorizationContext authorizationContext) {

        final String endpoint = getResourceName(context);
        if (endpointChecker.check(endpoint)) {
            authorizationContext.setAttribute("AuthorizationResult", "success");
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        }
        return Promises.newResultPromise(AuthorizationResult.accessDenied("Not authorized for endpoint: " + endpoint,
                json(object(field("internalCode", 123)))));
    }

    /**
     * Gets the resource name from the {@code Request}.
     *
     * @param context The request {@code Context} instance.
     * @return The resource name.
     */
    private String getResourceName(Context context) {
        return context.asContext(UriRouterContext.class).getRemainingUri();
    }
}
