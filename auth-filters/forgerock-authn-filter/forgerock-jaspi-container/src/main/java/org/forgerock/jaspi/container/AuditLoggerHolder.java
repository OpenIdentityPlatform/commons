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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container;

/**
 * Singleton pattern for ensuring only one instance of the AuditLogger is ever created.
 */
public enum AuditLoggerHolder {

    /** The Singleton instance. */
    INSTANCE;

    private AuditLogger authenticationAuditLogger;

    private AuditLoggerHolder() {
        authenticationAuditLogger = null;
    }

    /**
     * Creates the Enum instance with the AuditLogger instance.
     *
     * @param auditLogger The AuditLogger instance.
     */
    public void setAuthenticationAuditLogger(AuditLogger auditLogger) {
        this.authenticationAuditLogger = auditLogger;
    }

    /**
     * Returns the AuditLogger instance.
     *
     * @return The AuditLogger instance.
     */
    AuditLogger getInstance() {
        return authenticationAuditLogger;
    }
}
