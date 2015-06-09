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
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.promise.NeverThrowsException;
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

    private final Response successfulResponse = new Response().setStatus(Status.OK);
    private final Response unauthenticatedResponse = new Response().setStatus(Status.UNAUTHORIZED);
    private final Response failedResponse = new Response().setStatus(Status.BAD_REQUEST);
    private final Response serverErrorResponse = new Response().setStatus(Status.INTERNAL_SERVER_ERROR);

    @BeforeMethod
    public void setup() {
        auditApi = mock(AuditApi.class);
        responseHandler = mock(ResponseHandler.class);
        authContext = mock(AsyncServerAuthContext.class);
        serviceSubject = new Subject();
        Promise<List<Void>, AuthenticationException> initializationPromise =
                Promises.newResultPromise(Collections.<Void>emptyList());

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

    private Handler mockHandler(Request request, Promise<Response, NeverThrowsException> response) {
        Handler next = mock(Handler.class);
        given(next.handle(any(Context.class), eq(request))).willReturn(response);
        return next;
    }

    private void mockAuthContext(Promise<AuthStatus, AuthenticationException> validateRequestResult) {
        mockAuthContext(validateRequestResult, null,
                Promises.<Void, AuthenticationException>newResultPromise(null));
    }

    private void mockAuthContext(Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult) {
        mockAuthContext(validateRequestResult, secureResponseResult,
                Promises.<Void, AuthenticationException>newResultPromise(null));
    }

    private void mockAuthContext(Promise<AuthStatus, AuthenticationException> validateRequestResult,
            Promise<AuthStatus, AuthenticationException> secureResponseResult,
            Promise<Void, AuthenticationException> cleanSubjectResult) {
        given(authContext.validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject)))
                .willReturn(validateRequestResult);
        given(authContext.secureResponse(any(MessageContext.class), eq(serviceSubject)))
                .willReturn(secureResponseResult);
        given(authContext.cleanSubject(any(MessageContext.class), any(Subject.class)))
                .willReturn(cleanSubjectResult);
    }

    @Test
    public void whenInitializationFailsExceptionShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        runtime = createRuntime(Promises.<List<Void>, AuthenticationException>newExceptionPromise(
                new AuthenticationException("ERROR")));

        //When
        runtime.processMessage(context, request, next);

        //Then
        verify(responseHandler).handle(any(MessageContext.class), any(AuthenticationException.class));
        verify(authContext, never()).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext, never()).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext, never()).cleanSubject(any(MessageContext.class), any(Subject.class));
    }

    @Test
    public void whenInitializationFailsWithResourceExceptionItShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        ResourceException resourceException = mock(ResourceException.class);
        runtime = createRuntime(Promises.<List<Void>, AuthenticationException>newExceptionPromise(
                new AuthenticationException("ERROR", resourceException)));

        //When
        runtime.processMessage(context, request, next);

        //Then
        verify(responseHandler).handle(any(MessageContext.class), any(AuthenticationException.class));
        verify(authContext, never()).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext, never()).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext, never()).cleanSubject(any(MessageContext.class), any(Subject.class));
    }

    @Test
    public void whenMessageProcessingSucceedsResourceResponseShouldBeReturned() {

        //Given
        HttpContext context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        //When
        Promise<Response, NeverThrowsException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(successfulResponse);
        Assertions.assertThat(context.getAttributes()).containsKey(AuthenticationFramework.ATTRIBUTE_REQUEST_ID);
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));

        Assertions.assertThat(context.getAttributes())
                .containsKeys(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL,
                        AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT);
    }

    @Test
    public void whenValidateRequestReturnSendFailureShouldReturnAccessDeniedResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_FAILURE));

        //When
        Promise<Response, NeverThrowsException> promise = runtime.processMessage(context, request, next);

        //Then
        Response response = promise.getOrThrowUninterruptibly();
        Assertions.assertThat(response.getStatus()).isEqualTo(unauthenticatedResponse.getStatus());
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext, never()).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
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
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(authStatus));

        //When
        Promise<Response, NeverThrowsException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded();
        Assertions.assertThat(promise.getOrThrowUninterruptibly().getStatus()).isEqualTo(Status.OK);
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext, never()).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
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
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(authStatus));

        //When
        runtime.processMessage(context, request, next);

        //Then
        verify(responseHandler).handle(any(MessageContext.class), any(AuthenticationException.class));
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext, never()).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
    }

    @Test
    public void whenValidateRequestReturnsAuthenticationExceptionItShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newExceptionPromise(
                new AuthenticationException("ERROR")));

        //When
        runtime.processMessage(context, request, next);

        //Then
        verify(responseHandler).handle(any(MessageContext.class), any(AuthenticationException.class));
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext, never()).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
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
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(authStatus));

        //When
        Promise<Response, NeverThrowsException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded();
        Assertions.assertThat(promise.getOrThrowUninterruptibly().getStatus()).isEqualTo(expectedResponse.getStatus());
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
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
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(authStatus));

        //When
        runtime.processMessage(context, request, next);

        //Then
        verify(responseHandler).handle(any(MessageContext.class), any(AuthenticationException.class));
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
    }

    @Test
    public void whenSecureResponseReturnsAuthenticationExceptionItShouldBeWrittenToResponse() {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(successfulResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newExceptionPromise(
                        new AuthenticationException("ERROR")));

        //When
        runtime.processMessage(context, request, next);

        //Then
        verify(responseHandler).handle(any(MessageContext.class), any(AuthenticationException.class));
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
    }

    @Test
    public void whenResourceReturnsResponseExceptionItShouldBeSecuredAndReturned() throws Exception {

        //Given
        Context context = mockContext();
        Request request = new Request();
        Handler next = mockHandler(request,
                Promises.<Response, NeverThrowsException>newResultPromise(failedResponse));

        mockAuthContext(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS),
                Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        //When
        Promise<Response, NeverThrowsException> promise = runtime.processMessage(context, request, next);

        //Then
        assertThat(promise).succeeded();
        Assertions.assertThat(promise.getOrThrowUninterruptibly().getStatus()).isEqualTo(failedResponse.getStatus());
        verify(authContext).validateRequest(any(MessageContext.class), any(Subject.class), eq(serviceSubject));
        verify(authContext).secureResponse(any(MessageContext.class), eq(serviceSubject));
        verify(authContext).cleanSubject(any(MessageContext.class), any(Subject.class));
    }
}
