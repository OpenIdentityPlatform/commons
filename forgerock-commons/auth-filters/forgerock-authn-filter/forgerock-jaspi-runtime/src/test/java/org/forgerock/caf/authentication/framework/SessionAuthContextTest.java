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

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionAuthContextTest {

    private AsyncServerAuthContext authContext;

    private AsyncServerAuthModule sessionAuthModule;

    @BeforeMethod
    public void setup() {
        sessionAuthModule = mockSessionAuthModule();

        authContext = createSessionAuthContext(sessionAuthModule);
    }

    private AsyncServerAuthContext createSessionAuthContext(AsyncServerAuthModule sessionAuthModule) {
        Logger logger = mock(Logger.class);
        return new SessionAuthContext(logger, sessionAuthModule);
    }

    private AsyncServerAuthModule mockSessionAuthModule() {
        AsyncServerAuthModule sessionAuthModule = mock(AsyncServerAuthModule.class);

        given(sessionAuthModule.validateRequest(any(MessageInfoContext.class), any(Subject.class), any(Subject.class)))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS));
        given(sessionAuthModule.secureResponse(any(MessageInfoContext.class), any(Subject.class)))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));
        given(sessionAuthModule.cleanSubject(any(MessageInfoContext.class), any(Subject.class)))
                .willReturn(Promises.<Void, AuthenticationException>newResultPromise(null));

        return sessionAuthModule;
    }

    private MessageContext mockMessageContext() {
        MessageContext context = mock(MessageContext.class);
        Map<String, Object> requestContextMap = new HashMap<>();
        AuditTrail auditTrail = mock(AuditTrail.class);

        given(context.getRequestContextMap()).willReturn(requestContextMap);
        requestContextMap.put(AuditTrail.AUDIT_TRAIL_KEY, auditTrail);

        return context;
    }

    @Test
    public void validateRequestShouldCallSessionAuthModule() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SUCCESS);
        verify(sessionAuthModule).validateRequest(context, clientSubject, serviceSubject);
    }

    @Test
    public void whenSessionAuthModuleIsNullValidateRequestShouldReturnSendFailure() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        authContext = createSessionAuthContext(null);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.validateRequest(context, clientSubject,
                serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_FAILURE);
        verify(sessionAuthModule, never()).validateRequest(context, clientSubject, serviceSubject);
    }

    @Test
    public void secureResponseShouldCallSessionAuthModule() {

        //Given
        MessageContext context = mockMessageContext();
        Subject serviceSubject = new Subject();

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
        verify(sessionAuthModule).secureResponse(context, serviceSubject);
    }

    @Test
    public void whenSessionAuthModuleIsNullSecureResponseShouldReturnSendSuccess() {

        //Given
        MessageContext context = mockMessageContext();
        Subject serviceSubject = new Subject();

        authContext = createSessionAuthContext(null);

        //When
        Promise<AuthStatus, AuthenticationException> promise = authContext.secureResponse(context, serviceSubject);

        //Then
        assertThat(promise).succeeded().withObject().isEqualTo(AuthStatus.SEND_SUCCESS);
        verify(sessionAuthModule, never()).secureResponse(context, serviceSubject);
    }

    @Test
    public void cleanSubjectShouldCallSessionAuthModule() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();

        //When
        Promise<Void, AuthenticationException> promise = authContext.cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).succeeded();
        verify(sessionAuthModule).cleanSubject(context, clientSubject);
    }

    @Test
    public void whenSessionAuthModuleIsNullCleanSubjectShouldReturnNull() {

        //Given
        MessageContext context = mockMessageContext();
        Subject clientSubject = new Subject();

        authContext = createSessionAuthContext(null);

        //When
        Promise<Void, AuthenticationException> promise = authContext.cleanSubject(context, clientSubject);

        //Then
        assertThat(promise).succeeded();
        verify(sessionAuthModule, never()).cleanSubject(context, clientSubject);
    }
}
