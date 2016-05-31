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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.oauth2.resolver;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.forgerock.util.Utils.closeSilently;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.forgerock.http.oauth2.AccessTokenException;
import org.forgerock.http.oauth2.AccessTokenInfo;
import org.forgerock.http.oauth2.AccessTokenResolver;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Entity;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.time.TimeService;

/**
 * An {@link OpenAmAccessTokenResolver} knows how to resolve a given token identifier against an OpenAm instance.
 * <p>
 * Models an {@link AccessTokenInfo} as returned by the OpenAM {@literal tokeninfo} endpoint.
 * <pre>
 *     {@code
 *     curl https://openam.example.com:8443/openam/oauth2/tokeninfo?access_token=70e5776c-b0fa-4c70-9962-defb0e9c3cd6
 *     }
 * </pre>
 *
 * Example of OpenAM returned Json value (for the previous request):
 * <pre>
 *     {@code
 *     {
 *         "scope": [
 *             "email",
 *             "profile"
 *         ],
 *         "grant_type": "password",
 *         "realm": "/",
 *         "token_type": "Bearer",
 *         "expires_in": 471,
 *         "access_token": "70e5776c-b0fa-4c70-9962-defb0e9c3cd6",
 *         "email": "",
 *         "profile": ""
 *     }
 *     }
 * </pre>
 */
public class OpenAmAccessTokenResolver implements AccessTokenResolver {

    private final Handler client;
    private final String tokenInfoEndpoint;
    private final Function<JsonValue, AccessTokenInfo, AccessTokenException> accessToken;

    /**
     * Creates a new {@link OpenAmAccessTokenResolver} configured to access the given {@literal /oauth2/tokeninfo}
     * OpenAm endpoint.
     *
     * @param client
     *         Http client handler used to perform the request
     * @param time
     *         Time service used to compute the token expiration time
     * @param tokenInfoEndpoint
     *         full URL of the {@literal /oauth2/tokeninfo} endpoint
     */
    public OpenAmAccessTokenResolver(final Handler client,
                                     final TimeService time,
                                     final String tokenInfoEndpoint) {
        this(client, new TokenInfoParser(time), tokenInfoEndpoint);
    }

    private OpenAmAccessTokenResolver(final Handler client,
                                      final Function<JsonValue, AccessTokenInfo, AccessTokenException> accessToken,
                                      final String tokenInfoEndpoint) {
        this.client = client;
        this.accessToken = accessToken;
        this.tokenInfoEndpoint = tokenInfoEndpoint;
    }

    @Override
    public Promise<AccessTokenInfo, AccessTokenException> resolve(Context context, final String token) {
        try {
            Request request = new Request();
            request.setMethod("GET");
            request.setUri(new URI(tokenInfoEndpoint));

            // Append the access_token as a query parameter (automatically performs encoding)
            Form form = new Form();
            form.add("access_token", token);
            form.toRequestQuery(request);

            // Call the client handler
            return client.handle(context, request)
                         .then(onResult(), Responses.<AccessTokenInfo, AccessTokenException>noopExceptionFunction());
        } catch (URISyntaxException e) {
            return Promises.newExceptionPromise(new AccessTokenException(
                    format("The token_info endpoint %s could not be accessed because it is a malformed URI",
                           tokenInfoEndpoint),
                    e));
        }
    }

    private Function<Response, AccessTokenInfo, AccessTokenException> onResult() {
        return new Function<Response, AccessTokenInfo, AccessTokenException>() {
            @Override
            public AccessTokenInfo apply(Response response) throws AccessTokenException {
                if (isResponseEmpty(response)) {
                    throw new AccessTokenException("Authorization Server did not return any AccessToken");
                }
                JsonValue content = asJson(response.getEntity());
                if (isOk(response)) {
                    return content.as(accessToken);
                }

                if (content.isDefined("error")) {
                    String error = content.get("error").asString();
                    String description = content.get("error_description").asString();
                    throw new AccessTokenException(format("Authorization Server returned an error "
                                                                  + "(error: %s, description: %s)",
                                                          error,
                                                          description));
                }

                throw new AccessTokenException("AccessToken returned by the AuthorizationServer has a problem");
            }
        };
    }

    private boolean isResponseEmpty(final Response response) {
        // response.entity is NEVER null !!!
        return response == null || response.getEntity() == null;
    }

    private boolean isOk(final Response response) {
        return Status.OK.equals(response.getStatus());
    }

    /**
     * Parse the response's content as a JSON structure.
     * @param entity stream response's content
     * @return {@link JsonValue} representing the JSON content
     * @throws AccessTokenException if there was some errors during parsing
     */
    private JsonValue asJson(final Entity entity) throws AccessTokenException {
        try {
            return new JsonValue(entity.getJson());
        } catch (IOException e) {
            // Do not use Entity.toString(), we probably don't want to fully output the content here
            throw new AccessTokenException("Cannot read response content as JSON", e);
        } finally {
            closeSilently(entity);
        }
    }

    @VisibleForTesting
    static class TokenInfoParser implements Function<JsonValue, AccessTokenInfo, AccessTokenException> {

        private final TimeService time;

        /**
         * Creates a new parser with the given {@link TimeService}.
         *
         * @param time
         *         time service used to compute the expiration date
         */
        TokenInfoParser(final TimeService time) {
            this.time = time;
        }

        /**
         * Creates a new {@link AccessTokenInfo} from a raw JSON response returned by the {@literal /oauth2/tokeninfo}
         * endpoint.
         *
         * @param raw
         *         JSON response
         * @return a new {@link AccessTokenInfo}
         * @throws AccessTokenException
         *         if the JSON response is not formatted correctly.
         */
        @Override
        public AccessTokenInfo apply(final JsonValue raw) throws AccessTokenException {
            try {
                final long expiresIn = raw.get("expires_in").required().asLong();
                final Set<String> scopes = raw.get("scope").required().asSet(String.class);
                final String token = raw.get("access_token").required().asString();

                return new AccessTokenInfo(raw, token, scopes, getExpirationTime(expiresIn));
            } catch (JsonValueException e) {
                throw new AccessTokenException("Cannot build AccessToken from the given JSON: invalid format", e);
            }
        }

        private long getExpirationTime(final long delayInSeconds) {
            return time.now() + MILLISECONDS.convert(delayInSeconds, SECONDS);
        }
    }
}
