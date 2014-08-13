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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.context;

import org.forgerock.jaspi.runtime.AuditTrail;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class FallbackServerAuthContextTest {

    private FallbackServerAuthContext fallbackServerAuthContext;

    private MessageInfoUtils messageInfoUtils;

    @BeforeMethod
    public void setUp() throws AuthException {

        messageInfoUtils = mock(MessageInfoUtils.class);
        ContextHandler contextHandler = mock(ContextHandler.class);

        fallbackServerAuthContext = new FallbackServerAuthContext(messageInfoUtils, contextHandler, null, null);
    }

    @Test
    public void shouldReturnSuccessWhenFirstAuthModuleValidateRequestReturnsSuccess() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put("org.forgerock.authentication.audit.trail", auditTrail);

        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);
        given(authModuleOne.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);
        given(authModuleTwo.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject,
                serviceSubject);

        //Then
        verifyZeroInteractions(authModuleTwo);
        assertEquals(privateContextMap.size(), 1);
        assertTrue(privateContextMap.containsKey(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY));
        assertTrue(privateContextMap.containsValue(authModuleOne));
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldReturnSendSuccessWhenFirstAuthModuleValidateRequestReturnsSendSuccess() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put("org.forgerock.authentication.audit.trail", auditTrail);

        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);
        given(authModuleOne.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_SUCCESS);
        given(authModuleTwo.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject,
                serviceSubject);

        //Then
        verifyZeroInteractions(authModuleTwo);
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldReturnSendContinueWhenFirstAuthModuleValidateRequestReturnsSendContinue() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);
        given(authModuleOne.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_CONTINUE);
        given(authModuleTwo.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject,
                serviceSubject);

        //Then
        verifyZeroInteractions(authModuleTwo);
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void shouldReturnSuccessWhenAuthModulesValidateRequestReturnSendFailureSuccess() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put("org.forgerock.authentication.audit.trail", auditTrail);

        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);
        given(authModuleOne.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);
        given(authModuleTwo.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject,
                serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 1);
        assertTrue(privateContextMap.containsKey(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY));
        assertTrue(privateContextMap.containsValue(authModuleTwo));
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldReturnSendFailureWhenSingleAuthModuleValidateRequestReturnsSendFailure() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put("org.forgerock.authentication.audit.trail", auditTrail);
        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        authModules.add(authModuleOne);
        given(authModuleOne.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject,
                serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenAuthModuleValidateRequestThrowsAuthException() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put("org.forgerock.authentication.audit.trail", auditTrail);

        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        doThrow(AuthException.class).when(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);

        //When
        fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject, serviceSubject);

        //Then
        fail();
    }

    @Test
    public void validateRequestShouldReturnSendFailureWithNoAuthModules() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.validateRequest(authModules, messageInfo, clientSubject,
                serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void secureResponseShouldReturnNullWhenNoAuthModuleValidateRequestSuccessfullyAuthenticated()
            throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        ServerAuthModule authModule = null;
        privateContextMap.put(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY, authModule);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.secureResponse(authModules, messageInfo, serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 0);
        assertNull(authStatus);
    }

    @Test
    public void secureResponseShouldReturnSendSuccessWhenAuthModuleReturnsSendSuccess() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        ServerAuthModule authModule = mock(ServerAuthModule.class);
        given(authModule.secureResponse(messageInfo, serviceSubject)).willReturn(AuthStatus.SEND_SUCCESS);
        privateContextMap.put(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY, authModule);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.secureResponse(authModules, messageInfo, serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void secureResponseShouldReturnSendContinueWhenAuthModuleReturnsSendContinue() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        ServerAuthModule authModule = mock(ServerAuthModule.class);
        given(authModule.secureResponse(messageInfo, serviceSubject)).willReturn(AuthStatus.SEND_CONTINUE);
        privateContextMap.put(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY, authModule);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.secureResponse(authModules, messageInfo, serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void secureResponseShouldReturnSendFailureWhenAuthModuleReturnsSendFailure() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        ServerAuthModule authModule = mock(ServerAuthModule.class);
        given(authModule.secureResponse(messageInfo, serviceSubject)).willReturn(AuthStatus.SEND_FAILURE);
        privateContextMap.put(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY, authModule);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        AuthStatus authStatus = fallbackServerAuthContext.secureResponse(authModules, messageInfo, serviceSubject);

        //Then
        assertEquals(privateContextMap.size(), 0);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenAuthModuleSecureResponseThrowsAuthException() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> privateContextMap = new HashMap<String, Object>();

        ServerAuthModule authModule = mock(ServerAuthModule.class);
        doThrow(AuthException.class).when(authModule).secureResponse(messageInfo, serviceSubject);
        privateContextMap.put(FallbackServerAuthContext.AUTHENTICATING_AUTH_MODULE_KEY, authModule);

        given(messageInfoUtils.getMap(messageInfo, "_serverAuthContextMap")).willReturn(privateContextMap);

        //When
        fallbackServerAuthContext.secureResponse(authModules, messageInfo, serviceSubject);

        //Then
        fail();
    }
}
