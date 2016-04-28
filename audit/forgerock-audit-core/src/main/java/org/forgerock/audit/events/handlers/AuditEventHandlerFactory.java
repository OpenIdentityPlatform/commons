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
package org.forgerock.audit.events.handlers;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;

/**
 * Factory interface for creating instances of {@link AuditEventHandler}.
 */
public interface AuditEventHandlerFactory {

    /**
     * Create a new AuditEventHandler instance.
     *
     * @param name
     *          The name of the AuditEventHandler object.
     * @param clazz
     *          The type of AuditEventHandler to create.
     * @param configuration
     *          Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData
     *          Provides meta-data describing the audit event topics this AuditEventHandler may have to handle.
     * @param <T> The type of the handler.
     * @return The handler instance.
     * @throws AuditException
     *          If the required handler could not be constructed for any reason.
     */
    <T extends AuditEventHandler> T create(
            String name,
            Class<T> clazz,
            EventHandlerConfiguration configuration,
            EventTopicsMetaData eventTopicsMetaData) throws AuditException;
}
