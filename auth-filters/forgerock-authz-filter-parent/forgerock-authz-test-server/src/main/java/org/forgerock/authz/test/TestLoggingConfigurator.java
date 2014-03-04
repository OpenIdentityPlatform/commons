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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.authz.test;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.authz.AuthorizationLoggingConfigurator;

import javax.servlet.http.HttpServletRequest;

/**
 * Configurates the audit logging for the authz test servlet. Ignores everything.
 *
 * @since 1.4.0
 */
public class TestLoggingConfigurator implements AuthorizationLoggingConfigurator {
    @Override
    public AuditLogger<HttpServletRequest> getAuditLogger() {
        return new AuditLogger<HttpServletRequest>() {
            @Override
            public void audit(AuditRecord<HttpServletRequest> auditRecord) {
                // Ignore
            }
        };
    }

    /**
     * Static factory method used to get hold of the configurator instance.
     *
     * @return a logging configurator instance.
     */
    public static AuthorizationLoggingConfigurator getLoggingConfigurator() {
        return new TestLoggingConfigurator();
    }
}
