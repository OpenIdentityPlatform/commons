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

import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.Utils.joinAsString;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.ConnectionPool;
import org.forgerock.audit.handlers.jdbc.TableMappingAndParameters.FieldValuePair;
import org.forgerock.services.context.Context;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SortKey;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.util.Utils;

/**
 * Implements a {@link AuditEventHandler} to write {@link AuditEvent}s to a JDBC repository.
 **/
public class JDBCAuditEventHandler extends AuditEventHandlerBase<JDBCAuditEventHandlerConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCAuditEventHandler.class);

    private Map<String, JsonValue> auditEventsMetaData;
    private JDBCAuditEventHandlerConfiguration config;
    private DataSource dataSource;
    private DependencyProvider dependencyProvider;
    private final StringSQLQueryFilterVisitor queryFilterVisitor = new StringSQLQueryFilterVisitor();

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencyProvider(DependencyProvider provider) {
        this.dependencyProvider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAuditEventsMetaData(final Map<String, JsonValue> auditEventsMetaData) {
        LOGGER.info("Setting audit event metadata: {}", auditEventsMetaData);
        if (auditEventsMetaData == null) {
            this.auditEventsMetaData = Collections.emptyMap();
        } else {
            this.auditEventsMetaData = auditEventsMetaData;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(JDBCAuditEventHandlerConfiguration config) throws ResourceException {
        LOGGER.info("Configuring handler with config: {}", config.toString());
        this.config = config;
        this.dataSource = configureDatasource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startup() throws ResourceException {
        // TODO: Move all I/O initialization here to avoid possible interaction with another instance
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws ResourceException {
        this.config = null;
        if (dataSource != null && dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            dataSource = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<JDBCAuditEventHandlerConfiguration> getConfigurationClass() {
        return JDBCAuditEventHandlerConfiguration.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event) {
        try {
            LOGGER.info("Create called for audit event {} with content {}", topic, event);
            final Connection connection = dataSource.getConnection();
            if (connection == null) {
                LOGGER.error("No database connection");
                return new InternalServerErrorException("No database connection").asPromise();
            }

            final PreparedStatement createStatement =
                    buildCreateStatement(event, topic, connection);
            execute(createStatement);
        } catch (AuditException | SQLException e) {
            final String error = String.format("Unable to create audit entry for %s", topic);
            LOGGER.error(error);
            return new InternalServerErrorException(error, e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
        return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, event).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
            final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        try {
            LOGGER.info("Query called for audit event: {} with queryFilter: {}", topic,
                    queryRequest.getQueryFilter());

            final Connection connection = dataSource.getConnection();
            if (connection == null) {
                LOGGER.error("No database connection");
                return new InternalServerErrorException("No database connection").asPromise();
            }

            final TableMapping mapping = getTableMapping(topic);

            final PreparedStatement queryStatement =
                    buildQueryStatement(mapping, queryRequest, topic, connection);
            final List<Map<String,Object>> results = execute(queryStatement);

            for (Map<String, Object> entry : results) {
                final JsonValue result = processEntry(entry, mapping, topic);
                queryResourceHandler.handleResource(newResourceResponse(result.get(ResourceResponse.FIELD_CONTENT_ID)
                        .asString(), null, result));
            }
            return newQueryResponse(String.valueOf(queryRequest.getPagedResultsOffset() + results.size()),
                            CountPolicy.EXACT, results.size()).asPromise();
        } catch (AuditException | SQLException | IOException e) {
            final String error = String.format("Unable to create audit entry for %s", topic);
            LOGGER.error(error);
            return new InternalServerErrorException(error, e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        JsonValue result;
        try {
            LOGGER.info("Read called for audit event {} with id {}", topic, resourceId);

            final Connection connection = dataSource.getConnection();
            if (connection == null) {
                LOGGER.error("No database connection");
                return new InternalServerErrorException("No database connection").asPromise();
            }

            final TableMapping mapping = getTableMapping(topic);
            final PreparedStatement readStatement = buildReadStatement(mapping, resourceId, connection);
            final List<Map<String,Object>> resultSet = execute(readStatement);

            if (resultSet.isEmpty()) {
                return new NotFoundException(String.format("Entry not found for id: %s", resourceId)).asPromise();
            }
            result = processEntry(resultSet.get(0), mapping, topic);
        } catch (AuditException | SQLException | IOException e ) {
            final String error = String.format("Unable to create audit entry for %s", topic);
            LOGGER.error(error);
            return new InternalServerErrorException(error, e).asPromise();
        }
        return newResourceResponse(resourceId, null, result).asPromise();
    }

    private DataSource configureDatasource() throws ResourceException {
        // attempt to get the connection from the dependency provider
        if (this.dependencyProvider != null) {
            try {
                return this.dependencyProvider.getDependency(DataSource.class);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Unable to get the connection from the dependency provider");
            }
        }

        // create the connection from the json config
        HikariConfig config = new HikariConfig();
        configureConnectionPool(config);
        return new HikariDataSource(config);
    }

    private TableMapping getTableMapping(final String auditEventTopic) throws AuditException {
        for (TableMapping tableMapping : config.getTableMappings()) {
            if (tableMapping.getEvent().equalsIgnoreCase(auditEventTopic)) {
                return tableMapping;
            }
        }
        throw new AuditException(String.format("No table mapping found for audit event type: %s", auditEventTopic));
    }

    private List<Map<String,Object>> execute(final PreparedStatement preparedStatement)
            throws SQLException, AuditException {
        LOGGER.info("Executing sql query");
        try {
            preparedStatement.execute();
            return convertResultSetToList(preparedStatement.getResultSet());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    private JsonValue processEntry(final Map<String, Object> sqlResult, final TableMapping tableMapping,
            final String auditEventTopic) throws SQLException, IOException {
        final JsonValue result = JsonValue.json(object());
        for (Map.Entry<String, String> entry : tableMapping.getFieldToColumn().entrySet()) {
            final Object value = sqlResult.get(entry.getValue().toLowerCase());
            if (value != null) {
                final JsonPointer field = new JsonPointer(entry.getKey());
                final String fieldType = AuditEventHelper.getPropertyType(auditEventsMetaData.get(auditEventTopic), field);
                if (AuditEventHelper.ARRAY_TYPE.equalsIgnoreCase(fieldType)
                        || AuditEventHelper.OBJECT_TYPE.equalsIgnoreCase(fieldType)) {
                    // parse stringified json
                    result.putPermissive(field, Json.readJson((String) value));
                } else {
                    // value doesn't need parsing
                    result.putPermissive(field, value);
                }
            }
        }
        return result;
    }

    public List<Map<String,Object>> convertResultSetToList(final ResultSet resultSet) throws SQLException {
        final List<Map<String,Object>> list = new ArrayList<>();
        if (resultSet == null) {
            return list;
        }

        final ResultSetMetaData md = resultSet.getMetaData();
        final int columns = md.getColumnCount();
        while (resultSet.next()) {
            final HashMap<String,Object> row = new HashMap<>(columns);
            for(int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i).toLowerCase(), resultSet.getObject(i));
            }
            list.add(row);
        }

        return list;
    }

    private String buildQuerySql(final QueryRequest queryRequest,
            final TableMappingAndParameters tableMappingAndParameters) throws SQLException {
        final Map<String, Object> replacementTokens = new LinkedHashMap<>();
        final String offsetParam = String.valueOf(queryRequest.getPagedResultsOffset());
        final String pageSizeParam = String.valueOf(queryRequest.getPageSize());

        String pageClause = new String();
        if (queryRequest.getPageSize() > 0) {
            pageClause = " LIMIT " + pageSizeParam + " OFFSET " + offsetParam;
        }

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = queryRequest.getSortKeys();
        // Check for sort keys and build up order-by syntax
        if (sortKeys != null && sortKeys.size() > 0) {
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < sortKeys.size(); i++) {
                SortKey sortKey = sortKeys.get(i);
                String tokenName = "sortKey" + i;
                keys.add("${" + tokenName + "}" + (sortKey.isAscendingOrder() ? " ASC" : " DESC"));
                replacementTokens.put(tokenName, sortKey.getField().toString().substring(1));
            }
            pageClause = " ORDER BY " + joinAsString(", ", keys) + pageClause;
        }
        return "SELECT * FROM " + tableMappingAndParameters.getTableMapping().getTable() + " "
                + getFilterString(queryRequest.getQueryFilter(), tableMappingAndParameters)
                + pageClause;
    }

    private String getFilterString(final QueryFilter<JsonPointer> filter,
            final TableMappingAndParameters tableMappingAndParameters) {
        return " WHERE " + filter.accept(queryFilterVisitor, tableMappingAndParameters).toSQL();
    }

    private void setPreparedStatementValues(final NamedPreparedStatement namedPreparedStatement,
            final TableMappingAndParameters tableMappingAndParameters, final String auditEventTopic)
            throws SQLException, ResourceException, AuditException {
        for (Map.Entry<String, FieldValuePair> entry : tableMappingAndParameters.getParameters().entrySet()) {
            final JsonPointer field = entry.getValue().getField();
            final Object value = entry.getValue().getValue();
            final String auditEventTopicFieldType =
                    AuditEventHelper.getPropertyType(auditEventsMetaData.get(auditEventTopic), field);
            switch (auditEventTopicFieldType) {
                case AuditEventHelper.OBJECT_TYPE:
                case AuditEventHelper.ARRAY_TYPE:
                case AuditEventHelper.STRING_TYPE:
                    namedPreparedStatement.setString(entry.getKey(), (String) value);
                    break;
                case AuditEventHelper.BOOLEAN_TYPE:
                    namedPreparedStatement.setBoolean(entry.getKey(), (boolean) value);
                    break;
                case AuditEventHelper.NUMBER_TYPE:
                    namedPreparedStatement.setInt(entry.getKey(), (int) value);
                    break;
                default:
                    throw new AuditException("Unknown audit event topic type");
            }
        }
    }

    private PreparedStatement buildReadStatement(final TableMapping mapping, final String id,
            final Connection connection) throws SQLException {
        final String idTableColumn = mapping.getFieldToColumn().get("_id");

        // build the read sql statement
        StringBuilder readSql = new StringBuilder();

        readSql.append("SELECT * FROM").append(" ").append(mapping.getTable()).append(" ")
                .append("WHERE").append(" ").append(idTableColumn).append("=").append("?");

        final String readSqlQuery = readSql.toString();
        LOGGER.info("Built read sql: {}", readSqlQuery);

        final PreparedStatement preparedStatement = connection.prepareStatement(readSqlQuery);
        preparedStatement.setString(1, id);
        return preparedStatement;
    }

    private PreparedStatement buildQueryStatement(final TableMapping mapping, final QueryRequest queryRequest,
            final String auditEventTopic, final Connection connection)
            throws SQLException, ResourceException, AuditException {
        final TableMappingAndParameters tableMappingAndParameters = new TableMappingAndParameters(mapping);
        final String querySql = buildQuerySql(queryRequest, tableMappingAndParameters);
        final NamedPreparedStatement namedPreparedStatement = new NamedPreparedStatement(connection, querySql);
        setPreparedStatementValues(namedPreparedStatement, tableMappingAndParameters, auditEventTopic);
        return namedPreparedStatement.getPreparedStatement();

    }

    private PreparedStatement buildCreateStatement(final JsonValue content, final String auditEventTopic,
            final Connection connection) throws ResourceException, AuditException, SQLException {
        final TableMapping mapping = getTableMapping(auditEventTopic);

        // build the insert sql statement
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO").append(" ").append(mapping.getTable()).append(" ")
                .append("(");
        final Collection<String> columns = mapping.getFieldToColumn().values();
        for (final String column : columns) {
            insertSql.append(column).append(",");
        }
        insertSql.setLength(insertSql.length() - 1);
        insertSql.append(")");
        insertSql.append(" ").append("VALUES").append(" ")
                .append("(");
        final Set<String> fields = mapping.getFieldToColumn().keySet();
        for (final String field : fields) {
            final JsonPointer fieldPointer = new JsonPointer(field);
            insertSql.append("${" + fieldPointer + "}").append(",");
        }
        insertSql.setLength(insertSql.length() - 1);
        insertSql.append(")");
        final String insertStatement = insertSql.toString();
        LOGGER.info("Built insert sql: {}", insertStatement);
        final NamedPreparedStatement namedPreparedStatement = new NamedPreparedStatement(connection, insertStatement);
        for (final String field : fields) {
            final JsonPointer fieldPointer = new JsonPointer(field);
            final String namedParameter = "${" + fieldPointer + "}";
            final String auditEventTopicFieldType =
                    AuditEventHelper.getPropertyType(auditEventsMetaData.get(auditEventTopic), fieldPointer);
            switch (auditEventTopicFieldType) {
                case AuditEventHelper.OBJECT_TYPE:
                case AuditEventHelper.ARRAY_TYPE:
                    namedPreparedStatement.setString(
                            namedParameter,
                            content.get(fieldPointer) == null ? null : content.get(fieldPointer).toString());
                    break;
                case AuditEventHelper.STRING_TYPE:
                    namedPreparedStatement.setString(
                            namedParameter,
                            content.get(fieldPointer) == null ? null : content.get(fieldPointer).asString());
                    break;
                case AuditEventHelper.BOOLEAN_TYPE:
                    namedPreparedStatement.setBoolean(
                            namedParameter,
                            content.get(fieldPointer) == null ? null : content.get(fieldPointer).asBoolean());
                    break;
                case AuditEventHelper.NUMBER_TYPE:
                    namedPreparedStatement.setInt(
                            namedParameter,
                            content.get(fieldPointer) == null ? null : content.get(fieldPointer).asInteger());
                    break;
                default:
                    throw new AuditException("Unknown audit event topic type");
            }
        }
        return namedPreparedStatement.getPreparedStatement();
    }

    private void configureConnectionPool(final HikariConfig hikariConfig) {
        ConnectionPool connectionPool = config.getConnectionPool();
        hikariConfig.setAutoCommit(connectionPool.getAutoCommit());
        hikariConfig.setConnectionTimeout(connectionPool.getConnectionTimeout());
        hikariConfig.setIdleTimeout(connectionPool.getIdleTimeout());
        hikariConfig.setMaximumPoolSize(connectionPool.getMaximumPoolSize());
        hikariConfig.setMaxLifetime(connectionPool.getMaxLifetime());
        hikariConfig.setMinimumIdle(connectionPool.getMinimumIdle());
        if (!isBlank(connectionPool.getJdbcUrl())) {
            hikariConfig.setJdbcUrl(connectionPool.getJdbcUrl());
        }
        if (!isBlank(connectionPool.getDataSourceClassName())) {
            hikariConfig.setDataSourceClassName(connectionPool.getDataSourceClassName());
        }
        if (!isBlank(connectionPool.getUsername())) {
            hikariConfig.setUsername(connectionPool.getUsername());
        }
        if (!isBlank(connectionPool.getPassword())) {
            hikariConfig.setPassword(connectionPool.getPassword());
        }
        if (!isBlank(connectionPool.getPoolName())) {
            hikariConfig.setPoolName(connectionPool.getPoolName());
        }
    }

    private static boolean isBlank(CharSequence charSeq) {
        if (charSeq == null) {
            return true;
        }
        final int length = charSeq.length();
        if (length == 0) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(charSeq.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }
}
