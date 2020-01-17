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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.forgerock.util.test.assertj.AssertJPromiseAssert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.AuthenticationState;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AggregateAuthContextTest {

    private AggregateAuthContext authContext;

    private AsyncServerAuthContext sessionModuleContext;
    private AsyncServerAuthContext authModuleContext;
    private AuthenticationState state;

    @BeforeMethod
    public void setup() {
        Logger logger = mock(Logger.class);
        sessionModuleContext = mock(AsyncServerAuthContext.class);
        authModuleContext = mock(AsyncServerAuthContext.class);

        authContext = new AggregateAuthContext(logger, sessionModuleContext, authModuleContext);
    }

    @AfterMethod
    public void tearDown() {
        state = null;
    }

    private void mockAuthContext(AsyncServerAuthContext authContext,
            Promise<AuthStatus, AuthenticationException> validateRequestResult) {
        mockAuthContext(authContext, validateRequestResult, null, null);
    }

    private void mockAuthContext(AsyncServerAuthContext authContext,
            Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult) {
        mockAuthContext(authContext, validateRequestResult, secureResponseResult, null);
    }

    private void mockAuthContext(AsyncServerAuthContext authContext,
            Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult,
            Promise<Void, AuthenticationException> cleanSubjectResult) {
        given(authContext.validateRequest(any(MessageContext.class), any(Subject.class), any(Subject.class)))
                .willReturn(validateRequestResult);
        given(authContext.secureResponse(any(MessageContext.class), any(Subject.class)))
                .willReturn(secureResponseResult);
        given(authContext.cleanSubject(any(MessageContext.class), any(Subject.class)))
                .willReturn(cleanSubjectResult);
    }

    private MessageContext mockMessageContext() {
        MessageContext context = mock(MessageContext.class);

        when(context.getState(any(AsyncServerAuthContext.class)))
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

    @DataProvider(name = "successfulSessionValidateRequestResults")
    private Object[][] getSuccessfulSessionValidateRequestResultsData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
        };
    }

    @Test(dataProvider = "successfulSessionValidateRequestResults")
    public void whenSessionAuthContextReturnsSuccessfulResultTheAuthModuleContextShouldNotBeCalled(
            AuthStatus authStatus) {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        mockAuthContext(sessionModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(sessionModuleContext).validateRequest(context, clientSubject, serviceSubject);
        verify(authModuleContext, never()).validateRequest(context, clientSubject, serviceSubject);
    }

    @DataProvider(name = "authModuleValidateRequestResults")
    private Object[][] getAuthModuleValidateRequestResultsData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
            {AuthStatus.SEND_FAILURE},
        };
    }

    @Test(dataProvider = "authModuleValidateRequestResults")
    public void whenSessionAuthContextReturnsSendFailureTheAuthModuleContextShouldBeCalled(AuthStatus authStatus) {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        mockAuthContext(sessionModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_FAILURE));
        mockAuthContext(authModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(authStatus));

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(authStatus);
        verify(sessionModuleContext).validateRequest(context, clientSubject, serviceSubject);
        verify(authModuleContext).validateRequest(context, clientSubject, serviceSubject);
    }

    @Test
    public void whenAuthModuleContextDidNotAuthenticateTheRequestSecureResponseShouldOnlyCallSessionAuthContext() {

        //Given
        MessageContext context = mockMessageContext();
        Subject serviceSubject = new Subject();

        mockAuthContext(sessionModuleContext, null,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
        verify(sessionModuleContext).secureResponse(context, serviceSubject);
        verify(authModuleContext, never()).secureResponse(context, serviceSubject);
    }

    @Test
    public void whenAuthModuleContextAuthenticatesTheRequestSecureResponseShouldCallBothAuthContexts() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        mockAuthContext(sessionModuleContext, null,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        mockAuthContext(sessionModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_FAILURE),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));
        mockAuthContext(authModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        authContext.validateRequest(context, clientSubject, serviceSubject);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
        verify(sessionModuleContext).secureResponse(context, serviceSubject);
        verify(authModuleContext).secureResponse(context, serviceSubject);
    }

    @Test
    public void whenAuthModuleContextDoesNotReturnSendSuccessFromSecureSubjectSessionAuthContextShouldNotBeCalled() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        mockAuthContext(sessionModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_FAILURE),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));
        mockAuthContext(authModuleContext,
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_CONTINUE));

        authContext.validateRequest(context, clientSubject, serviceSubject);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_CONTINUE);
        verify(sessionModuleContext, never()).secureResponse(context, serviceSubject);
        verify(authModuleContext).secureResponse(context, serviceSubject);
    }

    @Test
    public void cleanSubjectShouldCallAllAuthContexts() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();

        mockAuthContext(sessionModuleContext, null, null,
                Promises.<Void, AuthenticationException>newResultPromise(null));
        mockAuthContext(authModuleContext, null, null,
                Promises.<Void, AuthenticationException>newResultPromise(null));

        //When
        Promise<Void, AuthenticationException> promise = authContext.cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).succeeded();
        verify(sessionModuleContext).cleanSubject(context, clientSubject);
        verify(authModuleContext).cleanSubject(context, clientSubject);
    }

    @Test
    public void cleanSubjectShouldReportExceptionFromAuthContexts() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();

        mockAuthContext(sessionModuleContext, null, null,
                Promises.<Void, AuthenticationException>newExceptionPromise(new AuthenticationException("ERROR")));
        mockAuthContext(authModuleContext, null, null,
                Promises.<Void, AuthenticationException>newResultPromise(null));

        //When
        Promise<Void, AuthenticationException> promise = authContext.cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).failedWithException();
        verify(sessionModuleContext).cleanSubject(context, clientSubject);
        verify(authModuleContext).cleanSubject(context, clientSubject);
    }
}
