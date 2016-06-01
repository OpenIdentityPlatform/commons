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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.json.resource.http.examples;

import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.json.resource.Applications.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Resources.*;
import static org.forgerock.json.resource.RouteMatchers.*;
import static org.forgerock.json.resource.http.CrestHttp.*;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.forgerock.api.markup.ApiDocGenerator;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.example.DescribedOauth2Endpoint;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.swagger.SwaggerApiProducer;
import org.forgerock.http.util.Uris;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.descriptor.examples.handler.UserCollectionHandler;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.DelegatingRouteMatcher;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import io.swagger.models.Info;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

/**
 * Http Application implementation to demonstrate integration with the Commons HTTP Framework.
 */
public class CrestHttpApplication implements DescribedHttpApplication {

    private static final String SWAGGER_JSON_ROUTE = "../..?_api";

    private static final ContentTypeHeader HTML_CONTENT_TYPE_HEADER =
            ContentTypeHeader.valueOf("text/html; charset=UTF-8");

    @Override
    public Handler start() throws HttpApplicationException {
        final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        final Router crestRouter = new Router();
        crestRouter.addRoute(requestUriMatcher(STARTS_WITH, "/users"), newHandler(new MemoryBackend()));
        crestRouter.addRoute(requestUriMatcher(STARTS_WITH, "/groups"), newHandler(new MemoryBackend()));
        crestRouter.addRoute(requestUriMatcher(STARTS_WITH, "/api/users"), UserCollectionHandler.getUsersRouter());
        crestRouter.addRoute(requestUriMatcher(STARTS_WITH, "/api/admins"), UserCollectionHandler.getAdminsRouter());

        Handler crestHandler = newHttpHandler(simpleCrestApplication(newInternalConnectionFactory(crestRouter),
                "frapi:example", "1.0"));

        final org.forgerock.http.routing.Router router = new org.forgerock.http.routing.Router();
        router.setDefaultRoute(crestHandler);
        router.addRoute(RouteMatchers.requestUriMatcher(RoutingMode.STARTS_WITH, "/chf/oauth2"),
                new DescribedOauth2Endpoint());

        // convert ApiDescription to HTML documentation
        router.addRoute(RouteMatchers.requestUriMatcher(RoutingMode.STARTS_WITH, "/docs/html"),
                new Handler() {
                    @Override
                    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                        ApiDescription apiDescription = crestRouter.handleApiRequest(context,
                                newApiRequest(ResourcePath.empty()));
                        final String asciiDocMarkup = ApiDocGenerator.execute("Users and Devices API", apiDescription,
                                null);

                        final String html;
                        synchronized (asciidoctor) {
                            html = asciidoctor.render(asciiDocMarkup,
                                    OptionsBuilder.options()
                                            .attributes(AttributesBuilder.attributes()
                                                    .tableOfContents(Placement.LEFT)
                                                    .sectNumLevels(5)
                                                    .attribute("toclevels", 5)
                                                    .get())
                                            .safe(SafeMode.SAFE)
                                            .headerFooter(true)
                                            .get());
                        }

                        final Response response = new Response(Status.OK);
                        response.getHeaders().add(HTML_CONTENT_TYPE_HEADER);
                        response.setEntity(html);
                        return Response.newResponsePromise(response);
                    }
                });

        // redirect to Swagger UI page, given a URL parameter to point to the Swagger JSON endpoint
        router.addRoute(RouteMatchers.requestUriMatcher(RoutingMode.EQUALS, "/docs/api"),
                new Handler() {
                    @Override
                    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                        final String uri = request.getUri().toString();
                        final String baseUrl = uri.substring(0, uri.indexOf("/docs/api"));
                        final String url = baseUrl + "/openapi/index.html?url="
                                + Uris.urlEncodeQueryParameterNameOrValue(SWAGGER_JSON_ROUTE)
                                + "&title=" + Uris.urlEncodeQueryParameterNameOrValue("Users and Devices API");

                        final Response response = new Response(Status.FOUND);
                        response.getHeaders().add("Location", url);
                        return Response.newResponsePromise(response);
                    }
                });

        // simple page providing links to HTML docs and Swagger UI
        router.addRoute(new DelegatingRouteMatcher<Request>(RouteMatchers.requestUriMatcher(RoutingMode.EQUALS, "/")) {
            @Override
            public RouteMatch evaluate(Context context, Request request) {
                if (request.getForm().containsKey("_crestapi")) {
                    return null;
                }
                return super.evaluate(context, request);
            }
        }, new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                final String html = "<!DOCTYPE html><html><head><title>CREST Examples</title></head><body>"
                        + "<p><a href=\"?_api\">Users and Devices API OpenAPI JSON</a></p>"
                        + "<p><a href=\"?_crestapi\">Users and Devices API CREST Descriptor JSON</a></p>"
                        + "<p><a href=\"./docs/api\">Users and Devices API explorer</a></p>"
                        + "<p><a href=\"./docs/html\">Users and Devices API documentation</a></p>"
                        + "</body></html>";

                final Response response = new Response(Status.OK);
                response.getHeaders().add(HTML_CONTENT_TYPE_HEADER);
                response.setEntity(html);
                return Response.newResponsePromise(response);
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
        // empty
    }

    @Override
    public ApiProducer<Swagger> getApiProducer() {
        return new SwaggerApiProducer(new Info().title("CREST Examples"), null, null, Scheme.HTTP);
    }
}
