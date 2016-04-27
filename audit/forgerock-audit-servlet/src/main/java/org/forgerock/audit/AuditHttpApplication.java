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

import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.json.AuditJsonConfig.getJson;
import static org.forgerock.audit.json.AuditJsonConfig.registerHandlerToService;
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
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crest Application that instantiates the AuditService on the crest router.
 */
public final class AuditHttpApplication implements HttpApplication {

    private static final Logger logger = LoggerFactory.getLogger(AuditHttpApplication.class);
    public static final String AUDIT_EVENT_HANDLERS_CONFIG = "/conf/audit-event-handlers.json";
    public static final String EVENT_HANDLERS = "eventHandlers";
    public static final String AUDIT_ROOT_PATH = "/audit";

    @Override
    public Handler start() throws HttpApplicationException {
        final Router router = new Router();
        final AuditServiceConfiguration auditServiceConfiguration = loadAuditServiceConfiguration();

        AuditServiceBuilder auditServiceBuilder = newAuditService();
        auditServiceBuilder.withConfiguration(auditServiceConfiguration);

        try (final InputStream eventHandlersConfig = this.getClass().getResourceAsStream(AUDIT_EVENT_HANDLERS_CONFIG)) {
            JsonValue auditEventHandlers = getJson(eventHandlersConfig).get(EVENT_HANDLERS);
            for (final JsonValue handlerConfig : auditEventHandlers) {
                try {
                    registerHandlerToService(handlerConfig, auditServiceBuilder, this.getClass().getClassLoader());
                } catch (Exception ex) {
                    logger.error("Unable to register handler defined by config: " + handlerConfig, ex);
                }
            }
        } catch (AuditException | IOException e) {
            logger.error("Failed to read audit event handler configurations", e);
            throw new HttpApplicationException(e);
        }

        final AuditService auditService = auditServiceBuilder.build();
        try {
            auditService.startup();
        } catch (ServiceUnavailableException e) {
            logger.error("Unable to start audit service", e);
            throw new HttpApplicationException(e);
        }
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, AUDIT_ROOT_PATH),
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

    /** Loads the audit service configuration from JSON. */
    private static AuditServiceConfiguration loadAuditServiceConfiguration() {
        try (InputStream inputStream = getResourceAsStream("/conf/audit-service.json")) {
            return AuditJsonConfig.parseAuditServiceConfiguration(inputStream);
        } catch (AuditException | IOException e) {
            final RuntimeException exception = new RuntimeException("Error while configuring the audit service", e);
            logger.error(exception.getMessage(), e);
            throw exception;
        }
    }

    private static InputStream getResourceAsStream(String path) {
        return AuditHttpApplication.class.getResourceAsStream(path);
    }
}
