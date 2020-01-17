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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2016 Nomura Research Institute, Ltd.
 */
package org.forgerock.audit.handlers.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.forgerock.audit.AuditException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility functions for use within this package.
 */
final class JdbcUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JdbcUtils() {
        // Prevent instantiation
    }

    static void initializePreparedStatement(final PreparedStatement preparedStatement, final List<Parameter> params)
            throws AuditException, SQLException, JsonProcessingException {
        int i = 1;
        for (final Parameter parameter : params) {
            final Object parameterValue = parameter.getParameter();
            switch (parameter.getParameterType()) {
            case STRING:
                preparedStatement.setString(i, (String) parameterValue);
                break;
            case NUMBER:
                if (parameterValue == null) {
                    preparedStatement.setNull(i, Types.FLOAT);
                    break; // avoid fall through into INTEGER case
                }
                if (parameterValue instanceof Float) {
                    preparedStatement.setFloat(i, (Float) parameterValue);
                } else if (parameterValue instanceof Double) {
                    preparedStatement.setDouble(i, (Double) parameterValue);
                } else if (parameterValue instanceof BigDecimal) {
                    preparedStatement.setBigDecimal(i, (BigDecimal) parameterValue);
                }
                // intentional fall through so that number can support the json integer type subset as well
            case INTEGER:
                if (parameterValue == null) {
                    preparedStatement.setNull(i, Types.INTEGER);
                } else if (parameterValue instanceof Long) {
                    preparedStatement.setLong(i, (Long) parameterValue);
                } else if (parameterValue instanceof Integer) {
                    preparedStatement.setInt(i, (Integer) parameterValue);
                } else {
                    final String error = String.format("Unable to map class type %s to %s for field %d",
                            parameterValue.getClass().getCanonicalName(),
                            parameter.getParameterType(),
                            i);
                    LOGGER.error(error);
                    throw new AuditException(error);
                }
                break;
            case BOOLEAN:
                if (parameterValue == null) {
                    preparedStatement.setNull(i, Types.BOOLEAN);
                } else {
                    preparedStatement.setBoolean(i, (Boolean) parameterValue);
                }
                break;
            case OBJECT:
            case ARRAY:
                if (parameterValue == null) {
                    preparedStatement.setString(i, null);
                } else {
                    preparedStatement.setString(i, MAPPER.writeValueAsString(parameterValue));
                }
                break;
            default:
                final String error = String.format("Schema defines unknown type %s for field %d",
                        parameter.getParameterType(),
                        i);
                LOGGER.error(error);
                throw new AuditException(error);
            }
            i++;
        }
    }
}
