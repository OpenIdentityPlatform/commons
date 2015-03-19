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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
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

/**
 * ConnectionFactory that instantiates the AuditService on the crest router.
 */
public final class AuditServiceConnectionFactoryProvider {
    private static final String INIT_PARAM_URI_TEMPLATE = "uri-template";

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceConnectionFactoryProvider.class);
    private static ObjectMapper mapper;

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
        final String uriTemplate = config.getInitParameter(INIT_PARAM_URI_TEMPLATE);
        if (uriTemplate == null) {
            throw new ServletException("Servlet initialization parameter '"
                    + INIT_PARAM_URI_TEMPLATE + "' not specified");
        }
        final Router router = new Router();
        final AuditService auditService = new AuditService();

        try {
            final InputStream configStream =
                    AuditServiceConnectionFactoryProvider.class.getResourceAsStream("/conf/audit.json");
            final JsonValue jsonConfig = new JsonValue(mapper.readValue(configStream, Map.class));
            auditService.configure(jsonConfig);
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse audit.json config", e);
        } catch (InternalServerErrorException e) {
            throw new RuntimeException("Unable to parse audit.json config", e);
        }

        router.addRoute(RoutingMode.STARTS_WITH, "/audit", auditService);
        return Resources.newInternalConnectionFactory(router);
    }
}
