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

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Configures the JDBC mapping and connection pool.
 */
public class JdbcAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonPropertyDescription("audit.handlers.jdbc.connectionPool")
    private ConnectionPool connectionPool = new ConnectionPool();

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jdbc.tableMappings")
    private List<TableMapping> tableMappings = new LinkedList<>();

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jdbc.databaseType")
    private String databaseType;

    @JsonPropertyDescription("audit.handlers.jdbc.buffering")
    private EventBufferingConfiguration buffering = new EventBufferingConfiguration();

    /**
     * Gets the table mappings for the audit events.
     * @return The table mappings for the audit events.
     */
    public List<TableMapping> getTableMappings() {
        if (tableMappings == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(tableMappings);
    }

    /**
     * Sets the table mappings for the audit events.
     * @param tableMappings The table mappings for the audit events.
     */
    public void setTableMappings(List<TableMapping> tableMappings) {
        this.tableMappings = tableMappings;
    }

    /**
     * Gets the connection pool settings.
     * @return The connection pool settings.
     */
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    /**
     * Sets the connection pool settings.
     * @param connectionPool The connection pool settings.
     */
    public void setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    /**
     * Gets the type of the database.
     * @return The type of the database.
     */
    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Sets the type of the database.
     * @param databaseType The type of the database.
     */
    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    @Override
    public boolean isUsableForQueries() {
        return true;
    }

    /**
     * Configuration for a connection pool.
     */
    public static class ConnectionPool {
        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.dataSourceClassName")
        private String dataSourceClassName;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.jdbcUrl")
        private String jdbcUrl;

        @JsonProperty(required = true)
        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.username")
        private String username;

        @JsonProperty(required = true)
        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.password")
        private String password;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.autoCommit")
        private boolean autoCommit = true;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.connectionTimeout")
        private int connectionTimeout = 30000;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.idleTimeout")
        private int idleTimeout = 600000;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.maxLifetime")
        private int maxLifetime = 1800000;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.minIdle")
        private int minIdle = 10;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.maxPoolSize")
        private int maxPoolSize = 10;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.poolName")
        private String poolName;

        @JsonPropertyDescription("audit.handlers.jdbc.connectionPool.driverClassName")
        private String driverClassName;

        /**
         * Gets the class name of the driver to use for the jdbc connection.
         * @return The class name.
         */
        public String getDriverClassName() {
            return driverClassName;
        }

        /**
         * Sets the class name of the driver to use for the jdbc connection.
         * @param driverClassName The driver class name.
         */
        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        /**
         * Gets the datasource class name for the JDBC database.
         * @return The JDBC driver class
         */
        public String getDataSourceClassName() {
            return dataSourceClassName;
        }

        /**
         * Sets the datasource class name for the configured database.
         * @param driverClass The name of the JDBC driver class.
         */
        public void setDataSourceClassName(final String driverClass) {
            this.dataSourceClassName = driverClass;
        }

        /**
         * Gets the JDBC database url.
         * @return The JDBC database url.
         */
        public String getJdbcUrl() {
            return jdbcUrl;
        }

        /**
         * Sets the JDBC database url.
         * @param jdbcUrl The name of the JDBC database url.
         */
        public void setJdbcUrl(final String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        /**
         * Gets the username to use to connect to the JDBC database.
         * @return The username to used to connect to the JDBC database.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets the username to use to connect to the JDBC database.
         * @param username The username to used to connect to the JDBC database.
         */
        public void setUsername(final String username) {
            this.username = username;
        }

        /**
         * Gets the password to use to connect to the JDBC database.
         * @return The password to used to connect to the JDBC database.
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the password to use to connect to the JDBC database.
         * @param password The password to used to connect to the JDBC database.
         */
        public void setPassword(final String password) {
            this.password = password;
        }

        /**
         * Gets the name of the connection pool.
         * @return The name of the connection pool.
         */
        public String getPoolName() {
            return poolName;
        }

        /**
         * Sets the name of the connection pool.
         * @param poolName The name of the connection pool.
         */
        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        /**
         * Gets the maximum size of the connection pool.
         * @return The maximum size of the connection pool.
         */
        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        /**
         * Sets the maximum size of the connection pool.
         * @param maxPoolSize The maximum pool size of the connection pool.
         */
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        /**
         * Gets the minimum number of idle connections in the connection pool.
         * @return The minimum number of idle connections in the connection pool.
         */
        public int getMinIdle() {
            return minIdle;
        }

        /**
         * Sets the minimum number of idle connections in the connection pool.
         * @param minIdle The minimum number of idle connections in the connection pool.
         */
        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        /**
         * Gets the maximum lifetime of a connection in the connection pool.
         * @return The maximum lifetime of a connection in the connection pool.
         */
        public int getMaxLifetime() {
            return maxLifetime;
        }

        /**
         * Sets the maximum lifetime of a connection in the connection pool.
         * @param maxLifetime The maximum lifetime of a connection in the connection pool.
         */
        public void setMaxLifetime(int maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        /**
         * Gets the maximum time a connection is allowed to be idle.
         * @return The maximum time a connection is allowed to be idle.
         */
        public int getIdleTimeout() {
            return idleTimeout;
        }

        /**
         * Sets the maximum time a connection is allowed to be idle.
         * @param idleTimeout The maximum time a connection is allowed to be idle.
         */
        public void setIdleTimeout(int idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        /**
         * Gets the maximum amount of time to wait for a connection from the connection pool.
         * @return The maximum amount of time to wait for a connection from the connection pool.
         */
        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        /**
         * Sets the maximum amount of time to wait for a connection from the connection pool.
         * @param connectionTimeout The maximum amount of time to wait for a connection from the connection pool.
         */
        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        /**
         * Gets the auto commit value.
         * @return The auto commit value.
         */
        public boolean getAutoCommit() {
            return autoCommit;
        }

        /**
         * Sets the auto commit value.
         * @param autoCommit The auto commit value.
         */
        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }
    }

    /**
     * Returns the configuration for events buffering.
     *
     * @return the configuration
     */
    public EventBufferingConfiguration getBuffering() {
        return buffering;
    }

    /**
     * Sets the configuration for events buffering.
     *
     * @param bufferingConfiguration
     *            The configuration
     */
    public void setBufferingConfiguration(EventBufferingConfiguration bufferingConfiguration) {
        this.buffering = bufferingConfiguration;
    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.jdbc.buffering.enabled")
        private boolean enabled = false;

        @JsonPropertyDescription("audit.handlers.jdbc.buffering.autoFlush")
        private boolean autoFlush = true;

        @JsonPropertyDescription("audit.handlers.jdbc.buffering.maxSize")
        private int maxSize = 5000;

        @JsonPropertyDescription("audit.handlers.jdbc.buffering.interval")
        private String writeInterval = "disabled";

        @JsonPropertyDescription("audit.handlers.jdbc.buffering.writerThreads")
        private int writerThreads = 1;

        @JsonPropertyDescription("audit.handlers.jdbc.buffering.maxBatchedEvents")
        private int maxBatchedEvents = 100;


        /**
         * Indicates if event buffering is enabled.
         *
         * @return {@code true} if buffering is enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the buffering status.
         *
         * @param enabled
         *            Indicates if buffering is enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Indicates if events are automatically flushed after being written.
         *
         * @return {@code true} if events must be flushed
         */
        public boolean isAutoFlush() {
            return autoFlush;
        }

        /**
         * Sets the auto flush indicator.
         *
         * @param auto
         *            Indicates if events are automatically flushed after being written.
         */
        public void setAutoFlush(boolean auto) {
            this.autoFlush = auto;
        }

        /**
         * Returns the maximum size of the queue.
         *
         * @return maxSize Maximum number of events in the queue.
         */
        public int getMaxSize() {
            return maxSize;
        }

        /**
         * Sets the maximum size of the events queue.
         *
         * @param maxSize
         *            Maximum number of events in the queue.
         */
        public void setMaxSize(int maxSize) {
            Reject.ifFalse(maxSize >= 1);
            this.maxSize = maxSize;
        }

        /**
         * Gets the interval to write the queued buffered events.
         * @return The interval as a string.
         */
        public String getWriteInterval() {
            return writeInterval;
        }

        /**
         * Sets the interval to write the queued buffered events.
         * @param writeInterval The interval as a string.
         */
        public void setWriteInterval(String writeInterval) {
            this.writeInterval = writeInterval;
        }

        /**
         * Gets the number of writer threads to use to write buffered events.
         * @return The number of writer threads.
         */
        public int getWriterThreads() {
            return writerThreads;
        }

        /**
         * Sets the number of writer threads to use to write buffered events.
         * @param writerThreads The number of writer threads.
         */
        public void setWriterThreads(int writerThreads) {
            Reject.ifFalse(writerThreads >= 1);
            this.writerThreads = writerThreads;
        }

        /**
         * Gets the maximum number of events that can be batched into a {@link PreparedStatement}.
         * @return The maximum number of batches.
         */
        public int getMaxBatchedEvents() {
            return maxBatchedEvents;
        }

        /**
         * Sets the maximum number of events that can be batched into a {@link PreparedStatement}.
         * @param maxBatchedEvents The maximum number of batches.
         */
        public void setMaxBatchedEvents(int maxBatchedEvents) {
            this.maxBatchedEvents = maxBatchedEvents;
        }
    }
}
