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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A configurator is used to configure the Servlet based on the version of the
 * Servlet container.
 */
public abstract class CompletionHandlerFactory {
    /**
     * Returns a completion handler factory appropriate for use with the Servlet
     * container.
     *
     * @param servletContext
     *            The context.
     * @return A completion handler factory appropriate for the Servlet
     *         container.
     * @throws ServletException
     *             If the Servlet container is not supported.
     */
    public static CompletionHandlerFactory getInstance(final ServletContext servletContext)
            throws ServletException {
        switch (servletContext.getMajorVersion()) {
        case 1:
            // FIXME: i18n.
            throw new ServletException("Unsupported Servlet version "
                    + servletContext.getMajorVersion());
        case 2:
            return new Servlet2CompletionHandlerFactory();
        default:
            return new Servlet3CompletionHandlerFactory();
        }
    }

    /**
     * Prevent sub-classing and instantiation outside this package.
     */
    CompletionHandlerFactory() {
        // Nothing to do.
    }

    /**
     * Creates a new completion handler appropriate for the Servlet container.
     *
     * @param httpRequest
     *            The HTTP request.
     * @param httpResponse
     *            The HTTP response.
     * @return Returns a new completion handler appropriate for the Servlet
     *         container.
     */
    public abstract CompletionHandler createCompletionHandler(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse);

}
