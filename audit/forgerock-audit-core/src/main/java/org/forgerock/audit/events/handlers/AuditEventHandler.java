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

package org.forgerock.audit.events.handlers;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ResourceException;

/**
 * The interface for an AuditEventHandler.
 *
 * @param <CFG> type of the configuration
 */
public interface AuditEventHandler<CFG> extends CollectionResourceProvider {

    /**
     * Configures the Audit Event Handler with the provided configuration.
     *
     * @param config the configuration of the Audit Event Handler
     * @throws ResourceException if configuration fails
     */
    public void configure(final CFG config) throws ResourceException;

    /**
     * Configures the Audit Event Handler with a config.
     * @throws ResourceException if closing the AuditEventHandler fails
     */
    public void close() throws ResourceException;

    /**
     * Set the audit events that this EventHandler may have to handle. This method is supposed to be called by the
     * AuditService when registering this AuditEventHandler.
     *
     * @param auditEvents
     *            List of AuditEvents to handle.
     */
    public void setAuditEventsMetaData(Map<String, JsonValue> auditEvents);
}
