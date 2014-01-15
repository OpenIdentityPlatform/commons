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

package org.forgerock.jaspi.test.server.modules;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class AuthModuleTwo implements ServerAuthModule {

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) throws AuthException {
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String header = request.getHeader("X-Jaspi-Auth2-Validate-Request".toLowerCase());

        if ("SUCCESS".equalsIgnoreCase(header)) {
            return AuthStatus.SUCCESS;
        }

        if ("SEND_SUCCESS".equalsIgnoreCase(header)) {
            return AuthStatus.SEND_SUCCESS;
        }

        if ("SEND_FAILURE".equalsIgnoreCase(header)) {
            return AuthStatus.SEND_FAILURE;
        }

        if ("SEND_CONTINUE".equalsIgnoreCase(header)) {
            return AuthStatus.SEND_CONTINUE;
        }

        throw new AuthException("X-Jaspi-Auth2-Validate-Request header not set, so throwing AuthException.");
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String header = request.getHeader("X-Jaspi-Auth2-Secure-Response".toLowerCase());

        if ("SEND_SUCCESS".equalsIgnoreCase(header)) {
            return AuthStatus.SEND_SUCCESS;
        }

        if ("SEND_FAILURE".equalsIgnoreCase(header)) {
            return AuthStatus.SEND_FAILURE;
        }

        if ("SEND_CONTINUE".equalsIgnoreCase(header)) {
            return AuthStatus.SEND_CONTINUE;
        }

        throw new AuthException("X-Jaspi-Auth2-Secure-Response header not set, so throwing AuthException.");
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
    }
}
