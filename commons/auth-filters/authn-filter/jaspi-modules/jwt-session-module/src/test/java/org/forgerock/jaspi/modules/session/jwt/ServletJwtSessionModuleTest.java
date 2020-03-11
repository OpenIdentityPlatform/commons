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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.jaspi.modules.session.jwt.AbstractJwtSessionModule.LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME;
import static org.forgerock.json.JsonValue.json;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Key;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.json.jose.builders.EncryptedJwtBuilder;
import org.forgerock.json.jose.builders.JweHeaderBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedEncryptedJwtBuilder;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.encode.Base64;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ServletJwtSessionModuleTest {

    private static final String HMAC_KEY;
    static {
        byte[] keyValue = new byte[32];
        Arrays.fill(keyValue, (byte) 42);
        HMAC_KEY = Base64.encode(keyValue);
    }

    private ServletJwtSessionModule jwtSessionModule;

    private JwtBuilderFactory jwtBuilderFactory;

    @BeforeMethod
    public void setUp() {

        jwtBuilderFactory = mock(JwtBuilderFactory.class);

        jwtSessionModule = new ServletJwtSessionModule(jwtBuilderFactory) {
            @Override
            protected String rebuildEncryptedJwt(Jwt jwt, Key publicKey) {
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

    private Map<String, Object> getOptionsMap(Integer idleTimeout, Integer maxLife, int timeUnit)
            throws UnsupportedEncodingException {

        Map<String, Object> options = new HashMap<>();
        options.put(JwtSessionModule.KEY_ALIAS_KEY, "jwt-test-ks");
        options.put(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY, "password");
        options.put(JwtSessionModule.KEYSTORE_TYPE_KEY, "JKS");
        options.put(JwtSessionModule.KEYSTORE_FILE_KEY,
                URLDecoder.decode(ClassLoader.getSystemResource("keystore.jks").getFile(), "UTF-8"));
        options.put(JwtSessionModule.KEYSTORE_PASSWORD_KEY, "password");
        if (timeUnit == Calendar.MINUTE) {
            options.put(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, idleTimeout != null
                    ? idleTimeout.toString() : null);
            options.put(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, maxLife != null ? maxLife.toString() : null);
        } else if (timeUnit == Calendar.SECOND) {
            options.put(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, idleTimeout != null
                    ? idleTimeout.toString() : null);
            options.put(JwtSessionModule.MAX_TOKEN_LIFE_IN_SECONDS_KEY, maxLife != null ? maxLife.toString() : null);
        }
        options.put(JwtSessionModule.HMAC_SIGNING_KEY, HMAC_KEY);
        return options;
    }

    @Test
    public void shouldValidateRequestWhenNoCookiesPresent() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getCookies()).willReturn(new Cookie[0]);

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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

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
    public void shouldValidateRequestWhenJwtSessionCookieHmacIsInvalid() throws Exception {
        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

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
        SignedEncryptedJwt encryptedJwt = mock(SignedEncryptedJwt.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("ENCRYPTED_JWT");
        given(jwtBuilderFactory.reconstruct("ENCRYPTED_JWT", SignedEncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.verify(any(HmacSigningHandler.class))).willReturn(false);

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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

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
    public void shouldValidateRequestWhenJwtSessionCookiePresentButIdleTimeoutExpired() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

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
        SignedEncryptedJwt encryptedJwt = mock(SignedEncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1_000L).intValue();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilderFactory.reconstruct("SESSION_JWT", SignedEncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);

        //When
        AuthStatus authStatus = jwtSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
        verifyZeroInteractions(response);
    }

    @Test
    public void validateJwtSessionCookieShouldReturnNullWhenJwtCannotBeDecrypted() throws AuthException,
            UnsupportedEncodingException {

        //Given
        jwtSessionModule.initialize(null, null, null, getOptionsMap(1, 2, Calendar.MINUTE));

        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{jwtSessionCookie};

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getCookies()).willReturn(cookies);
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");

        given(jwtBuilderFactory.reconstruct("SESSION_JWT", SignedEncryptedJwt.class))
                .willThrow(JweDecryptionException.class);

        //When
        Jwt jwt = jwtSessionModule.validateJwtSessionCookie(messageInfo);

        //Then
        assertNull(jwt);
    }

    @Test
    public void shouldValidateRequestWhenJwtSessionCookiePresentButMaxLifeExpired() throws AuthException,
            UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

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
        SignedEncryptedJwt encryptedJwt = mock(SignedEncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1_000L).intValue();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilderFactory.reconstruct("SESSION_JWT", SignedEncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Integer.class))
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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        SignedEncryptedJwt encryptedJwt = mock(SignedEncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1_000L).intValue();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, -1);
        Date issuedAtTime = calendar.getTime();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);
        map.put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, contextMap);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilderFactory.reconstruct("SESSION_JWT", SignedEncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.verify(any(HmacSigningHandler.class))).willReturn(true);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);
        given(claimsSet.getIssuedAtTime()).willReturn(issuedAtTime);
        given(claimsSet.getClaim("prn", String.class)).willReturn("PRINCIPAL");
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("KEY", "VALUE");
        given(claimsSet.getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class)).willReturn(newContext);
        given(claimsSet.get("sessionId")).willReturn(json("SESSION_ID"));

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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();
        Subject clientSubject = new Subject();
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie cookie1 = mock(Cookie.class);
        Cookie cookie2 = mock(Cookie.class);
        Cookie jwtSessionCookie = mock(Cookie.class);
        Cookie[] cookies = new Cookie[]{cookie1, jwtSessionCookie, cookie2};
        SignedEncryptedJwt encryptedJwt = mock(SignedEncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, 5);
        Date idleTimeout = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1_000L).intValue();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -5);
        calendar.add(Calendar.MINUTE, -1);
        Date issuedAtTime = calendar.getTime();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);
        map.put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, contextMap);

        given(request.getCookies()).willReturn(cookies);
        given(cookie1.getName()).willReturn("COOKIE1");
        given(cookie2.getName()).willReturn("COOKIE2");
        given(jwtSessionCookie.getName()).willReturn("session-jwt");
        given(jwtSessionCookie.getValue()).willReturn("SESSION_JWT");
        given(jwtBuilderFactory.reconstruct("SESSION_JWT", SignedEncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.verify(any(HmacSigningHandler.class))).willReturn(true);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);
        given(claimsSet.getIssuedAtTime()).willReturn(issuedAtTime);
        given(claimsSet.getClaim("prn", String.class)).willReturn("PRINCIPAL");
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("KEY", "VALUE");
        given(claimsSet.getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class)).willReturn(newContext);
        given(claimsSet.get("sessionId")).willReturn(json("SESSION_ID"));

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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<>();
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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<>();
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
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        SignedEncryptedJwtBuilder signedEncryptedJwtBuilder = mock(SignedEncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(jwtBuilderFactory.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);

        given(jwtBuilderFactory.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.build()).willReturn(claimsSet);
        given(encryptedJwtBuilder.claims(claimsSet)).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.sign(any(HmacSigningHandler.class), eq(JwsAlgorithm.HS256)))
                .willReturn(signedEncryptedJwtBuilder);
        given(signedEncryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

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
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY),
                idleTimeoutCaptor.capture());
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
        Map<String, Object> options = getOptionsMap(null, 2, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        SignedEncryptedJwtBuilder signedEncryptedJwtBuilder = mock(SignedEncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(jwtBuilderFactory.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);

        given(jwtBuilderFactory.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.build()).willReturn(claimsSet);
        given(encryptedJwtBuilder.claims(claimsSet)).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.sign(any(HmacSigningHandler.class), eq(JwsAlgorithm.HS256)))
                .willReturn(signedEncryptedJwtBuilder);
        given(signedEncryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

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
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY),
                idleTimeoutCaptor.capture());
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
    public void shouldReturnEmptyContextMap() {
        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);

        Map<String, Object> messageInfoMap = new HashMap<>();

        given(messageInfo.getMap()).willReturn(messageInfoMap);

        //When
        Map<String, Object> result = jwtSessionModule.getContextMap(messageInfo); //create this time
        Map<String, Object> sameResult = jwtSessionModule.getContextMap(messageInfo); //retrieve

        //Then
        assertNotNull(result);
        assertEquals(result.size(), 0);
        assertEquals(result, sameResult);
    }

    @Test
    public void shouldReturnContextMap() {
        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);

        Map<String, Object> internalMap = new HashMap<>();
        internalMap.put("TEST", "TEST");

        Map<String, Object> messageInfoMap = new HashMap<>();
        messageInfoMap.put("org.forgerock.authentication.context", internalMap);

        given(messageInfo.getMap()).willReturn(messageInfoMap);

        //When
        Map<String, Object> result = jwtSessionModule.getContextMap(messageInfo);

        //Then
        assertEquals(result.size(), 1);
        assertEquals(result.get("TEST"), "TEST");
    }

    @Test
    public void shouldSecureResponseWithNullMaxLife() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, null, Calendar.MINUTE);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        SignedEncryptedJwtBuilder signedEncryptedJwtBuilder = mock(SignedEncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(jwtBuilderFactory.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);

        given(jwtBuilderFactory.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.build()).willReturn(claimsSet);
        given(encryptedJwtBuilder.claims(claimsSet)).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.sign(any(HmacSigningHandler.class), eq(JwsAlgorithm.HS256)))
                .willReturn(signedEncryptedJwtBuilder);
        given(signedEncryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

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
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY),
                idleTimeoutCaptor.capture());
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

    @Test
    public void shouldSecureResponseWithMinusOneMaxLife() throws AuthException, UnsupportedEncodingException {

        //Given
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 10, Calendar.MINUTE);
        options.put(JwtSessionModule.BROWSER_SESSION_ONLY_KEY, true);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Map<String, Object> map = new HashMap<>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(map);

        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        SignedEncryptedJwtBuilder signedEncryptedJwtBuilder = mock(SignedEncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(jwtBuilderFactory.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);

        given(jwtBuilderFactory.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMapOf(String.class, Object.class))).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.build()).willReturn(claimsSet);
        given(encryptedJwtBuilder.claims(claimsSet)).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.sign(any(HmacSigningHandler.class), eq(JwsAlgorithm.HS256)))
                .willReturn(signedEncryptedJwtBuilder);
        given(signedEncryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

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
        verify(jwtClaimsSetBuilder).claim(eq(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY),
                idleTimeoutCaptor.capture());
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
        calendar.add(Calendar.MINUTE, 10);
        assertEquals(exp, calendar.getTime());
        calendar.add(Calendar.MINUTE, 1);
        calendar.add(Calendar.MINUTE, -10);
        assertEquals(idle, (Long) (calendar.getTime().getTime() / 1000L));

        Cookie jwtSessionCookie = cookieCaptor.getValue();
        assertEquals(jwtSessionCookie.getPath(), "/");
        assertEquals(jwtSessionCookie.getValue(), "ENCRYPTED_JWT");
        assertEquals(jwtSessionCookie.getMaxAge(), -1);
    }

    @BeforeMethod(groups = "sessionCookieName")
    public void setupSessionCookieNameTests() throws Exception {
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;
        CallbackHandler callbackHandler = null;
        Map<String, Object> options = getOptionsMap(1, 2, Calendar.MINUTE);
        options.put(JwtSessionModule.SESSION_COOKIE_NAME_KEY, "my-custom-name");
        options.put(JwtSessionModule.HMAC_SIGNING_KEY, HMAC_KEY);

        jwtSessionModule.initialize(requestPolicy, responsePolicy, callbackHandler, options);
    }

    @Test(groups = "sessionCookieName")
    public void shouldUseCustomSessionCookieNameWhenCreatingCookie() throws Exception {
        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Subject serviceSubject = null;
        EncryptedJwtBuilder encryptedJwtBuilder = mock(EncryptedJwtBuilder.class);
        SignedEncryptedJwtBuilder signedEncryptedJwtBuilder = mock(SignedEncryptedJwtBuilder.class);
        JweHeaderBuilder jweHeaderBuilder = mock(JweHeaderBuilder.class);
        JwtClaimsSetBuilder jwtClaimsSetBuilder = mock(JwtClaimsSetBuilder.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(jwtBuilderFactory.jwe(Matchers.<Key>anyObject())).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.headers()).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.enc(Matchers.<EncryptionMethod>anyObject())).willReturn(jweHeaderBuilder);
        given(jweHeaderBuilder.done()).willReturn(encryptedJwtBuilder);

        given(jwtBuilderFactory.claims()).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.jti(anyString())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.exp(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.nbf(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.iat(Matchers.<Date>anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claim(anyString(), anyObject())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.claims(anyMap())).willReturn(jwtClaimsSetBuilder);
        given(jwtClaimsSetBuilder.build()).willReturn(claimsSet);
        given(encryptedJwtBuilder.claims(claimsSet)).willReturn(encryptedJwtBuilder);
        given(encryptedJwtBuilder.sign(any(HmacSigningHandler.class), eq(JwsAlgorithm.HS256)))
                .willReturn(signedEncryptedJwtBuilder);
        given(signedEncryptedJwtBuilder.build()).willReturn("ENCRYPTED_JWT");

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo("my-custom-name");
    }

    @Test(groups = "sessionCookieName")
    public void shouldUseCustomSessionCookieNameWhenDeletingCookie() throws Exception {
        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getResponseMessage()).willReturn(response);

        //When
        jwtSessionModule.deleteSessionJwtCookie(messageInfo);

        //Then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo("my-custom-name");
    }

    @Test(groups = "sessionCookieName")
    public void shouldUseCustomSessionCookieNameWhenResettingCookie() throws Exception {
        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie[] cookies = new Cookie[]{new Cookie("my-custom-name", "SESSION_JWT")};

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getCookies()).willReturn(cookies);
        SignedEncryptedJwt encryptedJwt = mock(SignedEncryptedJwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 5);
        Date expiryTime = calendar.getTime();
        Date idleTimeout = calendar.getTime();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, -1);
        calendar.add(Calendar.SECOND, -5);
        Date issuedAtTime = calendar.getTime();
        int idleTimeoutSeconds = new Long(idleTimeout.getTime() / 1_000L).intValue();

        given(jwtBuilderFactory.reconstruct("SESSION_JWT", SignedEncryptedJwt.class)).willReturn(encryptedJwt);
        given(encryptedJwt.verify(any(HmacSigningHandler.class))).willReturn(true);
        given(encryptedJwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getIssuedAtTime()).willReturn(issuedAtTime);
        given(claimsSet.getExpirationTime()).willReturn(expiryTime);
        given(claimsSet.getClaim(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Integer.class))
                .willReturn(idleTimeoutSeconds);

        //When
        jwtSessionModule.validateJwtSessionCookie(messageInfo);

        //Then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo("my-custom-name");
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenUsingSecondsAndMinutesTokenIdleTimeOption() throws Exception  {
        //given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        Map<String, Object> options = getOptionsMap(1, 1, Calendar.MINUTE);
        JwtSessionModule jwtSessionModule = new JwtSessionModule();

        options.put(JwtSessionModule.TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, "1");

        //when
        jwtSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options)
                .getOrThrowUninterruptibly();

        //then
        //should never get here
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenUsingSecondsAndMinutesMaxTokenLifeOption() throws Exception {
        //given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        Map<String, Object> options = getOptionsMap(1, 1, Calendar.MINUTE);
        JwtSessionModule jwtSessionModule = new JwtSessionModule();

        options.put(JwtSessionModule.MAX_TOKEN_LIFE_IN_SECONDS_KEY, "1");

        //when
        jwtSessionModule.initialize(requestMessagePolicy, responseMessagePolicy, callbackHandler, options)
                .getOrThrowUninterruptibly();

        //then
        //should never get here
    }

    @Test(groups = "sessionCookieName")
    public void shouldLogoutSession() throws Exception {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(new HashMap());
        given(request.getAttribute(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL)).willReturn("PRINCIPAL");
        given(request.getAttribute(LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME)).willReturn(true);

        //When
        AuthStatus authStatus = jwtSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(authStatus).isSameAs(AuthStatus.SEND_SUCCESS);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo("my-custom-name");
        assertThat(cookieCaptor.getValue().getValue()).isNull();
        assertThat(cookieCaptor.getValue().getMaxAge()).isEqualTo(0);
        assertThat(cookieCaptor.getValue().getPath()).isEqualTo("/");
    }
}
