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
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.util.Collection;
import java.util.Map;

import org.forgerock.util.promise.Promise;

/**
 * <p>An asynchronous interface counterpart for the
 * {@link javax.security.auth.message.module.ServerAuthModule}. Responsible for validating and
 * securing request and response messages.</p>
 *
 * <p>Implementations of this interface must be thread-safe as instances may be used processes
 * concurrent requests. If the module needs to store any state for a single request it should
 * store the state in the {@link MessageContextInfo} so that it can be retrieved later for the in
 * the {@link #secureResponse(MessageContextInfo, javax.security.auth.Subject)} method.</p>
 *
 * @see javax.security.auth.message.module.ServerAuthModule
 * @see javax.security.auth.message.MessageInfo
 * @see Subject
 *
 * @since 2.0.0
 */
public interface AsyncServerAuthModule {

    /**
     * Gets the ID of the module to be used in creating authentication audit logs to uniquely
     * identify the authentication module and its outcome when processing a request message.
     *
     * @return The ID of the module.
     */
    String getModuleId();

    /**>
     * <p>Initialize this module with request and response message policies to enforce, a
     * {@code CallbackHandler}, and any module specific configuration properties.</p>
     *
     * <p>The request policy and the response policy must not both be null.</p>
     *
     * @param requestPolicy The request policy this module must enforce, or {@code null}.
     * @param responsePolicy The response policy this module must enforce, or {@code null}.
     * @param handler {@code CallbackHandler} used to request information.
     * @param options A {@code Map} of module-specific configuration properties.
     * @return A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value. A successfully completed {@code Promise} will
     * contain no value and a failed completed {@code Promise} will contain an
     * {@code AuthenticationException} if module initialization fails, including for the case
     * where the options argument contains elements that are not supported by the module.
     */
    Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler handler, Map<String, Object> options);

    /**
     * Gets the {@code Collection} of {@code Class} objects of the message types supported by the
     * module.
     *
     * @return A {@code Collection} of {@code Class} objects, with at least on element defining the
     * message type(s) supported by the module.
     */
    Collection<Class<?>> getSupportedMessageTypes();

    /**
     * Validates the incoming request message.
     *
     * @param messageInfo The message context info for this request.
     * @param clientSubject A {@code Subject} that represents the subject of this request.
     * @param serviceSubject A {@code Subject} that represents the subject for the server or
     *                       {@code null}. It may be used to secure the message response.
     * @return <p>A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value.</p>
     *
     * <p>A successfully completed {@code Promise} will contain an {@code AuthStatus} representing
     * the completion status of the message processing. See
     * {@link javax.security.auth.message.module.ServerAuthModule#validateRequest(
     * javax.security.auth.message.MessageInfo, Subject, Subject)} for the allowed
     * {@code AuthStatus} values.</p>
     *
     * <p>A failed completed {@code Promise} will contain an {@code AuthenticationException} when
     * the message processing failed without establishing a failure response message in the
     * {@code MessageContextInfo}.</p>
     *
     * @see AuthStatus
     * @see javax.security.auth.message.module.ServerAuthModule#validateRequest(
     * javax.security.auth.message.MessageInfo, Subject, Subject)
     */
    Promise<AuthStatus, AuthenticationException> validateRequest(MessageContextInfo messageInfo, Subject clientSubject,
            Subject serviceSubject);

    /**
     * Secures the outgoing response message.
     *
     * @param messageInfo The message context info for this request.
     * @param serviceSubject A {@code Subject} that represents the subject for the server or
     *                       {@code null}. It may be used to secure the message response.
     * @return <p>A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value.</p>
     *
     * <p>A successfully completed {@code Promise} will contain an {@code AuthStatus} representing
     * the completion status of the processing. See
     * {@link javax.security.auth.message.module.ServerAuthModule#secureResponse(
     * javax.security.auth.message.MessageInfo, Subject)} for the allowed
     * {@code AuthStatus} values. Note {@link AuthStatus#SEND_CONTINUE} is not supported by this
     * interface</p>
     *
     * <p>A failed completed {@code Promise} will contain an {@code AuthenticationException} when
     * the message processing failed without establishing a failure response message in the
     * {@code MessageContextInfo}.</p>
     *
     * @see AuthStatus
     * @see javax.security.auth.message.module.ServerAuthModule#secureResponse(
     * javax.security.auth.message.MessageInfo, Subject)
     */
    Promise<AuthStatus, AuthenticationException> secureResponse(MessageContextInfo messageInfo, Subject serviceSubject);

    /**
     * Removes any method specific principals and credentials from the client subject.
     *
     * @param messageInfo The message context info for this request.
     * @param clientSubject A {@code Subject} that represents the subject of this request.
     * @return A {@code Promise} that will be completed, as some point in the future, with
     * either a successful value or a failure value. A successfully completed {@code Promise} will
     * contain no value and a failed completed {@code Promise} will contain an
     * {@code AuthenticationException} if an error occurs during the {@code Subject} processing.
     *
     * @see javax.security.auth.message.module.ServerAuthModule#cleanSubject(
     * javax.security.auth.message.MessageInfo, Subject)
     */
    Promise<Void, AuthenticationException> cleanSubject(MessageContextInfo messageInfo, Subject clientSubject);

    /**
     * A short but useful description of this authentication context. Description should include
     * at least the ID of this module and optionally configuration details.
     */
    @Override
    String toString();
}
