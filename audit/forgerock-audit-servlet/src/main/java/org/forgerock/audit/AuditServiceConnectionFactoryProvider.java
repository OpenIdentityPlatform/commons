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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConnectionFactory that instantiates the AuditService on the crest router.
 */
public final class AuditServiceConnectionFactoryProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceConnectionFactoryProvider.class);

    private AuditServiceConnectionFactoryProvider() {
        // prevent instantiation
    }

    /**
     * Creates a connection factory with the AuditService on the router.
     *
     * @param config the configuration of the servlet
     * @return a ConnectionFactory containing the AuditService endpoint
     * @throws ServletException if the connection factory can't be created
     */
    public static ConnectionFactory createConnectionFactory(final ServletConfig config) throws ServletException {
        final Router router = new Router();
        final AuditService auditService = createAndConfigureAuditService();

        // TODO: replace hard-coded registration of handlers by dynamic registration to allow to plug-in new
        // handlers
        registerCsvHandler(auditService);

        router.addRoute(RoutingMode.STARTS_WITH, "/audit", auditService);
        return Resources.newInternalConnectionFactory(router);
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
        } catch (AuditException | ResourceException | IOException e) {
            RuntimeException exception = new RuntimeException("Error while configuring the audit service", e);
            logger.error(exception.getMessage(), e);
            throw exception;
        }
    }
}
