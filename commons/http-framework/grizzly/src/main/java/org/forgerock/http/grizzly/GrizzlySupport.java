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

import static org.forgerock.http.Applications.describedHttpApplication;
import static org.forgerock.http.Applications.simpleHttpApplication;

import org.forgerock.http.ApiProducer;
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
 * <p>
 * Simple example usage with OpenIG. It attempts to listen on port 8080 and expects to find an OpenIG config directory
 * under the config-base directory where the example is started. It runs until an exception is thrown or the process is
 * killed.
 * </p>
 * <pre>
 *     <code>
 *
 *        import org.forgerock.http.HttpApplication;
 *        import org.forgerock.http.grizzly.GrizzlySupport;
 *        import org.forgerock.openig.http.GatewayEnvironment;
 *        import org.forgerock.openig.http.GatewayHttpApplication;
 *        import org.glassfish.grizzly.http.server.HttpServer;
 *
 *        public class Main {
 *
 *            public static void main(String[] args) {
 *
 *                // Set this to an appropriate value to enable OpenIG to find its configuration files.
 *                System.setProperty(GatewayEnvironment.BASE_SYSTEM_PROPERTY, "config-base");
 *                HttpServer server = HttpServer.createSimpleServer(null, 8080);
 *                HttpApplication application = new GatewayHttpApplication();
 *                server.getServerConfiguration().addHttpHandler(GrizzlySupport.newGrizzlyHttpHandler(application));
 *                // Set to true if you want to enable HTTP methods such as Delete having a payload.
 *                server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
 *                try {
 *                    server.start();
 *                } catch (Exception e) {
 *                    server.shutdownNow();
 *                }
 *            }
 *        }
 *     </code>
 * </pre>
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
        return newGrizzlyHttpHandler(simpleHttpApplication(handler, storage));
    }

    /**
     * Create a new Grizzly {@link HttpHandler} from the given common HTTP Framework {@link Handler}. All the operations
     * performed on the Grizzly {@link HttpHandler} will be forwarded to the common HTTP Framework {@link Handler}.
     *
     * @param handler
     *            The {@link Handler} to wrap.
     * @param storage
     *            The {@link Factory} that will create temporary storage {@link Buffer}s to handle the processing of
     *            requests. If {@code null}, a default buffer factory will be used.
     * @param apiProducer
     *            The {@link ApiProducer} to use to expose an OpenAPI API Description.
     * @return A Grizzly {@link HttpHandler} ready to be added to an {@link HttpServer}
     */
    public static HttpHandler newGrizzlyHttpHandler(Handler handler, Factory<Buffer> storage,
            ApiProducer<Swagger> apiProducer) {
        return newGrizzlyHttpHandler(describedHttpApplication(handler, storage, apiProducer));
    }

}
