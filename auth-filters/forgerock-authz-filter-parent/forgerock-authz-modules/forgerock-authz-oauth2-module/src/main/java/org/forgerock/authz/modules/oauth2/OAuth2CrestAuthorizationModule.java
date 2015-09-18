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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * <p>An implementation of a {@code CrestAuthorizationModule} which uses a OAuth2 Access Token from the
 * {@code Authorization} header from the request.</p>
 *
 * @since 1.5.0
 */
public class OAuth2CrestAuthorizationModule implements CrestAuthorizationModule {

    private final OAuth2Module oAuth2Module;
    private final BearerTokenExtractor bearerTokenExtractor;

    /**
     * Creates a new {@code OAuth2CrestAuthorizationModule}.
     *
     * @param oAuth2Module A {@code OAuth2Module} instance.
     * @param bearerTokenExtractor A {@code BearerTokenExtractor} instance.
     */
    OAuth2CrestAuthorizationModule(OAuth2Module oAuth2Module, BearerTokenExtractor bearerTokenExtractor) {
        this.oAuth2Module = oAuth2Module;
        this.bearerTokenExtractor = bearerTokenExtractor;
    }

    @Override
    public String getName() {
        return "OAuth2";
    }

    /**
     * <p>Authorizes a received REST create request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context,
            CreateRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * <p>Authorizes a received REST read request using the OAuth2 Access Token present in the request header.</p>
     *
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * <p>Authorizes a received REST update request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context,
            UpdateRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * <p>Authorizes a received REST delete request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(Context context,
            DeleteRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * <p>Authorizes a received REST patch request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * <p>Authorizes a received REST action request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context,
            ActionRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * <p>Authorizes a received REST query request using the OAuth2 Access Token present in the request header.</p>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest request) {

        String accessToken = getAccessToken(context);
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        AuthorizationContext authorizationContext = new AuthorizationContext();

        return authorizeOAuth2Token(accessToken, securityContext, authorizationContext);
    }

    /**
     * Pulls the access token off of the request, by looking for the Authorization header containing a Bearer token.
     *
     * @param context The {@link Context} representing the context of the request.
     * @return The access token, or <code>null</code> if the access token was not present or was not using Bearer
     * authorization.
     */
    private String getAccessToken(Context context) {
        HttpContext httpContext = context.asContext(HttpContext.class);
        String header = httpContext.getHeaderAsString("authorization");
        return bearerTokenExtractor.getAccessToken(header);
    }

    /**
     * Performs the call to the {@code OAuth2Module} to perform the authorization and handles the result asynchronously.
     *
     * @param accessToken The OAuth2 access token.
     * @param securityContext The {@link SecurityContext} of the request.
     * @param authorizationContext The {@link AuthorizationContext} holding all the authorization context for the
     *                             request.
     * @return A {@code Promise} with the result of the authorization and the handling of the result.
     */
    private Promise<AuthorizationResult, ResourceException> authorizeOAuth2Token(String accessToken,
            final SecurityContext securityContext, final AuthorizationContext authorizationContext) {

        return oAuth2Module.authorize(accessToken, authorizationContext)
            .thenAsync(
                    new AsyncFunction<AuthorizationResult, AuthorizationResult, ResourceException>() {
                        @Override
                        public Promise<AuthorizationResult, ResourceException> apply(AuthorizationResult result) {
//                            securityContext.getAuthorizationId().putAll(authorizationContext.getAttributes());
// TODO cannot add to the authzId as SecurityContext ensures this....
                            return Promises.newResultPromise(result);
                        }
                    }, new AsyncFunction<AuthorizationException, AuthorizationResult, ResourceException>() {
                        @Override
                        public Promise<AuthorizationResult, ResourceException> apply(AuthorizationException e) {
                            return Promises.newExceptionPromise(ResourceException.getException(
                                    ResourceException.INTERNAL_ERROR, e.getMessage(), e));
                        }
                    }
            );
    }
}
