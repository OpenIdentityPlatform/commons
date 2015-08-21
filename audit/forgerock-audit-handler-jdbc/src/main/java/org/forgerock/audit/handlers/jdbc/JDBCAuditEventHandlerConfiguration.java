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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Collections;
import java.util.List;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

public class JDBCAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool")
    private ConnectionPool connectionPool = new ConnectionPool();

    @JsonProperty(required = true)
    @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.tableMappings")
    private List<TableMapping> tableMappings;

    /**
     * Gets the table mappings for the audit events.
     * @return The table mappings for the audit events.
     */
    public List<TableMapping> getTableMappings() {
        if (tableMappings == null) {
            return Collections.emptyList();
        }
        return tableMappings;
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

    public static class ConnectionPool {
        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.dataSourceClassName")
        private String dataSourceClassName;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.jdbcUrl")
        private String jdbcUrl;

        @JsonProperty(required = true)
        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.username")
        private String username;

        @JsonProperty(required = true)
        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.password")
        private String password;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.autoCommit")
        private boolean autoCommit = true;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.connectionTimeout")
        private int connectionTimeout = 30000;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.idleTimeout")
        private int idleTimeout = 600000;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.maxLifetime")
        private int maxLifetime = 1800000;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.minimumIdle")
        private int minimumIdle = 10;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.maximumPoolSize")
        private int maximumPoolSize = 10;

        @JsonPropertyDescription("org.forgerock.audit.handlers.jdbc.JDBCAuditEventHandlerConfiguration.connectionPool.poolName")
        private String poolName;


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
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        /**
         * Sets the maximum size of the connection pool.
         * @param maximumPoolSize The maximum pool size of the connection pool.
         */
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        /**
         * Gets the minimum number of idle connections in the connection pool.
         * @return The minimum number of idle connections in the connection pool.
         */
        public int getMinimumIdle() {
            return minimumIdle;
        }

        /**
         * Sets the minimum number of idle connections in the connection pool.
         * @param minimumIdle The minimum number of idle connections in the connection pool.
         */
        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
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
         * @return The auto commit value.
         */
        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }
    }
}
