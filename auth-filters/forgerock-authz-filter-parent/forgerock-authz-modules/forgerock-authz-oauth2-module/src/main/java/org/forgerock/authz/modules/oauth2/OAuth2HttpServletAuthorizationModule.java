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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>An implementation of a {@code OAuth2HttpServletAuthorizationModule} which uses a OAuth2 Access Token from the
 * {@code Authorization} header from the request.</p>
 *
 * @since 1.5.0
 */
public class OAuth2HttpServletAuthorizationModule implements HttpServletAuthorizationModule {

    private final Logger logger = LoggerFactory.getLogger(OAuth2HttpServletAuthorizationModule.class);

    private final OAuth2Module oAuth2Module;
    private final BearerTokenExtractor bearerTokenExtractor;

    /**
     * Creates a new {@code OAuth2HttpServletAuthorizationModule}.
     *
     * @param oAuth2Module A {@code OAuth2Module} instance.
     * @param bearerTokenExtractor A {@code BearerTokenExtractor} instance.
     */
    OAuth2HttpServletAuthorizationModule(OAuth2Module oAuth2Module, BearerTokenExtractor bearerTokenExtractor) {
        this.oAuth2Module = oAuth2Module;
        this.bearerTokenExtractor = bearerTokenExtractor;
    }

    /**
     * <p>Authorizes a received HTTP Servlet request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param req {@inheritDoc}
     * @param context {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, AuthorizationException> authorize(HttpServletRequest req,
            AuthorizationContext context) {
        return oAuth2Module.authorize(getAccessToken(req), context);
    }

    /**
     * Pulls the access token off of the request, by looking for the Authorization header containing a Bearer token.
     *
     * @param req The HttpServletRequest.
     * @return The access token, or <code>null</code> if the access token was not present or was not using Bearer
     * authorization.
     */
    private String getAccessToken(final HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        return bearerTokenExtractor.getAccessToken(header);
    }
}
