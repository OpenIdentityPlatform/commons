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
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.json.resource.Router.uriTemplate;

import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.util.Factory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Basic http application which initialises the user self service service.
 *
 * @since 0.1.0
 */
public final class ExampleSelfServiceApplication implements HttpApplication {

    private ConnectionFactory crestConnectionFactory;
    private Router crestRouter;
    private JsonValue appConfig;

    @Override
    public Handler start() throws HttpApplicationException {
        try {
            appConfig = JsonReader.jsonFileToJsonValue("/config.json");

            crestRouter = new Router();
            crestConnectionFactory = newInternalConnectionFactory(crestRouter);
            registerCRESTServices();

            org.forgerock.http.routing.Router chfRouter = new org.forgerock.http.routing.Router();
            chfRouter.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "internal"),
                    CrestHttp.newHttpHandler(crestConnectionFactory));
            chfRouter.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "reset"), registerResetHandler());
            chfRouter.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "registration"),
                    registerRegistrationHandler());
            return chfRouter;
        } catch (Exception e) {
            throw new HttpApplicationException("Some error starting", e);
        }
    }

    private void registerCRESTServices() throws ResourceException {
        crestRouter.addRoute(uriTemplate("users"), new MemoryBackend());
        crestRouter.addRoute(uriTemplate("email"), new ExampleEmailService(appConfig.get("mailserver")));
    }

    private Handler registerResetHandler() throws Exception {
        VerifyEmailAccountConfig emailConfig = new VerifyEmailAccountConfig()
                .setEmailServiceUrl("/email")
                .setEmailFrom("info@admin.org")
                .setEmailSubject("Reset password email")
                .setEmailMessage("<h3>This is your reset email.</h3>"
                        + "<h4><a href=\"%link%\">Email verification link</a></h4>")
                .setEmailVerificationLinkToken("%link%")
                .setEmailVerificationLink("http://localhost:9999/example/#passwordReset/");

        VerifyUserIdConfig verifyUserIdConfig = new VerifyUserIdConfig(emailConfig)
                .setQueryFields(new HashSet<>(Arrays.asList("_id", "mail")))
                .setIdentityIdField("_id")
                .setIdentityEmailField("mail")
                .setIdentityServiceUrl("/users");

        ResetStageConfig resetConfig = new ResetStageConfig()
                .setIdentityServiceUrl("/users")
                .setIdentityPasswordField("password");

        ProcessInstanceConfig config = new ProcessInstanceConfig()
                .setStageConfigs(Arrays.asList(verifyUserIdConfig, resetConfig))
                .setSnapshotTokenType(JwtTokenHandler.TYPE)
                .setStorageType(StorageType.STATELESS.name());

        byte[] sharedKey = "!tHiSsOmEsHaReDkEy!".getBytes(StandardCharsets.UTF_8);
        long tokenLifeTimeInSeconds = 3L * 60L;

        RequestHandler userSelfServiceService = new AnonymousProcessService(config,
                new ExampleProgressStageFactory(crestConnectionFactory),
                new ExampleTokenHandlerFactory(sharedKey, tokenLifeTimeInSeconds), new SimpleInMemoryStore());

        return CrestHttp.newHttpHandler(Resources.newInternalConnectionFactory(userSelfServiceService));
    }

    private Handler registerRegistrationHandler() throws Exception {
        VerifyEmailAccountConfig emailConfig = new VerifyEmailAccountConfig()
                .setEmailServiceUrl("/email")
                .setEmailFrom("info@admin.org")
                .setEmailSubject("Register new account")
                .setEmailMessage("<h3>This is your registration email.</h3>"
                        + "<h4><a href=\"%link%\">Email verification link</a></h4>")
                .setEmailVerificationLinkToken("%link%")
                .setEmailVerificationLink("http://localhost:9999/example/#register/");

        UserRegistrationConfig registrationConfig = new UserRegistrationConfig()
                .setIdentityServiceUrl("/users")
                .setIdentityEmailField("mail");

        ProcessInstanceConfig config = new ProcessInstanceConfig()
                .setStageConfigs(Arrays.asList(emailConfig, registrationConfig))
                .setSnapshotTokenType(JwtTokenHandler.TYPE)
                .setStorageType(StorageType.STATELESS.name());

        byte[] sharedKey = "!tHiSsOmEsHaReDkEy!".getBytes(StandardCharsets.UTF_8);
        long tokenLifeTimeInSeconds = 3L * 60L;

        RequestHandler userSelfServiceService = new AnonymousProcessService(config,
                new ExampleProgressStageFactory(crestConnectionFactory),
                new ExampleTokenHandlerFactory(sharedKey, tokenLifeTimeInSeconds), new SimpleInMemoryStore());

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
