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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Adapter class implementing methods that adapt to and from JASPI interfaces to be able to
 * inter-op with pure JASPI implementations.
 *
 * @since 2.0.0
 */
final class JaspiAdapters {

    private JaspiAdapters() {
        //Private utility constructor
    }

    /**
     * Adapts a JASPI {@code ServerAuthContext} into an {@code AsyncServerAuthContext}.
     *
     * @param authContext The {@code ServerAuthContext} to adapt.
     * @return An {@code AsyncServerAuthContext}.
     */
    static AsyncServerAuthContext adapt(ServerAuthContext authContext) {
        return new ServerAuthContextAdapter(authContext);
    }

    /**
     * Adapts a JASPI {@code ServerAuthModule} into an {@code AsyncServerAuthModule}.
     *
     * @param authModule The {@code ServerAuthModule} to adapt.
     * @return An {@code AsyncServerAuthModule}.
     */
    static AsyncServerAuthModule adapt(ServerAuthModule authModule) {
        return new ServerAuthModuleAdapter(authModule);
    }

    /**
     * Adapts a JASPI {@code AuthException} into an {@code AuthenticationException}.
     *
     * @param exception The {@code AuthException} to adapt.
     * @return An {@code AuthenticationException}.
     */
    static AuthenticationException adapt(AuthException exception) {
        return new AuthenticationException(exception.getMessage());
    }

    /**
     * Adapts an {@code MessageContextInfo} into a JASPI {@code MessageInfo}.
     *
     * @param messageInfoContext The {@code MessageContextInfo} to adapt.
     * @return An {@code MessageInfo}.
     */
    static MessageInfo adapt(MessageInfoContext messageInfoContext) {
        return new MessageInfoAdapter(messageInfoContext);
    }

    private static final class ServerAuthContextAdapter implements AsyncServerAuthContext {

        private final ServerAuthContext authContext;

        private ServerAuthContextAdapter(ServerAuthContext authContext) {
            this.authContext = authContext;
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
                Subject clientSubject, Subject serviceSubject) {
            try {
                AuthStatus authStatus = authContext.validateRequest(adapt(context), clientSubject, serviceSubject);
                return Promises.newResultPromise(authStatus);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
                Subject serviceSubject) {
            try {
                AuthStatus authStatus = authContext.secureResponse(adapt(context), serviceSubject);
                return Promises.newResultPromise(authStatus);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
            try {
                authContext.cleanSubject(adapt(context), clientSubject);
                return Promises.newResultPromise(null);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }
    }

    private static final class ServerAuthModuleAdapter implements AsyncServerAuthModule {

        private final ServerAuthModule authModule;

        private ServerAuthModuleAdapter(ServerAuthModule authModule) {
            this.authModule = authModule;
        }

        @Override
        public String getModuleId() {
            return authModule.getClass().getCanonicalName();
        }

        @Override
        public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy,
                MessagePolicy responsePolicy, CallbackHandler handler, Map<String, Object> options) {
            try {
                authModule.initialize(requestPolicy, responsePolicy, handler, options);
                return Promises.newResultPromise(null);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }

        @Override
        public Collection<Class<?>> getSupportedMessageTypes() {
            Class<?>[] supportedMessageTypes = authModule.getSupportedMessageTypes();
            return new HashSet<Class<?>>(Arrays.asList(supportedMessageTypes));
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
                Subject clientSubject, Subject serviceSubject) {
            try {
                AuthStatus authStatus = authModule.validateRequest(adapt(messageInfo), clientSubject,
                        serviceSubject);
                return Promises.newResultPromise(authStatus);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
                Subject serviceSubject) {
            try {
                AuthStatus authStatus = authModule.secureResponse(adapt(messageInfo), serviceSubject);
                return Promises.newResultPromise(authStatus);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo,
                Subject clientSubject) {
            try {
                authModule.cleanSubject(adapt(messageInfo), clientSubject);
                return Promises.newResultPromise(null);
            } catch (AuthException e) {
                return Promises.newExceptionPromise(adapt(e));
            }
        }
    }

    private static final class MessageInfoAdapter implements MessageInfo {

        private final MessageInfoContext messageInfoContext;

        private MessageInfoAdapter(MessageInfoContext messageInfoContext) {
            this.messageInfoContext = messageInfoContext;
        }

        @Override
        public Object getRequestMessage() {
            return messageInfoContext.getRequest();
        }

        @Override
        public Object getResponseMessage() {
            return messageInfoContext.getResponse();
        }

        @Override
        public void setRequestMessage(Object request) {
            Reject.ifFalse(request instanceof Request);
            messageInfoContext.setRequest((Request) request);
        }

        @Override
        public void setResponseMessage(Object response) {
            Reject.ifFalse(response instanceof Response);
            messageInfoContext.setResponse((Response) response);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map getMap() {
            return messageInfoContext.getRequestContextMap();
        }
    }
}
