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
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.stages.email.EmailStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.util.Factory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Basic http application which initialises the user self service service.
 *
 * @since 0.1.0
 */
public final class ExampleSelfServiceApplication implements HttpApplication {

    private final ConnectionFactory crestConnectionFactory;
    private final Properties properties;

    /**
     * Constructs the example application.
     */
    public ExampleSelfServiceApplication() {
        try {
            properties = new Properties(System.getProperties());
            properties.load(getClass().getResourceAsStream("/configuration.properties"));
        } catch (IOException ioE) {
            throw new RuntimeException(ioE);
        }

        try {
            crestConnectionFactory = new CrestServiceRegister().initialise(properties);
        } catch (ResourceException rE) {
            throw new RuntimeException(rE);
        }
    }

    @Override
    public Handler start() throws HttpApplicationException {
        try {
            Router router = new Router();
            router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/reset"), initialiseHandler());
            return router;
        } catch (Exception e) {
            throw new HttpApplicationException(e);
        }
    }

    private Handler initialiseHandler() throws Exception {
        EmailStageConfig emailConfig = new EmailStageConfig();
        emailConfig.setIdentityIdField("_id");
        emailConfig.setIdentityEmailField("mail");
        emailConfig.setIdentityServiceUrl("/users");
        emailConfig.setEmailServiceUrl("/email");
        emailConfig.setEmailFrom("info@admin.org");
        emailConfig.setEmailSubject("Reset password email");
        emailConfig.setEmailMessage("This is your reset email.\nLink: %link%");
        emailConfig.setEmailResetUrlToken("%link%");
        emailConfig.setEmailResetUrl("http://localhost:9999/example/#passwordReset/");

        ProcessInstanceConfig config = ProcessInstanceConfig
                .newBuilder()
                .addStageConfig(emailConfig)
                .addStageConfig(new ResetConfig())
                .setTokenType(JwtTokenHandler.TYPE)
                .setStorageType(StorageType.STATELESS)
                .build();

        byte[] sharedKey = "!tHiSsOmEsHaReDkEy!".getBytes(Charset.forName("UTF-8"));

        RequestHandler userSelfServiceService = new AnonymousProcessService(config,
                new ExampleProgressStageFactory(crestConnectionFactory),
                new ExampleTokenHandlerFactory(sharedKey), new SimpleInMemoryStore());

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
