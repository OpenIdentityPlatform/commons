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

import static org.forgerock.caf.authentication.framework.AuditTrail.AUDIT_SESSION_ID_KEY;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.http.HttpContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * A test "Session" auth module in which the
 * {@link #validateRequest(MessageInfoContext, Subject, Subject)} and
 * {@link #secureResponse(MessageInfoContext, Subject)} methods return values can be decided based
 * on the value of two request headers.
 *
 * @since 1.5.0
 */
public class SessionAuthModule implements AsyncServerAuthModule {

    /**
     * The request header for deciding the return value from {@link #validateRequest(MessageInfoContext, Subject, Subject)}.
     */
    public final static String SESSION_VALIDATE_REQUEST_HEADER_NAME = "X-JASPI-SESSION-VALIDATE-REQUEST";

    /**
     * The request header for deciding the return value from {@link #secureResponse(MessageInfoContext, Subject)}.
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
        Collection<Class<?>> supportedMessageTypes = new HashSet<>();
        supportedMessageTypes.add(Request.class);
        supportedMessageTypes.add(Response.class);
        return supportedMessageTypes;
    }

    /**
     * Return value is based on the presents and value of the {@code X-JASPI-SESSION-VALIDATE_REQUEST} request header.
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

        HttpContext httpContext = messageInfo.asContext(HttpContext.class);
        HttpServletRequest request = (HttpServletRequest) httpContext.getAttributes().get(HttpServletRequest.class.getName());

        String header = request.getHeader(SESSION_VALIDATE_REQUEST_HEADER_NAME.toLowerCase());

        Map<String, Object> context =
                (Map<String, Object>) messageInfo.getRequestContextMap().get(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT);
        context.put(SESSION_MODULE_CONTEXT_ENTRY, true);

        if (SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            clientSubject.getPrincipals().clear();
            clientSubject.getPrincipals().add(new Principal() {
                @Override
                public String getName() {
                    return SESSION_MODULE_PRINCIPAL;
                }
            });
            return Promises.newResultPromise(AuthStatus.SUCCESS);
        }

        if (SEND_SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            clientSubject.getPrincipals().clear();
            clientSubject.getPrincipals().add(new Principal() {
                @Override
                public String getName() {
                    return SESSION_MODULE_PRINCIPAL;
                }
            });
            return Promises.newResultPromise(AuthStatus.SEND_SUCCESS);
        }

        if (SEND_FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.SEND_FAILURE);
        }

        if (SEND_CONTINUE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.SEND_CONTINUE);
        }

        if (FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.FAILURE);
        }

        if (NULL_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(null);
        }

        return Promises.newExceptionPromise(new AuthenticationException(SESSION_VALIDATE_REQUEST_HEADER_NAME
                + " header not set, so throwing AuthException."));
    }

    /**
     * Return value is based on the presents and value of the {@code X-JASPI-SESSION-SECURE-RESPONSE} request header.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {

        HttpContext httpContext = messageInfo.asContext(HttpContext.class);
        HttpServletRequest request = (HttpServletRequest) httpContext.getAttributes().get(HttpServletRequest.class.getName());

        String header = request.getHeader(SESSION_SECURE_RESPONSE_HEADER_NAME.toLowerCase());

        if (SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.SUCCESS);
        }

        if (SEND_SUCCESS_AUTH_STATUS.equalsIgnoreCase(header)) {
            messageInfo.getRequestContextMap().put(AUDIT_SESSION_ID_KEY, "SESSION_ID");
            return Promises.newResultPromise(AuthStatus.SEND_SUCCESS);
        }

        if (SEND_FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.SEND_FAILURE);
        }

        if (SEND_CONTINUE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.SEND_CONTINUE);
        }

        if (FAILURE_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(AuthStatus.FAILURE);
        }

        if (NULL_AUTH_STATUS.equalsIgnoreCase(header)) {
            return Promises.newResultPromise(null);
        }

        return Promises.newExceptionPromise(new AuthenticationException(SESSION_SECURE_RESPONSE_HEADER_NAME
                + " header not set, so throwing AuthException."));
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
