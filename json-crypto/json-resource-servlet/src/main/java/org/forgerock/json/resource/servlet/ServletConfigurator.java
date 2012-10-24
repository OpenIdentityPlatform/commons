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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.codehaus.jackson.JsonFactory;
import org.forgerock.json.resource.ConnectionFactory;

/**
 * A configurator is used to configure the Servlet based on the version of the
 * Servlet container.
 */
abstract class ServletConfigurator {
    /**
     * Returns a configurator appropriate for the Servlet container.
     *
     * @param servletContext
     *            The context.
     * @return A configurator appropriate for the Servlet container.
     * @throws ServletException
     *             If an appropriate configurator could not be obtained.
     */
    static ServletConfigurator getServletConfigurator(final ServletContext servletContext)
            throws ServletException {
        switch (servletContext.getMajorVersion()) {
        case 1:
            // FIXME: i18n.
            throw new ServletException("Unsupported Servlet version "
                    + servletContext.getMajorVersion());
        case 2:
            return new Servlet2Configurator(servletContext);
        default:
            return new Servlet3Configurator(servletContext);
        }
    }

    /**
     * Creates a servlet configurator.
     */
    ServletConfigurator() {
        // Nothing to do.
    }

    /**
     * Creates a new request dispatcher appropriate for Servlet container.
     *
     * @param connectionFactory
     *            The underlying connection factory to which requests should be
     *            dispatched.
     * @param jsonFactory
     *            The JSON factory which should be used for writing and reading
     *            JSON.
     */
    abstract RequestDispatcher getRequestDispatcher(final ConnectionFactory connectionFactory,
            final JsonFactory jsonFactory);

}
