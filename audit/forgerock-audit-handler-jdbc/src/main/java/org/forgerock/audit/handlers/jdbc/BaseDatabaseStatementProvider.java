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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.handlers.jdbc.Parameter.Type;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains builds generic read and create events supported by multiple databases.
 */
abstract class BaseDatabaseStatementProvider implements DatabaseStatementProvider {

    private static final Logger logger = LoggerFactory.getLogger(BaseDatabaseStatementProvider.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public JdbcAuditEvent buildReadEvent(final TableMapping mapping, final String id,
            final JsonValue eventTopicMetaData) throws AuditException {
        final String idTableColumn = mapping.getFieldToColumn().get("_id");

        // build the read sql statement
        String selectStatement = String.format("SELECT * FROM %s WHERE %s = ?", mapping.getTable(), idTableColumn);

        logger.info("Built select statement: {}", selectStatement);
        final JdbcAuditEvent jdbcAuditEvent =
                new JdbcAuditEvent(
                        selectStatement,
                        Collections.singletonList(
                                new Parameter(
                                        getParameterType(eventTopicMetaData, new JsonPointer("_id")),
                                        id)));
        return jdbcAuditEvent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JdbcAuditEvent buildCreateEvent(final JsonValue content, final TableMapping tableMapping,
            final JsonValue eventTopicMetaData) throws AuditException {
        final Map<String, String> fieldToColumn = tableMapping.getFieldToColumn();

        String columns = joinAsString(", ", fieldToColumn.values());
        String replacementTokens = joinAsString(", ", createReplacementTokens(fieldToColumn.keySet()));
        String insertStatement = String.format("INSERT INTO %s ( %s ) VALUES ( %s )",
                tableMapping.getTable(), columns, replacementTokens);
        logger.info("Built insert sql: {}", insertStatement);

        final SqlStatementParser sqlStatementParser = new SqlStatementParser(insertStatement);
        final List<Parameter> params = new LinkedList<>();
        for (String field : sqlStatementParser.getNamedParameters()) {
            final JsonPointer fieldPointer = new JsonPointer(field);
            final Parameter parameter =
                    new Parameter(
                            getParameterType(eventTopicMetaData, fieldPointer),
                            content.get(fieldPointer) == null ? null : content.get(fieldPointer).getObject());
            params.add(parameter);
        }
        return new JdbcAuditEvent(sqlStatementParser.getSqlStatement(), params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract JdbcAuditEvent buildQueryEvent(final TableMapping mapping, final QueryRequest queryRequest,
            final JsonValue eventTopicMetaData) throws AuditException;

    /**
     * Creates a named parameter given a {@link JsonPointer}. A named parameter has the following format: ${SOME_VALUE}.
     * @param pointer The {@link JsonPointer} to wrap.
     * @return A {@link JsonPointer} wrapped as a named parameter.
     */
    protected String createNamedParameter(final JsonPointer pointer) {
        return "${" + pointer.toString() + "}";
    }

    /**
     * Transforms the input values into named parameters.
     * @param values The values to transform.
     * @return A collection of the transformed values.
     */
    protected Collection<String> createReplacementTokens(Collection<String> values) {
        Collection<String> transformedValues = new LinkedList<>();
        for (final String value : values) {
            transformedValues.add(createNamedParameter(new JsonPointer(value)));
        }
        return transformedValues;
    }

    /**
     * Gets the Type of the sql parameter.
     * @param eventTopicMetaData The event topic metadata.
     * @param field The field to get the type of.
     * @return The parameter type.
     * @throws AuditException If unable to get the parameter type.
     */
    protected Type getParameterType(final JsonValue eventTopicMetaData, final JsonPointer field)
            throws AuditException {
        try {
            return Utils.asEnum(AuditEventHelper.getPropertyType(eventTopicMetaData, field), Type.class);
        } catch (ResourceException e) {
            final String error = String.format("Unable to get type for filed %s", field);
            logger.error(error, field);
            throw new AuditException(error, e);
        }
    }
}
