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
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.servlet;

import static org.forgerock.http.io.IO.newBranchingInputStream;
import static org.forgerock.http.io.IO.newTemporaryStorage;
import static org.forgerock.util.Utils.closeSilently;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ServiceLoader;

import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.Session;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.SessionContext;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.util.CaseInsensitiveSet;
import org.forgerock.http.util.Uris;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.promise.RuntimeExceptionHandler;

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
    private static final long serialVersionUID = 3524182656424860912L;

    /**
     * Standard specified request attribute name for retrieving X509 Certificates.
     */
    private static final String SERVLET_REQUEST_X509_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    /** Methods that should not include an entity body. */
    private static final CaseInsensitiveSet NON_ENTITY_METHODS = new CaseInsensitiveSet(
            Arrays.asList("GET", "HEAD", "TRACE", "DELETE"));

    /**
     * Servlet 3.x defines ServletContext.TEMPDIR constant, but this does not
     * exist in Servlet 2.5, hence the constant redefined here.
     */
    private static final String SERVLET_TEMP_DIR = "javax.servlet.context.tempdir";

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
    private Handler handler;
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
    public void init() throws ServletException {
        adapter = getAdapter(getServletContext());
        routingBase = selectRoutingBase(getServletConfig());
        if (application == null) {
            HttpApplicationLoader applicationLoader = getApplicationLoader(getServletConfig());
            application = getApplication(applicationLoader, getServletConfig());
        }
        storage = application.getBufferFactory();
        if (storage == null) {
            final File tmpDir = (File) getServletContext().getAttribute(SERVLET_TEMP_DIR);
            storage = newTemporaryStorage(tmpDir);
        }
        try {
            handler = application.start();
        } catch (HttpApplicationException e) {
            throw new ServletException("Failed to start HTTP Application", e);
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
        final Request request = createRequest(req);
        final Session session = new ServletSession(req);
        final SessionContext sessionContext = new SessionContext(new RootContext(), session);
        final AttributesContext attributesContext = new AttributesContext(new RequestAuditContext(sessionContext));

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

        Context context = createRouterContext(createClientContext(attributesContext, req), req);

        // handle request
        final ServletSynchronizer sync = adapter.createServletSynchronizer(req, resp);
        final Promise<Response, NeverThrowsException> promise =
                handler.handle(context, request)
                        .thenOnResult(new ResultHandler<Response>() {
                            @Override
                            public void handleResult(Response response) {
                                try {
                                    writeResponse(sessionContext, resp, response);
                                } catch (IOException e) {
                                    log("Failed to write success response", e);
                                } finally {
                                    closeSilently(request, response);
                                    sync.signalAndComplete();
                                }
                            }
                        });
        promise.thenOnRuntimeException(new RuntimeExceptionHandler() {
            @Override
            public void handleRuntimeException(RuntimeException e) {
                Response response = new Response(Status.INTERNAL_SERVER_ERROR);
                try {
                    writeResponse(sessionContext, resp, response);
                } catch (IOException ioe) {
                    log("Failed to write success response", e);
                } finally {
                    closeSilently(request, response);
                    sync.signalAndComplete();
                }
            }
        });

        sync.setAsyncListener(new Runnable() {
            @Override
            public void run() {
                promise.cancel(true);
            }
        });

        try {
            sync.awaitIfNeeded();
        } catch (InterruptedException e) {
            throw new ServletException("Awaiting asynchronous request was interrupted.", e);
        }
    }

    private Request createRequest(HttpServletRequest req) throws ServletException, IOException {
        // populate request
        Request request = new Request();
        request.setMethod(req.getMethod());
        try {
            request.setUri(Uris.create(req.getScheme(), null, req.getServerName(),
                                       req.getServerPort(), req.getRequestURI(), req.getQueryString(), null));
        } catch (URISyntaxException use) {
            throw new ServletException(use);
        }

        // request headers
        for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            request.getHeaders().addAll(name, Collections.list(req.getHeaders(name)));
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
                .remoteHost(req.getRemoteHost())
                .remotePort(req.getRemotePort())
                .certificates((X509Certificate[]) req.getAttribute(SERVLET_REQUEST_X509_ATTRIBUTE))
                .userAgent(req.getHeader("User-Agent"))
                .secure("https".equalsIgnoreCase(req.getScheme()))
                .build();
    }

    private UriRouterContext createRouterContext(Context parent, HttpServletRequest req) {
        String matchedUri = routingBase.extractMatchedUri(req);
        String remaining = req.getRequestURI().substring(req.getRequestURI().indexOf(matchedUri) + matchedUri.length());
        return new UriRouterContext(parent, matchedUri, remaining, Collections.<String, String>emptyMap());
    }

    private void writeResponse(SessionContext context, HttpServletResponse resp, Response response)
            throws IOException {
        /*
         * Support for OPENIG-94/95 - The wrapped servlet may have already
         * committed its response w/o creating a new OpenIG Response instance in
         * the exchange.
         */
        if (response != null) {
            // response status-code (reason-phrase deprecated in Servlet API)
            resp.setStatus(response.getStatus().getCode());

            // ensure that the session has been written back to the response
            context.getSession().save(response);

            // response headers
            for (String name : response.getHeaders().keySet()) {
                for (String value : response.getHeaders().get(name)) {
                    if (value != null && value.length() > 0) {
                        resp.addHeader(name, value);
                    }
                }
            }
            // response entity (if applicable)
            // TODO does this also set content length?
            response.getEntity().copyRawContentTo(resp.getOutputStream());
        }
    }

    @Override
    public void destroy() {
        application.stop();
    }
}
