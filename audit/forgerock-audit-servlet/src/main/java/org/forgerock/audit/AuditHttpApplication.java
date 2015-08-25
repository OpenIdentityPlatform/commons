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

package org.forgerock.audit;

import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;

import java.io.IOException;
import java.io.InputStream;

import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crest Application that instantiates the AuditService on the crest router.
 */
public final class AuditHttpApplication implements HttpApplication {

    private static final Logger logger = LoggerFactory.getLogger(AuditHttpApplication.class);

    @Override
    public Handler start() throws HttpApplicationException {
        final Router router = new Router();
        final AuditService auditService = createAndConfigureAuditService();

        // TODO: replace hard-coded registration of handlers by dynamic registration to allow to plug-in new
        // handlers
        registerCsvHandler(auditService);

        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/audit"),
                CrestHttp.newHttpHandler(Resources.newInternalConnectionFactory(auditService)));
        return router;
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }

    /** Register the CSV handler based on JSON configuration. */
    private static void registerCsvHandler(final AuditService auditService) {
        try (InputStream handlerConfig = auditService.getClass().getResourceAsStream("/conf/audit-csv-handler.json")) {
            JsonValue jsonHandlerConfig = AuditJsonConfig.getJson(handlerConfig);
            AuditJsonConfig.registerHandlerToService(jsonHandlerConfig, auditService);
        } catch (AuditException | IOException e) {
            RuntimeException runtimeException = new RuntimeException("Error while enabling the CSV handler", e);
            logger.error(runtimeException.getMessage(), runtimeException.getCause());
            throw runtimeException;
        }
    }

    /** Returns the audit service configured with provided JSON configuration. */
    private static AuditService createAndConfigureAuditService() {
        final AuditService auditService = new AuditService();
        try (InputStream inputStream = auditService.getClass().getResourceAsStream("/conf/audit-service.json")) {
            AuditServiceConfiguration serviceConfig = AuditJsonConfig.parseAuditServiceConfiguration(inputStream);
            auditService.configure(serviceConfig);
            return auditService;
        } catch (AuditException | IOException e) {
            RuntimeException exception = new RuntimeException("Error while configuring the audit service", e);
            logger.error(exception.getMessage(), e);
            throw exception;
        }
    }
}
