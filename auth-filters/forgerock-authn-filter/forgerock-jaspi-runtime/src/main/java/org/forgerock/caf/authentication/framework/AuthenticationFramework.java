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

package org.forgerock.caf.authentication.framework;

import static org.forgerock.caf.authentication.framework.AuditTrail.AUDIT_TRAIL_KEY;
import static org.forgerock.caf.authentication.framework.AuthContexts.*;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.isSendFailure;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.isSuccess;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>An authentication framework for protecting all types of resources.</p>
 *
 * <p>The authentication framework can be configured with a single session authentication module,
 * which will authenticate requests based on some session identifier, and an ordered list of
 * authentication modules, that are executed in order on a first succeeds wins basis.</p>
 *
 * <p>The authentication framework must be configured with a non-{@code null} {@link AuditApi}
 * instance, so that it can audit authentication outcomes.</p>
 *
 * @since 2.0.0
 */
public final class AuthenticationFramework {

    /**
     * Runtime slf4j debug logger.
     */
    public static final Logger LOG = LoggerFactory.getLogger(AuthenticationFramework.class);

    /**
     * The name of the HTTP Servlet Request attribute where the principal name of the user/client
     * making the request will be set.
     */
    public static final String ATTRIBUTE_AUTH_PRINCIPAL = "org.forgerock.authentication.principal";

    /**
     * The name of the HTTP Servlet Request attribute where any additional authentication context
     * information will be set. It MUST contain a {@code Map} if present.
     */
    public static final String ATTRIBUTE_AUTH_CONTEXT = "org.forgerock.authentication.context";

    /**
     * The name of the HTTP Servlet Request attribute where the unique id of the request will be
     * set.
     */
    public static final String ATTRIBUTE_REQUEST_ID = "org.forgerock.authentication.request.id";

    @SuppressWarnings("unchecked")
    static final Collection<Class<?>> REQUIRED_MESSAGE_TYPES_SUPPORT =
            new HashSet<Class<?>>(Arrays.asList(Request.class, Response.class));

    private final Logger logger;
    private final AuditApi auditApi;
    private final ResponseHandler responseHandler;
    private final AsyncServerAuthContext authContext;
    private final Subject serviceSubject;
    private final Promise<List<Void>, AuthenticationException> initializationPromise;

    /**
     * Creates a new {@code JaspiRuntime} instance that will use the configured {@code authContext}
     * to authenticate incoming request and secure outgoing response messages.
     *
     * @param logger The non-{@code null} {@link Logger} instance.
     * @param auditApi The non-{@code null} {@link AuditApi} instance.
     * @param responseHandler The non-{@code null} {@link ResponseHandler} instance.
     * @param authContext The non-{@code null} {@link AsyncServerAuthContext} instance.
     * @param serviceSubject The non-{@code null} service {@link Subject}.
     * @param initializationPromise A {@link Promise} which will be completed once the configured
     *                              auth modules have been initialised.
     */
    AuthenticationFramework(Logger logger, AuditApi auditApi, ResponseHandler responseHandler,
            AsyncServerAuthContext authContext, Subject serviceSubject,
            Promise<List<Void>, AuthenticationException> initializationPromise) {
        Reject.ifNull(logger, auditApi, responseHandler, authContext, serviceSubject, initializationPromise);
        this.logger = logger;
        this.auditApi = auditApi;
        this.responseHandler = responseHandler;
        this.authContext = withValidation(withAuditing(withLogging(logger, authContext)));
        this.serviceSubject = serviceSubject;
        this.initializationPromise = initializationPromise;
    }

    /**
     * Authenticates incoming request messages and if successful calls the downstream filter or
     * handler and then secures the returned response.
     *
     * @param context The request context.
     * @param request The request.
     * @param next The downstream filter or handler in the chain that should only be called if the
     *             request was successfully authenticated.
     * @return A {@code Promise} representing the response to be returned to the client.
     */
    Promise<Response, NeverThrowsException> processMessage(Context context, Request request, final Handler next) {

        final Subject clientSubject = new Subject();
        Map<String, Object> contextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = new AuditTrail(auditApi, contextMap);
        final MessageContextImpl messageContext = new MessageContextImpl(context, request, auditTrail);

        //TODO these need to be gone...
        messageContext.getRequestContextMap().put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        messageContext.getRequestContextMap().put(AUDIT_TRAIL_KEY, auditTrail);

        return initializationPromise
                .thenAsync(onInitializationSuccess(messageContext, clientSubject, next), onFailure(messageContext));
    }

    private AsyncFunction<List<Void>, Response, NeverThrowsException> onInitializationSuccess(
            final MessageContext context, final Subject clientSubject, final Handler next) {
        return new AsyncFunction<List<Void>, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(List<Void> voids) {
                return validateRequest(context, clientSubject, next)
                        .thenAlways(new Runnable() {
                            @Override
                            public void run() {
                                authContext.cleanSubject(context, clientSubject);
                            }
                        });
            }
        };
    }

    private Promise<Response, NeverThrowsException> validateRequest(final MessageContext context, Subject clientSubject,
            final Handler next) {
        return authContext.validateRequest(context, clientSubject, serviceSubject)
                .thenAsync(processResult(context, clientSubject))
                .thenAsync(onValidateRequestSuccess(context, next), onFailure(context));
    }

    private AsyncFunction<AuthStatus, AuthStatus, AuthenticationException> processResult(final MessageContext context,
            final Subject clientSubject) {
        return new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
            @Override
            public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                if (isSendFailure(authStatus)) {
                    logger.debug("Authentication has failed.");
                    AuthenticationException exception = new AuthenticationFailedException();
                    return Promises.newExceptionPromise(exception);
                } else if (isSuccess(authStatus)) {
                    String principalName = null;
                    for (Principal principal : clientSubject.getPrincipals()) {
                        if (principal.getName() != null) {
                            principalName = principal.getName();
                            break;
                        }
                    }
                    Map<String, Object> contextMap =
                            getMap(context.getRequestContextMap(), AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT);
                    logger.debug("Setting principal name, {}, and {} context values on to the request.",
                            principalName, contextMap.size());
                    Map<String, Object> requestAttributes = context.asContext(HttpContext.class).getAttributes();
                    requestAttributes.put(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL, principalName);
                    requestAttributes.put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, contextMap);
                }
                return Promises.newResultPromise(authStatus);
            }
        };
    }

    private AsyncFunction<AuthStatus, Response, NeverThrowsException> onValidateRequestSuccess(
            final MessageContext context, final Handler next) {
        return new AsyncFunction<AuthStatus, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(AuthStatus authStatus) {
                if (isSuccess(authStatus)) {
                    AuditTrail auditTrail = context.getAuditTrail();
                    context.asContext(HttpContext.class).getAttributes()
                            .put(AuthenticationFramework.ATTRIBUTE_REQUEST_ID, auditTrail.getRequestId());
                    return grantAccess(context, next);
                }
                return Promises.newResultPromise(context.getResponse());
            }
        };
    }

    private Promise<Response, NeverThrowsException> grantAccess(final MessageContext context, Handler next) {
        return next.handle(context, context.getRequest())
                .thenAsync(new AsyncFunction<Response, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(Response response) {
                        context.setResponse(response);
                        return secureResponse(context);
                    }
                });
    }

    private Promise<Response, NeverThrowsException> secureResponse(final MessageContext context) {
        return authContext.secureResponse(context, serviceSubject)
                .thenAsync(onSecureResponseSuccess(context), onFailure(context));
    }

    private AsyncFunction<AuthStatus, Response, NeverThrowsException> onSecureResponseSuccess(
            final MessageContext context) {
        return new AsyncFunction<AuthStatus, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(AuthStatus authStatus) {
                if (isSendFailure(authStatus)) {
                    context.getResponse().setStatus(Status.INTERNAL_SERVER_ERROR);
                }
                return Promises.newResultPromise(context.getResponse());
            }
        };
    }

    private AsyncFunction<AuthenticationException, Response, NeverThrowsException> onFailure(
            final MessageContext context) {
        return new AsyncFunction<AuthenticationException, Response, NeverThrowsException>() {
            @Override
            public Promise<Response, NeverThrowsException> apply(AuthenticationException error) {
                responseHandler.handle(context, error);
                if (error instanceof AuthenticationFailedException) {
                    //Force 401 HTTP status
                    context.getResponse().setStatus(Status.UNAUTHORIZED);
                }
                logger.debug(error.getMessage(), error);
                return Promises.newResultPromise(context.getResponse());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> containingMap, String key) {
        Map<String, Object> map = (Map<String, Object>) containingMap.get(key);
        if (map == null) {
            map = new HashMap<String, Object>();
            containingMap.put(key, map);
        }
        return map;
    }

    @Override
    public String toString() {
        return "AuthContext: " + authContext.toString() + ", Response Handlers: " + responseHandler.toString();
    }
}
