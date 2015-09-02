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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link PreparedStatement} to allow named parameters instead of just ?.
 * <pre>
 *     For example, a named parameter sql string can be built like so:
 *     String sql = "SELECT * FROM table WHERE column = ${someValue}"
 *     NamedPreparedStatement ns = new NamedPreparedStatement(sql, con);
 *     ns.setObject("${someValue}", "value", String.class);
 * </pre>
 */
public class NamedPreparedStatement {
    private Logger logger = LoggerFactory.getLogger(NamedPreparedStatement.class);

    private PreparedStatement preparedStatement;
    private final List<String> namedParameters = new ArrayList<>();

    /** Pattern matches alphanumeric strings that may contain _ and / surrounded by ${} */
    private static final Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9/_]+\\}");
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a NamedPreparedStatement given a connection and sql statement.
     * @param connection A {@link Connection} used to create the wrapped {@link PreparedStatement}.
     * @param sql A sql string that contains some named parameters.
     * @throws SQLException If unable to create the prepared statement.
     */
    public NamedPreparedStatement(final Connection connection, final String sql) throws SQLException {
        final StringBuffer stringBuffer = new StringBuffer();
        Matcher m = pattern.matcher(sql);
        while (m.find()) {
            final String parameter = m.group(0);
            m.appendReplacement(stringBuffer, "?");
            namedParameters.add(parameter);
        }
        m.appendTail(stringBuffer);
        preparedStatement = connection.prepareStatement(stringBuffer.toString());
    }

    /**
     * Calls the appropriate set method on the wrapped {@link PreparedStatement} with the given value.
     * @param name The named parameter to set.
     * @param value The value to set for the named parameter.
     * @param type The type of the named parameter.
     * @throws SQLException If unable to set the named parameter value.
     */
    public void setObject(final String name, final Object value, final Class<?> type) throws SQLException {
        try {
            if (type.isAssignableFrom(String.class)) {
                preparedStatement.setString(getIndex(name), (String) value);
            } else if (type.isAssignableFrom(Boolean.class)) {
                preparedStatement.setBoolean(getIndex(name), (Boolean) value);
            } else if (type.isAssignableFrom(Integer.class)) {
                preparedStatement.setInt(getIndex(name), (Integer) value);
            } else if (type.isAssignableFrom(Map.class) || type.isAssignableFrom(List.class)) {
                preparedStatement.setString(getIndex(name), mapper.writeValueAsString(value));
            } else {
                logger.error("Unknown class type: {}", type.getName());
                throw new SQLException("Unknown class type: {}", type.getName());
            }
        } catch (JsonProcessingException e) {
            logger.error("Unable to serialize the value in the prepared statement", e);
            throw new SQLException("Unable to serialize the value in the prepared statement", e);
        }
    }

    /**
     * Gets the wrapped {@link PreparedStatement}.
     * @return The wrapped {@link PreparedStatement}.
     */
    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    /**
     * Gets the named parameters.
     * @return The list of named parameters.
     */
    public List<String> getNamedParameters() {
        return namedParameters;
    }

    private int getIndex(String name) {
        return namedParameters.indexOf(name)+1;
    }
}
