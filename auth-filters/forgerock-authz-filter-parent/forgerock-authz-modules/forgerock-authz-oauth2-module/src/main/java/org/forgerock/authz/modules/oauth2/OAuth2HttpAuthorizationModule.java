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
import org.forgerock.authz.filter.http.api.HttpAuthorizationModule;
import org.forgerock.services.context.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>An implementation of a {@code OAuth2HttpAuthorizationModule} which uses a OAuth2 Access Token from the
 * {@code Authorization} header from the request.</p>
 *
 * @since 1.5.0
 */
public class OAuth2HttpAuthorizationModule implements HttpAuthorizationModule {

    private final Logger logger = LoggerFactory.getLogger(OAuth2HttpAuthorizationModule.class);

    private final OAuth2Module oAuth2Module;
    private final BearerTokenExtractor bearerTokenExtractor;

    /**
     * Creates a new {@code OAuth2HttpAuthorizationModule}.
     *
     * @param oAuth2Module A {@code OAuth2Module} instance.
     * @param bearerTokenExtractor A {@code BearerTokenExtractor} instance.
     */
    OAuth2HttpAuthorizationModule(OAuth2Module oAuth2Module, BearerTokenExtractor bearerTokenExtractor) {
        this.oAuth2Module = oAuth2Module;
        this.bearerTokenExtractor = bearerTokenExtractor;
    }

    @Override
    public String getName() {
        return "OAuth2";
    }

    /**
     * <p>Authorizes a received CHF HTTP request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param authorizationContext {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, AuthorizationException> authorize(Context context, Request request,
            AuthorizationContext authorizationContext) {
        return oAuth2Module.authorize(getAccessToken(request), authorizationContext);
    }

    /**
     * Pulls the access token off of the request, by looking for the Authorization header containing a Bearer token.
     *
     * @param req The HTTP Request.
     * @return The access token, or <code>null</code> if the access token was not present or was not using Bearer
     * authorization.
     */
    private String getAccessToken(final Request req) {
        String header = req.getHeaders().getFirst("Authorization");
        return bearerTokenExtractor.getAccessToken(header);
    }
}
