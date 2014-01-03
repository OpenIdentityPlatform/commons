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

package org.forgerock.auth.common;

/**
 * Base interface for all audit logging implementations.
 *
 * @author Phill Cunnington
 * @since 1.0.0
 */
public interface AuditLogger<T> {

    /**
     * Audits the AuthN or AuthZ requests outcome.
     * <p>
     * The AuditRecord parameter will contain the originating HttpServletRequest, the AuthResult, with the
     * auth operations outcome and any other relevant information for the audit purposes.
     *
     * @param auditRecord The AuditRecord.
     */
    void audit(AuditRecord<T> auditRecord);
}
