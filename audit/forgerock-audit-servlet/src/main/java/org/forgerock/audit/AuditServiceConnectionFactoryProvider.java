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

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.audit.events.handlers.impl.CSVAuditEventHandler;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * ConnectionFactory that instantiates the AuditService on the crest router.
 */
public final class AuditServiceConnectionFactoryProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceConnectionFactoryProvider.class);
    private static final ObjectMapper mapper;

    static {
        final JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }
    private AuditServiceConnectionFactoryProvider() {

    }

    /**
     * Creates a connection factory with the AuditService on the router.
     * @param config the configuration of the servlet
     * @return a ConnectionFactory containing the AuditService endpoint
     * @throws ServletException if the connection factory can't be created
     */
    public static ConnectionFactory getConnectionFactory(final ServletConfig config)
            throws ServletException {
        final Router router = new Router();
        final AuditService auditService = new AuditService();

        Class<AuditServiceConnectionFactoryProvider> klass = AuditServiceConnectionFactoryProvider.class;
        try(InputStream configStream = klass.getResourceAsStream("/conf/audit.json");
            InputStream csvConfigStream = klass.getResourceAsStream("/conf/audit-csv-handler.json");
                ) {
        JsonValue csvConfig = new JsonValue(mapper.readValue(csvConfigStream, Map.class));
        Set<String> csvEvents = csvConfig.get("events").asSet(String.class);
        CSVAuditEventHandler csvAuditEventHandler;
        csvAuditEventHandler = new CSVAuditEventHandler();
        //csvAuditEventHandler.configure(csvConfig.get("config"));

        auditService.register(csvAuditEventHandler, "csv", csvEvents);

        final JsonValue jsonConfig = new JsonValue(mapper.readValue(configStream, Map.class));
        AuditServiceConfiguration serviceConfig = new AuditServiceConfiguration();
        serviceConfig.setQueryHandlerName(jsonConfig.get("useForQueries").asString());
        auditService.configure(serviceConfig);

        } catch (IOException | ResourceException e) {
            RuntimeException runtimeException = new RuntimeException("Unable to parse audit.json config", e);
            logger.error(runtimeException.getMessage(), runtimeException.getCause());
            throw runtimeException;
        } catch (AuditException e) {
            RuntimeException runtimeException = new RuntimeException("Error while registering the handlers", e);
            logger.error(runtimeException.getMessage(), runtimeException.getCause());
            throw runtimeException;
        }

        router.addRoute(RoutingMode.STARTS_WITH, "/audit", auditService);
        return Resources.newInternalConnectionFactory(router);
    }
}
