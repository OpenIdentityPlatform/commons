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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains builds generic read and create statements supported by multiple databases.
 */
public abstract class BaseDatabaseStatementProvider implements DatabaseStatementProvider {

    private static final Logger logger = LoggerFactory.getLogger(BaseDatabaseStatementProvider.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement buildReadStatement(final TableMapping mapping, final String id,
            final Connection connection) throws AuditException {
        final String idTableColumn = mapping.getFieldToColumn().get("_id");

        // build the read sql statement
        String selectStatement = String.format("SELECT * FROM %s WHERE %s = ?", mapping.getTable(), idTableColumn);

        logger.info("Built select statement: {}", selectStatement);
        final PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement(selectStatement);
            preparedStatement.setString(1, id);
            return preparedStatement;
        } catch (SQLException e) {
            logger.error("Unable to create the PreparedStatement for the read operation", e);
            throw new AuditException("Unable to create the PreparedStatement for the read operation", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement buildCreateStatement(final JsonValue content, final TableMapping tableMapping,
            final Connection connection, final JsonValue auditEventMetadata) throws AuditException {
        final Map<String, String> fieldToColumn = tableMapping.getFieldToColumn();

        String columns = joinAsString(", ", fieldToColumn.values());
        String replacementTokens = joinAsString(", ", createReplacementTokens(fieldToColumn.keySet()));
        String insertStatement = String.format("INSERT INTO %s ( %s ) VALUES ( %s )",
                tableMapping.getTable(), columns, replacementTokens);
        logger.info("Built insert sql: {}", insertStatement);

        try {
            final NamedPreparedStatement namedPreparedStatement =
                    new NamedPreparedStatement(connection, insertStatement);
            final Collection<String> fields = tableMapping.getFieldToColumn().keySet();
            for (final String field : fields) {
                final JsonPointer fieldPointer = new JsonPointer(field);
                final String namedParameter = createNamedParameter(fieldPointer);
                namedPreparedStatement.setObject(
                        namedParameter,
                        content.get(fieldPointer) == null ? null : content.get(fieldPointer).getObject(),
                        getType(auditEventMetadata, fieldPointer));
            }
            return namedPreparedStatement.getPreparedStatement();
        } catch (SQLException e) {
            logger.error("Unable to create the PreparedStatement for the create operation", e);
            throw new AuditException("Unable to create the PreparedStatement for the create operation", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract PreparedStatement buildQueryStatement(final TableMapping mapping, final QueryRequest queryRequest,
            final JsonValue auditEventMetadata, final Connection connection)
            throws AuditException;

    /**
     * Gets the json object type of the given field.
     * @param auditEventMetadata The metadata that stores the type information.
     * @param field The field to get the type of.
     * @return Will return a Java class that represents the json type of the given field.
     * @throws AuditException If unable to find the type of the given field.
     */
    protected Class<?> getType(final JsonValue auditEventMetadata, final JsonPointer field) throws AuditException {
        final String auditEventTopicFieldType;
        try {
            auditEventTopicFieldType = AuditEventHelper.getPropertyType(auditEventMetadata, field);
        } catch (ResourceException e) {
            final String error = String.format("Type for audit event topic: %s", auditEventMetadata.toString());
            logger.error(error);
            throw new AuditException(error, e);
        }
        switch (auditEventTopicFieldType) {
            case AuditEventHelper.OBJECT_TYPE:
                return Map.class;
            case AuditEventHelper.ARRAY_TYPE:
                return List.class;
            case AuditEventHelper.STRING_TYPE:
                return String.class;
            case AuditEventHelper.BOOLEAN_TYPE:
                return Boolean.class;
            case AuditEventHelper.NUMBER_TYPE:
                return Integer.class;
            default:
                throw new AuditException(String.format("Unknown audit event topic type for field: %s", field.toString()));
        }
    }

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
}
