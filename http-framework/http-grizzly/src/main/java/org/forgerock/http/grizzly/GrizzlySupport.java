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

import static org.forgerock.http.Applications.simpleHttpApplication;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.util.Factory;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;

import io.swagger.models.Swagger;

/**
 * Provides the Grizzly HTTP library support to the common HTTP Framework.
 */
public final class GrizzlySupport {

    private GrizzlySupport() { }

    /**
     * Create a new Grizzly {@link HttpHandler} wrapping the given common HTTP Framework {@link HttpApplication}. The
     * life-cycle of the provided {@link HttpApplication} is bound to the returned Grizzly {@link HttpHandler}. All the
     * operations performed on the Grizzly {@link HttpHandler} will be forwarded to the common HTTP Framework
     * {@link Handler}
     *
     * @param httpApplication
     *            The {@link HttpApplicationException} to wrap.
     * @return A Grizzly {@link HttpHandler} ready to be added to an {@link HttpServer}
     */
    public static HttpHandler newGrizzlyHttpHandler(HttpApplication httpApplication) {
        return new HandlerAdapter(httpApplication);
    }

    /**
     * Create a new Grizzly {@link HttpHandler} from the given common HTTP Framework {@link Handler}. All the operations
     * performed on the Grizzly {@link HttpHandler} will be forwarded to the common HTTP Framework {@link Handler}. No
     * API Description will be exposed.
     *
     * @param handler
     *            The {@link HttpHandler} to wrap.
     * @param storage
     *            The {@link Factory} that will create temporary storage {@link Buffer}s to handle the processing of
     *            requests. If {@code null}, a default buffer factory will be used.
     * @return A Grizzly {@link HttpHandler} ready to be added to an {@link HttpServer}
     */
    public static HttpHandler newGrizzlyHttpHandler(Handler handler, Factory<Buffer> storage) {
        return new HandlerAdapter(simpleHttpApplication(handler, storage));
    }

    /**
     * Create a new Grizzly {@link HttpHandler} from the given common HTTP Framework {@link Handler}. All the operations
     * performed on the Grizzly {@link HttpHandler} will be forwarded to the common HTTP Framework {@link Handler}.
     *
     * @param handler
     *            The {@link HttpHandler} to wrap.
     * @param storage
     *            The {@link Factory} that will create temporary storage {@link Buffer}s to handle the processing of
     *            requests. If {@code null}, a default buffer factory will be used.
     * @param apiProducer
     *            The {@link ApiProducer} to use to expose an OpenAPI API Description.
     * @return A Grizzly {@link HttpHandler} ready to be added to an {@link HttpServer}
     */
    public static HttpHandler newGrizzlyHttpHandler(Handler handler, Factory<Buffer> storage,
            ApiProducer<Swagger> apiProducer) {
        return new HandlerAdapter(new SimpleHttpApplication(handler, storage, apiProducer));
    }

    private static final class SimpleHttpApplication implements DescribedHttpApplication {

        private final Handler handler;
        private final Factory<Buffer> storage;
        private final ApiProducer<Swagger> apiContext;

        SimpleHttpApplication(Handler handler, Factory<Buffer> storage, ApiProducer<Swagger> apiContext) {
            this.handler = handler;
            this.storage = storage;
            this.apiContext = apiContext;
        }

        @Override
        public Handler start() throws HttpApplicationException {
            return handler;
        }

        @Override
        public Factory<Buffer> getBufferFactory() {
            return storage;
        }

        @Override
        public void stop() {
            // Nothing to do
        }

        @Override
        public ApiProducer<Swagger> getApiProducer() {
            return apiContext;
        }
    }
}
