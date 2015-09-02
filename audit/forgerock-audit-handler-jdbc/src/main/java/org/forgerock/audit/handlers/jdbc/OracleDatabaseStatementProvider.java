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

import static org.forgerock.util.Utils.joinAsString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.handlers.jdbc.TableMappingParametersPair.FieldValuePair;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.SortKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the query statement for an oracle database.
 */
public class OracleDatabaseStatementProvider extends BaseDatabaseStatementProvider {
    private static final Logger logger = LoggerFactory.getLogger(OracleDatabaseStatementProvider.class);

    private final StringSQLQueryFilterVisitor queryFilterVisitor = new StringSQLQueryFilterVisitor();

    /**
     * Builds a {@link PreparedStatement} that will query an oracle database.
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement buildQueryStatement(final TableMapping mapping, final QueryRequest queryRequest,
            final JsonValue auditEventMetadata, final Connection connection)
            throws AuditException {
        final NamedPreparedStatement namedPreparedStatement;
        final String querySelectStatement;
        final TableMappingParametersPair tableMappingParametersPair = new TableMappingParametersPair(mapping);

        querySelectStatement = buildQuerySQL(queryRequest, tableMappingParametersPair);
        logger.info("Built query select statement: {}", querySelectStatement);

        try {
            namedPreparedStatement = new NamedPreparedStatement(connection, querySelectStatement);
            for (Map.Entry<String, FieldValuePair> entry : tableMappingParametersPair.getParameters().entrySet()) {

                final JsonPointer field = entry.getValue().getField();
                final Object value = entry.getValue().getValue();
                namedPreparedStatement.setObject(entry.getKey(), value, getType(auditEventMetadata, field));
            }
            return namedPreparedStatement.getPreparedStatement();
        } catch (SQLException e) {
            logger.error("Unable to create the PreparedStatement for the query operation", e);
            throw new AuditException("Unable to create the PreparedStatement for the query operation", e);
        }
    }

    private String buildQuerySQL(final QueryRequest queryRequest,
            final TableMappingParametersPair tableMappingParametersPair) {
        final int offsetParam = queryRequest.getPagedResultsOffset();
        int pageSizeParam = queryRequest.getPageSize();
        if (pageSizeParam == 0) {
            pageSizeParam = Integer.MAX_VALUE;
        }

        final String filterString = queryRequest.getQueryFilter().accept(queryFilterVisitor, tableMappingParametersPair).toSQL();

        // default to ordering by id
        String keysClause = "ORDER BY id ASC";

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = queryRequest.getSortKeys();
        // Check for sort keys and build up order-by syntax

        if (sortKeys != null && sortKeys.size() > 0) {
            final List<String> keys = new ArrayList<>();
            for (final SortKey sortKey : sortKeys) {
                keys.add(tableMappingParametersPair.getColumnName(sortKey.getField()) + (sortKey.isAscendingOrder() ?
                        " ASC" : " DESC"));
            }
            keysClause = "ORDER BY " + joinAsString(", ", keys);
        }

        final String tableName = tableMappingParametersPair.getTableMapping().getTable();
        return String.format("SELECT * " +
                        "FROM ( SELECT %s.*, row_number() OVER ( %s ) AS R FROM %s WHERE %s ) " +
                        "WHERE R BETWEEN %d AND %d ORDER BY R",
                tableName,
                keysClause,
                tableName,
                filterString,
                offsetParam+1,
                offsetParam + pageSizeParam);
    }

}
