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

import static org.forgerock.caf.authentication.framework.AuthContexts.ON_SUCCESS_RETURN_VOID;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.isSendFailure;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.isSuccess;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthContextWithState;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.AuthenticationState;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.util.Reject;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;

/**
 * <p>Aggregates two {@link AsyncServerAuthContext}s together, if the first auth context fails to
 * authenticate the request message the second is called.</p>
 *
 * <p>The intended usage of this class for the first {@code AsyncServerAuthContext} to perform
 * session related validation on the request message and only if that fails then to attempt to use
 * the second {@code AsyncServerAuthContext} to authentication the request message.</>
 *
 * <p>Similarly the second {@code AsyncServerAuthContext} only secures the response if it
 * authenticated the request message and the first {@code AsyncServerAuthContext} always secures
 * the response so it can add any related session information.</p>
 *
 * <p>Both {@code AsyncServerAuthContext}s always get the opportunity clean the client subject.</p>
 *
 * @since 2.0.0
 */
final class AggregateAuthContext implements AsyncServerAuthContext, AuthContextWithState {

    private final Logger logger;
    private final AsyncServerAuthContext sessionModuleContext;
    private final AsyncServerAuthContext authModuleContext;

    /**
     * Creates a new {@code AggregateAuthContext} with the provided {@code AsyncServerAuthContexts}.
     *
     * @param logger The {@link Logger} instance.
     * @param sessionModuleContext The non-{@code null} session {@code AsyncServerAuthContext}.
     * @param authModuleContext The authenticating {@code AsyncServerAuthContext}.
     */
    AggregateAuthContext(Logger logger, AsyncServerAuthContext sessionModuleContext,
            AsyncServerAuthContext authModuleContext) {
        Reject.ifNull(logger, sessionModuleContext, authModuleContext);
        this.logger = logger;
        this.sessionModuleContext = sessionModuleContext;
        this.authModuleContext = authModuleContext;
    }

    /**
     * <p>Calls the session {@code AsyncServerAuthContext} to authenticate the request message and
     * if {@link AuthStatus#SEND_FAILURE} is returned the authentication
     * {@code AsyncServerAuthContext} is called to authenticate the request message.</p>
     *
     * @param context {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageContext context,
            final Subject clientSubject, final Subject serviceSubject) {
        final AggregateAuthContextState state = context.getState(this);
        return sessionModuleContext.validateRequest(context, clientSubject, serviceSubject)
                .thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
                    @Override
                    public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                        if (isSendFailure(authStatus)) {
                            return authModuleContext.validateRequest(context, clientSubject, serviceSubject)
                                    .thenOnResult(new ResultHandler<AuthStatus>() {
                                        @Override
                                        public void handleResult(AuthStatus authStatus) {
                                            // Record the AuthModules AuthStatus to decided later whether to call
                                            // secureResponse on the AuthModule.
                                            if (isSuccess(authStatus)) {
                                                state.setIsAuthenticatedByAuthContext(true);
                                            }
                                        }
                                    });
                        } else {
                            return Promises.newResultPromise(authStatus);
                        }
                    }
                });
    }

    /**
     * If the authentication {@code AsyncServerAuthContext} originally authenticated the
     * incoming request message then it is first called to secure the outgoing response. The
     * session {@code AsyncServerAuthContext} is always called, regardless of if it authenticated
     * the incoming request message, so that is can add any session related information to the
     * response.
     *
     * @param context {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(final MessageContext context,
            final Subject serviceSubject) {
        AggregateAuthContextState state = context.getState(this);
        Promise<AuthStatus, AuthenticationException> promise = Promises.newResultPromise(AuthStatus.SEND_SUCCESS);
        if (state.isAuthenticatedByAuthContext()) {
            promise = authModuleContext.secureResponse(context, serviceSubject);
        }
        return promise.thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
            @Override
            public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                    return sessionModuleContext.secureResponse(context, serviceSubject);
                } else {
                    return Promises.newResultPromise(authStatus);
                }
            }
        });
    }

    /**
     * Calls both the {@code AsyncServerAuthContext}s in parallel to clean the client subject and
     * only return a successful promise if both complete successfully otherwise returns the first
     * exception in a failed promise.
     *
     * @param context {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
        return Promises.when(
                sessionModuleContext.cleanSubject(context, clientSubject),
                authModuleContext.cleanSubject(context, clientSubject)
        ).thenAsync(ON_SUCCESS_RETURN_VOID);
    }

    @Override
    public AggregateAuthContextState createAuthenticationState() {
        return new AggregateAuthContextState();
    }

    private static final class AggregateAuthContextState extends AuthenticationState {

        private static final String IS_AUTHENTICATED_BY_AUTH_CONTEXT = "isAuthenticatedByAuthContext";

        private void setIsAuthenticatedByAuthContext(boolean isAuthenticatedByAuthContext) {
            add(IS_AUTHENTICATED_BY_AUTH_CONTEXT, isAuthenticatedByAuthContext);
        }

        private boolean isAuthenticatedByAuthContext() {
            if (isDefined(IS_AUTHENTICATED_BY_AUTH_CONTEXT)) {
                return get(IS_AUTHENTICATED_BY_AUTH_CONTEXT).asBoolean();
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {
        return "Session Authentication Module: " + sessionModuleContext.toString() + ", Authentication Modules: "
                + authModuleContext.toString();
    }
}
