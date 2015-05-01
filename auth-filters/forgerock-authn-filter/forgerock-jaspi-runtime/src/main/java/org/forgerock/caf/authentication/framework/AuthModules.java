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

import static org.forgerock.caf.authentication.framework.AuditTrail.*;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;

/**
 * <p>Utility class for applying success and failure functions to method calls on a
 * {@link AsyncServerAuthModule} to add logging, auditing and validation support.</p>
 *
 * <p>{@link #withValidation(AsyncServerAuthModule)} and {@link #withValidation(List)} wrap the
 * {@code AsyncServerAuthModule}(s) and apply functions to validate the result of the
 * {@code validateRequest} and {@code secureResponse} method calls.
 *
 * Note: wrapping the {@code AsyncServerAuthModule} changes the processing flow as it will throw
 * an {@link AuthenticationException} if the method call returns an invalid {@link AuthStatus}.</p>
 *
 * <p>{@link #withAuditing(AsyncServerAuthModule)} and {@link #withAuditing(List)} wrap the
 * {@code AsyncServerAuthModule}(s) and applies a function to audit the result of the
 * {@code validateRequest} method call.</p>
 *
 * <p>{@link #withSessionAuditing(AsyncServerAuthModule)} wrap the {@code AsyncServerAuthModule}
 * and applies the same function as {@link #withAuditing(AsyncServerAuthModule)} and in addition
 * audits the session id (if set) from the {@code AsyncServerAuthModule}.</p>
 *
 * <p>{@link #withLogging(Logger, AsyncServerAuthModule)} and {@link #withLogging(Logger, List)}
 * wrap the {@code AsyncServerAuthModule}(s) and apply functions to log the result of each method
 * call.</p>
 *
 * <p>As {@code withValidation} changes the processing flow, it must be applied after the other
 * methods, i.e.
 * <pre><code>
 * withValidation(withAuditing(withLogging(logger, authModule)));
 * </code></pre>
 * or
 * <pre><code>
 * withValidation(withSessionAuditing(withLogging(logger, authModule)));
 * </code></pre>
 * </p>
 *
 * @since 2.0.0
 */
final class AuthModules {

    private AuthModules() {
        // Private constructor
    }

    /**
     * <p>Attaches a success function to the result of both the
     * {@link AsyncServerAuthModule#validateRequest(MessageInfoContext, Subject, Subject)} and
     * {@link AsyncServerAuthModule#secureResponse(MessageInfoContext, Subject)} method calls, for
     * the given {@code AsyncServerAuthModule}, which validates that the returned
     * {@link AuthStatus} is valid for the method being called.</p>
     *
     * <p>If the wrapped auth module returns an invalid {@code AuthStatus} from either
     * {@code validateRequest} or {@code secureResponse} then a {@link AuthenticationException}
     * will be returned in a failed {@code Promise}. </p>
     *
     * <p>Invalid {@code AuthStatus} values from {@code validateRequest} are:
     * <ul>
     *     <li>{@link AuthStatus#FAILURE}</li>
     *     <li>{@code null}</li>
     * </ul>
     * </p>
     *
     * <p>Invalid {@code AuthStatus} values from {@code secureResponse} are:
     * <ul>
     *     <li>{@link AuthStatus#SUCCESS}</li>
     *     <li>{@link AuthStatus#FAILURE}</li>
     *     <li>{@code null}</li>
     * </ul>
     * </p>
     *
     * @param authModule The {@code AsyncServerAuthModule} to be be wrapped.
     * @return The provided {@code AsyncServerAuthModule} with validation functions attached.
     */
    static AsyncServerAuthModule withValidation(AsyncServerAuthModule authModule) {
        if (authModule == null) {
            return null;
        }
        return new ValidatingAuthModule(authModule);
    }

    /**
     * <p>Attaches a success function to the result of both the
     * {@link AsyncServerAuthModule#validateRequest(MessageInfoContext, Subject, Subject)} and
     * {@link AsyncServerAuthModule#secureResponse(MessageInfoContext, Subject)} method calls, for
     * the given {@code List} of {@code AsyncServerAuthModule}s, which validates that the returned
     * {@link AuthStatus} is valid for the method being called.</p>
     *
     * @param authModules The {@code List} of {@code AsyncServerAuthModule}s to be be wrapped.
     * @return A {@code List} of the provided {@code AsyncServerAuthModule}s with validation
     * functions attached.
     * @see {@link #withValidation(AsyncServerAuthModule)}
     */
    static List<AsyncServerAuthModule> withValidation(List<AsyncServerAuthModule> authModules) {
        List<AsyncServerAuthModule> modules = new ArrayList<AsyncServerAuthModule>();
        if (authModules != null) {
            for (AsyncServerAuthModule authModule : authModules) {
                modules.add(withValidation(authModule));
            }
        }
        return modules;
    }

    /**
     * <p>Attaches a success and failure functions to the result of the
     * {@link AsyncServerAuthModule#validateRequest(MessageInfoContext, Subject, Subject)} method
     * call, for the given {@code AsyncServerAuthModule}, which audits the outcome of the call.</p>
     *
     * <p>Audits:
     * <ul>
     *     <li>successful {@code AuthStatus} values ({@link AuthStatus#SUCCESS},
     *     {@link AuthStatus#SEND_SUCCESS})</li>
     *     <li>failure {@code AuthStatus} value ({@link AuthStatus#SEND_FAILURE})</li>
     *     <li>invalid {@code AuthStatus} values ({@link AuthStatus#FAILURE}, {@code null})</li>
     *     <li>any {@code AuthenticationException}</li>
     * </ul></p>
     *
     * @param authModule The {@code AsyncServerAuthModule} to be be wrapped.
     * @return The provided {@code AsyncServerAuthModule} with auditing functions attached.
     */
    static AsyncServerAuthModule withAuditing(AsyncServerAuthModule authModule) {
        if (authModule == null) {
            return null;
        }
        return new AuditingAuthModule(authModule);
    }

    /**
     * <p>Attaches a success and failure functions to the result of the
     * {@link AsyncServerAuthModule#validateRequest(MessageInfoContext, Subject, Subject)} method
     * call, for the given {@code List} of {@code AsyncServerAuthModule}s, which audits the
     * outcome of each the call.</p>
     *
     * @param authModules The {@code List} of {@code AsyncServerAuthModule}s to be be wrapped.
     * @return A {@code List} of the provided {@code AsyncServerAuthModule}s with auditing
     * functions attached.
     * @see {@link #withAuditing(AsyncServerAuthModule)}
     */
    static List<AsyncServerAuthModule> withAuditing(List<AsyncServerAuthModule> authModules) {
        List<AsyncServerAuthModule> modules = new ArrayList<AsyncServerAuthModule>();
        if (authModules != null) {
            for (AsyncServerAuthModule authModule : authModules) {
                modules.add(withAuditing(authModule));
            }
        }
        return modules;
    }

    /**
     * <p>Attaches a success and failure functions to the result of both the
     * {@link AsyncServerAuthModule#validateRequest(MessageInfoContext, Subject, Subject)} and
     * {@link AsyncServerAuthModule#secureResponse(MessageInfoContext, Subject)} method
     * calls, for the given "session" {@code AsyncServerAuthModule}, which audits the outcome of
     * the call.</p>
     *
     * <p>Applies the same function as the {@link #withAuditing(AsyncServerAuthModule)} method with
     * the addition of settings the session id of the request from the request context map.</p>
     *
     * @param authModule The {@code AsyncServerAuthModule} to be be wrapped.
     * @return The provided {@code AsyncServerAuthModule} with auditing, including session
     * auditing, functions attached.
     * @see {@link #withAuditing(AsyncServerAuthModule)}
     */
    static AsyncServerAuthModule withSessionAuditing(AsyncServerAuthModule authModule) {
        if (authModule == null) {
            return null;
        }
        return new SessionAuditingAuthModule(authModule);
    }

    /**
     * <p>Attaches a success and failure functions to the result of each method call,
     * for the given {@code AsyncServerAuthModule}, which logs the outcome of the call.</p>
     *
     * <p>
     * {@link AsyncServerAuthModule#initialize(MessagePolicy, MessagePolicy, CallbackHandler, Map)}
     * logs:
     * <ul>
     *     <li>successful initialization at <strong>debug</strong> level</li>
     *     <li>failed initialization at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * <p>
     * {@link AsyncServerAuthModule#validateRequest(MessageInfoContext, Subject, Subject)} logs:
     * <ul>
     *     <li>valid {@code AuthStatus} values at <strong>debug</strong> level</li>
     *     <li>invalid {@code AuthStatus} values at <strong>error</strong> level</li>
     *     <li>Any {@code AuthenticationException} at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * <p>
     * {@link AsyncServerAuthModule#secureResponse(MessageInfoContext, Subject)} logs:
     * <ul>
     *     <li>valid {@code AuthStatus} values at <strong>debug</strong> level</li>
     *     <li>invalid {@code AuthStatus} values at <strong>error</strong> level</li>
     *     <li>Any {@code AuthenticationException} at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * <p>
     * {@link AsyncServerAuthModule#cleanSubject(MessageInfoContext, Subject)} logs:
     * <ul>
     *     <li>successfully cleaning client subject at <strong>debug</strong> level</li>
     *     <li>failed cleaning of client subject at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * @param logger The {@code Logger} instance to log to.
     * @param authModule The {@code AsyncServerAuthModule} to be be wrapped.
     * @return The provided {@code AsyncServerAuthModule} with logging functions attached.
     */
    static AsyncServerAuthModule withLogging(Logger logger, AsyncServerAuthModule authModule) {
        if (authModule == null) {
            return null;
        }
        return new LoggingAuthModule(logger, authModule);
    }

    /**
     * <p>Attaches a success and failure functions to the result of each method call,
     * for the given {@code List} of {@code AsyncServerAuthModule}s, which logs the outcome of the
     * each call.</p>
     *
     * @param logger The {@code Logger} instance to log to.
     * @param authModules The {@code List} of {@code AsyncServerAuthModule}s to be be wrapped.
     * @return A {@code List} of the provided {@code AsyncServerAuthModule}s with logging
     * functions attached.
     * @see {@link #withLogging(Logger, AsyncServerAuthModule)}
     */
    static List<AsyncServerAuthModule> withLogging(Logger logger, List<AsyncServerAuthModule> authModules) {
        List<AsyncServerAuthModule> modules = new ArrayList<AsyncServerAuthModule>();
        if (authModules != null) {
            for (AsyncServerAuthModule authModule : authModules) {
                modules.add(withLogging(logger, authModule));
            }
        }
        return modules;
    }

    private static final class ValidatingAuthModule extends WrappedAuthModule {

        private ValidatingAuthModule(AsyncServerAuthModule authModule) {
            super(authModule);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
                Subject clientSubject, Subject serviceSubject) {
            return super.validateRequest(messageInfo, clientSubject, serviceSubject)
                    .thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
                        @Override
                        public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                            if (isNull(authStatus) || isFailure(authStatus)) {
                                return Promises.newExceptionPromise(
                                        new AuthenticationException("Invalid AuthStatus returned from validateRequest, "
                                                + asString(authStatus)));
                            }
                            return Promises.newResultPromise(authStatus);
                        }
                    });
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
                Subject serviceSubject) {
            return super.secureResponse(messageInfo, serviceSubject)
                    .thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
                        @Override
                        public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                            if (isSuccess(authStatus) || isFailure(authStatus) || isNull(authStatus)) {
                                return Promises.newExceptionPromise(
                                        new AuthenticationException("Invalid AuthStatus returned from secureResponse, "
                                                + asString(authStatus)));
                            }
                            return Promises.newResultPromise(authStatus);
                        }
                    });
        }
    }

    // @Checkstyle:off
    // Checkstyle for some reason is enforcing that this class must be final,
    // which cannot be the case as SessionAuditingAuthModule inherits from it!
    private static class AuditingAuthModule extends WrappedAuthModule {
    // @Checkstyle:on

        private AuditingAuthModule(AsyncServerAuthModule authModule) {
            super(authModule);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
                Subject clientSubject, Subject serviceSubject) {
            final AuditTrail auditTrail = (AuditTrail) messageInfo.getRequestContextMap().get(AUDIT_TRAIL_KEY);
            final Map<String, Object> moduleAuditInfo = getMap(messageInfo.getRequestContextMap(), AUDIT_INFO_KEY);
            final String moduleId = getModuleId();
            return super.validateRequest(messageInfo, clientSubject, serviceSubject)
                    .thenAlways(new Runnable() {
                        @Override
                        public void run() {
                            String principal = (String) messageInfo.getRequestContextMap()
                                    .get(AuditTrail.AUDIT_PRINCIPAL_KEY);
                            if (principal != null && !principal.isEmpty()) {
                                moduleAuditInfo.put(AuditTrail.AUDIT_PRINCIPAL_KEY, principal);
                            }
                        }
                    })
                    .thenOnResult(new ResultHandler<AuthStatus>() {
                        @Override
                        public void handleResult(AuthStatus authStatus) {
                            if (isSuccess(authStatus) || isSendSuccess(authStatus)) {
                                auditTrail.auditSuccess(moduleId, moduleAuditInfo);
                            } else if (isSendFailure(authStatus)) {
                                Map<String, Object> failureReason = removeMap(messageInfo, AUDIT_FAILURE_REASON_KEY);
                                if (failureReason == null) {
                                    failureReason = Collections.emptyMap();
                                }
                                auditTrail.auditFailure(moduleId, failureReason, moduleAuditInfo);
                            } else if (isFailure(authStatus) || isNull(authStatus)) {
                                String message = "Invalid AuthStatus returned from validateRequest, "
                                        + asString(authStatus);
                                Map<String, Object> failureReason =
                                        Collections.<String, Object>singletonMap("message", message);
                                auditTrail.auditFailure(moduleId, failureReason, moduleAuditInfo);
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            Map<String, Object> failureReason = removeMap(messageInfo, AUDIT_FAILURE_REASON_KEY);
                            if (failureReason == null) {
                                failureReason = new HashMap<String, Object>();
                            }
                            failureReason.put("exception", error.getMessage());
                            failureReason = Collections.<String, Object>singletonMap("exception", error.getMessage());
                            auditTrail.auditFailure(moduleId, failureReason, moduleAuditInfo);
                        }
                    });
        }
    }

    private static final class SessionAuditingAuthModule extends AuditingAuthModule {

        private SessionAuditingAuthModule(AsyncServerAuthModule authModule) {
            super(authModule);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
                Subject clientSubject, Subject serviceSubject) {
            return super.validateRequest(messageInfo, clientSubject, serviceSubject)
                    .thenAlways(new Runnable() {
                        @Override
                        public void run() {
                            auditSessionId(messageInfo);
                        }
                    });
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(final MessageInfoContext messageInfo,
                Subject serviceSubject) {
            return super.secureResponse(messageInfo, serviceSubject)
                    .thenAlways(new Runnable() {
                        @Override
                        public void run() {
                            auditSessionId(messageInfo);
                        }
                    });
        }

        private void auditSessionId(MessageInfoContext messageInfo) {
            AuditTrail auditTrail = (AuditTrail) messageInfo.getRequestContextMap().get(AUDIT_TRAIL_KEY);
            messageInfo.getRequestContextMap().remove(AUDIT_INFO_KEY);
            messageInfo.getRequestContextMap().remove(AUDIT_FAILURE_REASON_KEY);
            String sessionId = (String) messageInfo.getRequestContextMap().remove(AUDIT_SESSION_ID_KEY);
            auditTrail.setSessionId(sessionId);
        }
    }

    private static final class LoggingAuthModule extends WrappedAuthModule {

        private final Logger logger;

        private LoggingAuthModule(Logger logger, AsyncServerAuthModule authModule) {
            super(authModule);
            this.logger = logger;
        }

        @Override
        public Promise<Void, AuthenticationException> initialize(final MessagePolicy requestPolicy,
                final MessagePolicy responsePolicy, CallbackHandler handler, final Map<String, Object> options) {
            return super.initialize(requestPolicy, responsePolicy, handler, options)
                    .thenOnResult(new ResultHandler<Void>() {
                        @Override
                        public void handleResult(Void result) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("{} was successfully initialized. \nrequest MessagePolicy: {}, "
                                                + "\nresponse MessagePolicy: {}, \noptions: {}", getModuleId(),
                                        requestPolicy, responsePolicy, options);
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            if (logger.isErrorEnabled()) {
                                logger.error("{} failed to initialize. \nrequest MessagePolicy: {}, "
                                                + "\nresponse MessagePolicy: {}, \noptions: {}", getModuleId(),
                                        requestPolicy, responsePolicy, options, error);
                            }
                        }
                    });
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
                Subject clientSubject, Subject serviceSubject) {
            final String moduleId = getModuleId();
            return super.validateRequest(messageInfo, clientSubject, serviceSubject)
                    .thenOnResult(new ResultHandler<AuthStatus>() {
                        @Override
                        public void handleResult(AuthStatus authStatus) {
                            if (isSuccess(authStatus)) {
                                logger.debug("{} has successfully authenticated the client", moduleId);
                            } else if (isSendSuccess(authStatus)) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("{} may have completely/partially/not authenticated the client and "
                                            + "has a response to return to the client", moduleId);
                                }
                            } else if (isSendFailure(authStatus)) {
                                logger.debug("{} has failed to authenticate the client", moduleId);
                            } else if (isSendContinue(authStatus)) {
                                logger.debug("{} has not completed authenticating the client", moduleId);
                            } else {
                                String message = "{} has returned an invalid AuthStatus from validateRequest, {}";
                                logger.error(message, moduleId, asString(authStatus));
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            logger.error("{} has thrown an error whilst attempting to authenticate the client",
                                    moduleId, error);
                        }
                    });
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
                Subject serviceSubject) {
            final String moduleId = getModuleId();
            return super.secureResponse(messageInfo, serviceSubject)
                    .thenOnResult(new ResultHandler<AuthStatus>() {
                        @Override
                        public void handleResult(AuthStatus authStatus) {
                            if (isSendSuccess(authStatus)) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("{} may have completely/partially/not secured the response and "
                                            + "has a response to return to the client", moduleId);
                                }
                            } else if (isSendFailure(authStatus)) {
                                logger.debug("{} has failed to secure the response", moduleId);
                            } else if (isSendContinue(authStatus)) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("{} has not completed securing the response and has a response to "
                                            + "return to the client", moduleId);
                                }
                            } else {
                                String message = "{} has returned an invalid AuthStatus from validateRequest, {}";
                                logger.error(message, moduleId, asString(authStatus));
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            logger.error("{} has thrown an error whilst attempting to secure the response",
                                    moduleId, error);
                        }
                    });
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo,
                Subject clientSubject) {
            return super.cleanSubject(messageInfo, clientSubject)
                    .thenOnResult(new ResultHandler<Void>() {
                        @Override
                        public void handleResult(Void result) {
                            logger.debug("{} successfully cleaned client subject", getModuleId());
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            logger.error("{} failed clean client subject", getModuleId(), error);
                        }
                    });
        }
    }

    private static abstract class WrappedAuthModule implements AsyncServerAuthModule {

        private final AsyncServerAuthModule authModule;

        private WrappedAuthModule(AsyncServerAuthModule authModule) {
            this.authModule = authModule;
        }

        @Override
        public String getModuleId() {
            return authModule.getModuleId();
        }

        @Override
        public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy,
                MessagePolicy responsePolicy, CallbackHandler handler, Map<String, Object> options) {
            return authModule.initialize(requestPolicy, responsePolicy, handler, options);
        }

        @Override
        public Collection<Class<?>> getSupportedMessageTypes() {
            return authModule.getSupportedMessageTypes();
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
                Subject clientSubject, Subject serviceSubject) {
            return authModule.validateRequest(messageInfo, clientSubject, serviceSubject);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
                Subject serviceSubject) {
            return authModule.secureResponse(messageInfo, serviceSubject);
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo,
                Subject clientSubject) {
            return authModule.cleanSubject(messageInfo, clientSubject);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> containingMap, String key) {
        Map<String, Object> map = (Map<String, Object>) containingMap.get(key);
        if (map == null) {
            map = new HashMap<String, Object>();
            containingMap.put(key, map);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> removeMap(MessageInfoContext context, String mapKey) {
        return (Map<String, Object>) context.getRequestContextMap().remove(mapKey);
    }
}
