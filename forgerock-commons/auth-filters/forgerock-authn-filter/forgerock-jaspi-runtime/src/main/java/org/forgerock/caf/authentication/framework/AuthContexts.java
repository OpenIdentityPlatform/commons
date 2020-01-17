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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.forgerock.caf.authentication.framework.AuthStatusUtils.*;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import java.security.Principal;
import java.util.List;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.util.Reject;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;

/**
 * <p>Utility class for applying success and failure functions to method calls on a
 * {@link AsyncServerAuthContext} to add logging, auditing and validation support.</p>
 *
 * <p>{@link #withValidation(AsyncServerAuthContext)} wraps the {@code AsyncServerAuthModule}(s)
 * and apply functions to validate the result of the {@code validateRequest} and
 * {@code secureResponse} method calls.
 *
 * Note: wrapping the {@code AsyncServerAuthContext} changes the processing flow as it will throw
 * an {@link AuthenticationException} if the method call returns an invalid {@link AuthStatus}.</p>
 *
 * <p>{@link #withAuditing(AsyncServerAuthContext)} wraps the {@code AsyncServerAuthModule}(s)
 * and applies a function to audit the result of the {@code validateRequest} method call.</p>
 *
 * <p>{@link #withLogging(Logger, AsyncServerAuthContext)} wraps the
 * {@code AsyncServerAuthModule}(s) and apply functions to log the result of each method call.</p>
 *
 * <p>As {@code withValidation} changes the processing flow, it must be applied after the other
 * methods, i.e.
 * <pre><code>
 * withValidation(withAuditing(withLogging(logger, authContext)));
 * </code></pre>
 * or
 * <pre><code>
 * withValidation(withSessionAuditing(withLogging(logger, authContext)));
 * </code></pre>
 * </p>
 *
 * @since 2.0.0
 */
final class AuthContexts {

    private AuthContexts() {
        // Private constructor
    }

    /**
     * A {@code AsyncFunction} which takes a list of {@code Void}s and returns a single
     * {@code Void}.
     */
    static final AsyncFunction<List<Void>, Void, AuthenticationException> ON_SUCCESS_RETURN_VOID =
        new AsyncFunction<List<Void>, Void, AuthenticationException>() {
            @Override
            public Promise<Void, AuthenticationException> apply(List<Void> voids) {
                return Promises.newResultPromise(null);
            }
        };

    /**
     * <p>Attaches a success function to the result of both the
     * {@link AsyncServerAuthContext#validateRequest(MessageContext, Subject, Subject)} and
     * {@link AsyncServerAuthContext#secureResponse(MessageContext, Subject)} method calls, for
     * the given {@code AsyncServerAuthContext}, which validates that the returned
     * {@link AuthStatus} is valid for the method being called.</p>
     *
     * <p>If the wrapped auth context returns an invalid {@code AuthStatus} from either
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
     * @param authContext The {@code AsyncServerAuthContext} to be be wrapped.
     * @return The provided {@code AsyncServerAuthContext} with validation functions attached.
     */
    static AsyncServerAuthContext withValidation(AsyncServerAuthContext authContext) {
        Reject.ifNull(authContext);
        return new ValidatingAuthContext(authContext);
    }

    /**
     * <p>Attaches a success and failure functions to the result of the
     * {@link AsyncServerAuthContext#validateRequest(MessageContext, Subject, Subject)} method
     * call, for the given {@code AsyncServerAuthContext}, which audits the outcome of the call.
     * </p>
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
     * @param authContext The {@code AsyncServerAuthContext} to be be wrapped.
     * @return The provided {@code AsyncServerAuthContext} with auditing functions attached.
     */
    static AsyncServerAuthContext withAuditing(AsyncServerAuthContext authContext) {
        Reject.ifNull(authContext);
        return new AuditingAuthContext(authContext);
    }

    /**
     * <p>Attaches a success and failure functions to the result of each method call,
     * for the given {@code AsyncServerAuthContext}, which logs the outcome of the call.</p>
     *
     * <p>
     * {@link AsyncServerAuthContext#validateRequest(MessageContext, Subject, Subject)} logs:
     * <ul>
     *     <li>valid {@code AuthStatus} values at <strong>debug</strong> level</li>
     *     <li>invalid {@code AuthStatus} values at <strong>error</strong> level</li>
     *     <li>Any {@code AuthenticationException} at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * <p>
     * {@link AsyncServerAuthContext#secureResponse(MessageContext, Subject)} logs:
     * <ul>
     *     <li>valid {@code AuthStatus} values at <strong>debug</strong> level</li>
     *     <li>invalid {@code AuthStatus} values at <strong>error</strong> level</li>
     *     <li>Any {@code AuthenticationException} at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * <p>
     * {@link AsyncServerAuthContext#cleanSubject(MessageContext, Subject)} logs:
     * <ul>
     *     <li>successfully cleaning client subject at <strong>debug</strong> level</li>
     *     <li>failed cleaning of client subject at <strong>error</strong> level</li>
     * </ul>
     * </p>
     *
     * @param logger The {@code Logger} instance to log to.
     * @param authContext The {@code AsyncServerAuthContext} to be be wrapped.
     * @return The provided {@code AsyncServerAuthContext} with logging functions attached.
     */
    static AsyncServerAuthContext withLogging(Logger logger, AsyncServerAuthContext authContext) {
        Reject.ifNull(logger, authContext);
        return new LoggingAuthContext(logger, authContext);
    }

    private static final class ValidatingAuthContext extends WrappedAuthContext {

        private ValidatingAuthContext(AsyncServerAuthContext authContext) {
            super(authContext);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
                Subject clientSubject, Subject serviceSubject) {
            return super.validateRequest(context, clientSubject, serviceSubject)
                    .thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
                        @Override
                        public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                            if (isFailure(authStatus) || isNull(authStatus)) {
                                return Promises.newExceptionPromise(
                                        new AuthenticationException("Invalid AuthStatus from validateRequest: "
                                                + asString(authStatus)));
                            }
                            return Promises.newResultPromise(authStatus);
                        }
                    });
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
                Subject serviceSubject) {
            return super.secureResponse(context, serviceSubject)
                    .thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
                        @Override
                        public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                            if (isSuccess(authStatus) || isFailure(authStatus) || isNull(authStatus)) {
                                return Promises.newExceptionPromise(
                                        new AuthenticationException("Invalid AuthStatus from secureResponse: "
                                                + asString(authStatus)));
                            }
                            return Promises.newResultPromise(authStatus);
                        }
                    });
        }
    }

    private static final class AuditingAuthContext extends WrappedAuthContext {

        private AuditingAuthContext(AsyncServerAuthContext authContext) {
            super(authContext);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageContext context,
                final Subject clientSubject, Subject serviceSubject) {
            return super.validateRequest(context, clientSubject, serviceSubject)
                    .thenOnResult(new ResultHandler<AuthStatus>() {
                        @Override
                        public void handleResult(AuthStatus authStatus) {
                            AuditTrail auditTrail = context.getAuditTrail();
                            if (isSuccess(authStatus)) {
                                auditTrail.completeAuditAsSuccessful(getPrincipal(clientSubject));
                            } else if (isSendSuccess(authStatus)) {
                                auditTrail.completeAuditAsSuccessful(getPrincipal(clientSubject));
                            } else if (isSendFailure(authStatus)) {
                                auditTrail.completeAuditAsFailure();
                            } else if (isFailure(authStatus) || isNull(authStatus)) {
                                auditTrail.completeAuditAsFailure();
                            }

                            if (!isSendContinue(authStatus)) {
                                auditTrail.audit();
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            AuditTrail auditTrail = context.getAuditTrail();
                            auditTrail.completeAuditAsFailure(error);
                            auditTrail.audit();
                        }
                    });
        }

        private String getPrincipal(Subject clientSubject) {
            for (Principal principal : clientSubject.getPrincipals()) {
                if (principal.getName() != null) {
                    return principal.getName();
                }
            }

            return null;
        }
    }

    private static final class LoggingAuthContext extends WrappedAuthContext {

        private final Logger logger;

        private LoggingAuthContext(Logger logger, AsyncServerAuthContext authContext) {
            super(authContext);
            this.logger = logger;
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
                Subject clientSubject, Subject serviceSubject) {
            return super.validateRequest(context, clientSubject, serviceSubject)
                    .thenOnResult(new ResultHandler<AuthStatus>() {
                        @Override
                        public void handleResult(AuthStatus authStatus) {
                            if (isSuccess(authStatus)) {
                                logger.debug("Successfully validated request.");
                            } else if (isSendSuccess(authStatus)) {
                                logger.debug("Successfully validated request, with response message");
                            } else if (isSendFailure(authStatus)) {
                                logger.debug("Failed to validate request, included response message.");
                            } else if (isSendContinue(authStatus)) {
                                logger.debug("Has not finished validating request. "
                                        + "Requires more information from client.");
                            } else {
                                logger.error("Invalid AuthStatus, {}", asString(authStatus));
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            logger.error(error.getMessage(), error);
                        }
                    });
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
                Subject serviceSubject) {
            return super.secureResponse(context, serviceSubject)
                    .thenOnResult(new ResultHandler<AuthStatus>() {
                        @Override
                        public void handleResult(AuthStatus authStatus) {

                            if (isSendSuccess(authStatus)) {
                                // nothing to do here just carry on
                                logger.debug("Successfully secured response.");
                            } else if (isSendFailure(authStatus)) {
                                // Send Response to client and exit.
                                logger.debug("Failed to secured response, included response message");
                            } else if (isSendContinue(authStatus)) {
                                // Send Response to client and exit.
                                logger.debug("Has not finished securing response. "
                                        + "Requires more information from client.");
                            } else {
                                logger.error("Invalid AuthStatus, {}", asString(authStatus));
                            }
                        }
                    })
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            logger.error(error.getMessage(), error);
                        }
                    });
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
            return super.cleanSubject(context, clientSubject)
                    .thenOnException(new ExceptionHandler<AuthenticationException>() {
                        @Override
                        public void handleException(AuthenticationException error) {
                            logger.error(error.getMessage(), error);
                        }
                    });
        }
    }

    private static abstract class WrappedAuthContext implements AsyncServerAuthContext {

        private final AsyncServerAuthContext authContext;

        private WrappedAuthContext(AsyncServerAuthContext authContext) {
            this.authContext = authContext;
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
                Subject clientSubject, Subject serviceSubject) {
            return authContext.validateRequest(context, clientSubject, serviceSubject);
        }

        @Override
        public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
                Subject serviceSubject) {
            return authContext.secureResponse(context, serviceSubject);
        }

        @Override
        public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
            return authContext.cleanSubject(context, clientSubject);
        }
    }
}
