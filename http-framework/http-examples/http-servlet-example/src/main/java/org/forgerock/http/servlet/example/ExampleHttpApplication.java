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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.servlet.example;

import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Example single {@link HttpApplication} deployment which registers a
 * {@link Handler} that returns the application name and matched portion of
 * the request uri.
 *
 * <p>The application name is {@literal default} for single
 * {@code HttpApplication} deployments and can be set for multiple
 * {@code HttpApplication} deployments.</p>
 */
public class ExampleHttpApplication implements HttpApplication {

    private final String applicationName;

    /**
     * Default constructor for single {@code HttpApplication} deployments.
     */
    public ExampleHttpApplication() {
        this("default");
    }

    ExampleHttpApplication(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public Handler start() {
        return new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                Map<String, String> content = new HashMap<>();
                content.put("applicationName", applicationName);
                content.put("matchedUri", context.asContext(RouterContext.class).getBaseUri());
                return newResultPromise(new Response(Status.OK).setEntity(content));
            }
        };
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }
}
