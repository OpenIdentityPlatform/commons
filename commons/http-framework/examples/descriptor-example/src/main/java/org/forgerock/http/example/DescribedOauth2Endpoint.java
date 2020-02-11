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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.http.example;

import static org.forgerock.http.protocol.Status.*;
import static org.forgerock.http.routing.RouteMatchers.*;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.header.LocationHeader;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Router;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.swagger.models.Swagger;
import io.swagger.util.DeserializationModule;

/**
 * A described CHF Handler that implements a crude OAuth 2.0 AS. Exposes the following endpoints:
 * <ul>
 *     <li>{@code /api} - an API method that returns the username associated with the bearer token</li>
 *     <li>{@code /authorize} - the standard OAuth 2.0 authorization endpoint</li>
 *     <li>{@code /login} - logs the user in through HTTP basic authorization</li>
 *     <li>{@code /token} - the standard OAuth 2.0 token endpoint</li>
 * </ul>
 */
public class DescribedOauth2Endpoint implements DescribableHandler {

    private static final Swagger DESCRIPTOR;

    static {
        try {
            DESCRIPTOR = new ObjectMapper(new YAMLFactory()).registerModule(new DeserializationModule()).readValue(
                    DescribedOauth2Endpoint.class.getResourceAsStream("DescribedOAuth2Endpoint.openapi.yaml"),
                    Swagger.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<String, JsonValue> userCodes = new HashMap<>();
    private final Map<String, String> userTokens = new HashMap<>();
    private final Router router;
    private Swagger descriptor;

    /**
     * Create the endpoint.
     */
    public DescribedOauth2Endpoint() {
        this.router = new Router();
        router.addRoute(requestUriMatcher(EQUALS, "authorize"), new Handler() {
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                if (!"GET".equals(request.getMethod())) {
                    return newResultPromise(new Response(METHOD_NOT_ALLOWED));
                }
                String redirectUri = request.getForm().getFirst("redirect_uri");
                if (redirectUri == null) {
                    return newResultPromise(new Response(BAD_REQUEST));
                }
                String state = request.getForm().getFirst("state");
                if (!"code".equals(request.getForm().getFirst("response_type"))) {
                    return error(redirectUri, "unsupported_response_type", state);
                }
                if (!"myclient".equals(request.getForm().getFirst("client_id"))) {
                    return error(redirectUri, "unauthorized_client", state);
                }
                String user = request.getForm().getFirst("user");
                if (user != null) {
                    String code = UUID.randomUUID().toString();
                    userCodes.put(code, json(object(field("user", user), field("redirect_uri", redirectUri))));
                    return code(redirectUri, code, state);
                }
                Response response = new Response(FOUND);
                response.getHeaders().add(new LocationHeader("login?" + request.getForm().toQueryString()));
                return newResultPromise(response);
            }

            private Promise<Response, NeverThrowsException> error(String redirectUri, String error, String state) {
                return redirect(redirectUri, state, "error=" + error);
            }

            private Promise<Response, NeverThrowsException> code(String redirectUri, String code, String state) {
                return redirect(redirectUri, state, "code=" + code);
            }

            private Promise<Response, NeverThrowsException> redirect(String redirectUri, String state, String suffix) {
                Response response = new Response(FOUND);
                String uri = redirectUri + (redirectUri.contains("?") ? "&" : "?");
                if (state != null) {
                    uri += "state=" + state + "&";
                }
                response.getHeaders().add(new LocationHeader(uri + suffix));
                return newResultPromise(response);
            }
        });
        router.addRoute(requestUriMatcher(EQUALS, "login"), new Handler() {
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                String authValue = getAuthorizationHeader(request);
                if (authValue != null && authValue.startsWith("Basic ")) {
                    String[] creds = new String(Base64.decode(authValue.substring(6))).split(":");
                    if (creds[0].equals(creds[1])) {
                        Response response = new Response(FOUND);
                        response.getHeaders().add(new LocationHeader(
                                "authorize?user=" + creds[0] + "&" + request.getForm().toQueryString()));
                        return newResultPromise(response);
                    }
                }
                Response response = new Response(UNAUTHORIZED);
                response.getHeaders().add("WWW-Authenticate", "Basic realm=\"Really Secure OAuth2\"");
                return newResultPromise(response);
            }
        });
        router.addRoute(requestUriMatcher(EQUALS, "token"), new Handler() {
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                if (!"POST".equals(request.getMethod())) {
                    return newResultPromise(new Response(METHOD_NOT_ALLOWED));
                }
                if (!"myclient".equals(request.getForm().getFirst("client_id"))
                        || !"password".equals(request.getForm().getFirst("client_secret"))) {
                    return error("invalid_client");
                }
                if (!"authorization_code".equals(request.getForm().getFirst("grant_type"))) {
                    return error("unsupported_grant_type");
                }
                String code = request.getForm().getFirst("code");
                if (code == null) {
                    return error("invalid_request");
                }
                JsonValue codeDetails = userCodes.remove(code);
                if (codeDetails == null) {
                    return error("invalid_grant");
                }
                if (!codeDetails.get("redirect_uri").asString().equals(request.getForm().getFirst("redirect_uri"))) {
                    return error("invalid_request");
                }
                String token = UUID.randomUUID().toString();
                userTokens.put(token, codeDetails.get("user").asString());
                return newResultPromise(new Response(OK).setEntity(json(object(
                        field("access_token", token),
                        field("token_type", "bearer")
                ))));
            }

            private Promise<Response, NeverThrowsException> error(String error) {
                return newResultPromise(new Response(BAD_REQUEST).setEntity(json(object(field("error", error)))));
            }
        });
        router.addRoute(requestUriMatcher(EQUALS, "api"), new Handler() {
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                if (!"GET".equals(request.getMethod())) {
                    return newResultPromise(new Response(METHOD_NOT_ALLOWED));
                }
                String authValue = getAuthorizationHeader(request);
                if (authValue != null && authValue.startsWith("Bearer ")) {
                    String user = userTokens.get(authValue.substring(7));
                    if (user != null) {
                        return newResultPromise(new Response(OK).setEntity(json(object(field("user", user)))));
                    }
                }
                return newResultPromise(new Response(Status.UNAUTHORIZED));
            }
        });

    }

    private String getAuthorizationHeader(Request request) {
        Header authHeader = request.getHeaders().get("Authorization");
        String authValue = null;
        if (authHeader != null) {
            authValue = authHeader.getFirstValue();
        }
        return authValue;
    }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        return router.handle(context, request);
    }

    @Override
    public Swagger api(ApiProducer<Swagger> producer) {
        descriptor = producer.addApiInfo(DESCRIPTOR);
        return descriptor;
    }

    @Override
    public Swagger handleApiRequest(Context context, Request request) {
        return descriptor;
    }

    @Override
    public void addDescriptorListener(Listener listener) {

    }

    @Override
    public void removeDescriptorListener(Listener listener) {

    }
}
