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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpContext;
import org.forgerock.http.RootContext;
import org.forgerock.http.Session;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthenticationFrameworkTest {

    private AuthenticationFramework runtime;

    private AuditApi auditApi;
    private ResponseHandler responseHandler;
    private AsyncServerAuthContext authContext;
    private Subject serviceSubject;

    private final Response successfulResponse = new Response().setStatusAndReason(200);
    private final Response unauthenticatedResponse = new Response().setStatusAndReason(401);
    private final Response failedResponse = new Response().setStatusAndReason(400);
    private final Response serverErrorResponse = new Response().setStatusAndReason(500);

    @BeforeMethod
    public void setup() {
        auditApi = mock(AuditApi.class);
        responseHandler = mock(ResponseHandler.class);
        authContext = mock(AsyncServerAuthContext.class);
        serviceSubject = new Subject();
        Promise<List<Void>, AuthenticationException> initializationPromise =
                Promises.newSuccessfulPromise(Collections.<Void>emptyList());

        runtime = createRuntime(initializationPromise);
    }

    private AuthenticationFramework createRuntime(Promise<List<Void>, AuthenticationException> initializationPromise) {
        Logger logger = mock(Logger.class);
        return new AuthenticationFramework(logger, auditApi, responseHandler, authContext, serviceSubject,
                initializationPromise);
    }

    private HttpContext mockContext() {
        Session session = mock(Session.class);
        return new HttpContext(new RootContext(), session);
    }

    private Handler mockHandler(Request request, Promise<Response, ResponseException> response) {
        Handler next = mock(Handler.class);
        given(next.handle(Matchers.<Context>anyObject(), eq(request))).willReturn(response);
        return next;
    }

    private void mockAuthContext(Promise<AuthStatus, AuthenticationException> validateRequestResult) {
        mockAuthContext(validateRequestResult, null,
                Promises.<Void, AuthenticationException>newSuccessfulPromise(null));
    }

    private void mockAuthContext(Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult) {
        mockAuthContext(validateRequestResult, secureResponseResult,
                Promises.<Void, AuthenticationException>newSuccessfulPromise(null));
    }

    private void mockAuthContext(Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult,
            Promise<Void, AuthenticationException> cleanSubjectResult) {
        given(authContext.validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject))).willReturn(validateRequestResult);
        given(authContext.secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject)))
                .willReturn(secureResponseResult);
        given(authContext.cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(cleanSubjectResult);
    }

    @Test
    public void whenInitializationFailsExceptionShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        runtime = createRuntime(Promises.<List<Void>, AuthenticationException>newFailedPromise(
                new AuthenticationException("ERROR")));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        verify(responseHandler).handle(Matchers.<MessageContext>anyObject(),
                Matchers.<AuthenticationException>anyObject());
        verify(authContext, never()).validateRequest(Matchers.<MessageContext>anyObject(),
                Matchers.<Subject>anyObject(), eq(serviceSubject));
        verify(authContext, never()).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext, never()).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @Test
    public void whenInitializationFailsWithResourceExceptionItShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        ResourceException resourceException = mock(ResourceException.class);
        runtime = createRuntime(Promises.<List<Void>, AuthenticationException>newFailedPromise(
                new AuthenticationException("ERROR", resourceException)));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        verify(responseHandler).handle(Matchers.<MessageContext>anyObject(),
                Matchers.<AuthenticationException>anyObject());
        verify(authContext, never()).validateRequest(Matchers.<MessageContext>anyObject(),
                Matchers.<Subject>anyObject(), eq(serviceSubject));
        verify(authContext, never()).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext, never()).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @Test
    public void whenMessageProcessingSucceedsResourceResponseShouldBeReturned() {

        //Given
        HttpContext context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_SUCCESS));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(successfulResponse);
        Assertions.assertThat(context.getAttributes()).containsKey(AuthenticationFramework.ATTRIBUTE_REQUEST_ID);
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());

        Assertions.assertThat(context.getAttributes())
                .containsKeys(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL, AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT);
    }

    @Test
    public void whenValidateRequestReturnSendFailureShouldReturnAccessDeniedResponse() throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_FAILURE));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            Assertions.failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResponseException error) {
            Assertions.assertThat(error.getResponse().getStatus()).isEqualTo(unauthenticatedResponse.getStatus());
            verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                    eq(serviceSubject));
            verify(authContext, never()).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
            verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
        }
    }

    @DataProvider(name = "validateRequestResponse")
    private Object[][] getValidateRequestResponseData() {
        return new Object[][]{
            {AuthStatus.SEND_SUCCESS},
            {AuthStatus.SEND_CONTINUE},
        };
    }

    @Test(dataProvider = "validateRequestResponse")
    public void whenMessageProcessingStopsAfterValidatingRequestResponseShouldBeReturned(AuthStatus authStatus)
            throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded();
        Assertions.assertThat(promise.getOrThrowUninterruptibly().getStatus()).isEqualTo(200);
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext, never()).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @DataProvider(name = "invalidValidateRequestResults")
    private Object[][] getInvalidValidateRequestResultsData() {
        return new Object[][]{
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "invalidValidateRequestResults")
    public void whenValidateRequestReturnsInvalidResultExceptionShouldBeWrittenToResponse(AuthStatus authStatus) {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        verify(responseHandler).handle(Matchers.<MessageContext>anyObject(),
                Matchers.<AuthenticationException>anyObject());
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext, never()).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @Test
    public void whenValidateRequestReturnsAuthenticationExceptionItShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newFailedPromise(
                new AuthenticationException("ERROR")));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        verify(responseHandler).handle(Matchers.<MessageContext>anyObject(),
                Matchers.<AuthenticationException>anyObject());
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext, never()).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @DataProvider(name = "secureResponseResponse")
    private Object[][] getSecureResponseResponseData() {
        return new Object[][]{
            {AuthStatus.SEND_FAILURE, serverErrorResponse},
            {AuthStatus.SEND_CONTINUE, successfulResponse},
        };
    }

    @Test(dataProvider = "secureResponseResponse")
    public void whenMessageProcessingStopsAfterSecureResponseTheResponseShouldBeReturned(AuthStatus authStatus,
            Response expectedResponse) throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded();
        Assertions.assertThat(promise.getOrThrowUninterruptibly().getStatus()).isEqualTo(expectedResponse.getStatus());
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @DataProvider(name = "invalidSecureResponseResults")
    private Object[][] getInvalidSecureResponseResultsData() {
        return new Object[][]{
            {AuthStatus.SUCCESS},
            {AuthStatus.FAILURE},
            {null},
        };
    }

    @Test(dataProvider = "invalidSecureResponseResults")
    public void whenSecureResponseReturnsInvalidResultExceptionShouldBeWrittenToResponse(AuthStatus authStatus) {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(authStatus));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        verify(responseHandler).handle(Matchers.<MessageContext>anyObject(),
                Matchers.<AuthenticationException>anyObject());
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @Test
    public void whenSecureResponseReturnsAuthenticationExceptionItShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newSuccessfulPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newFailedPromise(new AuthenticationException("ERROR")));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).failedWithException();
        verify(responseHandler).handle(Matchers.<MessageContext>anyObject(),
                Matchers.<AuthenticationException>anyObject());
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }

    @Test
    public void whenResourceReturnsResponseExceptionItShouldBeSecuredAndReturned() throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, ResponseException>newFailedPromise(new ResponseException(failedResponse)));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newSuccessfulPromise(AuthStatus.SEND_SUCCESS));

        //When
        Promise<Response, ResponseException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded();
        Assertions.assertThat(promise.getOrThrowUninterruptibly().getStatus()).isEqualTo(failedResponse.getStatus());
        verify(authContext).validateRequest(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject(),
                eq(serviceSubject));
        verify(authContext).secureResponse(Matchers.<MessageContext>anyObject(), eq(serviceSubject));
        verify(authContext).cleanSubject(Matchers.<MessageContext>anyObject(), Matchers.<Subject>anyObject());
    }
}
