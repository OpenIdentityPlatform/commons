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

package org.forgerock.jaspi.filter;

import org.forgerock.jaspi.container.AuthConfigFactoryImpl;
import org.forgerock.jaspi.container.MessageInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.security.auth.message.AuthStatus.SEND_CONTINUE;
import static javax.security.auth.message.AuthStatus.SEND_FAILURE;
import static javax.security.auth.message.AuthStatus.SEND_SUCCESS;
import static javax.security.auth.message.AuthStatus.SUCCESS;

/**
 * Authentication Filter that makes use of the JASPI Container framework.
 *
 * The message layer used to get the AuthConfigProviders is always "HttpServlet", as per the Servlet Container profile.
 * The application context is built up from "hostname path", i.e. "openam.forgerock.org /openam/rest/".
 *
 * To select the chain of modules that this Authentication Filter should use to authenticate request is set in the
 * web.xml as an init-param with the name of "moduleConfiguration" the value of this param must match are valid Module
 * Chain name.
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class AuthNFilter implements Filter {

    public static final String ATTRIBUTE_AUTH_PRINCIPAL = "org.forgerock.authentication.principal";
    public static final String ATTRIBUTE_AUTH_CONTEXT = "org.forgerock.authentication.context";

    private final static Logger DEBUG = LoggerFactory.getLogger(AuthNFilter.class);

    private final static String MESSAGE_LAYER = "HttpServlet";

    /** Module Configuration Filter init parameter key. */
    public final static String MODULE_CONFIGURATION_PROPERTY = "moduleConfiguration";

    /*
     * The module configuration is configured in the provider to be a specific set of modules that will be executed by
     * the authContext. The moduleConfiguration value must therefore correspond to a set in the provider configuration.
     * How this is configured may vary - XML file, JSON via OSGi etc. Note that full support for module chains will not
     * be implemented. The AuthnFilter is only concerned with zero or one session module and zero or many auth
     * module(s).
     */
    private String moduleConfiguration;

    /**
     * Called by the web container and gets the correct provider factory to use.
     *
     * @param filterConfig The filter configuration containing the init-param for the module chain name.
     * @throws ServletException If there is no moduleConfiguration set in the init-params in the web.xml.
     */
    public void init(FilterConfig filterConfig) throws ServletException {

        moduleConfiguration = filterConfig.getInitParameter(MODULE_CONFIGURATION_PROPERTY);
        DEBUG.debug("moduleConfiguration set to: {}", moduleConfiguration);
        if (moduleConfiguration == null || moduleConfiguration.length() == 0) {
            DEBUG.error("moduleConfiguration not set in init-param");
            throw new ServletException("moduleConfiguration not set in init-param");
        }
    }

    /**
     * Calls the JASPI Container to perform authentication for each request this filter is set to catch.
     *
     * @param servletRequest The ServletRequest object.
     * @param servletResponse The ServletResponse object.
     * @param filterChain The FilterChain.
     * @throws IOException If there is a problem getting the request URL or when calling the FilterChain.
     * @throws ServletException If there is a exception whilst performing the authentication or when calling the
     *                          FilterChain.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if ((!HttpServletRequest.class.isAssignableFrom(servletRequest.getClass())
                || !HttpServletResponse.class.isAssignableFrom(servletResponse.getClass()))) {
            throw new ServletException("Unsupported protocol");
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Prepare the message info
        String layer = MESSAGE_LAYER;
        String appContext = getAppContext(new URL(request.getRequestURL().toString()), request.getContextPath());
        MessageInfo messageInfo = prepareMessageInfo(request, response);

        // Get the provider
        AuthConfigProvider authConfigProvider = AuthConfigFactoryImpl.getInstance().getConfigProvider(layer, appContext,
                null);

        if (authConfigProvider == null) {
            DEBUG.debug("No AuthConfigProvider found with layer, {} and appContext, {}", layer, appContext);
            DEBUG.debug("Proceeding with filter chain.");
            filterChain.doFilter(request, response);
            return;
        }

        CallbackHandler handler = null;

        try {
            // Retrieve the appropriate config and obtain a context
            ServerAuthConfig serverAuthConfig = authConfigProvider.getServerAuthConfig(layer, appContext, handler);

            // Can be null if no details required for service subject.
            final Subject serviceSubject = null;
            final String authContextID = serverAuthConfig.getAuthContextID(messageInfo);

            // Now get the context that will execute the modules
            ServerAuthContext serverAuthContext = serverAuthConfig.getAuthContext(authContextID, serviceSubject,
                    Collections.emptyMap());

            // Could be null if no modules found
            if (serverAuthContext == null) {
                DEBUG.debug("No Authentication Modules found for authContextID, {}", authContextID);
                DEBUG.debug("Proceeding with filter chain.");
                filterChain.doFilter(request, response);
                return;
            }

            // Must create new client Subject.
            final Subject clientSubject = new Subject();

            // This is where the modules are called
            AuthStatus requestAuthStatus = serverAuthContext.validateRequest(messageInfo, clientSubject,
                    serviceSubject);

            if (SUCCESS.equals(requestAuthStatus)) {
                // nothing to do here just carry on
                DEBUG.debug("Successfully validated request.");
            } else if (SEND_SUCCESS.equals(requestAuthStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.debug("Successfully validated request, with response message");
                return;
            } else if (SEND_FAILURE.equals(requestAuthStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.debug("Failed to validate request, included response message.");
                response.setStatus(401);
                return;
            } else if (SEND_CONTINUE.equals(requestAuthStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.debug("Has not finished validating request. Requires more information from client.");
                return;
            } else {
                DEBUG.error("Invalid AuthStatus, {}", requestAuthStatus.toString());
                throw new AuthException("Invalid AuthStatus from validateRequest: " + requestAuthStatus.toString());
            }

            filterChain.doFilter((ServletRequest) messageInfo.getRequestMessage(),
                    (ServletResponse) messageInfo.getResponseMessage());

            AuthStatus responseAuthStatus = serverAuthContext.secureResponse(messageInfo, serviceSubject);
            //TODO does this fix the redirection/forwarding issue that could occur here too???
                // If any filter/resource closes the response or redirects the response the secureResponse call cannot
                // add anything else to the response. Because of this the call to secureResponse has been moved before
                // the doFilter call to get around this issue.

            if (SEND_SUCCESS.equals(responseAuthStatus)) {
                // nothing to do here just carry on
                DEBUG.debug("Successfully secured response.");
            } else if (SEND_FAILURE.equals(responseAuthStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.debug("Failed to secured response, included response message");
                response.setStatus(500);
                return;
            } else if (SEND_CONTINUE.equals(responseAuthStatus)) {
                // Send HttpServletResponse to client and exit.
                DEBUG.debug("Has not finished securing response. Requires more information from client.");
                response.setStatus(100);
                return;
            } else {
                DEBUG.error("Invalid AuthStatus, {}", requestAuthStatus.toString());
                throw new AuthException("Invalid AuthStatus from secureResponse: " + responseAuthStatus.toString());
            }

        } catch (AuthException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    /**
     * Creates a MessageInfo instance containing the HttpServletRequest and HttpServletResponse.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @return A MessageInfo instance.
     */
    private MessageInfo prepareMessageInfo(HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(MODULE_CONFIGURATION_PROPERTY, moduleConfiguration);

        MessageInfo messageInfo = new MessageInfoImpl(messageProperties);
        messageInfo.setRequestMessage(request);
        messageInfo.setResponseMessage(response);

        return messageInfo;
    }

    /**
     * Uses the URL to create the Application Context form the hostname and path of the URL.
     *
     * i.e http://openam.forgerock.org/openam/rest would result in the following string being returned:
     *      "openam.forgerock.org /openam".
     *
     * @param url The URL the the request is for.
     * @param contextPath The contextual path of the web application
     * @return The application context derived from the URL.
     */
    private String getAppContext(URL url, String contextPath) {
        return url.getHost() + " " + contextPath;
    }

    /**
     * Called by the web container when the filter is being taken out of service.
     *
     * Not required to be implemented for this filter.
     */
    public void destroy() {
    }
}
