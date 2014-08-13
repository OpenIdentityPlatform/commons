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

package org.forgerock.caf.authn.test.modules;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;

import static org.forgerock.caf.authn.test.modules.SessionAuthModule.SESSION_MODULE_PRINCIPAL;
import static org.forgerock.jaspi.runtime.AuditTrail.AUDIT_INFO_KEY;
import static org.forgerock.jaspi.runtime.AuditTrail.AUDIT_SESSION_ID_KEY;

/**
 * A test auth module in which the {@link #validateRequest(MessageInfo, Subject, Subject)} adds additional audit
 * information and the {@link #secureResponse(MessageInfo, Subject)} audits a session id.
 *
 * @since 1.5.0
 */
public class AuditingSessionAuthModule implements ServerAuthModule {

    /**
     * Does nothing.
     *
     * @param requestMessagePolicy {@inheritDoc}
     * @param responseMessagePolicy {@inheritDoc}
     * @param callbackHandler {@inheritDoc}
     * @param config {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void initialize(MessagePolicy requestMessagePolicy, MessagePolicy responseMessagePolicy,
            CallbackHandler callbackHandler, Map config) {
    }

    /**
     * Returns the {@code HttpServletRequest} and {@code HttpServletResponse} classes.
     *
     * @return {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Adds module audit info and sets the principal.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws javax.security.auth.message.AuthException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        Map<String, Object> auditInfo = (Map<String, Object>) messageInfo.getMap().get(AUDIT_INFO_KEY);
        auditInfo.put("AUDITING_SESSION_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO");

        clientSubject.getPrincipals().clear();
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return SESSION_MODULE_PRINCIPAL;
            }
        });

        return AuthStatus.SUCCESS;
    }

    /**
     * Attempts to add to the module audit info, which should not be added, and sets the session id to be audited.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws javax.security.auth.message.AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        Map<String, Object> auditInfo = (Map<String, Object>) messageInfo.getMap().get(AUDIT_INFO_KEY);
        if (auditInfo != null) {
            auditInfo.put("MORE_AUDITING_SESSION_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO");
        }

        messageInfo.getMap().put(AUDIT_SESSION_ID_KEY, "AUDITING_SESSION_AUTH_MODULE_SESSION_ID");

        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Does nothing.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject clientSubject) {
    }
}
