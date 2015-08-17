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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedPreparedStatement {

    private PreparedStatement preparedStatement;
    private final List<String> namedParameters = new ArrayList<>();
    /** Pattern matches alphanumeric strings surrounded by ${} */
    private static final Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9/_]+\\}");

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


    public boolean execute() throws SQLException {
        return preparedStatement.execute();
    }

    public ResultSet getResultSet() throws SQLException {
        return preparedStatement.getResultSet();
    }

    public void close() throws SQLException {
        preparedStatement.close();
    }

    public void setString(final String name, final String value) throws SQLException{
        preparedStatement.setString(getIndex(name), value);
    }

    public void setBoolean(final String name, final boolean value) throws SQLException{
        preparedStatement.setBoolean(getIndex(name), value);
    }

    public void setInt(final String name, final int value) throws SQLException{
        preparedStatement.setInt(getIndex(name), value);
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    public List<String> getNamedParameters() {
        return namedParameters;
    }

    private int getIndex(String name) {
        return namedParameters.indexOf(name)+1;
    }
}
