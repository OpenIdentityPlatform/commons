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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container;

import org.forgerock.jaspi.filter.AuthNFilter;
import org.forgerock.json.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates ServerAuthModules that are used to validate service requests received from clients,
 * and to secure any response returned for those requests. A caller typically uses this class in the following manner:
 *
 * <ol>
 * <li>Retrieve an instance of this class via ServerAuthConfig.getAuthContext.</li>
 * <li>Invoke validateRequest.</li>
 *      ServerAuthContext implementation invokes validateRequest of one or more encapsulated ServerAuthModules.
 *      Modules validate credentials present in request (for example, decrypt and verify a signature).
 * <li>If credentials valid and sufficient, authentication complete.</li>
 * Perform authorization check on authenticated identity and, if successful, dispatch to requested service
 * application.
 * <li>Service application finished.</li>
 * <li>Invoke secureResponse.</li>
 * ServerAuthContext implementation invokes secureResponse of one or more encapsulated ServerAuthModules.
 * Modules secure response (sign and encrypt response, for example), and prepare response message.
 * <li>Send secured response to client.</li>
 * <li>Invoke cleanSubject (as necessary) to clean up any authentication state in Subject(s).</li>
 * </ol>
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class ServerAuthContextImpl implements ServerAuthContext {

    private static final Logger DEBUG = LoggerFactory.getLogger(ServerAuthContextImpl.class);

    //TODO change to CREST constant when AM and IDM have been updated to use 2.0.0 version
    private static final String AUTHC_ID_REQUEST_KEY = "org.forgerock.security.authcid";
    private static final String CONTEXT_REQUEST_KEY = "org.forgerock.security.context";

    private static final String PRIVATE_CONTEXT_MAP_KEY = "_serverAuthContextImplContextMap";
    private static final String AUTHENTICATING_AUTH_STATUS_KEY = "authenticatingAuthStatus";
    private static final String AUTHENTICATING_AUTH_MODULE_KEY = "authenticatingAuthModule";

    private final MessageInfoUtils messageInfoUtils = new MessageInfoUtils();
    private final ServerAuthModule sessionAuthModule;
    private final List<ServerAuthModule> serverAuthModules;

    /**
     * Constructs an instance of ServerAuthContextImpl.
     *
     * @param sessionAuthModule The SessionAuthModule that this context should run.
     * @param serverAuthModules A List of ServerAuthModules that this context should run.
     * @param requestPolicy The MessagePolicy to be applied to the request.
     * @param responsePolicy The MessagePolicy to be applied to the response.
     * @param properties A Map of key pairs to be used to initialise the ServerAuthModules.
     * @param handler A CallbackHandler to be passed to the ServerAuthModules to use.
     * @throws AuthException If there is an exception when initialising the ServerAuthModules.
     */
    public ServerAuthContextImpl(ServerAuthModule sessionAuthModule, List<ServerAuthModule> serverAuthModules,
            MessagePolicy requestPolicy, MessagePolicy responsePolicy, Map properties, CallbackHandler handler)
            throws AuthException {
        this.sessionAuthModule = sessionAuthModule;
        this.serverAuthModules = serverAuthModules;
    }

    /**
     * Authenticate a received service request by delegating calls to the List of ServerAuthModules.
     * If there is more than one ServerAuthModule the following logic is used to determine when and in which order
     * the modules are called.
     *
     * <ul>
     *     <li>Modules are called in insertion order in the list.</li>
     *     <li>If a module returns AuthStatus.SUCCESS the context will return the AuthStatus without calling
     *     any subsequent modules.</li>
     *     <li>If a module returns AuthStatus.SEND_SUCCESS the module has completely/partially/not authenticated the
     *     client and the context will call the next subsequent module.</li>
     *     <li>If a module returns AuthStatus.SEND_FAILURE the module has failed to authenticate the client and
     *     the context will call the next subsequent module.</li>
     *     <li>If a module returns AuthStatus.SEND_CONTINUE the context will return the AuthStatus without calling
     *     any subsequent modules.</li>
     *     <li>If a module returns AuthStatus.SEND_SUCCESS, and it is the last module in the list, the context will
     *     return the AuthStatus.</li>
     * </ul>
     *
     * Possible AuthStatus return values: SUCCESS, SEND_SUCCESS, SEND_FAILURE, SEND_CONTINUE.
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
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        Map<String, Object> authContextMap = messageInfoUtils.getMap(messageInfo, PRIVATE_CONTEXT_MAP_KEY);
        AuthStatus authenticatingAuthStatus = null;
        ServerAuthModule authenticatingAuthModule = null;
        try {
            messageInfo.getMap().put(CONTEXT_REQUEST_KEY, new HashMap<String, Object>());

            AuthStatus authStatus = null;
            if (sessionAuthModule != null) {
                authStatus = sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);

                if (AuthStatus.SUCCESS.equals(authStatus)) {
                    // The module has successfully authenticated the client.
                    authenticatingAuthStatus = authStatus;
                    setAuthenticationRequestAttributes(messageInfo, clientSubject);
                    return authStatus;
                } else if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                    // The module may have completely/partially/not authenticated the client.
                    setAuthenticationRequestAttributes(messageInfo, clientSubject);
                    return authStatus;
                } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                    // The module has failed to authenticate the client.
                    // -- In our implementation we will let subsequent modules try before sending the failure.
                } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                    // The module has not completed authenticating the client.
                    return authStatus;
                }
            }

            try {
                for (ServerAuthModule serverAuthModule : serverAuthModules) {
                    authStatus = serverAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
                    // Record the AuthModules AuthStatus to decided later whether to call secureResponse on the AuthModule.
                    if (AuthStatus.SUCCESS.equals(authStatus)) {
                        // The module has successfully authenticated the client.
                        authenticatingAuthModule = serverAuthModule;
                        authenticatingAuthStatus = authStatus;
                        break;
                    } else if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                        // The module may have completely/partially/not authenticated the client.
                        authenticatingAuthModule = serverAuthModule;
                        authenticatingAuthStatus = authStatus;
                        break;
                    } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                        // The module has failed to authenticate the client.
                        // -- In our implementation we will let subsequent modules try before sending the failure.
                        continue;
                    } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                        // The module has not completed authenticating the client.
                        authenticatingAuthStatus = authStatus;
                        break;
                    }
                }
            } finally {
                // Once all Auth modules have had the chance to authenticate, audit the attempt.
                if (AuditLoggerHolder.INSTANCE.getInstance() != null) {
                    AuditLoggerHolder.INSTANCE.getInstance().audit(messageInfo);
                } else {
                    DEBUG.warn("Failed to log entry for authentication attempt as router is null.");
                }
            }

            // Once all the Auth modules have had the change to authenticate, set error message in response if failed.
            if (authenticatingAuthStatus == null) {
                HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
                ResourceException jre = ResourceException.getException(401, "Access denied");
                try {
                    response.getWriter().write(jre.toJsonValue().toString());
                    response.setContentType("application/json");
                } catch (IOException e) {
                    throw new AuthException(e.getMessage());
                }
            } else {
                setAuthenticationRequestAttributes(messageInfo, clientSubject);
            }

            return authStatus;

        } finally {
            authContextMap.put(AUTHENTICATING_AUTH_STATUS_KEY, authenticatingAuthStatus);
            authContextMap.put(AUTHENTICATING_AUTH_MODULE_KEY, authenticatingAuthModule);
        }
    }

    /**
     * Sets the authentication id and context map into the HttpServletRequest for use by other filters and resources.
     * <p>
     * Will take the first Principal it finds in the client Subject and add the Principal's name to the authentication
     * context.
     *
     * @param messageInfo The MessageInfo object.
     * @param clientSubject The Client Subject.
     */
    private void setAuthenticationRequestAttributes(MessageInfo messageInfo, Subject clientSubject) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        String principalName = null;
        for (Principal principal : clientSubject.getPrincipals()) {
            principalName = principal.getName();
            break;
        }
        Map<String, Object> context = (Map<String, Object>) messageInfo.getMap().get(AuthNFilter.ATTRIBUTE_AUTH_CONTEXT);
        messageInfo.setRequestMessage(new AuthHttpServletRequestWrapper(request, principalName, context));
    }

    /**
     * Secure a service response before sending it to the client by delegating calls to the List of ServerAuthModules.
     * If there is more than one ServerAuthModule the following logic is used to determine when and in which order
     * the modules are called.
     *
     * <ul>
     *     <li>Modules are called in insertion order in the list.</li>
     *     <li>If a module returns AuthStatus.SEND_SUCCESS the next subsequent module is called.</li>
     *     <li>If a module returns AuthStatus.SEND_FAILURE the context will return the AuthStatus without calling any
     *     subsequent modules</li>
     *     <li>If a module returns AuthStatus.SEND_CONTINUE the context will return the AuthStatus without calling
     *     any subsequent modules.</li>
     *     <li>If a module returns AuthStatus.SEND_SUCCESS, and it is the last module in the list, the context will
     *     return the AuthStatus.</li>
     * </ul>
     *
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
     * response essage may be obtained by calling getResponseMessage on messageInfo.</li>
     * <li>AuthStatus.SEND_CONTINUE to indicate that the application response message (within messageInfo) was replaced
     * with a security message that should elicit a security-specific response (in the form of a request) from the
     * peer. This status value serves to inform the calling runtime that (to successfully complete the message
     * exchange) it will need to be capable of continuing the message dialog by processing at least one additional
     * request/response exchange (after having sent the response message returned in messageInfo). When this status
     * value is returned, the application response must be saved by the authentication module such that it can be
     * recovered when the module's validateRequest message is called to process the elicited response.</li>
     * <li>AuthStatus.SEND_FAILURE to indicate that a failure occurred while securing the response message and that an
     * appropriate failure response message is available by calling getResponseMeessage on messageInfo.</li>
     * </ul>
     * @throws AuthException When the message processing failed without establishing a failure response message
     * (in messageInfo).
     */
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        Map<String, Object> authContextMap = messageInfoUtils.getMap(messageInfo, PRIVATE_CONTEXT_MAP_KEY);
        final AuthStatus authenticatingAuthStatus = (AuthStatus) authContextMap.remove(AUTHENTICATING_AUTH_STATUS_KEY);
        final ServerAuthModule authenticatingAuthModule =
                (ServerAuthModule) authContextMap.remove(AUTHENTICATING_AUTH_MODULE_KEY);

        AuthStatus authStatus = null;
        if (authenticatingAuthModule != null && !AuthStatus.SEND_SUCCESS.equals(authenticatingAuthStatus)) {
            authStatus = authenticatingAuthModule.secureResponse(messageInfo, serviceSubject);
        }

        boolean authenticatedSuccessfully = AuthStatus.SUCCESS.equals(authenticatingAuthStatus)
                || AuthStatus.SEND_SUCCESS.equals(authenticatingAuthStatus);

        if (sessionAuthModule != null && ((authenticatedSuccessfully && authStatus == null)
                || AuthStatus.SEND_SUCCESS.equals(authStatus))) {
            authStatus = sessionAuthModule.secureResponse(messageInfo, serviceSubject);
        }

        if (authStatus == null) {
            return AuthStatus.SEND_SUCCESS;
        } else {
            return authStatus;
        }
    }

    /**
     * Remove method specific principals and credentials from the subject by delegating calls to the List of
     * ServerAuthModules.
     *
     * @param messageInfo A contextual object that encapsulates the client request and server response objects.
     * @param subject The Subject instance from which the Principals and credentials are to be removed.
     * @throws AuthException If an error occurs during the Subject processing.
     */
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        for (ServerAuthModule serverAuthModule : serverAuthModules) {
            serverAuthModule.cleanSubject(messageInfo, subject);
        }
    }
}
