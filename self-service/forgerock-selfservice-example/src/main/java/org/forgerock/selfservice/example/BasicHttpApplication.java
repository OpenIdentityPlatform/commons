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

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.selfservice.core.config.ProcessInstanceConfig.StorageType;

import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.stages.email.EmailStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.util.Factory;

import java.nio.charset.Charset;

/**
 * Basic http application which initialises the user self service service.
 *
 * @since 0.1.0
 */
public final class BasicHttpApplication implements HttpApplication {

    private final ConnectionFactory crestConnectionFactory;

    public BasicHttpApplication() {
        try {
            crestConnectionFactory = new CrestInitialiser().initialise();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

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
        EmailStageConfig emailConfig = new EmailStageConfig();
        emailConfig.setIdentityIdField("_id");
        emailConfig.setIdentityEmailField("mail");
        emailConfig.setIdentityServiceUrl("/users");

        ProcessInstanceConfig config = ProcessInstanceConfig
                .newBuilder()
                .addStageConfig(emailConfig)
                .addStageConfig(new ResetConfig())
                .setTokenType(JwtTokenHandler.TYPE)
                .setStorageType(StorageType.STATELESS)
                .build();

        byte[] sharedKey = "!tHiSsOmEsHaReDkEy!".getBytes(Charset.forName("UTF-8"));

        RequestHandler userSelfServiceService = new AnonymousProcessService(config,
                new BasicProgressStageFactory(crestConnectionFactory),
                new BasicSnapshotTokenHandlerFactory(sharedKey), new BasicLocalStorage());

        return CrestHttp.newHttpHandler(Resources.newInternalConnectionFactory(userSelfServiceService));
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
