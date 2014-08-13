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

package org.forgerock.jaspi.runtime;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

import static javax.security.auth.message.AuthStatus.*;
import static org.forgerock.jaspi.runtime.AuthStatusUtils.asString;

/**
 * Handler which handles the result of calls to a ServerAuthContexts validateRequest and secureResponse methods.
 *
 * @since 1.3.0
 */
public class RuntimeResultHandler {

    private static final DebugLogger DEBUG = LogFactory.getDebug();

    /**
     * <p>Handles the result of the call to the ServerAuthContext validateRequest method.</p>
     *
     * <p>Only an AuthStatus of SUCCESS will return true and an AuthStatus of SEND_FAILURE will set the response status
     * to 401.</p>
     *
     * @param authStatus The returned AuthStatus from the validateRequest method call.
     * @param auditTrail The instance of the {@code AuditTrail} for this authentication request.
     * @param clientSubject The client's subject.
     * @param response The HttpServletResponse.
     * @return <code>true</code> if the processing of the request should proceed, <code>false</code> otherwise.
     * @throws javax.security.auth.message.AuthException If the AuthStatus is not valid for a call to validateRequest.
     */
    public boolean handleValidateRequestResult(AuthStatus authStatus, AuditTrail auditTrail, Subject clientSubject,
            HttpServletResponse response) throws AuthException {

        if (SUCCESS.equals(authStatus)) {
            auditTrail.completeAuditAsSuccessful(getPrincipal(clientSubject));
            // nothing to do here just carry on
            DEBUG.debug("Successfully validated request.");
            return true;
        } else if (SEND_SUCCESS.equals(authStatus)) {
            auditTrail.completeAuditAsSuccessful(getPrincipal(clientSubject));
            // Send HttpServletResponse to client and exit.
            DEBUG.debug("Successfully validated request, with response message");
            return false;
        } else if (SEND_FAILURE.equals(authStatus)) {
            auditTrail.completeAuditAsFailure(getPrincipal(clientSubject));
            // Send HttpServletResponse to client and exit.
            DEBUG.debug("Failed to validate request, included response message.");
            response.setStatus(401);
            return false;
        } else if (SEND_CONTINUE.equals(authStatus)) {
            // Send HttpServletResponse to client and exit.
            DEBUG.debug("Has not finished validating request. Requires more information from client.");
            return false;
        } else {
            auditTrail.completeAuditAsFailure(getPrincipal(clientSubject));
            DEBUG.error("Invalid AuthStatus, " + asString(authStatus));
            throw new AuthException("Invalid AuthStatus from validateRequest: " + asString(authStatus));
        }
    }

    private String getPrincipal(Subject clientSubject) {
        for (Principal principal : clientSubject.getPrincipals()) {
            if (principal.getName() != null) {
                return principal.getName();
            }
        }

        return null;
    }

    /**
     * Handles the result of the call to the ServerAuthContext secureResponse method.
     * <br/>
     * An AuthStatus of SEND_CONTINUE will set the response status to 100.
     *
     * @param authStatus The returned AuthStatus from the secureResponse method call.
     * @param response The HttpServletResponse.
     * @throws javax.security.auth.message.AuthException If the AuthStatus is not valid for a call to secureResponse.
     */
    public void handleSecureResponseResult(final AuthStatus authStatus, final HttpServletResponse response)
            throws AuthException {

        if (SEND_SUCCESS.equals(authStatus)) {
            // nothing to do here just carry on
            DEBUG.debug("Successfully secured response.");
        } else if (SEND_FAILURE.equals(authStatus)) {
            // Send HttpServletResponse to client and exit.
            DEBUG.debug("Failed to secured response, included response message");
            response.setStatus(500);
        } else if (SEND_CONTINUE.equals(authStatus)) {
            // Send HttpServletResponse to client and exit.
            DEBUG.debug("Has not finished securing response. Requires more information from client.");
        } else {
            DEBUG.error("Invalid AuthStatus, " + asString(authStatus));
            throw new AuthException("Invalid AuthStatus from secureResponse: " + asString(authStatus));
        }
    }
}
