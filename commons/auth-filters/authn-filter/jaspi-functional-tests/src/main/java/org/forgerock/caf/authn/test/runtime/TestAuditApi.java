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

package org.forgerock.caf.authn.test.runtime;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Singleton;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;

/**
 * <p>Test implementation of the JASPI runtime's {@code AuditApi} interface.</p>
 *
 * <p>Stores each of the audit records locally and provides methods for reading and clearing the stored audit records.
 * </p>
 *
 * @since 1.5.0
 */
@Singleton
public class TestAuditApi implements AuditApi {

    /**
     * Factory method called by the Jaspi runtime to get the instance of the {@code AuditApi} to use when auditing
     * authentication requests.
     *
     * @return An instance of the {@code TestAuditApi}.
     */
    public static AuditApi getAuditApi() {
        return InjectorHolder.getInstance(AuditApi.class);
    }

    private final List<JsonValue> auditRecords = new ArrayList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(JsonValue auditMessage) {
        auditRecords.add(auditMessage);
    }

    /**
     * Gets the audit records.
     *
     * @return The audit records.
     */
    public List<JsonValue> getAuditRecords() {
        return auditRecords;
    }

    /**
     * Clears the stored audit records.
     */
    public void clear() {
        auditRecords.clear();
    }
}
