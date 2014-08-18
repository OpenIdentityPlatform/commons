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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime.context;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.runtime.AuditTrail;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.utils.MessageInfoUtils;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.jaspi.runtime.AuditTrail.*;
import static org.forgerock.jaspi.runtime.AuthStatusUtils.asString;

/**
 * Encapsulates ServerAuthModules that are used to validate service requests received from clients, and to secure any
 * response returned for those requests.
 * <br/>
 * This abstract implementation of the ServerAuthContext interface allows a single Session ServerAuthModule, to
 * provide session capabilities (i.e. on secureResponse, adds a cookie which is validate on each subsequent request to
 * skip re-authenticating each request), and then defers to the concrete implementation to co-ordinate calling
 * any other ServerAuthModules.
 * <br/>
 * This class or concrete implementations of it MUST not store any request specific state in it. This is because
 * many different requests will use this class, possibly concurrently. If the request-specific state needs to be
 * maintained then use the MessageInfo map provided. This is created and used only by this specific request - it will
 * be available for the entire time the request is being processed..
 *
 * @param <T> The type of the ServerAuthModule configured in the runtime.
 * @since 1.3.0
 */
public abstract class JaspiServerAuthContext<T extends ServerAuthModule> implements ServerAuthContext {

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    /**
     * Key to the private context map, shared between ServerAuthContext implementations.
     */
    protected static final String PRIVATE_CONTEXT_MAP_KEY = "_serverAuthContextMap";

    private final MessageInfoUtils messageInfoUtils;
    private final ContextHandler contextHandler;
    private final ServerAuthModule sessionAuthModule;
    private final List<T> authModules;

    /**
     * Constructs an instance of the JaspiServerAuthContext.
     * <br/>
     * Each configured ServerAuthModule has its #getSupportedMessageTypes() method called to ensure that each module
     * does support and conform to the Jaspi HttpServlet Profile.
     *
     * @param messageInfoUtils An instance of the MessageInfoUtils.
     * @param contextHandler An instance of the ContextHandler.
     * @param sessionAuthModule A Session AuthModule. Can be <code>null</code>.
     * @param authModules A List of ServerAuthModules. MUST not be <code>null</code>.
     * @throws AuthException If any of the configured ServerAuthModules do not conform to the Jaspi HttpServlet Profile.
     */
    public JaspiServerAuthContext(final MessageInfoUtils messageInfoUtils, final ContextHandler contextHandler,
            final ServerAuthModule sessionAuthModule, final List<T> authModules) throws AuthException {

        List<ServerAuthModule> allAuthModules;
        if (authModules == null) {
            allAuthModules = new ArrayList<ServerAuthModule>();
        } else {
            allAuthModules = new ArrayList<ServerAuthModule>(authModules);
        }
        allAuthModules.add(0, sessionAuthModule);
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(allAuthModules);

        this.messageInfoUtils = messageInfoUtils;
        this.contextHandler = contextHandler;
        this.sessionAuthModule = sessionAuthModule;
        this.authModules = authModules;
    }

    /**
     * Returns the MessageInfoUtils.
     *
     * @return The MessageInfoUtils.
     */
    protected MessageInfoUtils getMessageInfoUtils() {
        return messageInfoUtils;
    }


    /**
     * Authenticate a received service request by delegating calls to the configured ServerAuthModules.
     * <br/>
     * The logic used to determine whether the Session AuthModule is called is as follows:
     * <ol>
     *     <li>If a Session AuthModule is configured, it is called first.</li>
     *     <li>If the Session AuthModule is configured and returns AuthStatus.SUCCESS, AuthStatus.SEND_SUCCESS or
     *     AuthStatus.SEND_CONTINUE processing stops and the AuthStatus is returned.</li>
     *     <li>If the Session AuthModule is not configured or it returns AuthStatus.SEND_FAILURE then the list
     *     of ServerAuthModules are called.</li>
     * </ol>
     *
     * If the Session AuthModule is not configured or returns AuthStatus.SEND_FAILURE, then the concrete sub-types
     * #validateRequest(List, MessageInfo, Subject, Subject) will be called for the ServerAuthModules
     * #validateRequest(MessageInfo, Subject, Subject) to be called.
     * <br/>
     * The configured AuditLogger will be called to audit the request <strong>if</strong> the Session AuthModule
     * is not configured or returns AuthStatus.SEND_FAILURE as if the Session AuthModule passes the Jaspi Runtime
     * takes this as an existing session continuing.
     *
     * @param messageInfo A contextual object that encapsulates the client request and server response objects.
     * @param clientSubject A Subject that represents the source of the service request. It is used to store Principals
     *                      and credentials validated in the request.
     * @param serviceSubject A Subject that represents the recipient of the service request, or null. It may be used as
     *                       the source of Principals or credentials to be used to validate the request. If the Subject
     *                       is not null, the method implementation may add additional Principals or credentials
     *                       (pertaining to the recipient of the service request) to the Subject.
     * @return An AuthStatus object representing the completion status of the processing performed by the method. The
     * AuthStatus values that may be returned by this method are defined as follows:
     * <ul>
     * <li>AuthStatus.SUCCESS when the application request message was successfully validated. The validated request
     * message is available by calling getRequestMessage on messageInfo.<li>
     * <li>AuthStatus.SEND_SUCCESS to indicate that validation/processing of the request message successfully produced
     * the secured application response message (in messageInfo). The secured response message is available by calling
     * getResponseMessage on messageInfo.</li>
     * <li>AuthStatus.SEND_CONTINUE to indicate that message validation is incomplete, and that a preliminary response
     * was returned as the response message in messageInfo. When this status value is returned to challenge an
     * application request message, the challenged request must be saved by the authentication module such that it can
     * be recovered when the module's validateRequest message is called to process the request returned for the
     * challenge.</li>
     * <li>AuthStatus.SEND_FAILURE to indicate that message validation failed and that an appropriate failure response
     * message is available by calling getResponseMessage on messageInfo.</li>
     * </ul>
     * @throws AuthException When the message processing failed without establishing a failure response message
     * (in messageInfo).
     */
    @Override
    public final AuthStatus validateRequest(final MessageInfo messageInfo, final Subject clientSubject,
            final Subject serviceSubject) throws AuthException {

        AuditTrail auditTrail = (AuditTrail) messageInfo.getMap().get(AUDIT_TRAIL_KEY);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();
        messageInfo.getMap().put(AUDIT_INFO_KEY, moduleAuditInfo);

        AuthStatus authStatus = null;
        // validate session module
        if (sessionAuthModule != null) {
            String moduleId = "Session-" + sessionAuthModule.getClass().getSimpleName();
            try {
                try {
                    authStatus = sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
                } catch (AuthException e) {
                    LOGGER.debug("Auditing authentication result");
                    contextHandler.audit(messageInfo, authStatus);
                    auditTrail.auditFailure(moduleId, e.getMessage(), moduleAuditInfo);
                    throw e;
                }
                if (AuthStatus.SUCCESS.equals(authStatus)) {
                    auditTrail.auditSuccess(moduleId, moduleAuditInfo);
                    // The module has successfully authenticated the client.
                    LOGGER.debug(sessionAuthModule.getClass().getSimpleName() + " has successfully authenticated the "
                            + "client");
                    contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);
                    return authStatus;
                } else if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                    auditTrail.auditSuccess(moduleId, moduleAuditInfo);
                    // The module may have completely/partially/not authenticated the client.
                    LOGGER.debug(sessionAuthModule.getClass().getSimpleName() + " may have completely/partially/not "
                            + "authenticated the client and has a response to return to the client");
                    return authStatus;
                } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                    String failureReason = (String) messageInfo.getMap().remove(AUDIT_FAILURE_REASON_KEY);
                    auditTrail.auditFailure(moduleId, failureReason, moduleAuditInfo);
                    // The module has failed to authenticate the client.
                    // -- In our implementation we will let subsequent modules try before sending the failure.
                    LOGGER.debug(sessionAuthModule.getClass().getSimpleName() + " has failed to authenticated the "
                            + "client, passing to Auth Modules");
                } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                    // The module has not completed authenticating the client.
                    LOGGER.debug(sessionAuthModule.getClass().getSimpleName() + " has not completed authenticating the "
                            + "client");
                    return authStatus;
                } else {
                    String message = "Invalid AuthStatus returned from validateRequest, " + asString(authStatus);
                    auditTrail.auditFailure(moduleId, message, moduleAuditInfo);
                    LOGGER.error(message);
                    throw new JaspiAuthException(message);
                }
            } finally {
                messageInfo.getMap().remove(AUDIT_INFO_KEY);
                messageInfo.getMap().remove(AUDIT_FAILURE_REASON_KEY);
                String sessionId = (String) messageInfo.getMap().remove(AUDIT_SESSION_ID_KEY);
                auditTrail.setSessionId(sessionId);
            }
        }

        try {
            try {
                authStatus = validateRequest(authModules, messageInfo, clientSubject, serviceSubject);
            } catch (AuthException e) {
                LOGGER.debug("Auditing authentication result");
                contextHandler.audit(messageInfo, authStatus);
                throw e;
            }
            if (authStatus == null || AuthStatus.FAILURE.equals(authStatus)) {
                final AuthStatus exceptionAuthStatus = authStatus;
                // Setting authStatus to null so auditing does not happen. As this exception is a configuration issue.
                authStatus = null;
                LOGGER.error("Invalid AuthStatus returned from validateRequest, " + asString(exceptionAuthStatus));
                throw new JaspiAuthException("Invalid AuthStatus returned from validateRequest, "
                        + asString(exceptionAuthStatus));
            }
        } finally {
            // Should not audit if the authentication process hasn't yet finished
            if (authStatus != null && !AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                // Once all Auth modules have had the chance to authenticate, audit the attempt.
                LOGGER.debug("Auditing authentication result");
                contextHandler.audit(messageInfo, authStatus);
            }
        }

        contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

        return authStatus;
    }

    /**
     * Co-ordinates calling the configured ServerAuthModules #validateRequest method, when either the Session
     * AuthModule is not configured or has returned AuthStatus.SEND_FAILURE.
     * <br/>
     * Possible AuthStatus return values: SUCCESS, SEND_SUCCESS, SEND_FAILURE, SEND_CONTINUE.
     *
     * @param authModules The list of configured ServerAuthModules.
     * @param messageInfo A contextual object that encapsulates the client request and server response objects.
     * @param clientSubject A Subject that represents the source of the service request. It is used to store Principals
     *                      and credentials validated in the request.
     * @param serviceSubject A Subject that represents the recipient of the service request, or null. It may be used as
     *                       the source of Principals or credentials to be used to validate the request. If the Subject
     *                       is not null, the method implementation may add additional Principals or credentials
     *                       (pertaining to the recipient of the service request) to the Subject.
     * @return An AuthStatus object representing the completion status of the processing performed by the method. The
     * AuthStatus values that may be returned by this method are defined as follows:
     * <ul>
     * <li>AuthStatus.SUCCESS when the application request message was successfully validated. The validated request
     * message is available by calling getRequestMessage on messageInfo.<li>
     * <li>AuthStatus.SEND_SUCCESS to indicate that validation/processing of the request message successfully produced
     * the secured application response message (in messageInfo). The secured response message is available by calling
     * getResponseMessage on messageInfo.</li>
     * <li>AuthStatus.SEND_CONTINUE to indicate that message validation is incomplete, and that a preliminary response
     * was returned as the response message in messageInfo. When this status value is returned to challenge an
     * application request message, the challenged request must be saved by the authentication module such that it can
     * be recovered when the module's validateRequest message is called to process the request returned for the
     * challenge.</li>
     * <li>AuthStatus.SEND_FAILURE to indicate that message validation failed and that an appropriate failure response
     * message is available by calling getResponseMessage on messageInfo.</li>
     * </ul>
     * @throws AuthException When the message processing failed without establishing a failure response message
     * (in messageInfo).
     */
    protected abstract AuthStatus validateRequest(final List<T> authModules, final MessageInfo messageInfo,
            final Subject clientSubject, final Subject serviceSubject) throws AuthException;

    /**
     * Secures a service response before sending it to the client by delegating calls to the configured
     * ServerAuthModules.
     * <br/>
     * Firstly the concrete sub-types #secureResponse(List, MessageInfo, Subject) will be called for the
     * ServerAuthModules #secureResponse(MessageInfo, Subject) to be called.
     * <br/>
     * The Session AuthModule is only called if:
     * <ul>
     *     <li>it is configured</li>
     *     <li>the call to the concrete sub-types secureResponse returns AuthStatus.SEND_SUCCESS</li>
     * </ul>
     *
     * Any other AuthStatus returned from the concrete sub-types secureResponse method will result in the end of
     * processing and the AuthStatus to be returned.
     * <br/>
     * Note: secureResponse should only be called if the call to validateRequest returned AuthStatus.SUCCESS. Any other
     * AuthStatus should cause the runtime to finish processing the request and return a response to the client.
     * <br/>
     * Possible AuthStatus return values: SEND_SUCCESS, SEND_FAILURE, SEND_CONTINUE.
     *
     * @param messageInfo A contextual object that encapsulates the client request and server response objects.
     * @param serviceSubject A Subject that represents the recipient of the service request, or null. It may be used as
     *                       the source of Principals or credentials to be used to validate the request. If the Subject
     *                       is not null, the method implementation may add additional Principals or credentials
     *                       (pertaining to the recipient of the service request) to the Subject.
     * @return An AuthStatus object representing the completion status of the processing performed by the method. The
     * AuthStatus values that may be returned by this method are defined as follows:
     * <ul>
     * <li>AuthStatus.SEND_SUCCESS when the application response message was successfully secured. The secured
     * response message may be obtained by calling getResponseMessage on messageInfo.</li>
     * <li>AuthStatus.SEND_CONTINUE to indicate that the application response message (within messageInfo) was replaced
     * with a security message that should elicit a security-specific response (in the form of a request) from the
     * peer. This status value serves to inform the calling runtime that (to successfully complete the message
     * exchange) it will need to be capable of continuing the message dialog by processing at least one additional
     * request/response exchange (after having sent the response message returned in messageInfo). When this status
     * value is returned, the application response must be saved by the authentication module such that it can be
     * recovered when the module's validateRequest message is called to process the elicited response.</li>
     * <li>AuthStatus.SEND_FAILURE to indicate that a failure occurred while securing the response message and that an
     * appropriate failure response message is available by calling getResponseMessage on messageInfo.</li>
     * </ul>
     * @throws AuthException When the message processing failed without establishing a failure response message
     * (in messageInfo).
     */
    @Override
    public final AuthStatus secureResponse(final MessageInfo messageInfo, final Subject serviceSubject)
            throws AuthException {

        AuditTrail auditTrail = (AuditTrail) messageInfo.getMap().get(AUDIT_TRAIL_KEY);

        AuthStatus authStatus = secureResponse(authModules, messageInfo, serviceSubject);
        if (AuthStatus.SUCCESS.equals(authStatus) || AuthStatus.FAILURE.equals(authStatus)) {
            LOGGER.error("Invalid AuthStatus returned from validateRequest, " + asString(authStatus));
            throw new JaspiAuthException("Invalid AuthStatus returned from validateRequest, "
                    + asString(authStatus));
        }

        /*
         * If the SessionModule is configured and either the AuthModule secureResponse has been called and returned
         * AuthStatus.SUCCESS or the AuthModule secureResponse has not been called and hence the authStatus is null.
         */
        if (sessionAuthModule != null && (authStatus == null || AuthStatus.SEND_SUCCESS.equals(authStatus))) {
            try {
                authStatus = sessionAuthModule.secureResponse(messageInfo, serviceSubject);
            } finally {
                String sessionId = (String) messageInfo.getMap().remove(AUDIT_SESSION_ID_KEY);
                auditTrail.setSessionId(sessionId);
            }
        }

        /*
         * As this method can only be called when validateRequest returns a AuthStatus.SUCCESS, which means either the
         * SessionModule is configured or the authenticatingAuthModule will be set, so authStatus cannot be null and
         * MUST be set to a valid AuthStatus value for secureResponse.
         */
        return authStatus;
    }

    /**
     * Co-ordinates calling the configured ServerAuthModules #secureResponse method, before the Session AuthModule is
     * called (if it is configured).
     * <br/>
     * Any other AuthStatus that AuthStatus.SEND_SUCCESS returned from the configured ServerAuthModules secureResponse
     * method will result in the end of processing and the AuthStatus to be returned.
     * <br/>
     * Possible AuthStatus return values: SEND_SUCCESS, SEND_FAILURE, SEND_CONTINUE.
     *
     * @param authModules The list of configured ServerAuthModules.
     * @param messageInfo A contextual object that encapsulates the client request and server response objects.
     * @param serviceSubject A Subject that represents the recipient of the service request, or null. It may be used as
     *                       the source of Principals or credentials to be used to validate the request. If the Subject
     *                       is not null, the method implementation may add additional Principals or credentials
     *                       (pertaining to the recipient of the service request) to the Subject.
     * @return An AuthStatus object representing the completion status of the processing performed by the method. The
     * AuthStatus values that may be returned by this method are defined as follows:
     * <ul>
     * <li>AuthStatus.SEND_SUCCESS when the application response message was successfully secured. The secured
     * response message may be obtained by calling getResponseMessage on messageInfo.</li>
     * <li>AuthStatus.SEND_CONTINUE to indicate that the application response message (within messageInfo) was replaced
     * with a security message that should elicit a security-specific response (in the form of a request) from the
     * peer. This status value serves to inform the calling runtime that (to successfully complete the message
     * exchange) it will need to be capable of continuing the message dialog by processing at least one additional
     * request/response exchange (after having sent the response message returned in messageInfo). When this status
     * value is returned, the application response must be saved by the authentication module such that it can be
     * recovered when the module's validateRequest message is called to process the elicited response.</li>
     * <li>AuthStatus.SEND_FAILURE to indicate that a failure occurred while securing the response message and that an
     * appropriate failure response message is available by calling getResponseMessage on messageInfo.</li>
     * </ul>
     * @throws AuthException When the message processing failed without establishing a failure response message
     * (in messageInfo).
     */
    protected abstract AuthStatus secureResponse(final List<T> authModules, final MessageInfo messageInfo,
            final Subject serviceSubject) throws AuthException;

    /**
     * Remove method specific principals and credentials from the subject by delegating calls to the Session
     * AuthModule and the List of ServerAuthModules.
     *
     * @param messageInfo A contextual object that encapsulates the client request and server response objects.
     * @param subject The Subject instance from which the Principals and credentials are to be removed.
     * @throws AuthException If an error occurs during the Subject processing.
     */
    @Override
    public final void cleanSubject(final MessageInfo messageInfo, final Subject subject) throws AuthException {
        if (sessionAuthModule != null) {
            sessionAuthModule.cleanSubject(messageInfo, subject);
        }
        if (authModules != null) {
            for (ServerAuthModule serverAuthModule : authModules) {
                serverAuthModule.cleanSubject(messageInfo, subject);
            }
        }
    }
}
