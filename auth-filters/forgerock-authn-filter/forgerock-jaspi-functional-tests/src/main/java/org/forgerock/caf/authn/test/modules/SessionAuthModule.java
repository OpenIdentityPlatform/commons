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

import org.forgerock.jaspi.runtime.JaspiRuntime;

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

import static org.forgerock.jaspi.runtime.AuditTrail.AUDIT_SESSION_ID_KEY;

/**
 * A test "Session" auth module in which the {@link #validateRequest(MessageInfo, Subject, Subject)} and
 * {@link #secureResponse(MessageInfo, Subject)} methods return values can be decided based on the value of two request
 * headers.
 *
 * @since 1.5.0
 */
public class SessionAuthModule implements ServerAuthModule {

    /**
     * The request header for deciding the return value from {@link #validateRequest(MessageInfo, Subject, Subject)}.
     */
    public final static String SESSION_VALIDATE_REQUEST_HEADER_NAME = "X-JASPI-SESSION-VALIDATE-REQUEST";

    /**
     * The request header for deciding the return value from {@link #secureResponse(MessageInfo, Subject)}.
     */
    public final static String SESSION_SECURE_RESPONSE_HEADER_NAME = "X-JASPI-SESSION-SECURE-RESPONSE";

    /**
     * The request header value for returning {@code AuthStatus.SUCCESS}.
     */
    public final static String SUCCESS_AUTH_STATUS = "SUCCESS";

    /**
     * The request header value for returning {@code AuthStatus.SEND_SUCCESS}.
     */
    public final static String SEND_SUCCESS_AUTH_STATUS = "SEND_SUCCESS";

    /**
     * The request header value for returning {@code AuthStatus.SEND_FAILURE}.
     */
    public final static String SEND_FAILURE_AUTH_STATUS = "SEND_FAILURE";

    /**
     * The request header value for returning {@code AuthStatus.SEND_CONTINUE}.
     */
    public final static String SEND_CONTINUE_AUTH_STATUS = "SEND_CONTINUE";

    /**
     * The request header value for returning {@code AuthStatus.FAILURE}.
     */
    public final static String FAILURE_AUTH_STATUS = "FAILURE";

    /**
     * The request header value for returning {@code null}.
     */
    public final static String NULL_AUTH_STATUS = "NULL";

    /**
     * Principal name set by this auth module.
     */
    public final static String SESSION_MODULE_PRINCIPAL = "SESSION_PRINCIPAL";

    /**
     * Context entry set by this auth module.
     */
    public final static String SESSION_MODULE_CONTEXT_ENTRY = "SESSION_CONTEXT_ENTRY";

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
     * Return value is based on the presents and value of the {@code X-JASPI-SESSION-VALIDATE_REQUEST} request header.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String header = request.getHeader(SESSION_VALIDATE_REQUEST_HEADER_NAME.toLowerCase());

        Map<String, Object> context =
                (Map<String, Object>) messageInfo.getMap().get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);
        context.put(SESSION_MODULE_CONTEXT_ENTRY, true);

        if (SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            clientSubject.getPrincipals().clear();
            clientSubject.getPrincipals().add(new Principal() {
                @Override
                public String getName() {
                    return SESSION_MODULE_PRINCIPAL;
                }
            });
            return AuthStatus.SUCCESS;
        }

        if (SEND_SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            clientSubject.getPrincipals().clear();
            clientSubject.getPrincipals().add(new Principal() {
                @Override
                public String getName() {
                    return SESSION_MODULE_PRINCIPAL;
                }
            });
            return AuthStatus.SEND_SUCCESS;
        }

        if (SEND_FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SEND_FAILURE;
        }

        if (SEND_CONTINUE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SEND_CONTINUE;
        }

        if (FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.FAILURE;
        }

        if (NULL_AUTH_STATUS.equalsIgnoreCase(header)) {
            return null;
        }

        throw new AuthException(SESSION_VALIDATE_REQUEST_HEADER_NAME + " header not set, so throwing AuthException.");
    }

    /**
     * Return value is based on the presents and value of the {@code X-JASPI-SESSION-SECURE-RESPONSE} request header.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String header = request.getHeader(SESSION_SECURE_RESPONSE_HEADER_NAME.toLowerCase());

        if (SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SUCCESS;
        }

        if (SEND_SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            messageInfo.getMap().put(AUDIT_SESSION_ID_KEY, "SESSION_ID");
            return AuthStatus.SEND_SUCCESS;
        }

        if (SEND_FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SEND_FAILURE;
        }

        if (SEND_CONTINUE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SEND_CONTINUE;
        }

        if (FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.FAILURE;
        }

        if (NULL_AUTH_STATUS.equalsIgnoreCase(header)) {
            return null;
        }

        throw new AuthException(SESSION_SECURE_RESPONSE_HEADER_NAME + " header not set, so throwing AuthException.");
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
