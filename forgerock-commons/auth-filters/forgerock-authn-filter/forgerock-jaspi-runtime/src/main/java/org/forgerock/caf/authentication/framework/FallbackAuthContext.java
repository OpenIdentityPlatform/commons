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
import static org.forgerock.caf.authentication.framework.AuthModules.*;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.isSendFailure;
import static org.forgerock.caf.authentication.framework.AuthStatusUtils.isSuccess;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthContextWithState;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.AuthenticationState;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.util.Reject;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;

/**
 * <p>An {@link AsyncServerAuthContext} which manages a {@code List} of
 * {@link AsyncServerAuthModule}s that are in a desired order of preference for authenticating
 * incoming request messages.</p>
 *
 * <p>Order matters as one and only one auth module can successfully authenticate the request
 * message. Each {@code AsyncServerAuthModule} is called in order to authenticate the request
 * message and processing stops after the first auth module that successfully authenticates the
 * request message, or returns a failed promise with an {@link AuthenticationException}.</p>
 *
 * <p>Order does not matter when securing the as only the auth module that successfully
 * authenticated the incoming request message will get the opportunity to secure the response
 * message.</p>
 *
 * @since 2.0.0
 */
public final class FallbackAuthContext implements AsyncServerAuthContext, AuthContextWithState {

    private final Logger logger;
    private final List<AsyncServerAuthModule> authModules;

    /**
     * Creates a new {@code FallbackAuthContext} managing the provided
     * {@code AsyncServerAuthModule}s.
     *
     * @param logger The {@link Logger} instance.
     * @param authModules The {@code List} of {@code AsyncServerAuthModule}s.
     */
    public FallbackAuthContext(Logger logger, List<AsyncServerAuthModule> authModules) {
        Reject.ifNull(logger, authModules);
        this.logger = logger;
        this.authModules = withValidation(withAuditing(withLogging(logger, authModules)));
    }

    /**
     * <p>Authenticates the incoming request message by calling each {@code AsyncServerAuthModule}
     * in order until an auth module returns an {@code AuthStatus} value other than
     * {@code SEND_FAILURE}, or returns an {@code AuthenticationException} or the end of the
     * module list is reached.</p>
     *
     * <p>If the end of the module list is reached then an {@code AuthStatus} value of
     * {@code SEND_FAILURE} is returned.</p>
     *
     * @param context {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context, Subject clientSubject,
            Subject serviceSubject) {
        FallbackAuthContextState state = context.getState(this);
        return new FallbackChain(logger, authModules, 0, state).validateRequest(context, clientSubject, serviceSubject);
    }

    private static final class FallbackChain {
        private final Logger logger;
        private final List<AsyncServerAuthModule> authModules;
        private final int position;
        private final FallbackAuthContextState state;

        private FallbackChain(Logger logger, List<AsyncServerAuthModule> authModules, int position,
                FallbackAuthContextState state) {
            this.logger = logger;
            this.authModules = authModules;
            this.position = position;
            this.state = state;
        }

        private Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
                final Subject clientSubject, final Subject serviceSubject) {
            if (position < authModules.size()) {
                final AsyncServerAuthModule authModule = authModules.get(position);
                return authModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                        .thenOnResult(new ResultHandler<AuthStatus>() {
                            @Override
                            public void handleResult(AuthStatus authStatus) {
                                if (isSuccess(authStatus)) {
                                    /*
                                     * Save the index of the authenticating module so that it can
                                     * be retrieved when securing the response
                                     */
                                    logger.trace("Adding authenticating auth module to private context map, {}",
                                            authModule.getClass().getSimpleName());
                                    state.setAuthenticatedAuthModuleIndex(position);
                                }
                            }
                        })
                        .thenAsync(new AsyncFunction<AuthStatus, AuthStatus, AuthenticationException>() {
                            @Override
                            public Promise<AuthStatus, AuthenticationException> apply(AuthStatus authStatus) {
                                if (isSendFailure(authStatus)) {
                                    return next().validateRequest(messageInfo, clientSubject, serviceSubject);
                                } else {
                                    return Promises.newResultPromise(authStatus);
                                }
                            }
                        });
            } else {
                return Promises.newResultPromise(AuthStatus.SEND_FAILURE);
            }
        }

        private FallbackChain next() {
            return new FallbackChain(logger, authModules, position + 1, state);
        }
    }

    /**
     * <p>Secures the response message using the same {@code AsyncServerAuthModule} that
     * authenticated the incoming request message.</p>
     *
     * <p>If no {@code AsyncServerAuthModule} authenticated the incoming request message, then this
     * method should not have been called and a failed promise will be return with an
     * {@code AuthenticationException}.</p>
     *
     * @param context {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context, Subject serviceSubject) {
        FallbackAuthContextState state = context.getState(this);

        if (state.getAuthenticatedAuthModuleIndex() < 0) {
            return Promises.newExceptionPromise(
                    new AuthenticationException("No auth module authenticated the incoming request message. "
                            + "Cannot secure response message."));
        }
        AsyncServerAuthModule authModule = authModules.get(state.getAuthenticatedAuthModuleIndex());
        logger.debug("Using authenticating auth module from private context map, {}, to secure the response",
                authModule.getModuleId());

        return authModule.secureResponse(context, serviceSubject);
    }

    /**
     * Calls each {@code AsyncServerAuthContext} in parallel to clean the client subject and
     * only return a successful promise if all complete successfully otherwise returns the first
     * exception in a failed promise.
     *
     * @param context {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {

        List<Promise<Void, AuthenticationException>> promises = new ArrayList<>();
        for (AsyncServerAuthModule serverAuthModule : authModules) {
            promises.add(serverAuthModule.cleanSubject(context, clientSubject));
        }
        return Promises.when(promises).thenAsync(ON_SUCCESS_RETURN_VOID);
    }

    @Override
    public FallbackAuthContextState createAuthenticationState() {
        return new FallbackAuthContextState();
    }

    private static final class FallbackAuthContextState extends AuthenticationState {

        private static final String AUTHENTICATION_AUTH_MODULE_INDEX = "authenticatedAuthModuleIndex";

        private void setAuthenticatedAuthModuleIndex(int authenticatedAuthModuleIndex) {
            add(AUTHENTICATION_AUTH_MODULE_INDEX, authenticatedAuthModuleIndex);
        }

        private int getAuthenticatedAuthModuleIndex() {
            if (isDefined(AUTHENTICATION_AUTH_MODULE_INDEX)) {
                return get(AUTHENTICATION_AUTH_MODULE_INDEX).asInteger();
            } else {
                return -1;
            }
        }
    }

    @Override
    public String toString() {
        return authModules.toString();
    }
}
