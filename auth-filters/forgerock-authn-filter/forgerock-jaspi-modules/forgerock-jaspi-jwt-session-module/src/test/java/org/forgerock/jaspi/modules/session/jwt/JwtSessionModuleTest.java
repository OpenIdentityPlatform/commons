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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.modules.session.jwt;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.jose.builders.EncryptedJwtBuilder;
import org.forgerock.json.jose.builders.JweHeaderBuilder;
import org.forgerock.json.jose.builders.JwtBuilder;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;

public class JwtSessionModuleTest {

    private JwtSessionModule jwtSessionModule;

    private JwtBuilder jwtBuilder;

    @BeforeMethod
    public void setUp() {

        jwtBuilder = mock(JwtBuilder.class);

        jwtSessionModule = new JwtSessionModule(jwtBuilder) {
            @Override
            protected String rebuildEncryptedJwt(EncryptedJwt jwt, RSAPublicKey publicKey) {
                return "REBUILT_ENCRYPTED_JWT";
            }
        };
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //Given

        //When
        Class[] supportedMessageTypes = jwtSessionModule.getSupportedMessageTypes();

        //Then
        assertEquals(supportedMessageTypes.length, 2);
        assertEquals(supportedMessageTypes[0], HttpServletRequest.class);
        assertEquals(supportedMessageTypes[1], HttpServletResponse.class);
    }

    private Map<String, Object> getOptionsMap(Integer idleTimeout, Integer maxLife)
            throws UnsupportedEncodingException {

        Map<String, Object> options = new HashMap<String, Object>();
        options.put(JwtSessionModule.KEY_ALIAS_KEY, "jwt-test-ks");
        options.put(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY, "password");
        options.put(JwtSessionModule.KEYSTORE_TYPE_KEY, "JKS");
        options.put(JwtSessionModule.KEYSTORE_FILE_KEY,
                URLDecoder.decode(ClassLoader.getSystemResource("keystore.jks").getFile(), "UTF-8"));
        options.put(JwtSessionModule.KEYSTORE_PASSWORD_KEY, "password");
        options.put(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY, idleTimeout != null ? idleTimeout.toString() : null);
        options.put(JwtSessionModule.MAX_TOKEN_LIFE_KEY, maxLife != null ? maxLife.toString() : null);

        return options;
    }

    @Test
    public void shouldValidateRequestWhenNoCookiesPresent() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(null);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenCookiesPresentButJwtSessionCookie() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, cookie2};

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentButNull() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn(null);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentButEmptyString() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("");

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentButNotValidEncryption() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        EncryptedJwt encryptedJwt = mock(EncryptedJwt.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilder.reconstruct("SESSION_JWT", EncryptedJwt.class)).willReturn(encryptedJwt);
        doThrow(JsonException.class).when(encryptedJwt).decrypt(Matchers.<Key>anyObject());

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentButIdleTimeoutExpired() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        EncryptedJwt encryptedJwt = mock(EncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1000L).intValue();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilder.reconstruct("SESSION_JWT", EncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentButMaxLifeExpired() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        EncryptedJwt encryptedJwt = mock(EncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1000L).intValue();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilder.reconstruct("SESSION_JWT", EncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentAndValidCoolOffPeriodNotExpired() throws AuthException,
            IOException, UnsupportedCallbackException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        EncryptedJwt encryptedJwt = mock(EncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1000L).intValue();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, -1);
        Date issuedAtTime = calendar.getTime();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);
        map.put("org.forgerock.security.context", contextMap);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilder.reconstruct("SESSION_JWT", EncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);
        given(claimsSet.getIssuedAtTime()).willReturn(issuedAtTime);
        given(claimsSet.getClaim("prn", String.class)).willReturn("PRINCIPAL");
        Map<String, Object> newContext = new HashMap<String, Object>();
        newContext.put("KEY", "VALUE");
        given(claimsSet.getClaim("org.forgerock.security.context", Map.class)).willReturn(newContext);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        verifyZeroInteractions(response);
        ArgumentCaptor<Callback[]> callbackCaptor =
                ArgumentCaptor.forClass(Callback[].class);
        verify(callbackHandler).handle(callbackCaptor.capture());
        Callback[] callbacks = callbackCaptor.getValue();
        assertEquals(callbacks.length, 1);
        assertEquals(((CallerPrincipalCallback) callbacks[0]).getName(), "PRINCIPAL");
        assertNull(((CallerPrincipalCallback) callbacks[0]).getPrincipal());
        assertEquals(((CallerPrincipalCallback) callbacks[0]).getSubject(), clientSubject);
        assertEquals(contextMap.size(), 1);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentAndValidCoolOffPeriodExpired() throws AuthException,
            IOException, UnsupportedCallbackException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();
        Subject clientSubject = new Subject();
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        EncryptedJwt encryptedJwt = mock(EncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1000L).intValue();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -5);
        calendar.add(Calendar.MINUTE, -1);
        Date issuedAtTime = calendar.getTime();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);
        map.put("org.forgerock.security.context", contextMap);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilder.reconstruct("SESSION_JWT", EncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);
        given(claimsSet.getIssuedAtTime()).willReturn(issuedAtTime);
        given(claimsSet.getClaim("prn", String.class)).willReturn("PRINCIPAL");
        Map<String, Object> newContext = new HashMap<String, Object>();
        newContext.put("KEY", "VALUE");
        given(claimsSet.getClaim("org.forgerock.security.context", Map.class)).willReturn(newContext);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        verify(claimsSet).setIssuedAtTime(Matchers.<Date>anyObject());
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie newCookie = cookieCaptor.getValue();
        assertEquals(newCookie.getValue(), "REBUILT_ENCRYPTED_JWT");
        assertEquals(newCookie.getPath(), "/");
        assertNotEquals(newCookie.getMaxAge(), 0);
        assertNotEquals(newCookie.getMaxAge(), -1);
        ArgumentCaptor<Callback[]> callbackCaptor =
                ArgumentCaptor.forClass(Callback[].class);
        verify(callbackHandler).handle(callbackCaptor.capture());
        Callback[] callbacks = callbackCaptor.getValue();
        assertEquals(callbacks.length, 1);
        assertEquals(((CallerPrincipalCallback) callbacks[0]).getName(), "PRINCIPAL");
        assertNull(((CallerPrincipalCallback) callbacks[0]).getPrincipal());
        assertEquals(((CallerPrincipalCallback) callbacks[0]).getSubject(), clientSubject);
        assertEquals(contextMap.size(), 1);
    }

    @Test
    public void shouldSecureResponseWithSkipSessionParameter() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("skipSession", true);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldSecureResponseWithJwtValidatedParameter() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(JwtSessionModule.JWT_VALIDATED_KEY, true);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldSecureResponse() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<String, Object>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);

        given(jwtBuilder.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.done()).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verify(jweHeaderBuilder).alg(JweAlgorithm.RSAES_PKCS1_V1_5);
        verify(jweHeaderBuilder).enc(EncryptionMethod.A128CBC_HS256);

        ArgumentCaptor<Date> expCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> nbfCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> iatCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Long> idleTimeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        verify(jwtClaimsSetBuilder).exp(expCaptor.capture());
        verify(jwtClaimsSetBuilder).nbf(nbfCaptor.capture());
        verify(jwtClaimsSetBuilder).iat(iatCaptor.capture());
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY), idleTimeoutCaptor.capture());
        verify(jwtClaimsSetBuilder).claims(anyMap());
        verify(response).addCookie(cookieCaptor.capture());


        Date iat = iatCaptor.getValue();
        Date nbf = nbfCaptor.getValue();
        Date exp = expCaptor.getValue();
        Long idle = idleTimeoutCaptor.getValue();

        assertEquals(iat, nbf);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(iat);
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.MILLISECOND, 0);
        assertEquals(idle, (Long) (calendar.getTime().getTime() / 1000L));
        calendar.add(Calendar.MINUTE, 1);
        assertEquals(exp, calendar.getTime());


        Cookie jwtSessionCookie = cookieCaptor.getValue();
        assertEquals(jwtSessionCookie.getPath(), "/");
        assertEquals(jwtSessionCookie.getValue(), "ENCRYPTED_JWT");
        assertEquals(jwtSessionCookie.getMaxAge(), new Long(exp.getTime() - iat.getTime()).intValue() / 1000);
    }

    @Test
    public void shouldSecureResponseWithNullIdleTimeout() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(null, 2);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<String, Object>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);

        given(jwtBuilder.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.done()).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verify(jweHeaderBuilder).alg(JweAlgorithm.RSAES_PKCS1_V1_5);
        verify(jweHeaderBuilder).enc(EncryptionMethod.A128CBC_HS256);

        ArgumentCaptor<Date> expCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> nbfCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> iatCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Long> idleTimeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        verify(jwtClaimsSetBuilder).exp(expCaptor.capture());
        verify(jwtClaimsSetBuilder).nbf(nbfCaptor.capture());
        verify(jwtClaimsSetBuilder).iat(iatCaptor.capture());
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY), idleTimeoutCaptor.capture());
        verify(jwtClaimsSetBuilder).claims(anyMap());
        verify(response).addCookie(cookieCaptor.capture());


        Date iat = iatCaptor.getValue();
        Date nbf = nbfCaptor.getValue();
        Date exp = expCaptor.getValue();
        Long idle = idleTimeoutCaptor.getValue();

        assertEquals(iat, nbf);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(iat);
        calendar.set(Calendar.MILLISECOND, 0);
        assertEquals(idle, (Long) (calendar.getTime().getTime() / 1000L));
        calendar.add(Calendar.MINUTE, 2);
        assertEquals(exp, calendar.getTime());


        Cookie jwtSessionCookie = cookieCaptor.getValue();
        assertEquals(jwtSessionCookie.getPath(), "/");
        assertEquals(jwtSessionCookie.getValue(), "ENCRYPTED_JWT");
        assertEquals(jwtSessionCookie.getMaxAge(), new Long(exp.getTime() - iat.getTime()).intValue() / 1000);
    }

    @Test
    public void shouldSecureResponseWithNullMaxLife() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, null);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<String, Object>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);

        given(jwtBuilder.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.done()).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verify(jweHeaderBuilder).alg(JweAlgorithm.RSAES_PKCS1_V1_5);
        verify(jweHeaderBuilder).enc(EncryptionMethod.A128CBC_HS256);

        ArgumentCaptor<Date> expCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> nbfCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> iatCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Long> idleTimeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        verify(jwtClaimsSetBuilder).exp(expCaptor.capture());
        verify(jwtClaimsSetBuilder).nbf(nbfCaptor.capture());
        verify(jwtClaimsSetBuilder).iat(iatCaptor.capture());
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_CLAIM_KEY), idleTimeoutCaptor.capture());
        verify(jwtClaimsSetBuilder).claims(anyMap());
        verify(response).addCookie(cookieCaptor.capture());


        Date iat = iatCaptor.getValue();
        Date nbf = nbfCaptor.getValue();
        Date exp = expCaptor.getValue();
        Long idle = idleTimeoutCaptor.getValue();

        assertEquals(iat, nbf);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(iat);
        calendar.set(Calendar.MILLISECOND, 0);
        assertEquals(exp, calendar.getTime());
        calendar.add(Calendar.MINUTE, 1);
        assertEquals(idle, (Long) (calendar.getTime().getTime() / 1000L));


        Cookie jwtSessionCookie = cookieCaptor.getValue();
        assertEquals(jwtSessionCookie.getPath(), "/");
        assertEquals(jwtSessionCookie.getValue(), "ENCRYPTED_JWT");
        assertEquals(jwtSessionCookie.getMaxAge(), new Long(exp.getTime() - iat.getTime()).intValue() / 1000);
    }
}
