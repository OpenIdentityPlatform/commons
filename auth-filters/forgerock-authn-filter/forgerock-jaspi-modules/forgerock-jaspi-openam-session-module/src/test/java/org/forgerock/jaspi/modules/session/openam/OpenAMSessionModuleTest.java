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

package org.forgerock.jaspi.modules.session.openam;

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OpenAMSessionModuleTest {

    private OpenAMSessionModule openAMSessionModule;

    private RestClient restClient;

    @BeforeMethod
    public void setUp() {

        restClient = mock(RestClient.class);

        openAMSessionModule = new OpenAMSessionModule(restClient);
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenOpenAMDeploymentUrlNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
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
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
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
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
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
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
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
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
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
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
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
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "http://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");

        //When
        openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);

        //Then
        verifyZeroInteractions(restClient);
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePathNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("trustStorePath property must be set"));
            verifyZeroInteractions(restClient);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePathIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("trustStorePath property must be set"));
            verifyZeroInteractions(restClient);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStoreTypeNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("trustStoreType property must be set"));
            verifyZeroInteractions(restClient);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStoreTypeIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");
        options.put("trustStoreType", "");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("trustStoreType property must be set"));
            verifyZeroInteractions(restClient);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePasswordNotSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");
        options.put("trustStoreType", "TRUST_STORE_TYPE");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("trustStorePassword property must be set"));
            verifyZeroInteractions(restClient);
        }
    }

    @Test
    public void initialiseShouldThrowIllegalArgumentExceptionWhenTrustStorePasswordIsEmpty() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");
        options.put("trustStoreType", "TRUST_STORE_TYPE");
        options.put("trustStorePassword", "");

        //When
        try {
            openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
        } catch (IllegalArgumentException e) {
            //Then
            assertTrue(e.getMessage().contains("trustStorePassword property must be set"));
            verifyZeroInteractions(restClient);
        }
    }

    @Test
    public void initialiseShouldReturnSuccessFullyWhenRequiredPropertiesSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");
        options.put("trustStoreType", "TRUST_STORE_TYPE");
        options.put("trustStorePassword", "TRUST_STORE_PASSWORD");

        //When
        openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);

        //Then
        final ArgumentCaptor<SslConfiguration> sslConfigurationCaptor = ArgumentCaptor.forClass(SslConfiguration.class);
        verify(restClient).setSslConfiguration(sslConfigurationCaptor.capture());

        final SslConfiguration sslConfiguration = sslConfigurationCaptor.getValue();
        assertEquals(sslConfiguration.getTrustManagerAlgorithm(), "SunX509");
        assertEquals(sslConfiguration.getTrustStorePath(), "TRUST_STORE_PATH");
        assertEquals(sslConfiguration.getTrustStoreType(), "TRUST_STORE_TYPE");
        assertEquals(sslConfiguration.getTrustStorePassword(), "TRUST_STORE_PASSWORD".toCharArray());
    }

    @Test
    public void initialiseShouldReturnSuccessFullyWhenRequiredAndOptionalPropertiesSet() {

        //Given
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", "https://OPENAM_DEPLOYMENT_URI/");
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");
        options.put("trustStoreType", "TRUST_STORE_TYPE");
        options.put("trustStorePassword", "TRUST_STORE_PASSWORD");

        options.put("trustManagerAlgorithm", "TRUST_MANAGER_ALGORITHM");

        //When
        openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);

        //Then
        final ArgumentCaptor<SslConfiguration> sslConfigurationCaptor = ArgumentCaptor.forClass(SslConfiguration.class);
        verify(restClient).setSslConfiguration(sslConfigurationCaptor.capture());

        final SslConfiguration sslConfiguration = sslConfigurationCaptor.getValue();
        assertEquals(sslConfiguration.getTrustManagerAlgorithm(), "TRUST_MANAGER_ALGORITHM");
        assertEquals(sslConfiguration.getTrustStorePath(), "TRUST_STORE_PATH");
        assertEquals(sslConfiguration.getTrustStoreType(), "TRUST_STORE_TYPE");
        assertEquals(sslConfiguration.getTrustStorePassword(), "TRUST_STORE_PASSWORD".toCharArray());
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //Given

        //When
        final Class<?>[] supportedMessageTypes = openAMSessionModule.getSupportedMessageTypes();

        //Then
        assertEquals(supportedMessageTypes.length, 2);
        assertTrue(Arrays.asList(supportedMessageTypes).contains(HttpServletRequest.class));
        assertTrue(Arrays.asList(supportedMessageTypes).contains(HttpServletResponse.class));
    }

    private void initialise() {
        initialise("https://OPENAM_DEPLOYMENT_URI/");
    }

    private void initialise(final String openamDeploymentUrl) {
        final MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        final MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        final CallbackHandler callbackHandler = mock(CallbackHandler.class);
        final Map<String, Object> options = new HashMap<String, Object>();

        options.put("openamDeploymentUrl", openamDeploymentUrl);
        options.put("openamSSOTokenCookieName", "OPENAM_SSO_TOKEN_COOKIE_NAME");
        options.put("openamUserAttribute", "OPENAM_USER_ATTRIBUTE");
        options.put("trustStorePath", "TRUST_STORE_PATH");
        options.put("trustStoreType", "TRUST_STORE_TYPE");
        options.put("trustStorePassword", "TRUST_STORE_PASSWORD");

        options.put("trustManagerAlgorithm", "TRUST_MANAGER_ALGORITHM");

        openAMSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenSsoTokenNotOnRequest() throws JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn(null);
        given(request.getCookies()).willReturn(null);

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenSsoTokenNotInCookies() throws JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie cookie = mock(Cookie.class);
        final Cookie[] cookies = new Cookie[]{cookie};

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn(null);
        given(request.getCookies()).willReturn(cookies);
        given(cookie.getName()).willReturn("NOT_SSO_COOKIE_NAME");

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void validateRequestShouldReturnSendFailureWhenRestResponseIsEmpty() throws ResourceException,
            JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final JsonValue restResponse = json(object());

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn("SSO_TOKEN_ID");
        given(restClient.post(eq("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class))).willReturn(restResponse);

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        final ArgumentCaptor<Map> queryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).post(anyString(), queryParameterCaptor.capture(), anyMapOf(String.class, String.class));
        assertEquals(queryParameterCaptor.getValue().get("_action"), "validate");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void validateRequestShouldReturnSendFailureWhenRestResponseIsInvalidSession() throws ResourceException,
            JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final JsonValue restResponse = json(object(field("valid", false)));

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn("SSO_TOKEN_ID");
        given(restClient.post(eq("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class))).willReturn(restResponse);

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        final ArgumentCaptor<Map> queryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).post(anyString(), queryParameterCaptor.capture(), anyMapOf(String.class, String.class));
        assertEquals(queryParameterCaptor.getValue().get("_action"), "validate");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void validateRequestShouldReturnSuccessWhenSsoTokenIsValid() throws ResourceException, JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/REALM")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn("SSO_TOKEN_ID");
        given(restClient.post(eq("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class)))
                .willReturn(restValidateResponse);
        given(restClient.get(eq("https://OPENAM_DEPLOYMENT_URI/json/REALM/users/UID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class)))
                .willReturn(restUsersResponse);

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        final ArgumentCaptor<Map> validateQueryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).post(anyString(), validateQueryParameterCaptor.capture(),
                anyMapOf(String.class, String.class));
        assertEquals(validateQueryParameterCaptor.getValue().get("_action"), "validate");
        final ArgumentCaptor<Map> usersQueryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).get(anyString(), usersQueryParameterCaptor.capture(), anyMapOf(String.class, String.class));
        assertEquals(usersQueryParameterCaptor.getValue().get("_fields"), "OPENAM_USER_ATTRIBUTE");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void initialiseShouldAddTrailingSlashToOpenAMDeploymentURL() throws ResourceException, JaspiAuthException {

        //Given
        initialise("https://OPENAM_DEPLOYMENT_URI");
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/REALM")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn("SSO_TOKEN_ID");
        given(restClient.post(eq("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class)))
                .willReturn(restValidateResponse);
        given(restClient.get(eq("https://OPENAM_DEPLOYMENT_URI/json/REALM/users/UID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class)))
                .willReturn(restUsersResponse);

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        final ArgumentCaptor<Map> validateQueryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).post(anyString(), validateQueryParameterCaptor.capture(),
                anyMapOf(String.class, String.class));
        assertEquals(validateQueryParameterCaptor.getValue().get("_action"), "validate");
        final ArgumentCaptor<Map> usersQueryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).get(anyString(), usersQueryParameterCaptor.capture(), anyMapOf(String.class, String.class));
        assertEquals(usersQueryParameterCaptor.getValue().get("_fields"), "OPENAM_USER_ATTRIBUTE");
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenRestCallFails() throws ResourceException, JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn("SSO_TOKEN_ID");
        doThrow(ResourceException.class).when(restClient).post(anyString(), anyMapOf(String.class, String.class),
                anyMapOf(String.class, String.class));

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void validateRequestShouldReturnSuccessWhenSsoTokenOnCookie() throws ResourceException, JaspiAuthException {

        //Given
        initialise();
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();
        final Subject serviceSubject = new Subject();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie cookieOne = mock(Cookie.class);
        final Cookie cookieTwo = mock(Cookie.class);
        final Cookie[] cookies = new Cookie[]{cookieOne, cookieTwo};
        final JsonValue restValidateResponse = json(object(field("valid", true), field("uid", "UID"),
                field("realm", "/REALM")));
        final JsonValue restUsersResponse = json(object(field("OPENAM_USER_ATTRIBUTE", array("VALUE"))));

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("OPENAM_SSO_TOKEN_COOKIE_NAME")).willReturn(null);
        given(request.getCookies()).willReturn(cookies);
        given(cookieOne.getName()).willReturn("NOT_SSO_COOKIE_NAME");
        given(cookieTwo.getName()).willReturn("OPENAM_SSO_TOKEN_COOKIE_NAME");
        given(cookieTwo.getValue()).willReturn("SSO_TOKEN_ID");
        given(restClient.post(eq("https://OPENAM_DEPLOYMENT_URI/json/sessions/SSO_TOKEN_ID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class)))
                .willReturn(restValidateResponse);
        given(restClient.get(eq("https://OPENAM_DEPLOYMENT_URI/json/REALM/users/UID"),
                anyMapOf(String.class, String.class), anyMapOf(String.class, String.class)))
                .willReturn(restUsersResponse);

        //When
        final AuthStatus authStatus = openAMSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        final ArgumentCaptor<Map> validateQueryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).post(anyString(), validateQueryParameterCaptor.capture(),
                anyMapOf(String.class, String.class));
        assertEquals(validateQueryParameterCaptor.getValue().get("_action"), "validate");
        final ArgumentCaptor<Map> usersQueryParameterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).get(anyString(), usersQueryParameterCaptor.capture(), anyMapOf(String.class, String.class));
        assertEquals(usersQueryParameterCaptor.getValue().get("_fields"), "OPENAM_USER_ATTRIBUTE");
    }

    @Test
    public void secureResponseShouldReturnSendSuccess() {

        //Given
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject serviceSubject = new Subject();

        //When
        final AuthStatus authStatus = openAMSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void cleanSubjectShouldDoNothing() {

        //Given
        final MessageInfo messageInfo = mock(MessageInfo.class);
        final Subject clientSubject = new Subject();

        //When
        openAMSessionModule.cleanSubject(messageInfo, clientSubject);

        //Then
        verifyZeroInteractions(messageInfo, restClient);
    }
}
