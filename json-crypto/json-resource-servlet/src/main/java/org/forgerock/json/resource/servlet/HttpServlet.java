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
package org.forgerock.json.resource.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;

/**
 * An HTTP Servlet implementation which forwards requests to an
 * {@link HttpServletAdapter}. Instances must be able to obtain a
 * {@code ConnectionFactory} in order to operate and this should be provided in
 * one of two ways:
 * <ul>
 * <li>during construction - this approach is only available when the HTTP
 * Servlet is constructed programmatically
 * <li>during initialization - typically containers will construct Servlets
 * using their default constructor and initialize them by calling the
 * {@link #init()}. The default implementation of this method is to first check
 * to see if a connection factory was provided during construction and, if not,
 * obtain one by calling {@link #getConnectionFactory()}.
 * </ul>
 * Most implementations will need to take the second approach and, therefore,
 * must override {@link #getConnectionFactory()} and may be also
 * {@link #getParentContext()}.
 */
public class HttpServlet extends javax.servlet.http.HttpServlet {
    private static final String METHOD_PATCH = "PATCH";

    private static final long serialVersionUID = 6089858120348026823L;
    private HttpServletAdapter adapter;

    // Provided during construction.
    private final ConnectionFactory connectionFactory;
    private final Context parentContext;

    /**
     * Default constructor called during normal Servlet initialization.
     * Implementations MUST override {@link #getConnectionFactory()} and may
     * need to override {@link #getParentContext()} in order for the HTTP
     * Servlet to function.
     */
    public HttpServlet() {
        this(null, null);
    }

    /**
     * Creates a new JSON resource HTTP Servlet with the provided connection
     * factory and no parent request context. This constructor is provided as a
     * convenience for cases where the HTTP Servlet is instantiated
     * programmatically.
     * <p>
     * If the HTTP Servlet is created using this constructor then there is no
     * need to override {@link #getConnectionFactory()} or
     * {@link #getParentContext()}.
     *
     * @param factory
     *            The connection factory.
     */
    public HttpServlet(final ConnectionFactory factory) {
        this(factory, null);
    }

    /**
     * Creates a new JSON resource HTTP Servlet with the provided connection
     * factory and parent request context. This constructor is provided as a
     * convenience for cases where the HTTP Servlet is instantiated
     * programmatically.
     * <p>
     * If the HTTP Servlet is created using this constructor then there is no
     * need to override {@link #getConnectionFactory()} or
     * {@link #getParentContext()}.
     *
     * @param factory
     *            The connection factory.
     * @param parentContext
     *            The parent request context which should be used as the parent
     *            context of each request context, may be {@code null}.
     */
    public HttpServlet(final ConnectionFactory factory, final Context parentContext) {
        this.connectionFactory = factory;
        this.parentContext = parentContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // Construct the HTTP Servlet adapter.
        final ConnectionFactory factory =
                connectionFactory != null ? connectionFactory : getConnectionFactory();
        final Context context = parentContext != null ? parentContext : getParentContext();
        adapter = new HttpServletAdapter(getServletContext(), factory, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // Delegate to adapter.
        adapter.doDelete(req, resp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // Delegate to adapter.
        adapter.doGet(req, resp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Allow", "DELETE, GET, HEAD, PATCH, POST, PUT, OPTIONS, TRACE");
    }

    /**
     * Processes an HTTP PATCH request.
     *
     * @param req
     *            The HTTP request.
     * @param resp
     *            The HTTP response.
     * @throws IOException
     *             if an input or output error occurs while the Servlet is
     *             handling the PATCH request.
     * @throws ServletException
     *             If the request for the PATCH cannot be handled.
     */
    protected void doPatch(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // Delegate to adapter.
        adapter.doPatch(req, resp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // Delegate to adapter.
        adapter.doPost(req, resp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // Delegate to adapter.
        adapter.doPut(req, resp);
    }

    /**
     * Returns the {@code ConnectionFactory} which this HTTP Servlet should use.
     * <p>
     * This method is invoked by the {@link #init()} method during Servlet
     * initialization only if the connection factory wasn't set during
     * construction (which will be the case if the default constructor was
     * called during normal Servlet initialization).
     * <p>
     * The default implementation is to throw a {@code ServletException} since
     * this method MUST be overridden when no connection factory is provided
     * during construction.
     *
     * @return The connection factory which this Servlet should use.
     * @throws ServletException
     *             If this method has not been overridden, or if the connection
     *             factory could not be obtained for some other reason.
     */
    protected ConnectionFactory getConnectionFactory() throws ServletException {
        throw new ServletException("Connection factory not set and method "
                + "getConnectionFactory() has not been overridden");
    }

    /**
     * Returns the {@code Context} which this HTTP Servlet should use as the
     * parent context of each request context.
     * <p>
     * This method is invoked by the {@link #init()} method during Servlet
     * initialization only if the parent context wasn't set during construction
     * (which will be the case if the default constructor was called during
     * normal Servlet initialization).
     * <p>
     * The default implementation is to return the context provided during
     * construction which will be {@code null} if the default constructor was
     * called.
     *
     * @return The {@code Context} which this HTTP Servlet should use as the
     *         parent context of each request context.
     * @throws ServletException
     *             If the parent context could not be obtained for some reason.
     */
    protected Context getParentContext() throws ServletException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        final String method = req.getMethod();
        if (method.equals(METHOD_PATCH)) {
            doPatch(req, resp);
        } else {
            // Delegate all other methods to super class.
            super.service(req, resp);
        }
    }

}
