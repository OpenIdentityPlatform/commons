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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import java.util.Map;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.SecurityContext;

/**
 * An HTTP context factory which will create a {@link SecurityContext} whose
 * authentication ID and authorization ID are taken from attributes contained
 * in the HTTP request.
 * <p>
 * This class provides integration with the common authentication framework and
 * is intended to work as follows:
 * <ol>
 * <li>An incoming HTTP request is first intercepted by a HTTP filter
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
 * <li>The JSON Resource Handler uses the {@code SecurityContextFactory} to
 * obtain the authentication ID and authorization principals from the HTTP
 * request's attributes.
 * </ol>
 * The following code illustrates how an authentication HTTP filter can
 * populate the attributes:
 *
 * <pre>
 * {@code
 * public Promise<Response, ResponseException> filter(Context context, Request request, Handler next) {
 *     // Authenticate the user.
 *     String authcid = getUserName(request);
 *     String password = getPassword(request);
 *
 *     // Add the attributes.
 *     if (checkCredentials(authcid, password)) {
 *         // Obtain principals for authorization.
 *         Map<String, Object> authzid = new HashMap<>();
 *         authzid.put(AUTHZID_ID, id);
 *         ...
 *
 *         AttributesContext attributesContext = context.asContext(AttributesContext.class);
 *         attributesContext.getAttributes().put(ATTRIBUTE_AUTHCID, authcid);
 *         attributesContext.getAttributes().put(ATTRIBUTE_AUTHZID, authzid);
 *     }
 * }
 * }
 * </pre>
 *
 * @deprecated This class will be removed once CAF has been migrated fully to CHF, at which point components should
 * create {@link SecurityContext}s directly rather than via request attributes.
 */
@Deprecated
public final class SecurityContextFactory implements HttpContextFactory {

    /**
     * The name of the HTTP Request attribute where this factory expects to
     * find the authenticated user's authentication ID. The name of this
     * attribute is {@code org.forgerock.authentication.principal} and it MUST
     * contain a {@code String} if it is present.
     *
     * @see SecurityContext#getAuthenticationId()
     */
    public static final String ATTRIBUTE_AUTHCID = "org.forgerock.authentication.principal";

    /**
     * The name of the HTTP Request attribute where this factory expects to
     * find the authenticated user's authorization ID. The name of this
     * attribute is {@code org.forgerock.authentication.context} and it MUST
     * contain a {@code Map<String, Object>} if it is present.
     *
     * @see SecurityContext#getAuthorization()
     */
    public static final String ATTRIBUTE_AUTHZID = "org.forgerock.authentication.context";

    // Singleton instance.
    private static final SecurityContextFactory INSTANCE = new SecurityContextFactory();

    /**
     * Returns the singleton security context factory which can be used for
     * obtaining context information from a HTTP request.
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
     * the provided HTTP request. The authentication ID will be obtained from
     * the {@link #ATTRIBUTE_AUTHCID} attribute, and the authorization ID will
     * be obtained from the {@link #ATTRIBUTE_AUTHCID} attribute.
     * <p>
     * It is not an error if either of the attributes are not present, but a
     * {@link ResourceException} will be thrown if they are present but have the
     * wrong type.
     *
     * @param parent
     *            The parent context.
     * @return A security context initialized using the attributes contained in
     *         the provided HTTP request.
     * @throws ResourceException
     *             If one of the attributes was present but had the wrong type.
     */
    public SecurityContext createContext(Context parent) throws ResourceException {
        AttributesContext attributesContext = parent.asContext(AttributesContext.class);
        String authcid = getAuthenticationIdAttribute(ATTRIBUTE_AUTHCID, attributesContext);
        Map<String, Object> authzid = getAuthorizationIdAttribute(ATTRIBUTE_AUTHZID, attributesContext);
        return new SecurityContext(parent, authcid, authzid);
    }

    private String getAuthenticationIdAttribute(String attributeName, AttributesContext context)
            throws InternalServerErrorException {
        try {
            return (String) context.getAttributes().get(attributeName);
        } catch (final ClassCastException e) {
            throw new InternalServerErrorException(
                    "The security context could not be created because the "
                            + "authentication ID attribute, " + attributeName
                            + ", contained in the HTTP request did not have "
                            + "the correct type", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAuthorizationIdAttribute(String attributeName,
            AttributesContext context) throws InternalServerErrorException {
        try {
            return (Map<String, Object>) context.getAttributes().get(attributeName);
        } catch (final ClassCastException e) {
            throw new InternalServerErrorException(
                    "The security context could not be created because the "
                            + "authorization ID attribute, " + attributeName
                            + ", contained in the HTTP request did not have "
                            + "the correct type", e);
        }
    }

    /**
     * Creates a new {@code SecurityContext} using the attributes contained in
     * the provided HTTP request. The authentication ID will be obtained from
     * the {@link #ATTRIBUTE_AUTHCID} attribute, and the authorization ID will
     * be obtained from the {@link #ATTRIBUTE_AUTHCID} attribute.
     * <p>
     * It is not an error if either of the attributes are not present, but a
     * {@link ResourceException} will be thrown if they are present but have the
     * wrong type.
     *
     * @param context
     *            The parent context.
     * @param request
     *            The HTTP request from which the authentication ID and
     *            authorization ID attributes should be obtained.
     * @return A security context initialized using the attributes contained in
     *         the provided HTTP request.
     * @throws ResourceException
     *             If one of the attributes was present but had the wrong type.
     */
    @Override
    public SecurityContext createContext(Context context, org.forgerock.http.protocol.Request request)
            throws ResourceException {
        return createContext(context);
    }
}
