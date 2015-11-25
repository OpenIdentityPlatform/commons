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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.handlers.jdbc.utils.CleanupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcAuditEventExecutorImpl implements JdbcAuditEventExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JdbcAuditEventExecutorImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataSource dataSource;

    public JdbcAuditEventExecutorImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private List<Map<String,Object>> execute(final JdbcAuditEvent event) throws AuditException {
        logger.debug("Publishing event");
        Connection connection = null;
        final List<Map<String,Object>> results;
        try {
            connection = dataSource.getConnection();
            if (connection == null) {
                logger.error("Unable to get a datasource connection");
                throw new AuditException("Unable to get a datasource connection");
            }

            try (final PreparedStatement preparedStatement = connection.prepareStatement(event.getSql())) {
                initializePreparedStatement(preparedStatement, event.getParams());
                logger.debug("Executing prepared statement");
                preparedStatement.execute();
                results = convertResultSetToList(preparedStatement.getResultSet());
            }
            return results;
        } catch (SQLException | AuditException | JsonProcessingException e) {
            logger.error("Unable to publish audit event", e);
            if (connection != null) {
                CleanupHelper.rollback(connection);
            }
            throw new AuditException("Unable to publish audit event", e);
        } finally {
            CleanupHelper.close(connection);
        }
    }

    private List<Map<String,Object>> convertResultSetToList(final ResultSet resultSet) throws SQLException {
        final List<Map<String,Object>> list = new ArrayList<>();
        if (resultSet == null) {
            return list;
        }
        final ResultSetMetaData md = resultSet.getMetaData();
        final int columns = md.getColumnCount();
        while (resultSet.next()) {
            final HashMap<String, Object> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i).toLowerCase(), resultSet.getString(i));
            }
            list.add(row);
        }
        return list;
    }

    private void initializePreparedStatement(final PreparedStatement preparedStatement, final List<Parameter> params)
            throws AuditException, SQLException, JsonProcessingException {
        int i = 1;
        for (final Parameter parameter : params) {
            switch (parameter.getParameterType()) {
                case STRING:
                    preparedStatement.setString(i, (String) parameter.getParameter());
                    break;
                case NUMBER:
                    preparedStatement.setInt(i, (Integer) parameter.getParameter());
                    break;
                case BOOLEAN:
                    preparedStatement.setBoolean(i, (Boolean) parameter.getParameter());
                    break;
                case OBJECT:
                case ARRAY:
                    preparedStatement.setString(i, mapper.writeValueAsString(parameter.getParameter()));
                    break;
                default:
                    logger.error("Unknown class type");
                    throw new AuditException("Unknown class type");
            }
            i++;
        }
    }

    @Override
    public void createAuditEvent(JdbcAuditEvent event) throws AuditException {
        execute(event);
    }

    @Override
    public List<Map<String, Object>> readAuditEvent(JdbcAuditEvent event) throws AuditException {
        return execute(event);
    }

    @Override
    public List<Map<String, Object>> queryAuditEvent(JdbcAuditEvent event) throws AuditException {
        return execute(event);
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }
}
