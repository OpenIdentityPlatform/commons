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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.json.resource.ResourceException;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class is the entry point for the JASPI runtime.
 * <p>
 * Provides a single method #processMessage that takes a HttpServletRequest, HttpServletResponse and the FilterChain
 * and will run the configured ServerAuthModules to determine whether the request is allowed to be passed down
 * the filter chain.
 * <p>
 * <strong>Note:</strong> if the Jaspi runtime is not configured properly with a non-null ServerAuthContext
 * each request will be passed straight to the filter chain with no further processing. So it is VERY important
 * to ensure the runtime is configured correctly.
 *
 * @since 1.3.0
 */
public class JaspiRuntime {

    private static final DebugLogger DEBUG = LogFactory.getDebug();

    /**
     * Indicates that the request could not be authenticated either because no credentials were provided or
     * the credentials provided did not pass authentication. Equivalent to HTTP status: 401 Unauthorized.
     */
    public static final int UNAUTHORIZED_HTTP_ERROR_CODE = 401;

    /**
     * The exception message for a 401 ResourceException.
     */
    public static final String UNAUTHORIZED_ERROR_MESSAGE = "Access Denied";

    /**
     * The application/json HTTP Media Type.
     */
    public static final String JSON_HTTP_MEDIA_TYPE = "application/json";

    /**
     * The name of the HTTP Servlet Request attribute where the principal name of the user/client making the request
     * will be set.
     */
    public static final String ATTRIBUTE_AUTH_PRINCIPAL = "org.forgerock.authentication.principal";
    /**
     * The name of the HTTP Servlet Request attribute where any additional authentication context information
     * will be set. It MUST contain a {@code Map<String, Object>} if present.
     */
    public static final String ATTRIBUTE_AUTH_CONTEXT = "org.forgerock.authentication.context";

    private final ServerAuthContext serverAuthContext;
    private final RuntimeResultHandler resultHandler;

    /**
     * Constructs a new instance of the JaspiRuntime.
     *
     * @param serverAuthContext The instance of a ServerAuthContext that the runtime has been configured to use
     *                          to orchestrate calling the ServerAuthModules.
     * @param resultHandler An instance of the RuntimeResultHandler.
     */
    public JaspiRuntime(final ServerAuthContext serverAuthContext, final RuntimeResultHandler resultHandler) {
        this.serverAuthContext = serverAuthContext;
        this.resultHandler = resultHandler;
    }

    /**
     * Processes the given request and response.
     * <p>
     * Guaranteed to call the configured ServerAuthModules validateRequest method to determine whether the request is
     * allowed through and if validation was successful, to call secureResponse after the service call has completed.
     * <p>
     * Any authentication exceptions that occur during this process will be caught and converted into a Json response
     * that matches a HTTP 500 status code.
     * <p>
     * If no ServerAuthContext is configured for the HttpServlet layer and the requests application context, then
     * the request will be allowed through.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param filterChain The filter chain.
     * @throws java.io.IOException If there is an error processing the request.
     * @throws javax.servlet.ServletException If there is an error processing the request.
     */
    public void processMessage(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws IOException, ServletException {

        MessageInfo messageInfo = new HttpServletMessageInfo(request, response);

        // Must create new client Subject.
        final Subject clientSubject = new Subject();
        // Can be null if no details required for service subject.
        final Subject serviceSubject = null;

        try {
            // Could be null if no modules found
            if (serverAuthContext == null) {
                DEBUG.error("No ServerAuthContext configured! Jaspi Runtime not configured properly!");
                DEBUG.warn("Proceeding with filter chain call.");
                filterChain.doFilter(request, response);
                return;
            }

            // This is where the modules are called
            AuthStatus requestAuthStatus = serverAuthContext.validateRequest(messageInfo, clientSubject,
                    serviceSubject);

            if (!resultHandler.handleValidateRequestResult(requestAuthStatus,
                    (HttpServletResponse) messageInfo.getResponseMessage())) {
                return;
            }

            filterChain.doFilter((ServletRequest) messageInfo.getRequestMessage(),
                    (ServletResponse) messageInfo.getResponseMessage());

            AuthStatus responseAuthStatus = serverAuthContext.secureResponse(messageInfo, serviceSubject);

            resultHandler.handleSecureResponseResult(responseAuthStatus,
                    (HttpServletResponse) messageInfo.getResponseMessage());

            // Give the AuthModule a chance to clear out any authentication data from the subject.
            serverAuthContext.cleanSubject(messageInfo, clientSubject);

        } catch (AuthException e) {
            HttpServletResponse httpResponse = (HttpServletResponse) messageInfo.getResponseMessage();
            ResourceException jre = ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage());
            httpResponse.setStatus(ResourceException.INTERNAL_ERROR);
            try {
                httpResponse.getWriter().write(jre.toJsonValue().toString());
                httpResponse.setContentType(JSON_HTTP_MEDIA_TYPE);
            } catch (IOException ioe) {
                throw new ServletException(ioe.getMessage(), ioe);
            }
        }
    }
}
