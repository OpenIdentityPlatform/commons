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

package org.forgerock.caf.authn.test;

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;

import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.caf.authn.test.configuration.ConfigurationConnectionFactory;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.Router;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * HTTP Application for JASPI authentication framework functional tests.
 *
 * @since 2.0.0
 */
public class JaspiHttpApplication implements HttpApplication {

    @Override
    public Handler start() throws HttpApplicationException {
        Router router = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.EQUALS, "/protected/resource"),
                new ConfigurableAuthenticationFilterHandler());
        router.setDefaultRoute(CrestHttp.newHttpHandler(ConfigurationConnectionFactory.getConnectionFactory()));
        return router;
    }

    private static final class ConfigurableAuthenticationFilterHandler implements Handler {

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            ProtectedResource protectedResource = InjectorHolder.getInstance(ProtectedResource.class);
            AuthenticationFilter authenticationFilter = InjectorHolder.getInstance(AuthenticationFilter.class);
            return Handlers.chainOf(protectedResource, authenticationFilter).handle(context, request);
        }
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }
}
