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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.forgerock.audit.Audit;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.ConnectionPool;
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
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a {@link AuditEventHandler} to write {@link AuditEvent}s to a JDBC repository.
 **/
public class JDBCAuditEventHandler extends AuditEventHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(JDBCAuditEventHandler.class);
    public static final String MYSQL = "mysql";
    public static final String H2 = "h2";
    public static final String ORACLE = "oracle";

    private final JDBCAuditEventHandlerConfiguration configuration;
    private final DataSource dataSource;
    private final DatabaseStatementProvider databaseStatementProvider;
    private final boolean sharedDataSource;

    /**
     * Create a new JDBCAuditEventHandler instance.
     *
     * @param configuration
     *          Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData
     *          Meta-data for all audit event topics.
     * @param dataSource
     *          Connection pool. If this parameter is null, then a Hikari data source will be created.
     */
    @Inject
    public JDBCAuditEventHandler(
            final JDBCAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final DataSource dataSource) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.configuration = configuration;
        if (dataSource != null) {
            sharedDataSource = true;
            this.dataSource = dataSource;
        } else {
            logger.error("No connection pool (DataSource) provided; defaulting to Hikari");
            sharedDataSource = false;
            this.dataSource = new HikariDataSource(createHikariConfig(configuration.getConnectionPool()));
        }
        this.databaseStatementProvider = getDatabaseStatementProvider(configuration.getDatabaseName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startup() throws ResourceException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws ResourceException {
        if (!sharedDataSource && dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
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
        Connection connection = null;
        try {
            logger.info("Create called for audit event {} with content {}", topic, event);
            connection = dataSource.getConnection();
            if (connection == null) {
                logger.error("No database connection");
                return new InternalServerErrorException("No database connection").asPromise();
            }
            final TableMapping mapping = getTableMapping(topic);
            try (PreparedStatement createStatement =
                    databaseStatementProvider.buildCreateStatement(event, mapping, connection,
                            eventTopicsMetaData.getSchema(topic))) {
                execute(createStatement);
            }
        } catch (AuditException | SQLException e) {
            final String error = String.format("Unable to create audit entry for %s", topic);
            logger.error(error, e);
            CleanupHelper.rollback(connection);
            return new InternalServerErrorException(error, e).asPromise();
        } finally {
            CleanupHelper.close(connection);
        }
        return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, event).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
            final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        final String auditEventTopic = queryRequest.getResourcePathObject().get(0);
        Connection connection = null;
        try {
            logger.info("Query called for audit event: {} with queryFilter: {}", topic,
                    queryRequest.getQueryFilter());
            connection = dataSource.getConnection();
            if (connection == null) {
                logger.error("No database connection");
                return new InternalServerErrorException("No database connection").asPromise();
            }

            final TableMapping mapping = getTableMapping(topic);
            final List<Map<String, Object>> results = new LinkedList<>();
            try (PreparedStatement queryStatement =
                    databaseStatementProvider.buildQueryStatement(
                            mapping, queryRequest, eventTopicsMetaData.getSchema(auditEventTopic), connection)) {
                results.addAll(execute(queryStatement));
            }

            for (Map<String, Object> entry : results) {
                final JsonValue result = processEntry(entry, mapping, topic);
                queryResourceHandler.handleResource(newResourceResponse(result.get(ResourceResponse.FIELD_CONTENT_ID)
                        .asString(), null, result));
            }
            return newQueryResponse(String.valueOf(queryRequest.getPagedResultsOffset() + results.size()),
                            CountPolicy.EXACT, results.size()).asPromise();
        } catch (AuditException | SQLException e) {
            final String error = String.format("Unable to query audit entry for %s", auditEventTopic);
            logger.error(error, e);
            CleanupHelper.rollback(connection);
            return new InternalServerErrorException(error, e).asPromise();
        } finally {
            CleanupHelper.close(connection);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        JsonValue result;
        Connection connection = null;
        try {
            logger.info("Read called for audit event {} with id {}", topic, resourceId);
            connection = dataSource.getConnection();
            if (connection == null) {
                logger.error("No database connection");
                return new InternalServerErrorException("No database connection").asPromise();
            }

            final TableMapping mapping = getTableMapping(topic);
            final List<Map<String, Object>> resultSet = new LinkedList<>();
            try (PreparedStatement readStatement =
                    databaseStatementProvider.buildReadStatement(mapping, resourceId, connection)) {
                resultSet.addAll(execute(readStatement));
            }

            if (resultSet.isEmpty()) {
                return new NotFoundException(String.format("Entry not found for id: %s", resourceId)).asPromise();
            }
            result = processEntry(resultSet.get(0), mapping, topic);
        } catch (AuditException | SQLException e ) {
            final String error = String.format("Unable to read audit entry for %s", topic);
            logger.error(error, e);
            CleanupHelper.rollback(connection);
            return new InternalServerErrorException(error, e).asPromise();
        } finally {
            CleanupHelper.close(connection);
        }
        return newResourceResponse(resourceId, null, result).asPromise();
    }

    private TableMapping getTableMapping(final String auditEventTopic) throws AuditException {
        for (TableMapping tableMapping : configuration.getTableMappings()) {
            if (tableMapping.getEvent().equalsIgnoreCase(auditEventTopic)) {
                return tableMapping;
            }
        }
        throw new AuditException(String.format("No table mapping found for audit event type: %s", auditEventTopic));
    }

    private List<Map<String,Object>> execute(final PreparedStatement preparedStatement) throws AuditException {
        logger.info("Executing sql query");
        try {
            preparedStatement.execute();
            return convertResultSetToList(preparedStatement.getResultSet());
        } catch (SQLException e) {
            logger.error("Unable to execute the prepared statement", e.getMessage());
            throw new AuditException("Unable to execute the prepared statement", e);
        }
    }

    private JsonValue processEntry(final Map<String, Object> sqlResult, final TableMapping tableMapping,
            final String auditEventTopic) throws AuditException {
        final JsonValue result = JsonValue.json(object());
        try {
            for (Map.Entry<String, String> entry : tableMapping.getFieldToColumn().entrySet()) {
                final Object value = sqlResult.get(entry.getValue().toLowerCase());
                if (value != null) {
                    final JsonPointer field = new JsonPointer(entry.getKey());
                    final String fieldType =
                            AuditEventHelper.getPropertyType(eventTopicsMetaData.getSchema(auditEventTopic), field);
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
        } catch (IOException e) {
            logger.error("Unable to process retrieved entry", e);
            throw new AuditException("Unable to process retrieved entry", e);
        }
        return result;
    }

    private List<Map<String,Object>> convertResultSetToList(final ResultSet resultSet) throws AuditException{
        final List<Map<String,Object>> list = new ArrayList<>();
        try (final ResultSet rs = resultSet) {
            if (rs == null) {
                return list;
            }
            final ResultSetMetaData md = resultSet.getMetaData();
            final int columns = md.getColumnCount();
            while (rs.next()) {
                final HashMap<String, Object> row = new HashMap<>(columns);
                for (int i = 1; i <= columns; ++i) {
                    row.put(md.getColumnName(i).toLowerCase(), rs.getString(i));
                }
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            logger.error("Unable to handle result set.", e);
            throw new AuditException("Unable to handle result set", e);
        }
    }

    private HikariConfig createHikariConfig(ConnectionPool connectionPool) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(connectionPool.getAutoCommit());
        hikariConfig.setConnectionTimeout(connectionPool.getConnectionTimeout());
        hikariConfig.setIdleTimeout(connectionPool.getIdleTimeout());
        hikariConfig.setMaximumPoolSize(connectionPool.getMaxPoolSize());
        hikariConfig.setMaxLifetime(connectionPool.getMaxLifetime());
        hikariConfig.setMinimumIdle(connectionPool.getMinIdle());
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
        return hikariConfig;
    }

    private DatabaseStatementProvider getDatabaseStatementProvider(final String databaseName) {
        switch (databaseName) {
            case MYSQL:
            case H2:
                return new GenericDatabaseStatementProvider();
            case ORACLE:
                return new OracleDatabaseStatementProvider();
            default:
                logger.warn("Unknown databaseName provided. Using the generic statement provider: {}", databaseName);
                return new GenericDatabaseStatementProvider();
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
