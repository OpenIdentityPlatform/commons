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

package org.forgerock.jaspi.modules.session.openam;

import static org.forgerock.caf.authentication.framework.JaspiRuntime.LOG;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Reject;

/**
 * A JASPI Session Module which uses OpenAM to validate SSO Tokens issued by an OpenAM instance.
 * <br/>
 * Validates SSO Tokens by making REST calls to a OpenAM instance.
 *
 * @since 1.4.0
 */
public class OpenAMSessionModule implements ServerAuthModule {

    private static final String JSON_REST_ROOT_ENDPOINT = "json";
    private static final String JSON_SESSIONS_RELATIVE_URI = JSON_REST_ROOT_ENDPOINT + "/sessions/";
    private static final String JSON_USERS_ENDPOINT = "users/";

    private final RestClient restClient;

    private CallbackHandler handler;
    private String openamDeploymentUrl;
    private String openamSSOTokenCookieName;
    private String openamUserAttribute;

    /**
     * Constructs a new OpenAMSessionModule instance.
     */
    public OpenAMSessionModule() {
        this(new RestletRestClient());
    }

    /**
     * Constructs a new OpenAMSessionModule instance with the specified RestClient.
     * <br/>
     * For test use.
     *
     * @param restClient The RestClient instance.
     */
    OpenAMSessionModule(final RestClient restClient) {
        this.restClient = restClient;
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
     * @param requestMessagePolicy {@inheritDoc}
     * @param responseMessagePolicy {@inheritDoc}
     * @param callbackHandler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws java.lang.IllegalArgumentException If any of the required configuration properties are not set.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void initialize(final MessagePolicy requestMessagePolicy, final MessagePolicy responseMessagePolicy,
            final CallbackHandler callbackHandler, final Map options) {
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
            throw new IllegalArgumentException("OpenAM Deployment URL malformed.");
        }

        LOG.debug("Using SSL? {}", useSSL);

        if (useSSL) {
            restClient.setSslConfiguration(configureSsl(options));
        }
    }

    /**
     * Configures the REST connections to use SSL.
     *
     * @param options The configuration options of the module.
     * @throws java.lang.IllegalArgumentException If any of the required configuration properties are not set.
     */
    @SuppressWarnings("rawtypes")
    private SslConfiguration configureSsl(final Map options) {

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

        final SslConfiguration sslConfiguration = new SslConfiguration();

        sslConfiguration.setTrustManagerAlgorithm(trustManagerAlgorithm);
        sslConfiguration.setTrustStorePath(trustStorePath);
        sslConfiguration.setTrustStoreType(trustStoreType);
        sslConfiguration.setTrustStorePassword(trustStorePassword.toCharArray());

        return sslConfiguration;
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
     * Will return an array of classes indicating that the Http Servlet JASPI profile is supported.
     *
     * @return An array of the HttpServletRequest and HttpServletResponse classes.
     */
    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return new Class<?>[]{HttpServletRequest.class, HttpServletResponse.class};
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
     * @return {@inheritDoc}
     * @throws AuthenticationException If an error occurs when setting the authenticated principal on the client subject.
     */
    @Override
    public AuthStatus validateRequest(final MessageInfo messageInfo, final Subject clientSubject,
            final Subject serviceSubject) throws AuthenticationException {

        final String tokenId = getSsoTokenId((HttpServletRequest) messageInfo.getRequestMessage());
        LOG.debug("SSO Token found.");
        LOG.trace("SSO Token value, {}", tokenId);

        if (tokenId == null) {
            LOG.trace("SSO Token not found on request.");
            return AuthStatus.SEND_FAILURE;
        }

        try {
            final JsonValue validationResponse = restClient.post(openamDeploymentUrl + JSON_SESSIONS_RELATIVE_URI
                            + tokenId, Collections.singletonMap("_action", "validate"),
                    Collections.<String, String>emptyMap());

            if (validationResponse.isDefined("valid") && validationResponse.get("valid").asBoolean()) {
                LOG.debug("REST validation call returned true.");

                final String uid = validationResponse.get("uid").asString();
                final String realm = validationResponse.get("realm").asString();

                JsonValue response = restClient.get(openamDeploymentUrl + JSON_REST_ROOT_ENDPOINT
                                + normalizeRealm(realm) + JSON_USERS_ENDPOINT + uid,
                        Collections.singletonMap("_fields", openamUserAttribute),
                        Collections.singletonMap(openamSSOTokenCookieName, tokenId));

                handler.handle(new Callback[]{
                        new CallerPrincipalCallback(clientSubject, response.get(openamUserAttribute).get(0).asString())
                });

                return AuthStatus.SUCCESS;
            }

            LOG.debug("REST validation call returned false.");
            return AuthStatus.SEND_FAILURE;

        } catch (ResourceException e) {
            LOG.error("REST validation call returned non HTTP 200 response", e);
            return AuthStatus.SEND_FAILURE;
        } catch (UnsupportedCallbackException e) {
            throw new AuthenticationException(e.getMessage(), e);
        } catch (IOException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
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
     * @param request The HttpServletRequest
     * @return The SSO Token Id on the request. Will be {@code null} if the request contains no header or cookie with
     * the OpenAM SSO Token name.
     */
    private String getSsoTokenId(final HttpServletRequest request) {

        final String ssoTokenId = request.getHeader(openamSSOTokenCookieName);
        if (ssoTokenId != null) {
            LOG.debug("Found SSO Token in header with name, {}", openamSSOTokenCookieName);
            return ssoTokenId;
        }

        Cookie ssoTokenCookie = null;
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (openamSSOTokenCookieName.equals(cookie.getName())) {
                    LOG.debug("SSO Token cookie found");
                    ssoTokenCookie = cookie;
                    break;
                }
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
    public AuthStatus secureResponse(final MessageInfo messageInfo, final Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * No state to clear out from the client subject.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     */
    @Override
    public void cleanSubject(final MessageInfo messageInfo, final Subject clientSubject) {
    }
}
