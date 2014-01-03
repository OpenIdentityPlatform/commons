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

import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.logging.MessageInfoAuditRecord;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.resource.ResourceException;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.forgerock.jaspi.runtime.JaspiRuntime.JSON_HTTP_MEDIA_TYPE;
import static org.forgerock.jaspi.runtime.JaspiRuntime.UNAUTHORIZED_ERROR_MESSAGE;
import static org.forgerock.jaspi.runtime.JaspiRuntime.UNAUTHORIZED_HTTP_ERROR_CODE;

/**
 * A handler for ServerAuthContext, which exposes helper methods that will be called at the varying end states
 * of the ServerAuthContext processing.
 *
 * @since 1.3.0
 */
public class ContextHandler {

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    private final MessageInfoUtils messageInfoUtils;

    /**
     * Constructs a new instance of the ContextHandler.
     *
     * @param messageInfoUtils An instance of the MessageInfoUtils.
     */
    public ContextHandler(final MessageInfoUtils messageInfoUtils) {
        this.messageInfoUtils = messageInfoUtils;
    }

    /**
     * Validates that each given ServerAuthModule conforms to the Jaspi HttpServlet Profile by calling each
     * modules #getSupportedMessageTypes() method and ensuring it contains both the HttpServletRequest and
     * HttpServletResponse classes.
     *
     * @param authModules The List of ServerAuthModules to check.
     * @throws AuthException If any of the ServerAuthModules does not conform to the HttpServlet Profile.
     */
    public void validateServerAuthModuleConformToHttpServletProfile(final List<ServerAuthModule> authModules)
            throws AuthException {

        if (authModules != null) {
            for (ServerAuthModule authModule : authModules) {
                checkAuthModuleSupportsHttpServletProfile(authModule);
            }
        }
    }

    /**
     * Validates that the given ServerAuthModule conforms to the Jaspi HttpServlet Profile by calling the module's
     * #getSupportedMessageTypes() method and ensuring it contains both the HttpServletRequest and HttpServletResponse
     * classes.
     *
     * @param authModule The ServerAuthModule to check.
     * @throws AuthException If the ServerAuthModule does not conform to the HttpServlet Profile.
     */
    @SuppressWarnings("rawtypes")
    private void checkAuthModuleSupportsHttpServletProfile(final ServerAuthModule authModule) throws AuthException {

        if (authModule == null) {
            return;
        }

        List<Class> supportedTypes = Arrays.asList(authModule.getSupportedMessageTypes());

        if (!(supportedTypes.contains(HttpServletRequest.class)
                && supportedTypes.contains(HttpServletResponse.class))) {
            LOGGER.error("ServerAuthModule does not support the HttpServlet profile, "
                    + authModule.getClass().getName());
            throw new JaspiAuthException("ServerAuthModule does not support the HttpServlet profile, "
                    + authModule.getClass().getName());
        }
    }

    /**
     * Handles the completion of an authentication request.
     * <br/>
     * Checks if the authentication resulted in a valid AuthStatus and if not will write a 401 Unauthorized response
     * to the HttpServletResponse stored in the MessageInfo object.
     * Otherwise will set the authentication attributes, principal name and authentication context map, in the
     * HttpServletRequest.
     *
     * @param messageInfo The MessageInfo object.
     * @param clientSubject The Client Subject.
     * @param authStatus The AuthStatus of the request.
     * @throws AuthException If there is a problem writing to the response.
     */
    public void handleCompletion(final MessageInfo messageInfo, final Subject clientSubject,
            final AuthStatus authStatus) throws AuthException {

        if (authStatus == null) {
            LOGGER.debug("Authentication has failed.");
            HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
            ResourceException jre = ResourceException.getException(UNAUTHORIZED_HTTP_ERROR_CODE,
                    UNAUTHORIZED_ERROR_MESSAGE);
            try {
                response.getWriter().write(jre.toJsonValue().toString());
                response.setContentType(JSON_HTTP_MEDIA_TYPE);
            } catch (IOException e) {
                LOGGER.error("Failed to write to response", e);
                throw new JaspiAuthException(e);
            }
        } else {
            setAuthenticationRequestAttributes(messageInfo, clientSubject);
        }
    }

    /**
     * Sets the authentication id and context map into the HttpServletRequest, as attributes, for use by other
     * filters and resources.
     * <br/>
     * Will take the first Principal it finds in the client Subject and add the Principal's name to the authentication
     * context.
     *
     * @param messageInfo The MessageInfo object.
     * @param clientSubject The Client Subject.
     */
    private void setAuthenticationRequestAttributes(final MessageInfo messageInfo, final Subject clientSubject) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        String principalName = null;
        for (Principal principal : clientSubject.getPrincipals()) {
            principalName = principal.getName();
            break;
        }

        Map<String, Object> context = messageInfoUtils.getMap(messageInfo, JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);
        LOGGER.debug("Setting principal name, " + principalName + ", and " + context.size() + " context values on to "
                + "the request.");
        request.setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_PRINCIPAL, principalName);
        request.setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, context);
    }

    /**
     * Creates an audit record containing the given MessageInfo and whether the AuthStatus is a success or not
     * and calls the configured audit logger.
     *
     * @param messageInfo The MessageInfo to use as the audit object.
     * @param authStatus The AuthStatus of the request.
     */
    public void audit(final MessageInfo messageInfo, final AuthStatus authStatus) {
        AuditRecord<MessageInfo> auditRecord = new MessageInfoAuditRecord(
                isAuthStatusSuccessful(authStatus) ? AuthResult.SUCCESS : AuthResult.FAILURE, messageInfo);
        LogFactory.getAuditLogger().audit(auditRecord);
    }

    /**
     * Checks if the AuthStatus value is a successful status or not.
     *
     * @param authStatus The AuthStatus.
     * @return <code>true</code> if the AuthStatus is SUCCESS or SEND_SUCCESS.
     */
    private boolean isAuthStatusSuccessful(final AuthStatus authStatus) {
        return AuthStatus.SUCCESS.equals(authStatus) || AuthStatus.SEND_SUCCESS.equals(authStatus);
    }
}
