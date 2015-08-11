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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.example;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.selfservice.core.config.ProcessInstanceConfig.StorageType;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;

import org.forgerock.http.context.RootContext;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.stages.email.EmailStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.util.Factory;

import java.nio.charset.Charset;

/**
 * Basic http application which initialises the user self service service.
 *
 * @since 0.1.0
 */
public final class BasicHttpApplication implements HttpApplication {

    @Override
    public Handler start() throws HttpApplicationException {
        try {
            Router router = new Router();
            router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/reset"), initialiseHandler());
            return router;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Handler initialiseHandler() throws Exception {
        MemoryBackend userStore = new MemoryBackend();
        org.forgerock.json.resource.Router router = new org.forgerock.json.resource.Router();
        router.addRoute("users", userStore);

        // Create a demo user.
        ConnectionFactory connectionFactory = newInternalConnectionFactory(router);
        Connection connection = connectionFactory.getConnection();
        connection.create(new RootContext(), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 0)));

        ProcessInstanceConfig config = ProcessInstanceConfig
                .newBuilder()
                .addStageConfig(new EmailStageConfig())
                .addStageConfig(new ResetConfig())
                .setTokenType(JwtTokenHandler.TYPE)
                .setStorageType(StorageType.STATELESS)
                .build();

        byte[] sharedKey = "!tHiSsOmEsHaReDkEy!".getBytes(Charset.forName("UTF-8"));

        RequestHandler userSelfServiceService = new AnonymousProcessService(config,
                new BasicProgressStageFactory(connectionFactory), new BasicSnapshotTokenHandlerFactory(sharedKey), new BasicLocalStorage());

        return CrestHttp.newHttpHandler(Resources.newInternalConnectionFactory(userSelfServiceService));
    }

    static JsonValue userAliceWithIdAndRev(final int id, final int rev) {
        return json(object(field("name", "alice"), field("age", 20), field("role", "sales"), field(
                "_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        // Do nothing
        return null;
    }

    @Override
    public void stop() {
        // Do nothing
    }

}
