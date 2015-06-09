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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.Promise;
import org.mockito.Matchers;
import org.testng.annotations.Test;

public class JaspiAdaptersTest {

    @Test
    public void adaptedAsyncServerAuthContextShouldAdaptSuccessfulValidateRequestCall() throws AuthException {

        //Given
        ServerAuthContext authContext = mock(ServerAuthContext.class);
        MessageContext messageContext = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authContext.validateRequest(any(MessageInfo.class), eq(clientSubject), eq(serviceSubject)))
                .willReturn(AuthStatus.SUCCESS);

        //When
        AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthContext.validateRequest(messageContext, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SUCCESS);
    }

    @Test
    public void adaptedAsyncServerAuthContextShouldAdaptFailedValidateRequestCall() throws AuthException {

        //Given
        ServerAuthContext authContext = mock(ServerAuthContext.class);
        MessageContext messageContext = mock(MessageContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        doThrow(AuthException.class).when(authContext)
                .validateRequest(any(MessageInfo.class), eq(clientSubject), eq(serviceSubject));

        //When
        AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthContext.validateRequest(messageContext, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAsyncServerAuthContextShouldAdaptSuccessfulSecureResponseCall() throws AuthException {

        //Given
        ServerAuthContext authContext = mock(ServerAuthContext.class);
        MessageContext messageContext = mock(MessageContext.class);
        Subject serviceSubject = new Subject();

        given(authContext.secureResponse(any(MessageInfo.class), eq(serviceSubject)))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthContext.secureResponse(messageContext, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void adaptedAsyncServerAuthContextShouldAdaptFailedSecureResponseCall() throws AuthException {

        //Given
        ServerAuthContext authContext = mock(ServerAuthContext.class);
        MessageContext messageContext = mock(MessageContext.class);
        Subject serviceSubject = new Subject();

        doThrow(AuthException.class).when(authContext)
                .secureResponse(any(MessageInfo.class), eq(serviceSubject));

        //When
        AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthContext.secureResponse(messageContext, serviceSubject);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAsyncServerAuthContextShouldAdaptSuccessfulCleanSubjectCall() throws AuthException {

        //Given
        ServerAuthContext authContext = mock(ServerAuthContext.class);
        MessageContext messageContext = mock(MessageContext.class);
        Subject clientSubject = new Subject();

        //When
        AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
        Promise<Void, AuthenticationException> promise = asyncAuthContext.cleanSubject(messageContext, clientSubject);

        //Then
        assertThat(promise).succeeded().withObject().isNull();
        verify(authContext).cleanSubject(any(MessageInfo.class), eq(clientSubject));
    }

    @Test
    public void adaptedAsyncServerAuthContextShouldAdaptFailedCleanSubjectCall() throws AuthException {

        //Given
        ServerAuthContext authContext = mock(ServerAuthContext.class);
        MessageContext messageContext = mock(MessageContext.class);
        Subject clientSubject = new Subject();

        doThrow(AuthException.class).when(authContext)
                .cleanSubject(any(MessageInfo.class), eq(clientSubject));

        //When
        AsyncServerAuthContext asyncAuthContext = JaspiAdapters.adapt(authContext);
        Promise<Void, AuthenticationException> promise = asyncAuthContext.cleanSubject(messageContext, clientSubject);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptGetModuleIdCall() {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        String moduleId = asyncAuthModule.getModuleId();

        //Then
        Assertions.assertThat(moduleId).isEqualTo(authModule.getClass().getCanonicalName());
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptSuccessfulGetInitializeCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        Map<String, Object> options = Collections.emptyMap();

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<Void, AuthenticationException> promise =
                asyncAuthModule.initialize(requestPolicy, responsePolicy, handler, options);

        //Then
        assertThat(promise).succeeded().withObject().isNull();
        verify(authModule).initialize(requestPolicy, responsePolicy, handler, options);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptFailedGetInitializeCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        Map<String, Object> options = Collections.emptyMap();

        doThrow(AuthException.class).when(authModule).initialize(requestPolicy, responsePolicy, handler, options);

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<Void, AuthenticationException> promise =
                asyncAuthModule.initialize(requestPolicy, responsePolicy, handler, options);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptGetSupportedMessageTypesCall() {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class<?>[]{Request.class, Response.class});

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        asyncAuthModule.getSupportedMessageTypes();

        //Then
        verify(authModule).getSupportedMessageTypes();
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptSuccessfulValidateRequestCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        given(authModule.validateRequest(any(MessageInfo.class), eq(clientSubject), eq(serviceSubject)))
                .willReturn(AuthStatus.SUCCESS);

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SUCCESS);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptFailedValidateRequestCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        doThrow(AuthException.class).when(authModule)
                .validateRequest(any(MessageInfo.class), eq(clientSubject), eq(serviceSubject));

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptSuccessfulSecureResponseCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        Subject serviceSubject = new Subject();

        given(authModule.secureResponse(any(MessageInfo.class), eq(serviceSubject)))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptFailedSecureResponseCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        Subject serviceSubject = new Subject();

        doThrow(AuthException.class).when(authModule)
                .secureResponse(any(MessageInfo.class), eq(serviceSubject));

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<AuthStatus, AuthenticationException> promise =
                asyncAuthModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptSuccessfulCleanSubjectCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        Subject clientSubject = new Subject();

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<Void, AuthenticationException> promise = asyncAuthModule.cleanSubject(messageInfo, clientSubject);

        //Then
        assertThat(promise).succeeded().withObject().isNull();
        verify(authModule).cleanSubject(any(MessageInfo.class), eq(clientSubject));
    }

    @Test
    public void adaptedAsyncServerAuthModuleShouldAdaptFailedCleanSubjectCall() throws AuthException {

        //Given
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        Subject clientSubject = new Subject();

        doThrow(AuthException.class).when(authModule)
                .cleanSubject(any(MessageInfo.class), eq(clientSubject));

        //When
        AsyncServerAuthModule asyncAuthModule = JaspiAdapters.adapt(authModule);
        Promise<Void, AuthenticationException> promise = asyncAuthModule.cleanSubject(messageInfo, clientSubject);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(AuthenticationException.class);
    }

    @Test
    public void adaptedAuthenticationExceptionShouldHaveSameDetailMessage() {

        //Given
        AuthException authException = new AuthException("MESSAGE");

        //When
        AuthenticationException authenticationException = JaspiAdapters.adapt(authException);

        //Then
        Assertions.assertThat(authenticationException.getMessage()).isEqualTo("MESSAGE");
        Assertions.assertThat(authenticationException.getCause()).isNull();
    }

    @Test
    public void adaptedMessageInfoShouldAdaptGetRequestMessageCall() {

        //Given
        MessageInfoContext messageInfoContext = mock(MessageInfoContext.class);

        //When
        MessageInfo messageInfo = JaspiAdapters.adapt(messageInfoContext);
        messageInfo.getRequestMessage();

        //Then
        verify(messageInfoContext).getRequest();
    }

    @Test
    public void adaptedMessageInfoShouldAdaptSetRequestMessageCall() {

        //Given
        MessageInfoContext messageInfoContext = mock(MessageInfoContext.class);
        Request request = new Request();

        //When
        MessageInfo messageInfo = JaspiAdapters.adapt(messageInfoContext);
        messageInfo.setRequestMessage(request);

        //Then
        verify(messageInfoContext).setRequest(request);
    }

    @Test
    public void adaptedMessageInfoShouldAdaptGetResponseMessageCall() {

        //Given
        MessageInfoContext messageInfoContext = mock(MessageInfoContext.class);

        //When
        MessageInfo messageInfo = JaspiAdapters.adapt(messageInfoContext);
        messageInfo.getResponseMessage();

        //Then
        verify(messageInfoContext).getResponse();
    }

    @Test
    public void adaptedMessageInfoShouldAdaptSetResponseMessageCall() {

        //Given
        MessageInfoContext messageInfoContext = mock(MessageInfoContext.class);
        Response response = new Response();

        //When
        MessageInfo messageInfo = JaspiAdapters.adapt(messageInfoContext);
        messageInfo.setResponseMessage(response);

        //Then
        verify(messageInfoContext).setResponse(response);
    }

    @Test
    public void adaptedMessageInfoShouldAdaptGetMapCall() {

        //Given
        MessageInfoContext messageInfoContext = mock(MessageInfoContext.class);

        //When
        MessageInfo messageInfo = JaspiAdapters.adapt(messageInfoContext);
        messageInfo.getMap();

        //Then
        verify(messageInfoContext).getRequestContextMap();
    }
}
