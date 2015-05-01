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

package org.forgerock.caf.authn.test.modules;

import static org.forgerock.caf.authentication.framework.AuditTrail.AUDIT_INFO_KEY;
import static org.forgerock.caf.authentication.framework.AuditTrail.AUDIT_SESSION_ID_KEY;
import static org.forgerock.caf.authn.test.modules.AuthModuleOne.AUTH_MODULE_ONE_PRINCIPAL;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * A test auth module in which the {@link #validateRequest(MessageInfoContext, Subject, Subject)} adds additional audit
 * information and the {@link #secureResponse(MessageInfoContext, Subject)} attempts to audit a session id.
 *
 * @since 1.5.0
 */
public class AuditingAuthModule implements AsyncServerAuthModule {

    /**
     * Returns the class's short name.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String getModuleId() {
        return getClass().getSimpleName();
    }

    /**
     * Does nothing.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param callbackHandler {@inheritDoc}
     * @param config {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler callbackHandler, Map config) {
        return Promises.newResultPromise(null);
    }

    /**
     * Returns the {@code HttpServletRequest} and {@code HttpServletResponse} classes.
     *
     * @return {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        Collection<Class<?>> supportedMessageTypes = new HashSet<Class<?>>();
        supportedMessageTypes.add(Request.class);
        supportedMessageTypes.add(Response.class);
        return supportedMessageTypes;
    }

    /**
     * Adds module audit info and sets the principal.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        Map<String, Object> auditInfo = (Map<String, Object>) messageInfo.getRequestContextMap().get(AUDIT_INFO_KEY);
        auditInfo.put("AUDITING_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO");

        clientSubject.getPrincipals().clear();
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return AUTH_MODULE_ONE_PRINCIPAL;
            }
        });

        return Promises.newResultPromise(AuthStatus.SUCCESS);
    }

    /**
     * Attempts to add to the module audit info, which should not be added, and sets the session id to be audited, which
     * should be ignored as this module is not a "Session" auth module.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {

        Map<String, Object> auditInfo = (Map<String, Object>) messageInfo.getRequestContextMap().get(AUDIT_INFO_KEY);
        if (auditInfo != null) {
            auditInfo.put("MORE_AUDITING_AUTH_MODULE_AUDIT_INFO", "AUDIT_INFO");
        }

        messageInfo.getRequestContextMap().put(AUDIT_SESSION_ID_KEY, "AUDITING_AUTH_MODULE_SESSION_ID");

        return Promises.newResultPromise(AuthStatus.SEND_SUCCESS);
    }

    /**
     * Does nothing.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject clientSubject) {
        return Promises.newResultPromise(null);
    }
}
