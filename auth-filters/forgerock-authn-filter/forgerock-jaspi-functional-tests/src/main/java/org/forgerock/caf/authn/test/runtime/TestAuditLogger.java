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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn.test.runtime;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;

import javax.inject.Singleton;
import javax.security.auth.message.MessageInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Test implementation of the JASPI runtime's {@code AuditLogger} interface.</p>
 *
 * <p>Stores each of the audit records locally and provides methods for reading and clearing the store audit records.
 * </p>
 *
 * @since 1.5.0
 */
@Singleton
public class TestAuditLogger implements AuditLogger<MessageInfo> {

    private final List<AuditRecord<MessageInfo>> auditRecords = new ArrayList<AuditRecord<MessageInfo>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(AuditRecord<MessageInfo> auditRecord) {
        auditRecords.add(auditRecord);
    }

    /**
     * Gets the audit records.
     *
     * @return The audit records.
     */
    public List<AuditRecord<MessageInfo>> getAuditRecords() {
        return auditRecords;
    }

    /**
     * Clears the stored audit records.
     */
    public void clear() {
        auditRecords.clear();
    }
}
