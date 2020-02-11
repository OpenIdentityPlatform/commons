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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.http.grizzly;

import static org.forgerock.http.handler.Handlers.asDescribableHandler;
import static org.forgerock.http.handler.Handlers.chainOf;
import static org.forgerock.http.handler.Handlers.internalServerErrorHandler;
import static org.forgerock.http.io.IO.newBranchingInputStream;
import static org.forgerock.http.io.IO.newTemporaryStorage;
import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.util.Utils.closeSilently;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.filter.TransactionIdInboundFilter;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.io.IO;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.session.SessionContext;
import org.forgerock.http.util.CaseInsensitiveSet;
import org.forgerock.http.util.Uris;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.promise.RuntimeExceptionHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.util.Globals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.Swagger;

/**
 * A Grizzly implementation which provides integration between the Grizzly API and the common HTTP Framework.
 *
 * @see HttpApplication
 * @see Handler
 */
final class HandlerAdapter extends HttpHandler {

    /** Methods that should not include an entity body. */
    private static final CaseInsensitiveSet NON_ENTITY_METHODS = new CaseInsensitiveSet(
            Arrays.asList("GET", "HEAD", "TRACE"));

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerAdapter.class);

    private final HttpApplication httpApplication;
    private final Factory<Buffer> storage;
    private DescribableHandler describedHandler;

    HandlerAdapter(HttpApplication httpApplication) {
        this.httpApplication = httpApplication;
        final Factory<Buffer> applicationStorage = httpApplication.getBufferFactory();
        this.storage = applicationStorage != null
                ? applicationStorage
                : newTemporaryStorage(new File(System.getProperty("java.io.tmpdir")));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        super.start();
        try {
            describedHandler = chainOf(httpApplication.start(), new TransactionIdInboundFilter());
            if (httpApplication instanceof DescribedHttpApplication) {
                ApiProducer<Swagger> apiProducer = ((DescribedHttpApplication) httpApplication).getApiProducer();
                describedHandler.api(apiProducer);
            }
        } catch (HttpApplicationException e) {
            LOGGER.error("Error while starting the application.", e);
            describedHandler = asDescribableHandler(internalServerErrorHandler(e));
        }
    }

    @Override
    public void destroy() {
        httpApplication.stop();
        describedHandler = null;
        super.destroy();
    }

    @Override
    public void service(final Request request, final Response response) throws Exception {
        final org.forgerock.http.protocol.Request chfRequest = toChfRequest(request);
        final RootContext rootContext = new RootContext();
        final SessionContext sessionContext = new SessionContext(rootContext, new SessionAdapter(request.getSession()));
        final UriRouterContext uriRouterContext = createRouterContext(sessionContext, request, chfRequest);
        final AttributesContext attributesContext = new AttributesContext(new RequestAuditContext(uriRouterContext));
        final ClientContext context = createClientContext(attributesContext, request);

        response.suspend();
        describedHandler.handle(context, chfRequest)
                .thenOnResult(new ResultHandler<org.forgerock.http.protocol.Response>() {
                    @Override
                    public void handleResult(org.forgerock.http.protocol.Response chfResponse) {
                        writeResponse(chfResponse, response, sessionContext);
                    }
                })
                .thenOnRuntimeException(new RuntimeExceptionHandler() {
                    @Override
                    public void handleRuntimeException(RuntimeException e) {
                        LOGGER.error("RuntimeException caught", e);
                        writeResponse(
                                newInternalServerError(e),
                                response, sessionContext);
                    }
                })
                .thenAlways(new Runnable() {
                    @Override
                    public void run() {
                        response.resume();
                    }
                });
    }

    private void writeResponse(final org.forgerock.http.protocol.Response chfResponse, final Response grizzlyResponse,
            final SessionContext sessionContext) {
        try {
            grizzlyResponse.setStatus(chfResponse.getStatus().getCode());
            sessionContext.getSession().save(chfResponse);

            // response headers
            for (String name : chfResponse.getHeaders().keySet()) {
                for (String value : chfResponse.getHeaders().get(name).getValues()) {
                    if (value != null && !value.isEmpty()) {
                        grizzlyResponse.addHeader(name, value);
                    }
                }
            }
            IO.stream(chfResponse.getEntity().getRawContentInputStream(), grizzlyResponse.getOutputStream());
        } catch (IOException e) {
            LOGGER.trace("Failed to write response", e);
        } finally {
            closeSilently(chfResponse);
        }
    }

    private org.forgerock.http.protocol.Request toChfRequest(Request req) throws URISyntaxException {
        // populate request
        org.forgerock.http.protocol.Request request = new org.forgerock.http.protocol.Request();
        request.setMethod(req.getMethod().toString());

        /*
         * CHF-81: containers are generally quite tolerant of invalid query strings, so we'll try to be as well by
         * decoding the query string and re-encoding it correctly before constructing the URI.
         */
        request.setUri(Uris.createNonStrict(req.getScheme(), null, req.getServerName(), req.getServerPort(),
                req.getRequestURI(), req.getQueryString(), null));

        // request headers
        for (String e : req.getHeaderNames()) {
            final ArrayList<String> values = new ArrayList<>(1);
            for (String value : req.getHeaders(e)) {
                values.add(value);
            }
            request.getHeaders().add(e, values);
        }

        // include request entity if appears to be provided with request
        if ((req.getContentLength() > 0 || req.getHeader("Transfer-Encoding") != null)
                && !NON_ENTITY_METHODS.contains(request.getMethod())) {
            request.setEntity(newBranchingInputStream(req.getInputStream(), storage));
        }

        return request;
    }

    private UriRouterContext createRouterContext(Context parent, Request req,
            org.forgerock.http.protocol.Request request) {
        return new UriRouterContext(parent, "", req.getRequestURI(), Collections.<String, String> emptyMap(),
                request.getUri().asURI());
    }

    private ClientContext createClientContext(Context parent, Request req) {
        return ClientContext.buildExternalClientContext(parent)
                            .remoteUser(req.getRemoteUser())
                            .remoteAddress(req.getRemoteAddr())
                            .remotePort(req.getRemotePort())
                            .secure("https".equalsIgnoreCase(req.getScheme()))
                            .certificates((X509Certificate[]) req.getAttribute(Globals.CERTIFICATES_ATTR))
                            .userAgent(req.getHeader("User-Agent"))
                            .localAddress(req.getLocalAddr())
                            .localPort(req.getLocalPort())
                            .build();
    }

}
