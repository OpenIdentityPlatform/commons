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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.http;

import org.forgerock.http.io.Buffer;
import org.forgerock.util.Factory;
import org.forgerock.util.Reject;

import io.swagger.models.Swagger;

/**
 * Utility methods to work with CHF Applications.
 */
public final class Applications {

    /**
     * Create a simple {@link DescribedHttpApplication} that just returns the provided arguments from the appropriate
     * methods.
     *
     * @param handler
     *            The {@link Handler} to wrap.
     * @param storage
     *            The {@link Factory} that will create temporary storage {@link Buffer}s to handle the processing of
     *            requests. If {@code null}, a default buffer factory will be used.
     * @param apiProducer
     *            The {@link ApiProducer} to use to expose an OpenAPI API Description.
     * @return The {@link HttpApplication}.
     */
    public static DescribedHttpApplication describedHttpApplication(final Handler handler,
            final Factory<Buffer> storage, final ApiProducer<Swagger> apiProducer) {
        Reject.ifNull(handler, apiProducer);
        return new DescribedHttpApplication() {

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
                return apiProducer;
            }
        };
    }

    /**
     * Create a simple {@link HttpApplication} that just returns the provided arguments from the appropriate methods.
     *
     * @param handler
     *            The {@link Handler} to wrap.
     * @param storage
     *            The {@link Factory} that will create temporary storage {@link Buffer}s to handle the processing of
     *            requests. If {@code null}, a default buffer factory will be used.
     * @return The {@link HttpApplication}.
     */
    public static HttpApplication simpleHttpApplication(final Handler handler, final Factory<Buffer> storage) {
        Reject.ifNull(handler);
        return new HttpApplication() {

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
        };
    }

    private Applications() {
        // utility class
    }
}
