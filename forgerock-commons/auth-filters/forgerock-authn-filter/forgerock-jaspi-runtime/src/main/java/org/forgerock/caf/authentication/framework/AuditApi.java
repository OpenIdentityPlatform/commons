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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import org.forgerock.json.JsonValue;

/**
 * Audit API interface for auditing the result of an authentication request.
 *
 * @since 1.5.0
 */
public interface AuditApi {

    /**
     * <p>Audits the authentication request, using the audit information from the given audit message.</p>
     *
     * <p>For successful authentications:</p>
     * {@code
     * {
     *   "result": "SUCCESSFUL",
     *   "requestId": "...",
     *   "principal": [
     *     "demo"
     *   ],
     *   "context": {
     *     ...
     *   },
     *   "sessionId": "...",
     *   "entries": [
     *     {
     *       "moduleId": "Session-JwtSessionModule",
     *       "result": "SUCCESSFUL",
     *       "info": {
     *         "principal": "alice",
     *         "...": "...",
     *         ...
     *       }
     *     }, ...
     *   ],
     *   "transactionId" : "..."
     * }
     * }
     *
     * <p>For failed authentications:</p>
     * {@code
     * {
     *   "result": "FAILED",
     *   "requestId": "...",
     *   "principal": [
     *     "demo",
     *     ... //Multiple auth modules could identify different principals
     *   ],
     *   "context": {
     *     ...
     *   },
     *   "entries": [
     *     {
     *       "moduleId": "Session-JwtSessionModule",
     *       "result": "FAILED",
     *       "reason": "...",
     *       "info": {
     *         "principal": "bob",
     *         "...": "...",
     *         ...
     *       }
     *     }, ...
     *   ],
     *   "transactionId" : "..."
     * }
     * }
     *
     * @param auditMessage The audit message.
     */
    void audit(JsonValue auditMessage);
}
