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

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResultHandler;

/**
 * A configurator appropriate for use in Servlet 3.x containers:
 * <ul>
 * <li>it will return the class loader associated with this
 * {@link ServletContext}
 * <li>it creates request dispatchers which processes requests asynchronously.
 * </ul>
 */
final class Servlet3Configurator extends ServletConfigurator {
    /**
     * Servlet 3.x request dispatcher which will attempt to process requests
     * asynchronously and using NIO if the platform supports it.
     */
    private final class Servlet3RequestDispatcher implements RequestDispatcher {
        private final ConnectionFactory connectionFactory;

        private Servlet3RequestDispatcher(final ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispatchRequest(final Context context, final Request request,
                final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
                throws Exception {
            if (httpRequest.isAsyncSupported()) {
                // Process the request asynchronously.
                final AsyncContext asyncContext = httpRequest.startAsync();

                // Disable timeouts - see http://java.net/jira/browse/GRIZZLY-1325
                asyncContext.setTimeout(0);

                final ResultHandler<Connection> handler = new RequestRunner(context, request,
                        httpRequest, httpResponse) {
                    @Override
                    protected void postComplete() {
                        asyncContext.complete();
                    }

                    @Override
                    void postError(final Exception e) {
                        fail(httpRequest, httpResponse, e);
                        asyncContext.complete();
                    }

                };
                connectionFactory.getConnectionAsync(handler);
            } else {
                // Fall-back to 2.x synchronous processing.
                final CountDownLatch latch = new CountDownLatch(1);
                final ResultHandler<Connection> handler = new RequestRunner(context, request,
                        httpRequest, httpResponse) {
                    @Override
                    protected void postComplete() {
                        latch.countDown();
                    }

                    @Override
                    void postError(final Exception e) {
                        fail(httpRequest, httpResponse, e);
                        latch.countDown();
                    }

                };
                connectionFactory.getConnectionAsync(handler);
                latch.await();
            }
        }
    }

    /**
     * Creates a new configurator for 3.x Servlet containers.
     *
     * @param servletContext
     *            The context.
     */
    Servlet3Configurator(final ServletContext servletContext) {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    RequestDispatcher getRequestDispatcher(final ConnectionFactory connectionFactory) {
        return new Servlet3RequestDispatcher(connectionFactory);
    }

}
