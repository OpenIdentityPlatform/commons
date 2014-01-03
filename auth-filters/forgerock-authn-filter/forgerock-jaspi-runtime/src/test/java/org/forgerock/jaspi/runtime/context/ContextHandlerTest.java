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

package org.forgerock.jaspi.runtime.context;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ContextHandlerTest {

    private ContextHandler contextHandler;

    private MessageInfoUtils messageInfoUtils;

    @BeforeMethod
    public void setUp() {

        messageInfoUtils = mock(MessageInfoUtils.class);

        contextHandler = new ContextHandler(messageInfoUtils);
    }

    @Test
    public void shouldValidateServerAuthModulesWhenAuthModulesNull() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
    }

    @SuppressWarnings("rawtypes")
    @Test  (expectedExceptions = JaspiAuthException.class)
    public void validateServerAuthModulesShouldThrowJaspiAuthExceptionWhenDoesNotSupportEither() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[0]);

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
        fail();
    }

    @SuppressWarnings("rawtypes")
    @Test  (expectedExceptions = JaspiAuthException.class)
    public void validateServerAuthModulesShouldThrowJaspiAuthExceptionWhenOnlySupportsHttpServletRequest()
            throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[]{HttpServletRequest.class});

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
        fail();
    }

    @SuppressWarnings("rawtypes")
    @Test (expectedExceptions = JaspiAuthException.class)
    public void validateServerAuthModulesShouldThrowJaspiAuthExceptionWhenOnlySupportsHttpServletResponse()
            throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[]{HttpServletResponse.class});

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
        fail();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldValidateServerAuthModules() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes())
                .willReturn(new Class[]{HttpServletRequest.class, HttpServletResponse.class});

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
    }

    @Test
    public void shouldValidateServerAuthModulesWhenAuthModuleListContainsNullAuthModule() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(null);

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
    }

    @Test
    public void shouldHandleCompletionWhenAuthStatusIsNull() throws AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = null;
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter writer = mock(PrintWriter.class);

        given(messageInfo.getResponseMessage()).willReturn(response);
        given(response.getWriter()).willReturn(writer);

        //When
        contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

        //Then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(contentCaptor.capture());
        verify(response).setContentType("application/json");
        assertTrue(contentCaptor.getValue().contains("401"));
        assertTrue(contentCaptor.getValue().contains("Unauthorized"));
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void handleCompletionShouldThrowJaspiAuthExceptionWhenAuthStatusIsNullAndWriterThrowsIOException()
            throws AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = null;
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter writer = mock(PrintWriter.class);

        given(messageInfo.getResponseMessage()).willReturn(response);
        given(response.getWriter()).willReturn(writer);
        doThrow(IOException.class).when(writer).write(anyString());

        //When
        try {
            contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);
        } catch (JaspiAuthException e) {
            verify(response).setContentType("application/json");
            throw e;
        }

        //Then
        fail();
    }

    @Test
    public void shouldHandleCompletionWhenAuthStatusIsNotNullAndPrincipalNameNotSet() throws AuthException,
            IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = AuthStatus.SUCCESS;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> contextMap = new HashMap<String, Object>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfoUtils.getMap(messageInfo, JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT)).willReturn(contextMap);

        //When
        contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

        //Then
        verify(request).setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        verify(messageInfo, never()).setRequestMessage(anyObject());
    }

    @Test
    public void shouldHandleCompletionWhenAuthStatusIsNotNull() throws AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = AuthStatus.SUCCESS;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        Principal principalOne = mock(Principal.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfoUtils.getMap(messageInfo, JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT)).willReturn(contextMap);
        clientSubject.getPrincipals().add(principalOne);
        given(principalOne.getName()).willReturn("PRN_ONE");

        //When
        contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

        //Then
        verify(request).setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_PRINCIPAL, "PRN_ONE");
        verify(request).setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, contextMap);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditWhenAuthStatusSuccess() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthStatus authStatus = AuthStatus.SUCCESS;
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        LogFactory.setAuditLogger(auditLogger);

        //When
        contextHandler.audit(messageInfo, authStatus);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        AuditRecord<MessageInfo> auditRecord = auditRecordCaptor.getValue();
        assertEquals(auditRecord.getAuditObject(), messageInfo);
        assertEquals(auditRecord.getAuthResult(), AuthResult.SUCCESS);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditWhenAuthStatusSendSuccess() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthStatus authStatus = AuthStatus.SEND_SUCCESS;
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        LogFactory.setAuditLogger(auditLogger);

        //When
        contextHandler.audit(messageInfo, authStatus);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        AuditRecord<MessageInfo> auditRecord = auditRecordCaptor.getValue();
        assertEquals(auditRecord.getAuditObject(), messageInfo);
        assertEquals(auditRecord.getAuthResult(), AuthResult.SUCCESS);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditWhenAuthStatusSendContinue() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthStatus authStatus = AuthStatus.SEND_CONTINUE;
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        LogFactory.setAuditLogger(auditLogger);

        //When
        contextHandler.audit(messageInfo, authStatus);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        AuditRecord<MessageInfo> auditRecord = auditRecordCaptor.getValue();
        assertEquals(auditRecord.getAuditObject(), messageInfo);
        assertEquals(auditRecord.getAuthResult(), AuthResult.FAILURE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditWhenAuthStatusSendFailure() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthStatus authStatus = AuthStatus.SEND_FAILURE;
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        LogFactory.setAuditLogger(auditLogger);

        //When
        contextHandler.audit(messageInfo, authStatus);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        AuditRecord<MessageInfo> auditRecord = auditRecordCaptor.getValue();
        assertEquals(auditRecord.getAuditObject(), messageInfo);
        assertEquals(auditRecord.getAuthResult(), AuthResult.FAILURE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditWhenAuthStatusFailure() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthStatus authStatus = AuthStatus.FAILURE;
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        LogFactory.setAuditLogger(auditLogger);

        //When
        contextHandler.audit(messageInfo, authStatus);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        AuditRecord<MessageInfo> auditRecord = auditRecordCaptor.getValue();
        assertEquals(auditRecord.getAuditObject(), messageInfo);
        assertEquals(auditRecord.getAuthResult(), AuthResult.FAILURE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditWhenAuthStatusNull() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthStatus authStatus = null;
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        LogFactory.setAuditLogger(auditLogger);

        //When
        contextHandler.audit(messageInfo, authStatus);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        AuditRecord<MessageInfo> auditRecord = auditRecordCaptor.getValue();
        assertEquals(auditRecord.getAuditObject(), messageInfo);
        assertEquals(auditRecord.getAuthResult(), AuthResult.FAILURE);
    }
}
