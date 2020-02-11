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

import static org.forgerock.caf.authentication.framework.AuthModules.*;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;

/**
 * An {@link AsyncServerAuthContext} which manages a session {@link AsyncServerAuthModule} that
 * performs session related validation on the request message and secures the response message with
 * session related information.
 *
 * @since 2.0.0
 */
final class SessionAuthContext implements AsyncServerAuthContext {

    private final Logger logger;
    private final AsyncServerAuthModule sessionAuthModule;

    /**
     * Creates a new {@code SessionAuthContext} managing the provided {@code AsyncServerAuthModule}.
     *
     * @param logger The {@link Logger} instance.
     * @param sessionAuthModule The session {@code AsyncServerAuthModule}.
     */
    SessionAuthContext(Logger logger, AsyncServerAuthModule sessionAuthModule) {
        Reject.ifNull(logger);
        this.logger = logger;
        this.sessionAuthModule = withValidation(withSessionAuditing(withLogging(logger, sessionAuthModule)));
    }

    /**
     * <p>Calls {@link AsyncServerAuthModule#validateRequest(
     * org.forgerock.caf.authentication.api.MessageInfoContext, Subject, Subject)} directly.</p>
     *
     * <p>If the configured {@code AsyncServerAuthModule} is {@code null} return a successful
     * promise with the value {@link AuthStatus#SEND_FAILURE}.</p>
     *
     * @param context {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context, Subject clientSubject,
            Subject serviceSubject) {
        if (sessionAuthModule == null) {
            return Promises.newResultPromise(AuthStatus.SEND_FAILURE);
        } else {
            return sessionAuthModule.validateRequest(context, clientSubject, serviceSubject);
        }
    }

    /**
     * <p>Calls {@link AsyncServerAuthModule#secureResponse(
     * org.forgerock.caf.authentication.api.MessageInfoContext, Subject)} directly.</p>
     *
     * <p>If the configured {@code AsyncServerAuthModule} is {@code null} return a successful
     * promise with the value {@link AuthStatus#SEND_SUCCESS}.</p>
     *
     * @param context {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context, Subject serviceSubject) {
        if (sessionAuthModule == null) {
            return Promises.newResultPromise(AuthStatus.SEND_SUCCESS);
        }
        return sessionAuthModule.secureResponse(context, serviceSubject);
    }

    /**
     * <p>Calls {@link AsyncServerAuthModule#cleanSubject(
     * org.forgerock.caf.authentication.api.MessageInfoContext, Subject)} directly.</p>
     *
     * <p>If the configured {@code AsyncServerAuthModule} is {@code null} return a successful
     * promise with the value {@code null}.</p>
     *
     * @param context {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
        if (sessionAuthModule == null) {
            return Promises.newResultPromise(null);
        }
        return sessionAuthModule.cleanSubject(context, clientSubject);
    }

    @Override
    public String toString() {
        if (sessionAuthModule == null) {
            return "N/A";
        } else {
            return sessionAuthModule.toString();
        }
    }
}
