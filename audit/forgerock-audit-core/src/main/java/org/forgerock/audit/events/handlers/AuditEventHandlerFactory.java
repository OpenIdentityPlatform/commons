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

package org.forgerock.audit.events.handlers;

import org.forgerock.audit.events.handlers.impl.CSVAuditEventHandler;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Creates an AuditEventHandler.
 */
public final class AuditEventHandlerFactory {
    private final static Logger logger = LoggerFactory.getLogger(AuditEventHandlerFactory.class);

    private static final String CONFIG_LOG_TYPE_CSV = "csv";
    private static final String CONFIG_LOG_TYPE_REPO = "repo";
    private static final String CONFIG_LOG_TYPE_ROUTER = "router";

    private static final String CONFIG = "config";

    private AuditEventHandlerFactory() { }

    /**
     * Creates an AuditEventHandler.
     * @param name the name of the audit event handler.
     * @param auditEventHandler the definition of the audit event handler
     * @param auditEvents the audit events supported in the audit service.
     * @param connectionFactory the internal crest connection factory.
     * @return an audit event handler.
     * @throws ResourceException if unable to create a AuditEventHandler
     */
    public static AuditEventHandler createAuditEventHandler(
            final String name, final JsonValue auditEventHandler,
            final Map<String, JsonValue> auditEvents,
            final ConnectionFactory connectionFactory) throws ResourceException {
        AuditEventHandler handler = null;
        if (CONFIG_LOG_TYPE_CSV.equalsIgnoreCase(name)) {
            handler = new CSVAuditEventHandler(auditEvents);
        } else if (CONFIG_LOG_TYPE_REPO.equalsIgnoreCase(name)) {
            //TODO add repo audit event logger
        } else if (CONFIG_LOG_TYPE_ROUTER.equalsIgnoreCase(name)) {
            //TODO add router audit event logger
        } else {
            //TODO add custom audit event logger
            logger.error("Configured audit event handler is unknown: {}", name);
            return null;
        }

        handler.configure(auditEventHandler.get(CONFIG));
        return handler;
    }
}
