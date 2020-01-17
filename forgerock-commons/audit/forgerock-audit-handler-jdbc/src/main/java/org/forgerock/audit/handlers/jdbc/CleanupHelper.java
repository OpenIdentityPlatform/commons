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

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleans up jdbc database objects.
 */
final class CleanupHelper {
    final static Logger logger = LoggerFactory.getLogger(CleanupHelper.class);

    /**
     * Commits the changes to the JDBC connection.
     * @param connection The jdbc connection.
     * @throws SQLException If the commit fails.
     * Failures to commit are logged
     */
    public static void commit(Connection connection) throws SQLException {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException ex) {
                logger.warn("Failure during connection commit ", ex);
                throw ex;
            }
        }
    }

    /**
     * Closes the JDBC connection.
     * @param connection The connection to try to close if not null.
     * Failures to close are logged, no exception is propagated up
     */
    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                logger.warn("Failure during connection close ", ex);
            }
        }
    }

    /**
     * Rolls back changes to the {@link Connection}.
     * @param connection The {@link Connection} to rollback the changes.
     * Failures to rollback are logged, no exception is propagated up
     */
    public static void rollback(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException sqlException) {
            logger.error("Unable to rollback changes to database", sqlException);
        }
    }

    private CleanupHelper() {
        // utility class
    }
}
