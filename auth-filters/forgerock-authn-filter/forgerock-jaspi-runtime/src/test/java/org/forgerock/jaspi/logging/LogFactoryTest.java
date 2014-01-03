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
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.DebugLogger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.message.MessageInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class LogFactoryTest {

    @BeforeMethod
    public void setUp() {
        LogFactory.setDebugLogger(null);
        LogFactory.setAuditLogger(null);
    }

    @Test
    public void shouldGetDebugLogger() {

        //Given
        Exception exception = mock(Exception.class);

        //When
        DebugLogger debugLogger = LogFactory.getDebug();

        //Then
        assertNotNull(debugLogger);
        debugLogger.debug("");
        debugLogger.debug("", exception);
        debugLogger.error("");
        debugLogger.error("", exception);
        debugLogger.warn("");
        debugLogger.warn("", exception);
        verifyZeroInteractions(exception);
    }

    @Test
    public void shouldSetDebugLogger() {

        //Given
        DebugLogger debugLogger = mock(DebugLogger.class);

        //When
        LogFactory.setDebugLogger(debugLogger);

        //Then
        DebugLogger actualDebug = LogFactory.getDebug();
        assertEquals(actualDebug, debugLogger);
    }

    @Test
    public void shouldGetAuditLogger() {

        //Given
        AuditRecord<MessageInfo> auditRecord = mock(MessageInfoAuditRecord.class);

        //When
        AuditLogger<MessageInfo> auditLogger = LogFactory.getAuditLogger();
        auditLogger.audit(auditRecord);

        //Then
        verifyZeroInteractions(auditRecord);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetAuditLogger() {

        //Given
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        //When
        LogFactory.setAuditLogger(auditLogger);

        //Then
        AuditLogger<MessageInfo> actualAuditLogger = LogFactory.getAuditLogger();
        assertEquals(actualAuditLogger, auditLogger);
    }
}
