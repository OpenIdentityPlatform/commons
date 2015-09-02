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

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;

/**
 * Provides Create, Read, and Query statements for the implementing database.
 */
public interface DatabaseStatementProvider {

    /**
     * Builds a {@link PreparedStatement} to store an audit event in a database. The {@link PreparedStatement}
     * should be in a state so that it can be executed right after being returned.
     * @param content The content of the audit event.
     * @param tableMapping The TableMapping of json fields to table columns.
     * @param connection The JDBC connection to use to create the {@link PreparedStatement}.
     * @param auditEventMetadata The metadata of the audit event.
     * @return A {@link PreparedStatement} that is ready to be executed.
     * @throws AuditException If unable to create the {@link PreparedStatement}.
     */
    PreparedStatement buildCreateStatement(JsonValue content, TableMapping tableMapping, Connection connection,
            JsonValue auditEventMetadata) throws AuditException;

    /**
     * Builds a {@link PreparedStatement} to read an audit event from a database. The {@link PreparedStatement}
     * should be in a state so that it can be executed right after being returned.
     * @param mapping The TableMapping of json fields to table columns.
     * @param id The id of the object to read.
     * @param connection The JDBC connection to use to create the {@link PreparedStatement}.
     * @return A {@link PreparedStatement} that is ready to be executed.
     * @throws AuditException If unable to create the {@link PreparedStatement}.
     */
    PreparedStatement buildReadStatement(TableMapping mapping, String id, Connection connection) throws AuditException;

    /**
     * Builds a {@link PreparedStatement} to query audit events from a database. The {@link PreparedStatement} should
     * be in a state so that it can be executed right after being returned.
     * @param mapping The TableMapping of json fields to table columns.
     * @param queryRequest The QueryRequest sent to the audit event handler.
     * @param auditEventMetadata The metadata of the audit event.
     * @param connection The JDBC connection to use to create the {@link PreparedStatement}.
     * @return A {@link PreparedStatement} that is ready to be executed.
     * @throws AuditException If unable to create the {@link PreparedStatement}.
     */
    PreparedStatement buildQueryStatement(TableMapping mapping, QueryRequest queryRequest, JsonValue auditEventMetadata,
            Connection connection) throws AuditException;
}
