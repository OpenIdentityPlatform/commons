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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;

/**
 * An HTTP servlet context factory which will create a {@link SecurityContext}
 * whose authentication ID and authorization ID are taken from attributes
 * contained in the HTTP servlet request.
 * <p>
 * This class provides integration with the common authentication framework and
 * is intended to work as follows:
 * <ol>
 * <li>An incoming HTTP request is first intercepted by a Servlet filter
 * responsible for authenticating the request.
 * <li>If authentication is successful, the authentication filter determines the
 * set of principals associated with the user which may be required in order to
 * perform authorization. These principals may include the user's unique ID,
 * realm, groups, roles, or LDAP DN, etc.
 * <li>The authentication filter constructs a {@code Map<String, Object>}
 * containing the principals keyed on the principal name. <b>NOTE:</b> various
 * reserved principal names are defined in {@link SecurityContext}.
 * <li>The authentication filter stores the authentication ID (the name which
 * the user identified themselves with during authentication) in the HTTP
 * servlet request's {@link #ATTRIBUTE_AUTHCID} attribute.
 * <li>The authentication filter stores the {@code Map} containing the
 * authorization principals in the HTTP servlet request's
 * {@link #ATTRIBUTE_AUTHZID} attribute.
 * <li>The JSON Resource Servlet uses the {@code SecurityContextFactory} to
 * obtain the authentication ID and authorization principals from the HTTP
 * servlet request's attributes.
 * </ol>
 * The following code illustrates how an authentication Servlet filter can
 * populate the attributes:
 *
 * <pre>
 * public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
 *     // Authenticate the user.
 *     String authcid = getUserName(request);
 *     String password = getPassword(request);
 *
 *     // Add the attributes.
 *     if (checkCredentials(authcid, password)) {
 *         // Obtain principals for authorization.
 *         Map<String, Object> authzid = new HashMap<String, Object>();
 *         authzid.put(AUTHZID_ID, id);
 *         ...
 *
 *         request.setAttribute(ATTRIBUTE_AUTHCID, authcid);
 *         request.setAttribute(ATTRIBUTE_AUTHZID, authzid);
 *     }
 * }
 * </pre>
 */
public final class SecurityContextFactory implements HttpServletContextFactory {

    /**
     * The value of {@link #ATTRIBUTE_AUTHCID} used in CREST versions 2.0.0 -
     * 2.0.2. See the description of {@link #ATTRIBUTE_AUTHCID} for more
     * information.
     *
     * @deprecated This version of the attribute was used in CREST versions
     *             2.0.0 - 2.0.2 and deprecated in order to align with the
     *             attribute names used by the common JASPI authentication
     *             project. Applications should use
     *             {@link #ATTRIBUTE_AUTHCID_V2} instead.
     */
    @Deprecated
    public static final String ATTRIBUTE_AUTHCID_V1 = "org.forgerock.security.authcid";

    /**
     * The most recent value of {@link #ATTRIBUTE_AUTHCID}. See the description
     * of {@link #ATTRIBUTE_AUTHCID} for more information.
     */
    public static final String ATTRIBUTE_AUTHCID_V2 = "org.forgerock.authentication.principal";

    /**
     * The name of the HTTP Servlet Request attribute where this factory expects
     * to find the authenticated user's authentication ID. The name of this
     * attribute is {@code org.forgerock.authentication.principal} and it MUST
     * contain a {@code String} if it is present.
     * <p>
     * This constant has the same value as {@link #ATTRIBUTE_AUTHCID_V2}.
     *
     * @see SecurityContext#getAuthenticationId()
     */
    public static final String ATTRIBUTE_AUTHCID = ATTRIBUTE_AUTHCID_V2;

    /**
     * The value of {@link #ATTRIBUTE_AUTHZID} used in CREST versions 2.0.0 -
     * 2.0.2. See the description of {@link #ATTRIBUTE_AUTHZID} for more
     * information.
     *
     * @deprecated This version of the attribute was used in CREST versions
     *             2.0.0 - 2.0.2 and deprecated in order to align with the
     *             attribute names used by the common JASPI authentication
     *             project. Applications should use
     *             {@link #ATTRIBUTE_AUTHZID_V2} instead.
     */
    @Deprecated
    public static final String ATTRIBUTE_AUTHZID_V1 = "org.forgerock.security.authzid";

    /**
     * The most recent value of {@link #ATTRIBUTE_AUTHZID}. See the description
     * of {@link #ATTRIBUTE_AUTHZID} for more information.
     */
    public static final String ATTRIBUTE_AUTHZID_V2 = "org.forgerock.authentication.context";

    /**
     * The name of the HTTP Servlet Request attribute where this factory expects
     * to find the authenticated user's authorization ID. The name of this
     * attribute is {@code org.forgerock.authentication.context} and it MUST
     * contain a {@code Map<String, Object>} if it is present.
     * <p>
     * This constant has the same value as {@link #ATTRIBUTE_AUTHZID_V2}.
     * 
     * @see SecurityContext#getAuthorizationId()
     */
    public static final String ATTRIBUTE_AUTHZID = ATTRIBUTE_AUTHZID_V2;

    // Singleton instance.
    private static final SecurityContextFactory INSTANCE = new SecurityContextFactory();

    /**
     * Returns the singleton security context factory which can be used for
     * obtaining context information from a HTTP servlet request.
     * <p>
     * This method is named {@code getHttpServletContextFactory} so that it can
     * easily be used for {@link HttpServlet#getHttpServletContextFactory
     * configuring} JSON Resource Servlets.
     *
     * @return The singleton security context factory.
     */
    public static SecurityContextFactory getHttpServletContextFactory() {
        return INSTANCE;
    }

    private SecurityContextFactory() {
        // Prevent instantiation.
    }

    /**
     * Creates a new {@code SecurityContext} using the attributes contained in
     * the provided HTTP servlet request. The authentication ID will be obtained
     * from the {@link #ATTRIBUTE_AUTHCID} attribute, and the authorization ID
     * will be obtained from the {@link #ATTRIBUTE_AUTHCID} attribute.
     * <p>
     * It is not an error if either of the attributes are not present, but a
     * {@link ResourceException} will be thrown if they are present but have the
     * wrong type.
     *
     * @param parent
     *            The parent context.
     * @param request
     *            The HTTP servlet request from which the authentication ID and
     *            authorization ID attributes should be obtained.
     * @return A security context initialized using the attributes contained in
     *         the provided HTTP servlet request.
     * @throws ResourceException
     *             If one of the attributes was present but had the wrong type.
     */
    public SecurityContext createContext(final Context parent, final HttpServletRequest request)
            throws ResourceException {
        String authcid = getAuthenticationIdAttribute(ATTRIBUTE_AUTHCID, request);
        if (authcid == null) {
            // Check legacy attribute name.
            authcid = getAuthenticationIdAttribute(ATTRIBUTE_AUTHCID_V1, request);
        }
        Map<String, Object> authzid = getAuthorizationIdAttribute(ATTRIBUTE_AUTHZID, request);
        if (authzid == null) {
            // Check legacy attribute name.
            authzid = getAuthorizationIdAttribute(ATTRIBUTE_AUTHZID_V1, request);
        }
        return new SecurityContext(parent, authcid, authzid);
    }

    private String getAuthenticationIdAttribute(final String attributeName,
            final HttpServletRequest request) throws InternalServerErrorException {
        try {
            return (String) request.getAttribute(attributeName);
        } catch (final ClassCastException e) {
            throw new InternalServerErrorException(
                    "The security context could not be created because the "
                            + "authentication ID attribute, " + attributeName
                            + ", contained in the HTTP servlet request did "
                            + "not have the correct type", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAuthorizationIdAttribute(final String attributeName,
            final HttpServletRequest request) throws InternalServerErrorException {
        try {
            return (Map<String, Object>) request.getAttribute(attributeName);
        } catch (final ClassCastException e) {
            throw new InternalServerErrorException(
                    "The security context could not be created because the "
                            + "authorization ID attribute, " + attributeName
                            + ", contained in the HTTP servlet request did "
                            + "not have the correct type", e);
        }
    }

    /**
     * Creates a new {@code SecurityContext} using the attributes contained in
     * the provided HTTP servlet request. The authentication ID will be obtained
     * from the {@link #ATTRIBUTE_AUTHCID} attribute, and the authorization ID
     * will be obtained from the {@link #ATTRIBUTE_AUTHCID} attribute.
     * <p>
     * It is not an error if either of the attributes are not present, but a
     * {@link ResourceException} will be thrown if they are present but have the
     * wrong type.
     *
     * @param request
     *            The HTTP servlet request from which the authentication ID and
     *            authorization ID attributes should be obtained.
     * @return A security context initialized using the attributes contained in
     *         the provided HTTP servlet request.
     * @throws ResourceException
     *             If one of the attributes was present but had the wrong type.
     */
    @Override
    public SecurityContext createContext(final HttpServletRequest request) throws ResourceException {
        return createContext(new RootContext(), request);
    }

}
