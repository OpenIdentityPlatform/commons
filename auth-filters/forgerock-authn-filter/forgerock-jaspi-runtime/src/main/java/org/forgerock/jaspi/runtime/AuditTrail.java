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

package org.forgerock.jaspi.runtime;

import org.forgerock.json.fluent.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.forgerock.json.fluent.JsonValue.*;

/**
 * <p>Is responsible for tracking the auditing of an authentication attempt including auditing each of the modules that
 * are executed and the overall result of the authentication.</p>
 *
 * <p>The audit record will include a unique request id, the principal (if authentication was successful) and a session
 * id (if a session was created).</p>
 *
 * @since 1.5.0
 */
public class AuditTrail {

    /**
     * MessageInfo map key for retrieving the audit trail instance.
     */
    public static final String AUDIT_TRAIL_KEY = "org.forgerock.authentication.audit.trail";

    /**
     * MessageInfo map key for setting additional audit information from a module.
     */
    public static final String AUDIT_INFO_KEY = "org.forgerock.authentication.audit.info";

    /**
     * MessageInfo map key for setting the session id for the authentication request.
     */
    public static final String AUDIT_SESSION_ID_KEY = "org.forgerock.authentication.audit.session.id";

    /**
     * MessageInfo map key for setting the reason for the module failure.
     */
    public static final String AUDIT_FAILURE_REASON_KEY = "org.forgerock.authentication.audit.failure.reason";

    private static final String MODULE_ID_KEY = "moduleId";
    private static final String RESULT_KEY = "result";
    private static final String PRINCIPAL_KEY = "principal";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String ENTRIES_KEY = "entries";
    private static final String REASON_KEY = "reason";
    private static final String INFO_KEY = "info";
    private static final String SUCCESSFUL_RESULT = "SUCCESSFUL";
    private static final String FAILED_RESULT = "FAILED";

    private final AuditApi api;
    private final List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
    private final JsonValue auditMessage = json(object(
            field(REQUEST_ID_KEY, UUID.randomUUID().toString()),
            field(ENTRIES_KEY, entries)));

    /**
     * Constructs a new AuditTrail instance.
     *
     * @param api An instance of the {@code AuditApi}.
     */
    public AuditTrail(AuditApi api) {
        this.api = api;
    }

    /**
     * Audits a module as having completed successfully.
     *
     * @param moduleId The id of the module.
     * @param info The module audit info map.
     */
    public void auditSuccess(String moduleId, Map<String, Object> info) {
        entries.add(json(object(
                field(MODULE_ID_KEY, moduleId),
                field(RESULT_KEY, SUCCESSFUL_RESULT),
                field(INFO_KEY, info))
        ).asMap());
    }

    /**
     * Audits a module as having completed as a failure.
     *
     * @param moduleId The id of the module.
     * @param reason The reason the module is reporting a failure.
     * @param info The module audit info map.
     */
    public void auditFailure(String moduleId, String reason, Map<String, Object> info) {
        entries.add(json(object(
                field(MODULE_ID_KEY, moduleId),
                field(RESULT_KEY, FAILED_RESULT),
                field(REASON_KEY, reason),
                field(INFO_KEY, info))
        ).asMap());
    }

    /**
     * Completes the entire audit record as successful.
     *
     * @param principal The principal. Must have been set by the successful module.
     */
    void completeAuditAsSuccessful(String principal) {
        auditMessage.put(RESULT_KEY, "SUCCESSFUL").put(PRINCIPAL_KEY, principal);
    }

    /**
     * Completes the entire audit record as a failure.
     *
     * @param principal The principal. May have been set by one of the modules.
     */
    void completeAuditAsFailure(String principal) {
        auditMessage.put(RESULT_KEY, "FAILED").put(PRINCIPAL_KEY, principal);
    }

    /**
     * Performs the actual audit by calling the {@link AuditApi#audit(JsonValue)} with the audit record.
     */
    void audit() {
        api.audit(auditMessage);
    }

    /**
     * Sets the session id on the audit record, if a session has been created. Will not set the session id on the audit
     * record if it is {@code null} or an empty {@code String}.
     *
     * @param sessionId The session id.
     */
    public void setSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            auditMessage.put(SESSION_ID_KEY, sessionId);
        }
    }

    /**
     * Gets the request id.
     *
     * @return The request id.
     */
    String getRequestId() {
        return auditMessage.get(REQUEST_ID_KEY).asString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return auditMessage.toString();
    }
}
