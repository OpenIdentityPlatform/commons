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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.CookieHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.Options;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenAMSessionModuleTest {

    private OpenAMSessionModule sessionModule;

    private Options httpClientOptions;
    private Handler httpHandler;
    private String trustStorePath;
    private String trustStoreType;
    private String trustStoreAlgorithm;
    private String trustStorePassword;

    @BeforeMethod
    public void setup() {
        httpHandler = mock(Handler.class);
        httpClientOptions = null;

        sessionModule = new OpenAMSessionModule() {
            @Override
            Client createHttpClient(Options options) {
                httpClientOptions = options;
                return new Client(httpHandler);
            }

            @Override
            TrustManager[] getTrustManagers(String truststoreFile, String type, String algorithm,
                    String password) throws AuthenticationException {
                trustStorePath = truststoreFile;
                trustStoreType = type;
                trustStoreAlgorithm = algorithm;
                trustStorePassword = password;
                return new TrustManager[0];
            }
        };
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMDeploymentUrlNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("openamDeploymentUrl property must be set"));
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMDeploymentUrlIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("openamDeploymentUrl property must be set"));
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMSSOTokenCookieNameNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("openamSSOTokenCookieName property must be set"));
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMSSOTokenCookieNameIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("openamSSOTokenCookieName property must be set"));
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMUserAttributeNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("openamUserAttribute property must be set"));
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMUserAttributeIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("openamUserAttribute property must be set"));
        }
    }

    @Test
    public void initialiseShouldReturnSuccessfullyWhenUseSSLSetToFalse() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "http://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");

        //When
        sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);

        //Then
        verifyZeroInteractions(httpHandler);
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePathNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("truststorePath property must be set"));
            verifyZeroInteractions(httpHandler);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePathIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("truststorePath property must be set"));
            verifyZeroInteractions(httpHandler);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStoreTypeNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("truststoreType property must be set"));
            verifyZeroInteractions(httpHandler);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStoreTypeIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");
        options.put("truststoreType", "");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("truststoreType property must be set"));
            verifyZeroInteractions(httpHandler);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePasswordNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");
        options.put("truststoreType", "TRUST_STORE_TYPE");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("truststorePassword property must be set"));
            verifyZeroInteractions(httpHandler);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePasswordIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");
        options.put("truststoreType", "TRUST_STORE_TYPE");
        options.put("truststorePassword", "");

        //When
        try {
            sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("truststorePassword property must be set"));
            verifyZeroInteractions(httpHandler);
        }
    }

    @Test
    public void initialiseShouldReturnSuccessFullyWhenRequiredPropertiesSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");
        options.put("truststoreType", "TRUST_STORE_TYPE");
        options.put("truststorePassword", "TRUST_STORE_PASSWORD");

        //When
        sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);

        //Then
        assertThat(httpClientOptions.get(HttpClientHandler.OPTION_SSLCONTEXT_ALGORITHM)).isEqualTo("TLS");
        assertThat(httpClientOptions.get(HttpClientHandler.OPTION_TRUST_MANAGERS)).isNotNull();
        assertThat(trustStorePath).isEqualTo("TRUST_STORE_PATH");
        assertThat(trustStoreType).isEqualTo("TRUST_STORE_TYPE");
        assertThat(trustStoreAlgorithm).isEqualTo("SunX509");
        assertThat(trustStorePassword).isEqualTo("TRUST_STORE_PASSWORD");
    }

    @Test
    public void initialiseShouldReturnSuccessFullyWhenRequiredAndOptionalPropertiesSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");
        options.put("truststoreType", "TRUST_STORE_TYPE");
        options.put("truststorePassword", "TRUST_STORE_PASSWORD");

        options.put("trustManagerAlgorithm", "TRUST_MANAGER_ALGORITHM");

        //When
        sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);

        //Then
        assertThat(httpClientOptions.get(HttpClientHandler.OPTION_SSLCONTEXT_ALGORITHM)).isEqualTo("TLS");
        assertThat(httpClientOptions.get(HttpClientHandler.OPTION_TRUST_MANAGERS)).isNotNull();
        assertThat(trustStorePath).isEqualTo("TRUST_STORE_PATH");
        assertThat(trustStoreType).isEqualTo("TRUST_STORE_TYPE");
        assertThat(trustStoreAlgorithm).isEqualTo("TRUST_MANAGER_ALGORITHM");
        assertThat(trustStorePassword).isEqualTo("TRUST_STORE_PASSWORD");
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //When
        final Collection<Class<?>> supportedMessageTypes = sessionModule.getSupportedMessageTypes();

        //Then
        assertThat(supportedMessageTypes)
                .hasSize(2)
                .containsExactly(Request.class, Response.class);
    }

    private void initialise() {
        initialise("https://OPENAM_DEPLOYMENT_URI/");
    }

    private void initialise(final String openamDeploymentUrl) {
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<>();

        options.put("openamDeploymentUrl", openamDeploymentUrl);
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("truststorePath", "TRUST_STORE_PATH");
        options.put("truststoreType", "TRUST_STORE_TYPE");
        options.put("truststorePassword", "TRUST_STORE_PASSWORD");

        options.put("trustManagerAlgorithm", "TRUST_MANAGER_ALGORITHM");

        sessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenSsoTokenNotOnRequest() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", null);

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenSsoTokenNotInCookies() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", null);
        request.getHeaders().put(CookieHeader.valueOf("NOT_SSO_COOKIE_NAME=cookieVal"));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenRestResponseIsEmpty() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        final JsonValue restResponse = json(object());

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", "SSO_TOKEN_ID");
        mockHttpRequests(
                Pair.of("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID?_action=validate", restResponse));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenRestResponseIsInvalidSession() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        final JsonValue restResponse = json(object(field("valid", false)));

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", "SSO_TOKEN_ID");
        mockHttpRequests(
                Pair.of("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID?_action=validate", restResponse));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void validateRequestShouldReturnSuccessWhenSsoTokenIsValid() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/REALM")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", "SSO_TOKEN_ID");
        mockHttpRequests(Pair.of("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID?_action=validate",
                        restValidateResponse),
                Pair.of("https://OPENAM_DEPLOYMENT_URI/json/REALM/users/UID?_fields=OPENAM_USER_ATTRIBUTE",
                        restUsersResponse));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void initialiseShouldAddTrailingSlashToOpenAMDeploymentURL() throws Exception {

        //Given
        initialise("https://OPENAM_DEPLOYMENT_URI");
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/REALM")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", "SSO_TOKEN_ID");
        mockHttpRequests(Pair.of("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID?_action=validate",
                        restValidateResponse),
                Pair.of("https://OPENAM_DEPLOYMENT_URI/json/REALM/users/UID?_fields=OPENAM_USER_ATTRIBUTE",
                        restUsersResponse));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenRestCallFails() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        Promise<Response, NeverThrowsException> badRequestPromise = newResultPromise(new Response(Status.BAD_REQUEST));

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", "SSO_TOKEN_ID");
        given(httpHandler.handle(any(Context.class), any(Request.class))).willReturn(badRequestPromise);

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void validateRequestShouldReturnSuccessWhenSsoTokenOnCookie() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/REALM")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", null);
        request.getHeaders().put(
                CookieHeader.valueOf("NOT_SSO_COOKIE_NAME=cookieVal,OPENAM_SSO_TOKEN_COOKIE_NAME=SSO_TOKEN_ID"));
        mockHttpRequests(Pair.of("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID?_action=validate",
                        restValidateResponse),
                Pair.of("https://OPENAM_DEPLOYMENT_URI/json/REALM/users/UID?_fields=OPENAM_USER_ATTRIBUTE",
                        restUsersResponse));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void validateRequestShouldReturnSuccessWhenAccessingRootRealm() throws Exception {

        //Given
        initialise();
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final Request request = new Request();
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("OPENAM_SSO_TOKEN_COOKIE_NAME", null);
        request.getHeaders().put(
                CookieHeader.valueOf("OPENAM_SSO_TOKEN_COOKIE_NAME=SSO_TOKEN_ID"));
        mockHttpRequests(Pair.of("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID?_action=validate",
                        restValidateResponse),
                Pair.of("https://OPENAM_DEPLOYMENT_URI/json/users/UID?_fields=OPENAM_USER_ATTRIBUTE",
                        restUsersResponse));

        //When
        final AuthStatus authStatus = sessionModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void secureResponseShouldReturnSendSuccess() throws Exception {

        //Given
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject serviceSubject = new Subject();

        //When
        final AuthStatus authStatus = sessionModule.secureResponse(messageInfo, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void cleanSubjectShouldDoNothing() {

        //Given
        final MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        final Subject clientSubject = new Subject();

        //When
        sessionModule.cleanSubject(messageInfo, clientSubject);

        //Then
        verifyZeroInteractions(messageInfo, httpHandler);
    }

    private void mockHttpRequests(final Pair<String, JsonValue>... requests) {
        given(httpHandler.handle(any(Context.class), any(Request.class)))
                .willAnswer(new Answer<Promise<Response, NeverThrowsException>>() {
                    @Override
                    public Promise<Response, NeverThrowsException> answer(InvocationOnMock invocationOnMock) {
                        Request request = (Request) invocationOnMock.getArguments()[1];
                        for (Pair<String, JsonValue> req : requests) {
                            if (req.getFirst().equals(request.getUri().toString())) {
                                return newResultPromise(new Response(Status.OK).setEntity(req.getSecond().getObject()));
                            }
                        }
                        return null;
                    }
                });
    }
}
