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
package org.forgerock.audit.handlers.jdbc.publishers;

import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.handlers.jdbc.JDBCAuditEvent;

/**
 * Interface that defines the  methods needed to interact with a JDBC database.
 */
public interface JDBCAuditEventExecutor {

    /**
     * Creates a {@link JDBCAuditEvent}
     * @param event The {@link JDBCAuditEvent} to create.
     * @throws AuditException If unable to create the {@link JDBCAuditEvent}.
     */
    void createAuditEvent(final JDBCAuditEvent event) throws AuditException;

    /**
     * Reads a {@link JDBCAuditEvent}
     * @param event The {@link JDBCAuditEvent} to read.
     * @return The read {@link JDBCAuditEvent} data.
     * @throws AuditException If unable to read the {@link JDBCAuditEvent}.
     */
    List<Map<String,Object>> readAuditEvent(final JDBCAuditEvent event) throws AuditException;

    /**
     * Queries a {@link JDBCAuditEvent}
     * @param event The {@link JDBCAuditEvent} to query.
     * @return The read {@link JDBCAuditEvent} data.
     * @throws AuditException If unable to query the {@link JDBCAuditEvent}.
     */
    List<Map<String,Object>> queryAuditEvent(final JDBCAuditEvent event) throws AuditException;

    /**
     * Closes JDBCAuditEventExecutor and performs cleanup.
     */
    void close();

    /**
     * Flushes all create events.
     */
    void flush();
}
