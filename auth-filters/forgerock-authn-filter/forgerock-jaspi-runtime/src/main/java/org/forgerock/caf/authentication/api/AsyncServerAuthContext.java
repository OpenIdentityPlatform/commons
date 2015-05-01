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

package org.forgerock.caf.authentication.api;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;

import org.forgerock.util.promise.Promise;

/**
 * <p>An asynchronous interface counterpart for the
 * {@link javax.security.auth.message.config.ServerAuthContext}. Responsible for passing request
 * and response messages to its configured modules based on the logic this authentication context
 * defines.</p>
 *
 * <p>Module instance MUST be constructed and initialized before being passed to the authentication
 * context instance.</p>
 *
 * <p>Implementations of this interface must be thread-safe as instances may be used concurrently
 * by multiple requests. If the authentication context needs to store any state for a single
 * request it should store the state in the {@link MessageContext} so that it can be retrieved
 * later for the in the {@link #secureResponse(MessageContext, javax.security.auth.Subject)}
 * method.</p>
 *
 * @see javax.security.auth.message.config.ServerAuthContext
 * @see MessageContext
 * @see Subject
 *
 * @since 2.0.0
 */
public interface AsyncServerAuthContext {

    /**
     * Validates the incoming request message.
     *
     * @param context The message context for this request.
     * @param clientSubject A {@code Subject} that represents the subject of this request.
     * @param serviceSubject A {@code Subject} that represents the subject for the server or
     *                       {@code null}. It may be used to secure the message response.
     * @return <p>A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value.</p>
     *
     * <p>A successfully completed {@code Promise} will contain an {@code AuthStatus} representing
     * the completion status of the message processing. See
     * {@link javax.security.auth.message.config.ServerAuthContext#validateRequest(
     * javax.security.auth.message.MessageInfo, Subject, Subject)} for the allowed
     * {@code AuthStatus} values.</p>
     *
     * <p>A failed completed {@code Promise} will contain an {@code AuthenticationException} when
     * the message processing failed without establishing a failure response message in the
     * {@code MessageContext}.</p>
     *
     * @see AuthStatus
     * @see javax.security.auth.message.config.ServerAuthContext#validateRequest(
     * javax.security.auth.message.MessageInfo, Subject, Subject)
     */
    Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context, Subject clientSubject,
            Subject serviceSubject);

    /**
     * Secures the outgoing response message.
     *
     * @param context The message context for this request.
     * @param serviceSubject A {@code Subject} that represents the subject for the server or
     *                       {@code null}. It may be used to secure the message response.
     * @return <p>A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value.</p>
     *
     * <p>A successfully completed {@code Promise} will contain an {@code AuthStatus} representing
     * the completion status of the processing. See
     * {@link javax.security.auth.message.config.ServerAuthContext#secureResponse(
     * javax.security.auth.message.MessageInfo, Subject)} for the allowed
     * {@code AuthStatus} values. Note {@link AuthStatus#SEND_CONTINUE} is not supported by this
     * interface</p>
     *
     * <p>A failed completed {@code Promise} will contain an {@code AuthenticationException} when
     * the message processing failed without establishing a failure response message in the
     * {@code MessageContext}.</p>
     *
     * @see AuthStatus
     * @see javax.security.auth.message.config.ServerAuthContext#secureResponse(
     * javax.security.auth.message.MessageInfo, Subject)
     */
    Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context, Subject serviceSubject);

    /**
     * Removes any method specific principals and credentials from the client subject.
     *
     * @param context The message context for this request.
     * @param clientSubject A {@code Subject} that represents the subject of this request.
     * @return A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value. A successfully completed {@code Promise} will
     * contain no value and a failed completed {@code Promise} will contain an
     * {@code AuthenticationException} if an error occurs during the {@code Subject} processing.
     *
     * @see javax.security.auth.message.config.ServerAuthContext#cleanSubject(
     * javax.security.auth.message.MessageInfo, Subject)
     */
    Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject);

    /**
     * A short but useful description of this authentication context. Description should include
     * at least the IDs of the module this context manages.
     */
    @Override
    String toString();
}
