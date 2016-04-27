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
package org.forgerock.audit.handlers.jdbc;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;

/**
 * Provides Create, Read, and Query events for the implementing database.
 */
interface DatabaseStatementProvider {

    /**
     * Builds a {@link JdbcAuditEvent} that can be used to create a prepared statement to create an event.
     * @param content The content of the audit event.
     * @param tableMapping The TableMapping of json fields to table columns.
     * @return A {@link JdbcAuditEvent}.
     * @throws AuditException If unable to create the {@link JdbcAuditEvent}.
     */
    JdbcAuditEvent buildCreateEvent(JsonValue content, TableMapping tableMapping,
            JsonValue eventTopicMetaData) throws AuditException;

    /**
     * Builds a {@link JdbcAuditEvent} that can be used to create a prepared statement to read an event.
     * @param mapping The TableMapping of json fields to table columns.
     * @param id The id of the object to read.
     * @return A {@link JdbcAuditEvent}.
     * @throws AuditException If unable to create the {@link JdbcAuditEvent}.
     */
    JdbcAuditEvent buildReadEvent(TableMapping mapping, String id, JsonValue eventTopicMetaData)
            throws AuditException;

    /**
     * Builds a {@link JdbcAuditEvent} that can be used to create a prepared statement to query an event.
     * @param mapping The TableMapping of json fields to table columns.
     * @param queryRequest The QueryRequest sent to the audit event handler.
     * @return A {@link JdbcAuditEvent}.
     * @throws AuditException If unable to create the {@link JdbcAuditEvent}.
     */
    JdbcAuditEvent buildQueryEvent(TableMapping mapping, QueryRequest queryRequest,
            JsonValue eventTopicMetaData) throws AuditException;
}
