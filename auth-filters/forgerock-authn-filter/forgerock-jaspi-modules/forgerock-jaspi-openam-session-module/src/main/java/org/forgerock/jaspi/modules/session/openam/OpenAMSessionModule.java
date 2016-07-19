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

package org.forgerock.jaspi.modules.session.openam;

import static java.lang.String.format;
import static javax.security.auth.message.AuthStatus.*;
import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.http.protocol.Responses.noopExceptionFunction;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.CloseSilentlyFunction.closeSilently;
import static org.forgerock.util.Utils.closeSilently;
import static org.forgerock.util.promise.Promises.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.RequestCookies;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;
import org.forgerock.util.Utils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * A JASPI Session Module which uses OpenAM to validate SSO Tokens issued by an OpenAM instance.
 * <br/>
 * Validates SSO Tokens by making REST calls to a OpenAM instance.
 *
 * @since 1.4.0
 */
public class OpenAMSessionModule implements AsyncServerAuthModule {

    private static final String JSON_REST_ROOT_ENDPOINT = "json";
    private static final String JSON_SESSIONS_RELATIVE_URI = JSON_REST_ROOT_ENDPOINT + "/sessions/";
    private static final String JSON_USERS_ENDPOINT = "users/";

    private static final String MIME_TYPE_APPLICATION_JSON = "application/json";
    private static final String CHARACTER_ENCODING = "UTF-8";

    private final Options httpClientOptions;

    private Client httpClient;

    private CallbackHandler handler;
    private String openamDeploymentUrl;
    private String openamSSOTokenCookieName;
    private String openamUserAttribute;

    /**
     * Construct OpenAMSessionModule - use default options.
     */
    public OpenAMSessionModule() {
        this.httpClientOptions = Options.defaultOptions();
    }

    /**
     * Cosntruct OpenAMSessionModule - use provide options for loading the HttpClientProvider.
     *
     * @param httpClientOptions
     *            The options which will be used to configure the HTTP client.
     */
    public OpenAMSessionModule(Options httpClientOptions) {
        this.httpClientOptions = httpClientOptions;
    }

    @Override
    public String getModuleId() {
        return "OpenAM Session";
    }

    /**
     * Will initialise the module with the specified configuration properties.
     * <br/>
     * <table>
     * <thead>
     * <tr><th>Property</th><th>Type</th><th>Required</th><th>Default Value</th><th>Description</th><th>Example</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><td>openamDeploymentUrl</td><td>String</td><td>Yes</td><td>N/A</td><td>The fully qualified URL of the OpenAM
     * deployment, including the context path</td><td>http://example.com:8080/openam/</td></tr>
     * <tr><td>openamSSOTokenCookieName</td><td>String</td><td>Yes</td><td>N/A</td><td>The name of the cookie used by
     * OpenAM to set the SSO Token Id</td><td>iPlanetDirectoryPro</td></tr>
     * <tr><td>trustManagerAlgorithm</td><td>String</td><td>When useSSL is true</td><td>SunX509</td><td>Certificate
     * algorithm for the trust manager</td><td>SunX509</td></tr>
     * <tr><td>truststorePath</td><td>String</td><td>When useSSL is true</td><td>N/A</td><td>The absolute path to the
     * location of the SSL Trust Store</td><td>/opt/truststore.jks</td></tr>
     * <tr><td>truststoreType</td><td>String</td><td>When useSSL is true</td><td>N/A</td><td>The type of the SSL Trust
     * Store</td><td>JKS</td></tr>
     * <tr><td>truststorePassword</td><td>String</td><td>When useSSL is true</td><td>N/A</td><td>The password for the
     * SSL Trust Store</td><td>cangetin</td></tr>
     * </tbody>
     * </table>
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param callbackHandler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws java.lang.IllegalArgumentException If any of the required configuration properties are not set.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler callbackHandler, Map<String, Object> options) {
        this.handler = callbackHandler;

        openamDeploymentUrl = (String) options.get("openamDeploymentUrl");
        openamSSOTokenCookieName = (String) options.get("openamSSOTokenCookieName");
        openamUserAttribute = (String) options.get("openamUserAttribute");
        Reject.ifTrue(isEmpty(openamDeploymentUrl), "openamDeploymentUrl property must be set.");
        Reject.ifTrue(isEmpty(openamSSOTokenCookieName), "openamSSOTokenCookieName property must be set.");
        Reject.ifTrue(isEmpty(openamUserAttribute), "openamUserAttribute property must be set.");

        if (!openamDeploymentUrl.endsWith("/")) {
            openamDeploymentUrl += "/";
        }
        LOG.debug("OpenAM configuration: Deployment URL = {}, SSO Token Cookie Name = {}", openamDeploymentUrl,
                openamSSOTokenCookieName);

        final boolean useSSL;
        try {
            useSSL = !"http".equals(URI.create(openamDeploymentUrl).toURL().getProtocol());
        } catch (MalformedURLException e) {
            return newExceptionPromise(new AuthenticationException(
                    new IllegalArgumentException("OpenAM Deployment URL malformed.")));
        }

        LOG.debug("Using SSL? {}", useSSL);
        try {
            if (useSSL) {
                configureSsl(options, httpClientOptions);
            }
            this.httpClient = createHttpClient(httpClientOptions);
        } catch (HttpApplicationException e) {
            return newExceptionPromise(new AuthenticationException("Failed to get HTTP Client", e));
        } catch (AuthenticationException e) {
            return newExceptionPromise(e);
        }
        return newResultPromise(null);
    }

    @VisibleForTesting
    Client createHttpClient(Options options) throws HttpApplicationException {
        return new Client(new HttpClientHandler(options));
    }

    /**
     * Configures the REST connections to use SSL.
     *
     * @param options The configuration options of the module.
     */
    private void configureSsl(Map<String, Object> options, Options httpClientOptions) throws AuthenticationException {

        final String trustStorePath = (String) options.get("truststorePath");
        final String trustStoreType = (String) options.get("truststoreType");
        final String trustStorePassword = (String) options.get("truststorePassword");

        Reject.ifTrue(isEmpty(trustStorePath), "truststorePath property must be set.");
        Reject.ifTrue(isEmpty(trustStoreType), "truststoreType property must be set.");
        Reject.ifTrue(isEmpty(trustStorePassword), "truststorePassword property must be set.");

        String trustManagerAlgorithm = (String) options.get("trustManagerAlgorithm");
        if (isEmpty(trustManagerAlgorithm)) {
            trustManagerAlgorithm = "SunX509";
        }

        LOG.debug("SSL configuration:  Trust Manager Algorithm = {}, Trust Store Path = {}, Trust Store Type = {}",
                trustManagerAlgorithm, trustStorePath, trustStoreType);

        TrustManager[] trustManagers = getTrustManagers(trustStorePath, trustStoreType, trustManagerAlgorithm,
                trustStorePassword);

        httpClientOptions.set(HttpClientHandler.OPTION_SSLCONTEXT_ALGORITHM, "TLS");
        httpClientOptions.set(HttpClientHandler.OPTION_TRUST_MANAGERS, trustManagers);
    }

    @VisibleForTesting
    TrustManager[] getTrustManagers(String truststoreFile, String type, String algorithm,
            String password) throws AuthenticationException {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
            KeyStore store = buildKeyStore(truststoreFile, type, password);
            factory.init(store);
            return factory.getTrustManagers();
        } catch (Exception e) {
            throw new AuthenticationException(
                    format("Cannot build TrustManagerFactory[alg:%s] from KeyStore[type:%s] stored in %s", algorithm,
                            type, truststoreFile), e);
        }
    }

    private KeyStore buildKeyStore(final String keystoreFile, final String type, final String password)
            throws Exception {
        return new KeyStoreBuilder()
                .withKeyStoreFile(keystoreFile)
                .withKeyStoreType(Utils.asEnum(type, KeyStoreType.class))
                .withPassword(password)
                .build();
    }

    /**
     * Checks if the String is non-null and a non-empty String.
     *
     * @param s The String to check.
     * @return <code>true</code> if the String is non-null and non-empty.
     */
    private boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * Will return an array of classes indicating that the CHF Http profile is supported.
     *
     * @return An array of the Request and Response classes.
     */
    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
    }

    /**
     * Validates whether or not the request contains a valid OpenAM SSO Token Id.
     * <br/>
     * Attempts to get the SSO Token Id from the request, if no SSO Token Id exists on the request then SEND_FAILURE is
     * returned. If a SSO Token Id is found a REST call is made to the configured OpenAM URL to validate that the
     * SSO Token Id is valid and has not expired.
     * <br/>
     * If the SSO Token Id is valid then SUCCESS is returned. For all other cases (i.e. invalid SSO Token Id,
     * any exceptions) SEND_FAILURE is returned.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return A Promise which will be completed with either a successful
     * AuthStatus value or a AuthenticationException If an error occurs when
     * setting the authenticated principal on the client subject.
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            final Subject clientSubject, Subject serviceSubject) {

        final String tokenId = getSsoTokenId(messageInfo.getRequest());
        LOG.debug("SSO Token found.");
        LOG.trace("SSO Token value, {}", tokenId);

        if (tokenId == null) {
            LOG.trace("SSO Token not found on request.");
            return newResultPromise(SEND_FAILURE);
        }

        Request validateRequest = new Request()
                .setMethod("POST")
                .setUri(URI.create(openamDeploymentUrl + JSON_SESSIONS_RELATIVE_URI + tokenId
                        + "?_action=validate"));

        // set empty body and Content-Type header for AM 12.
        validateRequest.getEntity().setJson(Collections.emptyMap());

        return httpClient.send(validateRequest)
                .thenAsync(onValidateSuccess(tokenId, clientSubject), onValidateFailure());
    }

    private AsyncFunction<Response, AuthStatus, AuthenticationException> onValidateSuccess(final String tokenId,
            final Subject clientSubject) {
        return new AsyncFunction<Response, AuthStatus, AuthenticationException>() {
            @Override
            public Promise<AuthStatus, AuthenticationException> apply(Response response) {
                try {
                    if (!response.getStatus().isSuccessful()) {
                        LOG.error("REST validation call returned non HTTP 200 response",
                                response.getEntity().getString());
                        return newResultPromise(SEND_FAILURE);
                    }
                    JsonValue validationResponse = json(response.getEntity().getJson());
                    if (validationResponse.isDefined("valid") && validationResponse.get("valid").asBoolean()) {
                        LOG.debug("REST validation call returned true.");

                        final String uid = validationResponse.get("uid").asString();
                        final String realm = validationResponse.get("realm").asString();

                        Request usersRequest = new Request()
                                .setMethod("GET")
                                .setUri(URI.create(openamDeploymentUrl + JSON_REST_ROOT_ENDPOINT
                                        + normalizeRealm(realm) + JSON_USERS_ENDPOINT + uid + "?_fields="
                                        + openamUserAttribute));
                        usersRequest.getHeaders().put(openamSSOTokenCookieName, tokenId);
                        return httpClient.send(usersRequest)
                                .then(onUserResponse(clientSubject), onUserRequestFailure());
                    }

                    LOG.debug("REST validation call returned false.");
                    return newResultPromise(SEND_FAILURE);
                } catch (IOException e) {
                    return newExceptionPromise(new AuthenticationException(
                            new InternalServerErrorException(e.getMessage(), e)));
                } finally {
                    closeSilently(response);
                }
            }
        };
    }

    private AsyncFunction<NeverThrowsException, AuthStatus, AuthenticationException> onValidateFailure() {
        return new AsyncFunction<NeverThrowsException, AuthStatus, AuthenticationException>() {
            @Override
            public Promise<AuthStatus, AuthenticationException> apply(NeverThrowsException e) {
                //This can never happen but the exception handler is needed
                // to change the types of the returned Promise.
                throw new IllegalStateException(
                        "HTTP Client threw a NeverThrowsException?!");
            }
        };
    }

    private Function<Response, AuthStatus, AuthenticationException> onUserResponse(final Subject clientSubject) {
        return closeSilently(new Function<Response, AuthStatus, AuthenticationException>() {
            @Override
            public AuthStatus apply(Response response) throws AuthenticationException {
                if (!response.getStatus().isSuccessful()) {
                    try {
                        LOG.error("REST validation call returned non HTTP 200 response",
                                response.getEntity().getString());
                        return SEND_FAILURE;
                    } catch (IOException e) {
                        throw new AuthenticationException(e);
                    }
                }
                try {
                    JsonValue usersResponse = json(response.getEntity().getJson());
                    handler.handle(new Callback[]{
                        new CallerPrincipalCallback(clientSubject, usersResponse.get(openamUserAttribute)
                                .get(0).asString())
                    });
                    return SUCCESS;
                } catch (IOException | UnsupportedCallbackException e) {
                    throw new AuthenticationException(
                            new InternalServerErrorException(e.getMessage(), e));
                }
            }
        });
    }

    private Function<NeverThrowsException, AuthStatus, AuthenticationException> onUserRequestFailure() {
        //This can never happen but the exception handler is needed
        // to change the types of the returned Promise.
        return noopExceptionFunction();
    }

    private String normalizeRealm(String realm) {
        if ("/".equals(realm)) {
            return "/";
        } else {
            return realm + "/";
        }
    }

    /**
     * Gets the SSO Token Id from the request.
     * <br/>
     * Firstly the SSO Token Id is attempted to be retrieved from a request header with the configured OpenAM SSO Token
     * name. If no header is found, or the value is empty, then the methods tries to find a cookie on the request
     * with the OpenAM SSO Token name.
     *
     * @param request The Request
     * @return The SSO Token Id on the request. Will be {@code null} if the request contains no header or cookie with
     * the OpenAM SSO Token name.
     */
    private String getSsoTokenId(Request request) {

        final String ssoTokenId = request.getHeaders().getFirst(openamSSOTokenCookieName);
        if (ssoTokenId != null) {
            LOG.debug("Found SSO Token in header with name, {}", openamSSOTokenCookieName);
            return ssoTokenId;
        }

        Cookie ssoTokenCookie = null;
        RequestCookies cookies = request.getCookies();
        if (cookies.containsKey(openamSSOTokenCookieName)) {
            for (Cookie cookie : cookies.get(openamSSOTokenCookieName)) {
                LOG.debug("SSO Token cookie found");
                ssoTokenCookie = cookie;
                break;
            }
        }

        if (ssoTokenCookie == null) {
            return null;
        }

        return ssoTokenCookie.getValue();
    }

    /**
     * No action to perform on secure response. Will always return SEND_SUCCESS.
     * <br/>
     * As this module uses OpenAM to verify whether a session is valid or not, when a session is valid the only way
     * for the client to gain a session is to authenticate using OpenAM. This will then result in a SSOToken being
     * set on the response as a cookie, hence doing the job of the secureResponse method.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {
        return newResultPromise(SEND_SUCCESS);
    }

    /**
     * No state to clear out from the client subject.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject clientSubject) {
        return newResultPromise(null);
    }
}
