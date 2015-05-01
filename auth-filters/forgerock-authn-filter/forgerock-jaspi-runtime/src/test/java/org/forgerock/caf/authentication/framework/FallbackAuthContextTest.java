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

import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.AuthenticationState;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.api.MessageContextInfo;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FallbackAuthContextTest {

    private FallbackAuthContext authContext;

    private AuthenticationState state;

    @AfterMethod
    public void tearDown() {
        authContext = null;
        state = null;
    }

    private FallbackAuthContext createFallbackAuthContext(AsyncServerAuthModule... authModules) {
        Logger logger = mock(Logger.class);
        return new FallbackAuthContext(logger, Arrays.asList(authModules));
    }

    private AsyncServerAuthModule mockAuthModule(Promise<AuthStatus, AuthenticationException> validateRequestResult) {
        return mockAuthModule(validateRequestResult, null, null);
    }

    private AsyncServerAuthModule mockAuthModule(Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult) {
        return mockAuthModule(validateRequestResult, secureResponseResult, null);
    }

    private AsyncServerAuthModule mockAuthModule(Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult,
            Promise<Void, AuthenticationException> cleanSubjectResult) {
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);

        given(authModule.validateRequest(Matchers.<MessageContextInfo>anyObject(), Matchers.<Subject>anyObject(),
                Matchers.<Subject>anyObject())).willReturn(validateRequestResult);
        given(authModule.secureResponse(Matchers.<MessageContextInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(secureResponseResult);
        given(authModule.cleanSubject(Matchers.<MessageContextInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(cleanSubjectResult);

        return authModule;
    }

    private MessageContext mockMessageContext() {
        MessageContext context = mock(MessageContext.class);
        Map<String, Object> requestContextMap = new HashMap<String, Object>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(context.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);

        when(context.getState(Matchers.<AsyncServerAuthContext>anyObject()))
                .thenAnswer(new Answer<AuthenticationState>() {
                    @Override
                    public AuthenticationState answer(InvocationOnMock invocationOnMock) {
                        if (state == null) {
                            state = authContext.createAuthenticationState();
                        }
                        return state;
                    }
                });

        return context;
    }

    @Test
    public void whenNoAuthModulesConfiguredValidateRequestShouldReturnSendFailure() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        authContext = createFallbackAuthContext();

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_FAILURE);
    }

    @DataProvider(name = "authModuleOneValidateRequestResults")
    private Object[][] getAuthModuleOneValidateRequestResultsData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
        };
    }

    @Test(dataProvider = "authModuleOneValidateRequestResults")
    public void validateRequestShouldCallFirstAuthModule(AuthStatus authStatus) {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AsyncServerAuthModule authModuleOne = mockAuthModule(
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));
        AsyncServerAuthModule authModuleTwo = mockAuthModule(
                Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                        new AuthenticationException("ERROR")));

        authContext = createFallbackAuthContext(authModuleOne, authModuleTwo);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(authModuleOne).validateRequest(context, clientSubject, serviceSubject);
        verify(authModuleTwo, never()).validateRequest(context, clientSubject, serviceSubject);
    }

    @DataProvider(name = "authModuleTwoValidateRequestResults")
    private Object[][] getAuthModuleTwoValidateRequestResultsData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
        };
    }

    @Test(dataProvider = "authModuleTwoValidateRequestResults")
    public void whenFirstAuthModuleReturnsSendFailureValidateRequestShouldCallNextAuthModule(AuthStatus authStatus) {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AsyncServerAuthModule authModuleOne = mockAuthModule(
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_FAILURE));
        AsyncServerAuthModule authModuleTwo = mockAuthModule(
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        authContext = createFallbackAuthContext(authModuleOne, authModuleTwo);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(authModuleOne).validateRequest(context, clientSubject, serviceSubject);
        verify(authModuleTwo).validateRequest(context, clientSubject, serviceSubject);
    }

    @Test
    public void whenNoAuthModuleAuthenticatedTheRequestSecureResponseShouldThrowAuthenticationException() {

        //Given
        MessageContext context = mockMessageContext();
        Subject serviceSubject = new Subject();

        authContext = createFallbackAuthContext();

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).failedWithException().hasMessageStartingWith("No auth module authenticated the incoming "
                + "request message");
    }

    @Test
    public void whenAuthModuleAuthenticatedRequestSecureResponseShouldCallAuthenticatingAuthModule() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AsyncServerAuthModule authModuleTwo = mockAuthModule(
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_SUCCESS));

        authContext = createFallbackAuthContext(
                mockAuthModule(
                        Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_FAILURE),
                        Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_FAILURE)),
                authModuleTwo);

        authContext.validateRequest(context, clientSubject, serviceSubject);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
        verify(authModuleTwo).secureResponse(context, serviceSubject);
    }

    @Test
    public void cleanSubjectShouldCallAllAuthModules() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        AsyncServerAuthModule authModuleOne = mockAuthModule(null, null,
                Promises.<Void, AuthenticationException>newSuccessfulPromise(null));
        AsyncServerAuthModule authModuleTwo = mockAuthModule(null, null,
                Promises.<Void, AuthenticationException>newSuccessfulPromise(null));

        authContext = createFallbackAuthContext(authModuleOne, authModuleTwo);

        //When
        Promise<Void, AuthenticationException> promise = authContext.cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).succeeded();
        verify(authModuleOne).cleanSubject(context, clientSubject);
        verify(authModuleTwo).cleanSubject(context, clientSubject);
    }

    @Test
    public void cleanSubjectShouldReportExceptionFromAuthModules() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        AsyncServerAuthModule authModuleOne = mockAuthModule(null, null,
                Promises.<Void, AuthenticationException>newFailedPromise(new AuthenticationException("ERROR")));
        AsyncServerAuthModule authModuleTwo = mockAuthModule(null, null,
                Promises.<Void, AuthenticationException>newSuccessfulPromise(null));

        authContext = createFallbackAuthContext(authModuleOne, authModuleTwo);

        //When
        Promise<Void, AuthenticationException> promise = authContext.cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).failedWithException();
        verify(authModuleOne).cleanSubject(context, clientSubject);
        verify(authModuleTwo).cleanSubject(context, clientSubject);
    }
}
