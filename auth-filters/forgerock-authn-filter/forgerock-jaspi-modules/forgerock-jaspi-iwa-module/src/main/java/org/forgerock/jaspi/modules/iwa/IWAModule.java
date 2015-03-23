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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.iwa;

import static org.forgerock.caf.authentication.framework.JaspiRuntime.LOG;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import org.forgerock.jaspi.modules.iwa.wdsso.WDSSO;

/**
 * Authentication module that uses IWA for authentication.
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class IWAModule implements ServerAuthModule {

    private static final String IWA_FAILED = "iwa-failed";

    private CallbackHandler handler;
    private Map options;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) throws AuthException {
        this.handler = handler;
        this.options = options;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Validates the request by checking the Authorization header in the request for a IWA token and processes that
     * for authentication.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        LOG.debug("IWAModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        String httpAuthorization = request.getHeader("Authorization");

        try {
            if (httpAuthorization == null || "".equals(httpAuthorization)) {
                LOG.debug("IWAModule: Authorization Header NOT set in request.");

                response.addHeader("WWW-Authenticate", "Negotiate");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                try {
                    response.getWriter().write("{\"failure\":true,\"reason\":\"" + IWA_FAILED + "\"}");
                } catch (IOException e) {
                    LOG.debug("IWAModule: Error writing Negotiate header to Response. {}", e.getMessage());
                    throw new AuthException("Error writing to Response");
                }

                return AuthStatus.SEND_CONTINUE;
            } else {
                LOG.debug("IWAModule: Authorization Header set in request.");
                try {
                    final String username = new WDSSO().process(options, request);
                    LOG.debug("IWAModule: IWA successful with username, {}", username);

                    clientSubject.getPrincipals().add(new Principal() {
                        public String getName() {
                            return username;
                        }
                    });
                } catch (Exception e) {
                    LOG.debug("IWAModule: IWA has failed. {}", e.getMessage());
                    throw new AuthException("IWA has failed");
                }

                return AuthStatus.SUCCESS;
            }
        } finally {
            LOG.debug("IWAModule: validateRequest END");
        }
    }

    /**
     * Always returns AuthStatus.SEND_SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
}
