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

package org.forgerock.caf.authn;

import org.assertj.core.data.MapEntry;

import java.util.Arrays;
import java.util.List;

/**
 * Object containing the expected audit operations to have been performed.
 *
 * @since 1.5.0
 */
final class AuditParameters {

    private final String result;
    private final String principal;
    private final boolean sessionPresent;
    private final List<Entry> entries;

    private AuditParameters(String result, String principal, boolean sessionPresent, List<Entry> entries) {
        this.result = result;
        this.principal = principal;
        this.sessionPresent = sessionPresent;
        this.entries = entries;
    }

    /**
     * Creates a new {@code AuditParameters} object with the given parameters.
     *
     * @param result The expected overall audit result of the request.
     * @param principal The expected principal to be set in the audit record.
     * @param sessionPresent Whether a session id should be present in the audit record.
     * @param entries The expected module entries to be in the audit record.
     * @return An {@code AuditParameters} object.
     */
    static AuditParameters auditParams(String result, String principal, boolean sessionPresent, Entry... entries) {
        return new AuditParameters(result, principal, sessionPresent, Arrays.asList(entries));
    }

    public String result() {
        return result;
    }

    public String principal() {
        return principal;
    }

    public boolean sessionPresent() {
        return sessionPresent;
    }

    public List<Entry> entries() {
        return entries;
    }

    static final class Entry {

        private final String moduleId;
        private final String result;
        private final MapEntry[] reasonMatchers;

        private Entry(String moduleId, String result, MapEntry... reasonMatchers) {
            this.moduleId = moduleId;
            this.result = result;
            this.reasonMatchers = reasonMatchers;
        }

        static Entry entry(String moduleId, String result) {
            return entry(moduleId, result, new MapEntry[0]);
        }

        static Entry entry(String moduleId, String result, MapEntry... reasonMatchers) {
            return new Entry(moduleId, result, reasonMatchers);
        }

        public String getModuleId() {
            return moduleId;
        }

        public String getResult() {
            return result;
        }

        public MapEntry[] getReasonMatchers() {
            return reasonMatchers;
        }
    }
}
