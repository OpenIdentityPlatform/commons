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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an sql statement containing named parameters into a sql string, and a list of named parameters.
 */
class SqlStatementParser {

    private String sqlStatement;
    private final List<String> namedParameters = new LinkedList<>();

    /** Pattern matches alphanumeric strings that may contain _ and / surrounded by ${}. */
    private static final Pattern PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9/_]+)\\}");

    /**
     * Creates a SQLStatementParser given a sql string.
     * @param sql A sql string that contains some named parameters.
     */
    public SqlStatementParser(final String sql) {
        final StringBuffer stringBuffer = new StringBuffer();
        Matcher m = PATTERN.matcher(sql);
        while (m.find()) {
            final String parameter = m.group(1);
            m.appendReplacement(stringBuffer, "?");
            namedParameters.add(parameter);
        }
        m.appendTail(stringBuffer);
        sqlStatement = stringBuffer.toString();
    }

    /**
     * Gets the sql string with the named parameters replaced with ?.
     * @return The sql string.
     */
    public String getSqlStatement() {
        return sqlStatement;
    }

    /**
     * Gets the named parameters.
     * @return The list of named parameters.
     */
    public List<String> getNamedParameters() {
        return namedParameters;
    }
}
