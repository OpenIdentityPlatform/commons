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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.forgerock.caf.authentication.framework.AuthStatusUtils.asString;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContextInfo;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthModulesTest {

    @DataProvider(name = "validValidateRequestResultValidatingModule")
    private Object[][] getValidValidateRequestResultValidatingModuleData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
        };
    }

    @Test(dataProvider = "validValidateRequestResultValidatingModule")
    public void validatingAuthModuleShouldValidateValidValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withValidation(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
    }

    @DataProvider(name = "invalidValidateRequestResultValidatingModule")
    private Object[][] getInvalidValidateRequestResultValidatingModuleData() {
        return new Object[][]{
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "invalidValidateRequestResultValidatingModule")
    public void validatingAuthModuleShouldValidateInvalidValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withValidation(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException()
                .hasMessageStartingWith("Invalid AuthStatus")
                .hasMessageContaining("validateRequest")
                .hasMessageContaining(asString(authStatus));
    }

    @DataProvider(name = "validSecureResponseResultValidatingModule")
    private Object[][] getValidSecureResponseResultValidatingModuleData() {
        return new Object[][]{
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
        };
    }

    @Test(dataProvider = "validSecureResponseResultValidatingModule")
    public void validatingAuthModuleShouldValidateValidSecureResponseResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject serviceSubject = new Subject();

        given(authModule.secureResponse(messageInfo, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withValidation(authModule)
                .secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
    }

    @DataProvider(name = "invalidSecureResponseResultValidatingModule")
    private Object[][] getInvalidSecureResponseResultValidatingModuleData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "invalidSecureResponseResultValidatingModule")
    public void validatingAuthModuleShouldValidateInvalidSecureResponseResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject serviceSubject = new Subject();

        given(authModule.secureResponse(messageInfo, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withValidation(authModule)
                .secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).failedWithException()
                .hasMessageStartingWith("Invalid AuthStatus")
                .hasMessageContaining("secureResponse")
                .hasMessageContaining(asString(authStatus));
    }

    @Test
    public void auditingAuthModuleShouldAuditPrincipalInAuditModuleInfo() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);
        requestContextMap.put(AuditTrail.AUDIT_PRINCIPAL_KEY, "PRINCIPAL_NAME");

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SUCCESS);
        Assertions.assertThat(moduleAuditInfo).containsEntry(AuditTrail.AUDIT_PRINCIPAL_KEY, "PRINCIPAL_NAME");
    }

    @DataProvider(name = "successfulValidateRequestResultAuditingModule")
    private Object[][] getSuccessfulValidateRequestResultModuleContextData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
        };
    }

    @Test(dataProvider = "successfulValidateRequestResultAuditingModule")
    public void auditingAuthModuleShouldAuditSuccessfulValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(auditTrail).auditSuccess("MODULE_ID", moduleAuditInfo);
    }

    @Test
    public void auditingAuthModuleShouldAuditFailedValidateRequestResultWithFailureReason() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();
        Map<String, Object> failureReason = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(
                        AuthStatus.SEND_FAILURE));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);
        requestContextMap.put(AuditTrail.AUDIT_FAILURE_REASON_KEY, failureReason);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_FAILURE);
        verify(auditTrail).auditFailure("MODULE_ID", failureReason, moduleAuditInfo);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void auditingAuthModuleShouldAuditFailedValidateRequestResultWithoutFailureReason() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(
                        AuthStatus.SEND_FAILURE));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_FAILURE);
        ArgumentCaptor<Map> reasonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditTrail).auditFailure(eq("MODULE_ID"), reasonCaptor.capture(), eq(moduleAuditInfo));
        Assertions.assertThat(reasonCaptor.getValue()).isEmpty();
    }

    @DataProvider(name = "invalidValidateRequestResultAuditingModule")
    private Object[][] getInvalidValidateRequestResultAuditingModuleData() {
        return new Object[][]{
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(dataProvider = "invalidValidateRequestResultAuditingModule")
    public void auditingAuthModuleShouldAuditInvalidValidateRequestResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        ArgumentCaptor<Map> reasonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditTrail).auditFailure(eq("MODULE_ID"), reasonCaptor.capture(), eq(moduleAuditInfo));
        String reasonMessage = (String) reasonCaptor.getValue().get("message");
        Assertions.assertThat(reasonMessage)
                .startsWith("Invalid AuthStatus")
                .contains("validateRequest")
                .contains(asString(authStatus));
    }

    @Test
    public void auditingAuthModuleShouldAuditValidateRequestAuthenticationExceptionWithFailureReason() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();
        Map<String, Object> failureReason = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                        new AuthenticationException("ERROR")));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);
        requestContextMap.put(AuditTrail.AUDIT_FAILURE_REASON_KEY, failureReason);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(auditTrail).auditFailure("MODULE_ID", failureReason, moduleAuditInfo);
        Assertions.assertThat(failureReason).containsEntry("exception", "ERROR");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void auditingAuthModuleShouldAuditValidateRequestAuthenticationExceptionWithoutFailureReason() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                        new AuthenticationException("ERROR")));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        ArgumentCaptor<Map> reasonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditTrail).auditFailure(eq("MODULE_ID"), reasonCaptor.capture(), eq(moduleAuditInfo));
        Assertions.assertThat(reasonCaptor.getValue()).containsEntry("exception", "ERROR");
    }

    @DataProvider(name = "validateRequestResultSessionAuditingModule")
    private Object[][] getValidateRequestResultSessionAuditingModuleData() {
        return new Object[][]{
            {AuthStatus.SUCCESS, true, true},
            {AuthStatus.SEND_SUCCESS, true, true},
            {AuthStatus.SEND_CONTINUE, false, false},
            {AuthStatus.SEND_FAILURE, true, false},
            {AuthStatus.FAILURE, true, false},
            {null, true, false},
        };
    }

    @Test(dataProvider = "validateRequestResultSessionAuditingModule")
    public void sessionAuditingAuthModuleShouldAuditValidateRequestResult(AuthStatus authStatus,
            boolean shouldBeAudited, boolean isSuccessful) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();


        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);
        requestContextMap.put(AuditTrail.AUDIT_SESSION_ID_KEY, "SESSION_ID");
        requestContextMap.put(AuditTrail.AUDIT_FAILURE_REASON_KEY, new HashMap<String, Object>());

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withSessionAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        if (shouldBeAudited && isSuccessful) {
            verify(auditTrail).auditSuccess("MODULE_ID", moduleAuditInfo);
        } else if (shouldBeAudited) {
            verify(auditTrail).auditFailure(eq("MODULE_ID"), anyMapOf(String.class, Object.class), eq(moduleAuditInfo));
        }
        verify(auditTrail).setSessionId("SESSION_ID");
        Assertions.assertThat(requestContextMap)
                .doesNotContainKey(AuditTrail.AUDIT_INFO_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_FAILURE_REASON_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_SESSION_ID_KEY);
    }

    @Test
    public void sessionAuditingAuthModuleShouldAuditValidateRequestAuthenticationException() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);
        Map<String, Object> moduleAuditInfo = new HashMap<String, Object>();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                        new AuthenticationException("ERROR")));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, moduleAuditInfo);
        requestContextMap.put(AuditTrail.AUDIT_FAILURE_REASON_KEY, new HashMap<String, Object>());
        requestContextMap.put(AuditTrail.AUDIT_SESSION_ID_KEY, "SESSION_ID");

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withSessionAuditing(authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(auditTrail).auditFailure(eq("MODULE_ID"), anyMapOf(String.class, Object.class), eq(moduleAuditInfo));
        verify(auditTrail).setSessionId("SESSION_ID");
        Assertions.assertThat(requestContextMap)
                .doesNotContainKey(AuditTrail.AUDIT_INFO_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_FAILURE_REASON_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_SESSION_ID_KEY);
    }

    @DataProvider(name = "secureResponseResultSessionAuditingModule")
    private Object[][] getSecureResponseResultSessionAuditingModuleData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "secureResponseResultSessionAuditingModule")
    public void sessionAuditingAuthModuleShouldAuditSecureResponseResult(AuthStatus authStatus) {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(authModule.secureResponse(messageInfo, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, new HashMap<String, Object>());
        requestContextMap.put(AuditTrail.AUDIT_FAILURE_REASON_KEY, new HashMap<String, Object>());
        requestContextMap.put(AuditTrail.AUDIT_SESSION_ID_KEY, "SESSION_ID");

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withSessionAuditing(authModule)
                .secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(auditTrail).setSessionId("SESSION_ID");
        Assertions.assertThat(requestContextMap)
                .doesNotContainKey(AuditTrail.AUDIT_INFO_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_FAILURE_REASON_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_SESSION_ID_KEY);
    }

    @Test
    public void sessionAuditingAuthModuleShouldAuditSecureResponseAuthenticationException() {

        //Given
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject serviceSubject = new Subject();
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(authModule.secureResponse(messageInfo, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                        new AuthenticationException("ERROR")));
        given(messageInfo.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);
        requestContextMap.put(AuditTrail.AUDIT_INFO_KEY, new HashMap<String, Object>());
        requestContextMap.put(AuditTrail.AUDIT_FAILURE_REASON_KEY, new HashMap<String, Object>());
        requestContextMap.put(AuditTrail.AUDIT_SESSION_ID_KEY, "SESSION_ID");

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withSessionAuditing(authModule)
                .secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(auditTrail).setSessionId("SESSION_ID");
        Assertions.assertThat(requestContextMap)
                .doesNotContainKey(AuditTrail.AUDIT_INFO_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_FAILURE_REASON_KEY)
                .doesNotContainKey(AuditTrail.AUDIT_SESSION_ID_KEY);
    }

    @DataProvider(name = "initializeResultLoggingModule")
    private Object[][] getInitializeResultLoggingModuleData() {
        return new Object[][]{
            {true},
            {false},
        };
    }

    @Test(dataProvider = "initializeResultLoggingModule")
    public void loggingAuthModuleShouldLogInitializeResult(boolean logLevelEnabled) {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        Map<String, Object> options = new HashMap<String, Object>();

        given(authModule.initialize(requestPolicy, responsePolicy, handler, options))
                .willReturn(Promises.<Void, AuthenticationException>newSuccessfulPromise(null));
        given(logger.isDebugEnabled()).willReturn(logLevelEnabled);

        //When
        Promise<Void, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .initialize(requestPolicy, responsePolicy, handler, options);

        //Then
        assertThat(promise).succeeded();
        if (logLevelEnabled) {
            verify(logger).debug(anyString(), anyObject(), anyObject(), anyObject(), anyObject());
        } else {
            verify(logger, never()).debug(anyString(), anyObject(), anyObject(), anyObject(), anyObject());
        }
    }

    @Test(dataProvider = "initializeResultLoggingModule")
    public void loggingAuthModuleShouldLogInitializeAuthenticationException(boolean logLevelEnabled) {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        Map<String, Object> options = new HashMap<String, Object>();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authModule.initialize(requestPolicy, responsePolicy, handler, options))
                .willReturn(Promises.<Void, AuthenticationException>newFailedPromise(exception));
        given(logger.isErrorEnabled()).willReturn(logLevelEnabled);

        //When
        Promise<Void, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .initialize(requestPolicy, responsePolicy, handler, options);

        //Then
        assertThat(promise).failedWithException();
        if (logLevelEnabled) {
            verify(logger).error(anyString(), anyObject(), anyObject(), anyObject(), anyObject(), eq(exception));
        } else {
            verify(logger, never()).error(anyString(), anyObject(), anyObject(), anyObject(), anyObject(),
                    eq(exception));
        }
    }

    @DataProvider(name = "validateRequestResultLoggingModule")
    private Object[][] getValidateRequestResultLoggingModuleData() {
        return new Object[][]{
            {AuthStatus.SUCCESS, "success", false},
            {AuthStatus.SEND_SUCCESS, "may have", false},
            {AuthStatus.SEND_CONTINUE, "has not", false},
            {AuthStatus.SEND_FAILURE, "failed", false},
            {AuthStatus.FAILURE, "invalid AuthStatus", true},
            {null, "invalid AuthStatus", true},
        };
    }

    @Test(dataProvider = "validateRequestResultLoggingModule")
    public void loggingAuthModuleShouldLogValidateRequestResult(AuthStatus authStatus, String expectedMessage,
            boolean isError) {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(logger.isDebugEnabled()).willReturn(true);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        if (isError) {
            verify(logger).error(contains(expectedMessage), eq("MODULE_ID"), eq(asString(authStatus)));
        } else {
            verify(logger).debug(contains(expectedMessage), eq("MODULE_ID"));
        }
    }

    @Test
    public void loggingAuthModuleShouldLogValidateRequestAuthenticationException() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(exception));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(logger).error(contains("thrown an error"), eq("MODULE_ID"), eq(exception));
    }

    @DataProvider(name = "secureResponseResultLoggingModule")
    private Object[][] getSecureResponseResultLoggingModuleData() {
        return new Object[][]{
            {AuthStatus.SEND_SUCCESS, "may have", false},
            {AuthStatus.SEND_CONTINUE, "has not", false},
            {AuthStatus.SEND_FAILURE, "failed", false},
            {AuthStatus.SUCCESS, "invalid AuthStatus", true},
            {AuthStatus.FAILURE, "invalid AuthStatus", true},
            {null, "invalid AuthStatus", true},
        };
    }

    @Test(dataProvider = "secureResponseResultLoggingModule")
    public void loggingAuthModuleShouldLogSecureResponseResult(AuthStatus authStatus, String expectedMessage,
            boolean isError) {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject serviceSubject = new Subject();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.secureResponse(messageInfo, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        given(logger.isDebugEnabled()).willReturn(true);

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        if (isError) {
            verify(logger).error(contains(expectedMessage), eq("MODULE_ID"), eq(asString(authStatus)));
        } else {
            verify(logger).debug(contains(expectedMessage), eq("MODULE_ID"));
        }
    }

    @Test
    public void loggingAuthModuleShouldLogSecureResponseAuthenticationException() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject serviceSubject = new Subject();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.secureResponse(messageInfo, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newFailedPromise(exception));

        //When
        Promise<AuthStatus, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessage("ERROR");
        verify(logger).error(contains("thrown an error"), eq("MODULE_ID"), eq(exception));
    }

    @Test
    public void loggingAuthModuleShouldLogCleanSubjectResult() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.cleanSubject(messageInfo, clientSubject))
                .willReturn(Promises.<Void, AuthenticationException>newSuccessfulPromise(null));

        //When
        Promise<Void, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .cleanSubject(messageInfo, clientSubject);

        //Then
        assertThat(promise).succeeded();
        verify(logger).debug(contains("success"), eq("MODULE_ID"));
    }

    @Test
    public void loggingAuthModuleShouldLogCleanSubjectAuthenticationException() {

        //Given
        Logger logger = mock(Logger.class);
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);
        MessageContextInfo messageInfo = mock(MessageContextInfo.class);
        Subject clientSubject = new Subject();
        AuthenticationException exception = new AuthenticationException("ERROR");

        given(authModule.getModuleId()).willReturn("MODULE_ID");
        given(authModule.cleanSubject(messageInfo, clientSubject))
                .willReturn(Promises.<Void, AuthenticationException>newFailedPromise(exception));

        //When
        Promise<Void, AuthenticationException> promise = AuthModules.withLogging(logger, authModule)
                .cleanSubject(messageInfo, clientSubject);

        //Then
        assertThat(promise).failedWithException();
        verify(logger).error(contains("failed"), eq("MODULE_ID"), eq(exception));
    }
}
