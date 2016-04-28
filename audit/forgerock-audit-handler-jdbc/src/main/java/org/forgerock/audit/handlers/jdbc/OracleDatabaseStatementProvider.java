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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.jdbc;

import static org.forgerock.util.Utils.joinAsString;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.SortKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the query event for an oracle database.
 */
class OracleDatabaseStatementProvider extends BaseDatabaseStatementProvider {
    private static final Logger logger = LoggerFactory.getLogger(OracleDatabaseStatementProvider.class);

    private final StringSqlQueryFilterVisitor queryFilterVisitor = new StringSqlQueryFilterVisitor();

    /**
     * Builds a {@link JdbcAuditEvent} that will query an oracle database.
     * {@inheritDoc}
     */
    @Override
    public JdbcAuditEvent buildQueryEvent(final TableMapping mapping, final QueryRequest queryRequest,
            final JsonValue eventTopicMetaData) throws AuditException {
        final String querySelectStatement;
        final TableMappingParametersPair tableMappingParametersPair = new TableMappingParametersPair(mapping);

        querySelectStatement = buildQuerySql(queryRequest, tableMappingParametersPair);
        logger.info("Built query select statement: {}", querySelectStatement);

        final SqlStatementParser sqlStatementParser = new SqlStatementParser(querySelectStatement);
        final List<Parameter> params = new LinkedList<>();
        for (String field : sqlStatementParser.getNamedParameters()) {
            params.add(
                    new Parameter(
                            getParameterType(eventTopicMetaData, new JsonPointer(field)),
                            tableMappingParametersPair.getParameters().get(field)));
        }
        return new JdbcAuditEvent(sqlStatementParser.getSqlStatement(), params);
    }

    private String buildQuerySql(final QueryRequest queryRequest,
            final TableMappingParametersPair tableMappingParametersPair) {
        final int offsetParam = queryRequest.getPagedResultsOffset();
        int pageSizeParam = queryRequest.getPageSize();
        if (pageSizeParam == 0) {
            pageSizeParam = Integer.MAX_VALUE;
        }

        final String filterString = queryRequest.getQueryFilter().accept(queryFilterVisitor, tableMappingParametersPair)
                .toSql();

        // default to ordering by id
        String keysClause = "ORDER BY id ASC";

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = queryRequest.getSortKeys();
        // Check for sort keys and build up order-by syntax

        if (sortKeys != null && sortKeys.size() > 0) {
            final List<String> keys = new ArrayList<>();
            for (final SortKey sortKey : sortKeys) {
                keys.add(tableMappingParametersPair.getColumnName(sortKey.getField()) + (sortKey.isAscendingOrder()
                        ? " ASC" : " DESC"));
            }
            keysClause = "ORDER BY " + joinAsString(", ", keys);
        }

        final String tableName = tableMappingParametersPair.getTableMapping().getTable();
        return String.format("SELECT * "
                + "FROM ( SELECT %s.*, row_number() OVER ( %s ) AS R FROM %s WHERE %s ) "
                + "WHERE R BETWEEN %d AND %d ORDER BY R",
                tableName,
                keysClause,
                tableName,
                filterString,
                offsetParam + 1,
                offsetParam + pageSizeParam);
    }

}
