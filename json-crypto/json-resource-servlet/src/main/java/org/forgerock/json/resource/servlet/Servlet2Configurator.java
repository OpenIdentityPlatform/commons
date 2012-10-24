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

import static org.forgerock.json.resource.servlet.HttpUtils.fail;

import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResultHandler;

/**
 * A configurator appropriate for use in Servlet 2.x containers:
 * <ul>
 * <li>it will return the class loader associated with this class
 * <li>it creates request dispatchers which processes requests synchronously
 * using blocking IO.
 * </ul>
 */
final class Servlet2Configurator extends ServletConfigurator {
    /**
     * Servlet 2.x request dispatcher which processes requests synchronously
     * using blocking IO.
     */
    private final static class Servlet2RequestDispatcher implements RequestDispatcher {

        private final ConnectionFactory connectionFactory;
        private final JsonFactory jsonFactory;

        private Servlet2RequestDispatcher(final ConnectionFactory connectionFactory,
                final JsonFactory jsonFactory) {
            this.connectionFactory = connectionFactory;
            this.jsonFactory = jsonFactory;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispatchRequest(final Context context, final Request request,
                final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
                throws Exception {
            final CountDownLatch latch = new CountDownLatch(1);
            final ResultHandler<Connection> handler = new RequestRunner(context, request,
                    httpRequest, httpResponse, jsonFactory) {
                @Override
                protected void postComplete() {
                    latch.countDown();
                }

                @Override
                void postError(final Exception e) {
                    fail(httpResponse, e);
                    latch.countDown();
                }
            };
            connectionFactory.getConnectionAsync(handler);
            latch.await();
        }

    }

    /**
     * Creates a new configurator for 2.x Servlet containers.
     *
     * @param servletContext
     *            The context.
     */
    Servlet2Configurator(final ServletContext servletContext) {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    RequestDispatcher getRequestDispatcher(final ConnectionFactory connectionFactory,
            final JsonFactory jsonFactory) {
        return new Servlet2RequestDispatcher(connectionFactory, jsonFactory);
    }

}
