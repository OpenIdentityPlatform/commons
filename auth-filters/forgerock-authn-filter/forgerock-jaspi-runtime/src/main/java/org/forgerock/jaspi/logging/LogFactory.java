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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.logging;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.auth.common.NoOpAuditLogger;
import org.forgerock.auth.common.NoOpDebugLogger;

import javax.security.auth.message.MessageInfo;

/**
 * Responsible for managing logging implementation, for both Debug and Audit logging.
 *
 * @since 1.3.0
 */
public final class LogFactory {

    private static DebugLogger debugLogger;
    private static AuditLogger<MessageInfo> auditLogger;

    /**
     * Private constructor to prevent instantiation.
     */
    private LogFactory() {
    }

    /**
     * Sets the Debug Logger for the JASPI runtime.
     *
     * @param debug The DebugLogger instance.
     */
    public static void setDebugLogger(final DebugLogger debug) {
        debugLogger = debug;
    }

    /**
     * Gets the Debug Logger for the JASPI runtime.
     * <p>
     * If no Debug Logger has been set then a NoOp Debug Logger will be returned.
     *
     * @return The DebugLogger instance.
     */
    public static DebugLogger getDebug() {
        if (debugLogger == null) {
            return new NoOpDebugLogger();
        }
        return debugLogger;
    }

    /**
     * Sets the Audit Logger for the JASPI runtime.
     *
     * @param audit The AuditLogger instance.
     */
    public static void setAuditLogger(final AuditLogger<MessageInfo> audit) {
        auditLogger = audit;
    }

    /**
     * Gets the Audit Logger for the JASPI runtime.
     * <p>
     * If no Audit Logger has been set then a NoOp Audit Logger will be returned.
     *
     * @return The AuditLogger instance.
     */
    public static AuditLogger<MessageInfo> getAuditLogger() {
        if (auditLogger == null) {
            return new NoOpAuditLogger<MessageInfo>();
        }
        return auditLogger;
    }
}
