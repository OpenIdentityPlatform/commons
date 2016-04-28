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
package org.forgerock.audit.handlers.syslog;

/**
 * Defines the standard Syslog message severities.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.1">RFC-5424 section 6.2.1</a>
 */
public enum Severity {

    /**
     * System is unusable.
     */
    EMERGENCY(0),
    /**
     * Action must be taken immediately.
     */
    ALERT(1),
    /**
     * Critical conditions.
     */
    CRITICAL(2),
    /**
     * Error conditions.
     */
    ERROR(3),
    /**
     * Warning conditions.
     */
    WARNING(4),
    /**
     * Normal but significant condition.
     */
    NOTICE(5),
    /**
     * Informational messages.
     */
    INFORMATIONAL(6),
    /**
     * Debug-level messages.
     */
    DEBUG(7);

    private final int code;

    Severity(int code) {
        this.code = code;
    }

    /**
     * Get the syslog code for the severity.
     * @return The code.
     */
    public int getCode() {
        return code;
    }
}
