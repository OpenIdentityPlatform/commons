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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.http.servlet.example;

import static io.swagger.models.Scheme.HTTP;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.example.DescribedOauth2Endpoint;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.swagger.SwaggerApiProducer;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

/**
 * Example single {@link HttpApplication} deployment which registers a
 * {@link Handler} that returns the application name and matched portion of
 * the request uri.
 *
 * <p>The application name is {@literal default} for single
 * {@code HttpApplication} deployments and can be set for multiple
 * {@code HttpApplication} deployments.</p>
 */
public class ExampleHttpApplication implements DescribedHttpApplication {

    private final String applicationName;

    /**
     * Default constructor for single {@code HttpApplication} deployments.
     */
    public ExampleHttpApplication() {
        this("default");
    }

    ExampleHttpApplication(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public Handler start() {
        Router router = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "oauth2"), new DescribedOauth2Endpoint());
        router.setDefaultRoute(new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                Map<String, String> content = new HashMap<>();
                content.put("applicationName", applicationName);
                content.put("matchedUri", context.asContext(UriRouterContext.class).getBaseUri());
                return newResultPromise(new Response(Status.OK).setEntity(content));
            }
        });
        return router;
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }

    @Override
    public ApiProducer<Swagger> getApiProducer() {
        return new SwaggerApiProducer(new Info().title("Example HTTP Application"), "/servlet", "localhost", HTTP);
    }
}
