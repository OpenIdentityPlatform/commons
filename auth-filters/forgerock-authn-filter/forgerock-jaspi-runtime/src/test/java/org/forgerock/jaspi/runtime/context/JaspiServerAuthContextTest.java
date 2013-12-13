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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime.context;

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.mockito.Matchers;
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

import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class JaspiServerAuthContextTest {

    private MessageInfoUtils messageInfoUtils;
    private ContextHandler contextHandler;

    @BeforeMethod
    public void setUp() {
        messageInfoUtils = mock(MessageInfoUtils.class);
        contextHandler = mock(ContextHandler.class);
    }

    private JaspiServerAuthContext<ServerAuthModule> createJaspiServerAuthContext(ServerAuthModule sessionAuthModule,
            List<ServerAuthModule> authModules, final AuthStatus validateRequestAuthStatus,
            final AuthStatus secureResponseAuthStatus) throws AuthException {
        return new JaspiServerAuthContext<ServerAuthModule>(messageInfoUtils, contextHandler, sessionAuthModule,
                authModules) {

            @Override
            protected AuthStatus validateRequest(List authModules, MessageInfo messageInfo, Subject clientSubject,
                    Subject serviceSubject) throws AuthException {
                return validateRequestAuthStatus;
            }

            @Override
            protected AuthStatus secureResponse(List authModules, MessageInfo messageInfo, Subject serviceSubject)
                    throws AuthException {
                return secureResponseAuthStatus;
            }
        };
    }

    @Test
    public void shouldCreateJaspiServerAuthContextWithNoSessionAuthModuleAndEmptyListOfAuthModules()
            throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();

        //When
        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);

        //Then
        assertNotNull(jaspiServerAuthContext);
    }

    @Test
    public void shouldCreateJaspiServerAuthContextWithNoSessionAuthModuleAndNullListOfAuthModules()
            throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        //When
        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);

        //Then
        assertNotNull(jaspiServerAuthContext);
    }

    @Test
    public void shouldCreateJaspiServerAuthContext() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);

        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);

        //When
        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);

        //Then
        assertNotNull(jaspiServerAuthContext);
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowJaspiAuthExceptionWhenAuthModuleDoesNotConformToHttpServletProfile() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);

        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);

        doThrow(AuthException.class).when(contextHandler)
                .validateServerAuthModuleConformToHttpServletProfile(anyListOf(ServerAuthModule.class));

        //When
        createJaspiServerAuthContext(sessionAuthModule, authModules, null, null);

        //Then
        fail();
    }

    @Test
    public void shouldGetMessageInfoUtils() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);

        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);

        //When
        MessageInfoUtils actualMessageInfoUtils = jaspiServerAuthContext.getMessageInfoUtils();

        //Then
        assertEquals(actualMessageInfoUtils, messageInfoUtils);
    }

    @Test
    public void shouldValidateRequestWithNoAuthModules() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SUCCESS, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(messageInfoUtils).addMap(messageInfo, JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SUCCESS);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWithOnlySessionModuleSuccess() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SUCCESS);
        verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWithOnlySessionModuleSendSuccess() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler, never()).handleCompletion(messageInfo, clientSubject, AuthStatus.SEND_SUCCESS);
        verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestWithOnlySessionModuleSendContinue() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler, never()).handleCompletion(messageInfo, clientSubject, AuthStatus.SEND_CONTINUE);
        verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void validateRequestShouldThrowJaspiAuthExceptionWhenSessionModuleReturnsInvalidAuthStatus()
            throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.FAILURE);

        //When
        try {
            jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);
        } catch (JaspiAuthException e) {
            verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
            throw e;
        }

        //Then
        fail();
    }

    @Test
    public void shouldValidateRequestWithOnlySessionModuleSendFailure() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SEND_FAILURE, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> contextMap = new HashMap<String, Object>();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);
        given(messageInfoUtils.getMap(messageInfo, JaspiServerAuthContext.PRIVATE_CONTEXT_MAP_KEY))
                .willReturn(contextMap);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SEND_FAILURE);
        verify(contextHandler).audit(messageInfo, AuthStatus.SEND_FAILURE);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void validateRequestShouldThrowJaspiAuthExceptionWhenAuthModulesReturnNull() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        try {
            jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);
        } catch (JaspiAuthException e) {
            verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
            throw e;
        }

        //Then
        fail();
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void validateRequestShouldThrowJaspiAuthExceptionWhenAuthModulesReturnFailure() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.FAILURE, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        try {
            jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);
        } catch (JaspiAuthException e) {
            verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
            throw e;
        }

        //Then
        fail();
    }

    @Test
    public void validateRequestShouldNotAuditWhenAuthModulesReturnSendContinue() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SEND_CONTINUE, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SEND_CONTINUE);
        verify(contextHandler, never()).audit(eq(messageInfo), Matchers.<AuthStatus>anyObject());
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void validateRequestShouldAuditWhenAuthModulesReturnSuccess() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SUCCESS, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SUCCESS);
        verify(contextHandler).audit(messageInfo, AuthStatus.SUCCESS);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void validateRequestShouldAuditWhenAuthModulesReturnSendSuccess() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SEND_SUCCESS, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SEND_SUCCESS);
        verify(contextHandler).audit(messageInfo, AuthStatus.SEND_SUCCESS);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void validateRequestShouldAuditWhenAuthModulesReturnSendFailure() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SEND_FAILURE, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(sessionAuthModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SEND_FAILURE);
        verify(contextHandler).audit(messageInfo, AuthStatus.SEND_FAILURE);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWithNoSessionModuleAndWhenAuthModulesReturnSuccess() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, AuthStatus.SUCCESS, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        //When
        AuthStatus authStatus = jaspiServerAuthContext.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(contextHandler).handleCompletion(messageInfo, clientSubject, AuthStatus.SUCCESS);
        verify(contextHandler).audit(messageInfo, AuthStatus.SUCCESS);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void secureResponseShouldThrowJaspiAuthExceptionWhenAuthModulesReturnSuccess() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, AuthStatus.SUCCESS);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        //When
        jaspiServerAuthContext.secureResponse(messageInfo, serviceSubject);

        //Then
        fail();
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void secureResponseShouldThrowJaspiAuthExceptionWhenAuthModulesReturnFailure() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, AuthStatus.FAILURE);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        //When
        jaspiServerAuthContext.secureResponse(messageInfo, serviceSubject);

        //Then
        fail();
    }

    @Test
    public void shouldSecureResponseWhenAuthModulesReturnSendSuccessWithNoSessionAuthModule() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, AuthStatus.SEND_SUCCESS);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        //When
        AuthStatus authStatus = jaspiServerAuthContext.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldSecureResponseWhenAuthModulesReturnNullWithSessionAuthModule() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.secureResponse(messageInfo, serviceSubject)).willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldSecureResponseWhenAuthModulesReturnSendSuccessAndSessionModuleReturnsSendFailure()
            throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, AuthStatus.SEND_SUCCESS);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        given(sessionAuthModule.secureResponse(messageInfo, serviceSubject)).willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = jaspiServerAuthContext.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldCleanSubjectWithNoSessionAuthModuleAndNullAuthModules() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject subject = new Subject();

        //When
        jaspiServerAuthContext.cleanSubject(messageInfo, subject);

        //Then
    }

    @Test
    public void shouldCleanSubjectWithOnlySessionAuthModuleConfigured() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = null;

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject subject = new Subject();

        //When
        jaspiServerAuthContext.cleanSubject(messageInfo, subject);

        //Then
        verify(sessionAuthModule).cleanSubject(messageInfo, subject);
    }

    @Test
    public void shouldCleanSubjectWithOnlyAuthModulesConfigured() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = null;
        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject subject = new Subject();

        //When
        jaspiServerAuthContext.cleanSubject(messageInfo, subject);

        //Then
        verify(authModuleOne).cleanSubject(messageInfo, subject);
        verify(authModuleTwo).cleanSubject(messageInfo, subject);
    }

    @Test
    public void shouldCleanSubject() throws AuthException {

        //Given
        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);
        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(authModuleOne);
        authModules.add(authModuleTwo);

        JaspiServerAuthContext<ServerAuthModule> jaspiServerAuthContext = createJaspiServerAuthContext(
                sessionAuthModule, authModules, null, null);
        //-----------------

        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject subject = new Subject();

        //When
        jaspiServerAuthContext.cleanSubject(messageInfo, subject);

        //Then
        verify(sessionAuthModule).cleanSubject(messageInfo, subject);
        verify(authModuleOne).cleanSubject(messageInfo, subject);
        verify(authModuleTwo).cleanSubject(messageInfo, subject);
    }
}
