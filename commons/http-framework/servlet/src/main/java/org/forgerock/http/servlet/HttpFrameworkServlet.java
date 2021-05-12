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
 * Copyright 2010?2011 ApexIdentity Inc.
 * Portions Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.http.servlet;

import static java.util.Collections.list;
import static org.forgerock.http.handler.Handlers.asDescribableHandler;
import static org.forgerock.http.handler.Handlers.chainOf;
import static org.forgerock.http.handler.Handlers.internalServerErrorHandler;
import static org.forgerock.http.io.IO.newBranchingInputStream;
import static org.forgerock.http.io.IO.newTemporaryStorage;
import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.http.routing.UriRouterContext.uriRouterContext;
import static org.forgerock.util.Utils.closeSilently;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ServiceLoader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.filter.TransactionIdInboundFilter;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.http.util.CaseInsensitiveSet;
import org.forgerock.http.util.Uris;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.promise.RuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.Swagger;

/**
 * <p>
 * An HTTP servlet implementation which provides integration between the Servlet
 * API and the common HTTP Framework.
 * </p>
 * <p>
 * A {@link HttpApplication} implementation must be registered in the
 * {@link ServiceLoader} framework
 * </p>
 *
 * @see HttpApplication
 */
public final class HttpFrameworkServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(HttpFrameworkServlet.class);
    private static final long serialVersionUID = 3524182656424860912L;

    /** Standard specified request attribute name for retrieving X509 Certificates. */
    private static final String SERVLET_REQUEST_X509_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    /** Methods that should not include an entity body. */
    private static final CaseInsensitiveSet NON_ENTITY_METHODS = new CaseInsensitiveSet(
            Arrays.asList("GET", "HEAD", "TRACE"));

    /**
     * Servlet init-param for configuring the routing base for the
     * {@link HttpApplication}.
     *
     *  @see ServletRoutingBase
     */
    public static final String ROUTING_BASE_INIT_PARAM_NAME = "routing-base";

    private ServletVersionAdapter adapter;
    private HttpApplication application;
    private Factory<Buffer> storage;
    private DescribableHandler handler;
    private ServletRoutingBase routingBase;

    /**
     * Default constructor for use via web.xml declaration.
     */
    public HttpFrameworkServlet() {
    }

    /**
     * Creates a new {@code HttpFrameworkServlet} programmatically using the
     * specified {@link HttpApplication}.
     *
     * @param application The {@code HttpApplication} instance.
     */
    public HttpFrameworkServlet(HttpApplication application) {
        this.application = application;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        adapter = getAdapter(getServletContext());
        routingBase = selectRoutingBase(getServletConfig());
        if (application == null) {
            HttpApplicationLoader applicationLoader = getApplicationLoader(getServletConfig());
            application = getApplication(applicationLoader, getServletConfig());
        }
        storage = application.getBufferFactory();
        if (storage == null) {
            final File tmpDir = (File) getServletContext().getAttribute(ServletContext.TEMPDIR);
            storage = newTemporaryStorage(tmpDir);
        }
        try {
            this.handler = chainOf(asDescribableHandler(application.start()), new TransactionIdInboundFilter());
            if (application instanceof DescribedHttpApplication) {
                ApiProducer<Swagger> apiProducer = ((DescribedHttpApplication) application).getApiProducer();
                this.handler.api(apiProducer);
            }
        } catch (HttpApplicationException e) {
            logger.error("Error while starting the application.", e);
            handler = asDescribableHandler(internalServerErrorHandler(e));
        }
    }

    private ServletVersionAdapter getAdapter(ServletContext servletContext) throws ServletException {
        switch (servletContext.getMajorVersion()) {
        case 1:
            // FIXME: i18n.
            throw new ServletException("Unsupported Servlet version "
                    + servletContext.getMajorVersion());
        case 2:
            return new Servlet2Adapter();
        default:
            return new Servlet3Adapter();
        }
    }

    private ServletRoutingBase selectRoutingBase(ServletConfig servletConfig) throws ServletException {
        String routingModeParam = servletConfig.getInitParameter(ROUTING_BASE_INIT_PARAM_NAME);
        if (routingModeParam == null) {
            return ServletRoutingBase.SERVLET_PATH;
        }
        try {
            return ServletRoutingBase.valueOf(routingModeParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServletException("Invalid routing mode: " + routingModeParam);
        }
    }

    private HttpApplicationLoader getApplicationLoader(ServletConfig config) throws ServletException {
        String applicationLoaderParam = config.getInitParameter("application-loader");
        if (applicationLoaderParam == null) {
            return HttpApplicationLoader.SERVICE_LOADER;
        }
        try {
            return HttpApplicationLoader.valueOf(applicationLoaderParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServletException("Invalid HTTP application loader: " + applicationLoaderParam);
        }
    }

    private HttpApplication getApplication(HttpApplicationLoader applicationLoader, ServletConfig config)
            throws ServletException {
        return applicationLoader.load(config);
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        final Session session = new ServletSession(req);
        final SessionContext sessionContext = new SessionContext(new RootContext(), session);

        final Request request;
        final UriRouterContext uriRouterContext;
        try {
            request = createRequest(req);
            uriRouterContext = createRouterContext(sessionContext, req, request);
        } catch (URISyntaxException e) {
            Response response = new Response(Status.BAD_REQUEST);
            response.setEntity(e.getMessage());
            writeResponse(response, resp, sessionContext);
            return;
        }

        final AttributesContext attributesContext = new AttributesContext(new RequestAuditContext(uriRouterContext));

        /* TODO
         * add comment on why this was added as probably shouldn't stick around as
         * only to fix AM's case of forwarding the request from a different servlet?....
         */
        Enumeration<String> attributeNames = req.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            attributesContext.getAttributes().put(attributeName, req.getAttribute(attributeName));
        }

        //FIXME ideally we don't want to expose the HttpServlet Request and Response
        // handy servlet-specific attributes, sure to be abused by downstream filters
        attributesContext.getAttributes().put(HttpServletRequest.class.getName(), req);
        attributesContext.getAttributes().put(HttpServletResponse.class.getName(), resp);

        Context context = createClientContext(attributesContext, req);

        // handle request
        final ServletSynchronizer sync = adapter.createServletSynchronizer(req, resp);
        try {
            final Promise<Response, NeverThrowsException> promise =
                    handler.handle(context, request)
                            .thenOnResult(new ResultHandler<Response>() {
                                @Override
                                public void handleResult(Response response) {
                                	//save request and response
                                	req.setAttribute(request.getClass().getName(), request);
                                	req.setAttribute(response.getClass().getName(), response);
                                	
                                    writeResponse(request, response, resp, sessionContext, sync);
                                }
                            })
                            .thenOnRuntimeException(new RuntimeExceptionHandler() {
                                @Override
                                public void handleRuntimeException(RuntimeException e) {
                                    logger.error("RuntimeException caught", e);
                                    writeResponse(request, newInternalServerError(), resp, sessionContext, sync);
                                }
                            });

            sync.setAsyncListener(new Runnable() {
                @Override
                public void run() {
                    promise.cancel(true);
                }
            });
        } catch (Throwable throwable) {
            // Guard against any kind of Throwable that may be thrown synchronously (not caught by promise
            // RuntimeExceptionHandler), possibly leaving a stale response in the web container :'(
            // Servlet specification indicates that it's the responsibility of the Servlet implementer to call
            // AsyncContext.complete()
            logger.error("Throwable caught", throwable);
            writeResponse(request, newInternalServerError(), resp, sessionContext, sync);
        }

        try {
            sync.awaitIfNeeded();
        } catch (InterruptedException e) {
            throw new ServletException("Awaiting asynchronous request was interrupted.", e);
        }
    }

    private Request createRequest(HttpServletRequest req) throws IOException, URISyntaxException {
        // populate request
        Request request = new Request();
        request.setMethod(req.getMethod());

        /* CHF-81: containers are generally quite tolerant of invalid query strings, so we'll try to be as well
         * by decoding the query string and re-encoding it correctly before constructing the URI. */
        request.setUri(Uris.createNonStrict(req.getScheme(),
                                            null,
                                            req.getServerName(),
                                            req.getServerPort(),
                                            req.getRequestURI(),
                                            req.getQueryString(),
                                            null));

        // request headers
        for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            request.getHeaders().add(name, list(req.getHeaders(name)));
        }

        // include request entity if appears to be provided with request
        if ((req.getContentLength() > 0 || req.getHeader("Transfer-Encoding") != null)
                && !NON_ENTITY_METHODS.contains(request.getMethod())) {
            request.setEntity(newBranchingInputStream(req.getInputStream(), storage));
        }

        return request;
    }

    private ClientContext createClientContext(Context parent, HttpServletRequest req) {
        return ClientContext.buildExternalClientContext(parent)
                .remoteUser(req.getRemoteUser())
                .remoteAddress(req.getRemoteAddr())
                .remotePort(req.getRemotePort())
                .certificates((X509Certificate[]) req.getAttribute(SERVLET_REQUEST_X509_ATTRIBUTE))
                .userAgent(req.getHeader("User-Agent"))
                .secure("https".equalsIgnoreCase(req.getScheme()))
                .localAddress(req.getLocalAddr())
                .localPort(req.getLocalPort())
                .build();
    }

    private UriRouterContext createRouterContext(Context parent, HttpServletRequest req, final Request request)
            throws URISyntaxException {
        String matchedUri = routingBase.extractMatchedUri(req);
        final String requestURI = req.getRequestURI();
        String remaining = requestURI.substring(requestURI.indexOf(matchedUri) + matchedUri.length());
        return uriRouterContext(parent).matchedUri(matchedUri).remainingUri(remaining)
                .originalUri(request.getUri().asURI()).build();
    }

    private void writeResponse(Request request, Response response, HttpServletResponse servletResponse,
            SessionContext sessionContext, ServletSynchronizer synchronizer) {
        try {
            writeResponse(response, servletResponse, sessionContext);
        } finally {
            closeSilently(request);
            synchronizer.signalAndComplete();
        }
    }

    private void writeResponse(final Response response, final HttpServletResponse servletResponse,
            final SessionContext sessionContext) {
        try {
            /*
             * Support for OPENIG-94/95 - The wrapped servlet may have already
             * committed its response w/o creating a new OpenIG Response instance in
             * the exchange.
             */
            if (response != null) {
                // response status-code (reason-phrase deprecated in Servlet API)
                servletResponse.setStatus(response.getStatus().getCode());

                // ensure that the session has been written back to the response
                sessionContext.getSession().save(response);

                // response headers
                for (String name : response.getHeaders().keySet()) {
                    for (String value : response.getHeaders().get(name).getValues()) {
                        if (value != null && value.length() > 0) {
                            servletResponse.addHeader(name, value);
                        }
                    }
                }
                // response entity (if applicable)
                // TODO does this also set content length?
                if (!response.getEntity().isRawContentEmpty()) {
                	response.getEntity().copyRawContentTo(servletResponse.getOutputStream());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write response", e);
        } finally {
            closeSilently(response);
        }
    }

    @Override
    public void destroy() {
        application.stop();
    }
}



























































