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

import java.util.LinkedList;
import java.util.List;

/**
 * Stores the sql query string and the parameters that correspond to the ?'s in the query string.
 */
class JdbcAuditEvent {
    private final String sql;
    private final List<Parameter> params;

    /**
     * Creates a JdbcAuditEvent given a sql string and a list of parameters.
     * @param sql The sql string.
     * @param params The list of parameters for the sql string.
     */
    public JdbcAuditEvent(final String sql, final List<Parameter> params) {
        this.sql = sql;
        this.params = new LinkedList<>(params);
    }

    /**
     * Gets the sql string.
     * @return The sql string.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Gets the parameters.
     * @return The parameters.
     */
    public List<Parameter> getParams() {
        return params;
    }
}
