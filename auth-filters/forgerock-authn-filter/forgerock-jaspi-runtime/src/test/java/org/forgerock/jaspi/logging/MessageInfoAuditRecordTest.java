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

package org.forgerock.jaspi.logging;

import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.testng.annotations.Test;

import javax.security.auth.message.MessageInfo;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

public class MessageInfoAuditRecordTest {

    @Test
    public void shouldCreateMessageInfoAuditRecord() {

        //Given
        AuthResult authResult = AuthResult.SUCCESS;
        MessageInfo messageInfo = mock(MessageInfo.class);

        //When
        AuditRecord<MessageInfo> auditRecord = new MessageInfoAuditRecord(authResult, messageInfo);

        //Then
        assertEquals(auditRecord.getAuthResult(), AuthResult.SUCCESS);
        assertEquals(auditRecord.getAuditObject(), messageInfo);
    }
}
