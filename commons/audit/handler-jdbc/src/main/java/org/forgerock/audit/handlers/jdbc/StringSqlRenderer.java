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

/**
 * An {@link SqlRenderer} implementation that renders SQL as a String.
 */
class StringSqlRenderer implements SqlRenderer<String> {
    private final StringBuilder sb = new StringBuilder();

    /**
     * Constructor.
     *
     * @param s the string to startup
     */
    public StringSqlRenderer(String s) {
        sb.append(s);
    }

    /**
     * Append an additional SQL string.
     *
     * @param s the additional SQL string
     * @return the StringSQLRenderer object
     */
    public StringSqlRenderer append(String s) {
        sb.append(s);
        return this;
    }

    /**
     * Render the SQL as a String.
     *
     * @return the SQL String.
     */
    @Override
    public String toSql() {
        return sb.toString();
    }

    /**
     * Provide a string representation of this object.
     *
     * @return the SQL string represented by this object.
     */
    @Override
    public String toString() {
        return toSql();
    }
}
