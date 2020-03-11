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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.iwa;

import static javax.security.auth.message.AuthStatus.*;
import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.jaspi.modules.iwa.wdsso.WDSSO;
import org.forgerock.util.promise.Promise;

/**
 * Authentication module that uses IWA for authentication.
 *
 * @since 1.0.0
 */
public class IWAModule implements AsyncServerAuthModule {

    private static final String IWA_FAILED = "iwa-failed";

    private CallbackHandler handler;
    private Map options;

    @Override
    public String getModuleId() {
        return "IWA";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler handler, Map<String, Object> options) {
        this.handler = handler;
        this.options = options;
        return newResultPromise(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
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
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        LOG.debug("IWAModule: validateRequest START");

        Request request = messageInfo.getRequest();
        Response response = messageInfo.getResponse();

        String httpAuthorization = request.getHeaders().getFirst("Authorization");

        try {
            if (httpAuthorization == null || "".equals(httpAuthorization)) {
                LOG.debug("IWAModule: Authorization Header NOT set in request.");

                response.getHeaders().put("WWW-Authenticate", "Negotiate");
                response.setStatus(Status.UNAUTHORIZED);
                Map<String, Object> entity = new HashMap<>();
                entity.put("failure", true);
                entity.put("recason", IWA_FAILED);
                response.setEntity(entity);

                return newResultPromise(SEND_CONTINUE);
            } else {
                LOG.debug("IWAModule: Authorization Header set in request.");
                try {
                    final String username = new WDSSO().process(options, messageInfo, request);
                    LOG.debug("IWAModule: IWA successful with username, {}", username);

                    clientSubject.getPrincipals().add(new Principal() {
                        public String getName() {
                            return username;
                        }
                    });
                } catch (Exception e) {
                    LOG.debug("IWAModule: IWA has failed. {}", e.getMessage());
                    return newExceptionPromise(new AuthenticationException("IWA has failed"));
                }

                return newResultPromise(SUCCESS);
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
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {
        return newResultPromise(SEND_SUCCESS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject subject) {
        return newResultPromise(null);
    }
}
