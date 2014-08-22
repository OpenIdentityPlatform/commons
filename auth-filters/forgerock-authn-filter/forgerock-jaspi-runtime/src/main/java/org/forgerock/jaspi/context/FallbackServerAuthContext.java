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

package org.forgerock.jaspi.context;

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.runtime.AuditTrail;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.runtime.context.JaspiServerAuthContext;
import org.forgerock.jaspi.utils.MessageInfoUtils;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.jaspi.runtime.AuditTrail.*;
import static org.forgerock.jaspi.runtime.AuthStatusUtils.asString;
import static org.forgerock.jaspi.runtime.JaspiRuntime.LOG;

/**
 * Encapsulates ServerAuthModules that are used to validate service requests received from clients, and to secure any
 * response returned for those requests.
 * <br/>
 * This FallbackServerAuthContext can be configured to use a single Session ServerAuthModule, to provide session
 * capabilities (i.e. on secureResponse adds a cookie which is validated on subsequent request to skip re-authenticating
 * for each request), and a list of ServerAuthModule that will be called in order until one returns a non-failure
 * AuthStatus, or the list is exhausted.
 *
 * @since 1.3.0
 */
public class FallbackServerAuthContext extends JaspiServerAuthContext<ServerAuthModule> {

    static final String AUTHENTICATING_AUTH_MODULE_KEY = "authenticatingAuthModule";

    /**
     * Constructs a new FallbackServerAuthContext instance.
     *
     * @param messageInfoUtils An instance of the MessageInfoUtils.
     * @param contextHandler An instance of the ContextHandler.
     * @param sessionAuthModule The configured Session AuthModule.
     * @param authModules The list of configure ServerAuthModules.
     * @throws AuthException If any of the configured ServerAuthModules do not conform to the Jaspi HttpServlet Profile.
     */
    public FallbackServerAuthContext(final MessageInfoUtils messageInfoUtils, final ContextHandler contextHandler,
            final ServerAuthModule sessionAuthModule, final List<ServerAuthModule> authModules) throws AuthException {
        super(messageInfoUtils, contextHandler, sessionAuthModule, authModules);
    }

    /**
     * {@inheritDoc}
     *
     * The logic used to determine which of the list of ServerAuthModules are called is as follows:
     * <ol>
     *     <li>Modules are called in insertion order in the list.</li>
     *     <li>If a module returns AuthStatus.SUCCESS the context will return the AuthStatus without calling
     *     any subsequent modules.</li>
     *     <li>If a module returns AuthStatus.SEND_SUCCESS the module has completely/partially/not authenticated the
     *     client and the context will return the AuthStatus without calling any subsequent modules.</li>
     *     <li>If a module returns AuthStatus.SEND_CONTINUE the context will return the AuthStatus without calling
     *     any subsequent modules.</li>
     *     <li>If a module returns AuthStatus.SEND_FAILURE the module has failed to authenticate the client and
     *     the context will call the next subsequent module.</li>
     *     <li>If there are no more modules in the configured list the last AuthStatus is returned and processing
     *     stops..</li>
     * </ol>
     *
     * @param authModules {@inheritDoc}
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus validateRequest(final List<ServerAuthModule> authModules, final MessageInfo messageInfo,
            final Subject clientSubject, final Subject serviceSubject) throws AuthException {

        AuditTrail auditTrail = (AuditTrail) messageInfo.getMap().get(AUDIT_TRAIL_KEY);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();
        messageInfo.getMap().put(AUDIT_INFO_KEY, moduleAuditInfo);

        AuthStatus authStatus = AuthStatus.SEND_FAILURE;
        int index = 0;
        for (ServerAuthModule authModule : authModules) {
            String moduleId = "AuthModule-" + authModule.getClass().getSimpleName() + "-" + index++;
            try {
                try {
                    authStatus = authModule.validateRequest(messageInfo, clientSubject, serviceSubject);
                } catch (AuthException e) {
                    auditTrail.auditFailure(moduleId, e.getMessage(), moduleAuditInfo);
                    throw e;
                }
                // Record the AuthModules AuthStatus to decided later whether to call secureResponse on the
                // AuthModule.
                if (AuthStatus.SUCCESS.equals(authStatus)) {
                    auditTrail.auditSuccess(moduleId, moduleAuditInfo);
                    // The module has successfully authenticated the client.
                    LOG.debug("{} has successfully authenticated the client", authModule.getClass().getSimpleName());
                    ServerAuthModule authenticatingAuthModule = authModule;
                    /*
                     * put authStatus and authenticatingAuthModule in MessageInfo so can accessed by secureResponse in a
                     * stateless way
                     */
                    LOG.trace("Adding authenticating auth module to private context map, {}",
                            authenticatingAuthModule.getClass().getSimpleName());
                    Map<String, Object> authContextMap = getMessageInfoUtils().getMap(messageInfo,
                            PRIVATE_CONTEXT_MAP_KEY);
                    authContextMap.put(AUTHENTICATING_AUTH_MODULE_KEY, authenticatingAuthModule);
                    break;
                } else if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                    auditTrail.auditSuccess(moduleId, moduleAuditInfo);
                    // The module may have completely/partially/not authenticated the client.
                    LOG.debug("{} may have completely/partially/not authenticated the client and has a response to "
                            + "return to the client", authModule.getClass().getSimpleName());
                    break;
                } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                    String failureReason = (String) messageInfo.getMap().remove(AUDIT_FAILURE_REASON_KEY);
                    auditTrail.auditFailure(moduleId, failureReason, moduleAuditInfo);
                    // The module has failed to authenticate the client.
                    // -- In our implementation we will let subsequent modules try before sending the failure.
                    LOG.debug("{} has failed to authenticated the client, passing to Auth Modules",
                            authModule.getClass().getSimpleName());
                    continue;
                } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                    // The module has not completed authenticating the client.
                    LOG.debug("{} has not completed authenticating the client", authModule.getClass().getSimpleName());
                    break;
                } else {
                    String message = "Invalid AuthStatus returned from validateRequest, "
                            + asString(authStatus);
                    auditTrail.auditFailure(moduleId, message, moduleAuditInfo);
                    LOG.error(message);
                    throw new JaspiAuthException(message);
                }
                /**
                 * In this implementation of the ServerAuthContext we are allowing a single "session" auth module to
                 * run and possible succeed and if so will stop processing, otherwise we allow a list of "normal"
                 * auth modules to try and authenticate the request. Hence why here a SEND_FAILURE will carry on
                 * processing, where as SEND_CONTINUE and SEND_SUCCESS will cause processing to end.
                 *
                 * SEND_CONTINUE implies that there is to be more communication between server and client before
                 * authentication can be deemed successful or failed. So processing needs to stop and a response
                 * sent back to the client.
                 */
            } finally {
                messageInfo.getMap().remove(AUDIT_INFO_KEY);
                messageInfo.getMap().remove(AUDIT_FAILURE_REASON_KEY);
            }
        }

        return authStatus;
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Only the ServerAuthModule which returned AuthStatus.SUCCESS for its #validateRequest method will have its
     * secureResponse method call and if no ServerAuthModule successfully validated the request then this method
     * will return <code>null</code>.
     *
     * @param authModules {@inheritDoc}
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus secureResponse(final List<ServerAuthModule> authModules, final MessageInfo messageInfo,
            final Subject serviceSubject) throws AuthException {

        Map<String, Object> authContextMap = getMessageInfoUtils().getMap(messageInfo, PRIVATE_CONTEXT_MAP_KEY);
        final ServerAuthModule authenticatingAuthModule =
                (ServerAuthModule) authContextMap.remove(AUTHENTICATING_AUTH_MODULE_KEY);
        /*
         * If authenticationAuthModule has been set, ie a normal ServerAuthModule successfully validated the request.
         */
        if (authenticatingAuthModule != null) {
            LOG.trace("Using authenticating auth module from private context map, "
                    + authenticatingAuthModule.getClass().getSimpleName() + ", to secure the response");
            return authenticatingAuthModule.secureResponse(messageInfo, serviceSubject);
        }

        return null;
    }
}
