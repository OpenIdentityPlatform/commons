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

import static org.forgerock.caf.authn.test.modules.SessionAuthModule.*;

/**
 * A test auth module in which the {@link #validateRequest(MessageInfo, Subject, Subject)} and
 * {@link #secureResponse(MessageInfo, Subject)} methods return values can be decided based on the value of two request
 * headers.
 *
 * @since 1.5.0
 */
public class AuthModuleTwo implements ServerAuthModule {

    /**
     * The request header for deciding the return value from {@link #validateRequest(MessageInfo, Subject, Subject)}.
     */
    public final static String AUTH_MODULE_TWO_VALIDATE_REQUEST_HEADER_NAME =
            "X-JASPI-AUTH-MODULE-TWO-VALIDATE-REQUEST";

    /**
     * The request header for deciding the return value from {@link #secureResponse(MessageInfo, Subject)}.
     */
    public final static String AUTH_MODULE_TWO_SECURE_RESPONSE_HEADER_NAME = "X-JASPI-AUTH-MODULE-TWO-SECURE-RESPONSE";

    /**
     * Principal name set by this auth module.
     */
    public final static String AUTH_MODULE_TWO_PRINCIPAL = "AUTH_MODULE_TWO_PRINCIPAL";

    /**
     * Context entry set by this auth module.
     */
    public final static String AUTH_MODULE_TWO_CONTEXT_ENTRY = "AUTH_MODULE_TWO_CONTEXT_ENTRY";

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
     * Return value is based on the presents and value of the {@code X-JASPI-AUTH-MODULE-TWO-VALIDATE-REQUEST} request
     * header.
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

        String header = request.getHeader(AUTH_MODULE_TWO_VALIDATE_REQUEST_HEADER_NAME.toLowerCase());

        clientSubject.getPrincipals().clear();
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return AUTH_MODULE_TWO_PRINCIPAL;
            }
        });

        Map<String, Object> context =
                (Map<String, Object>) messageInfo.getMap().get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);
        context.put(AUTH_MODULE_TWO_CONTEXT_ENTRY, true);

        if (SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SUCCESS;
        }

        if (SEND_SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
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

        throw new AuthException(AUTH_MODULE_TWO_VALIDATE_REQUEST_HEADER_NAME
                + " header not set, so throwing AuthException.");
    }

    /**
     * Return value is based on the presents and value of the {@code X-JASPI-AUTH-MODULE-TWO-SECURE-RESPONSE} request
     * header.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String header = request.getHeader(AUTH_MODULE_TWO_SECURE_RESPONSE_HEADER_NAME.toLowerCase());

        if (SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            return AuthStatus.SUCCESS;
        }

        if (SEND_SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
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

        throw new AuthException(AUTH_MODULE_TWO_SECURE_RESPONSE_HEADER_NAME
                + " header not set, so throwing AuthException.");
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
