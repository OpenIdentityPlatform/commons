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

import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;

import javax.security.auth.message.MessageInfo;

/**
 * Audit Record which contains a MessageInfo instance.
 *
 * @since 1.3.0
 */
public class MessageInfoAuditRecord extends AuditRecord<MessageInfo> {

    /**
     * Constructs a new AuditRecord with the given AuthResult and MessageInfo.
     *
     * @param authResult The AuthResult of the auth operation.
     * @param messageInfo The MessageInfo of the auth operation.
     */
    public MessageInfoAuditRecord(final AuthResult authResult, final MessageInfo messageInfo) {
        super(authResult, messageInfo);
    }
}
