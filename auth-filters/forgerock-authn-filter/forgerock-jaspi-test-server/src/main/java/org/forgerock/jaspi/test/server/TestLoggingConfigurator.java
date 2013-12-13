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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.test.server;

import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.JaspiAuditLogger;
import org.forgerock.jaspi.logging.JaspiLoggingConfigurator;

import javax.security.auth.message.MessageInfo;

public class TestLoggingConfigurator implements JaspiLoggingConfigurator {

    public static JaspiLoggingConfigurator getLoggingConfigurator() {
        return new TestLoggingConfigurator();
    }

    @Override
    public DebugLogger getDebugLogger() {
        return new DebugLogger() {
            @Override
            public void trace(String message) {
            }

            @Override
            public void trace(String message, Throwable t) {
            }

            @Override
            public void debug(String message) {
            }

            @Override
            public void debug(String message, Throwable t) {
            }

            @Override
            public void error(String message) {
            }

            @Override
            public void error(String message, Throwable t) {
            }

            @Override
            public void warn(String message) {
            }

            @Override
            public void warn(String message, Throwable t) {
            }
        };
    }

    @Override
    public JaspiAuditLogger getAuditLogger() {
        return new JaspiAuditLogger() {
            @Override
            public void audit(AuditRecord<MessageInfo> auditRecord) {
                AuthResult authResult = auditRecord.getAuthResult();
                MessageInfo messageInfo = auditRecord.getAuditObject();
            }
        };
    }
}
