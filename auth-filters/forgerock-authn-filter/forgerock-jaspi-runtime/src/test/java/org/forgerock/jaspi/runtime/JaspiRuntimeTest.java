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

package org.forgerock.jaspi.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import org.forgerock.jaspi.runtime.response.FailureResponseHandler;
import org.forgerock.json.resource.ResourceException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class JaspiRuntimeTest {

    private RuntimeResultHandler runtimeResultHandler;
    private AuditApi auditApi;

    @BeforeMethod
    public void setUp() {
        runtimeResultHandler = mock(RuntimeResultHandler.class);

        auditApi = mock(AuditApi.class);
    }

    @Test
    public void shouldProcessMessageAndPassStraightToChainWhenServerAuthContextNotConfigured() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        ContextFactory contextFactory = mock(ContextFactory.class);
        ServerAuthContext serverAuthContext = null;

        given(contextFactory.getContext()).willReturn(serverAuthContext);
        JaspiRuntime jaspiRuntime = new JaspiRuntime(contextFactory, runtimeResultHandler, auditApi, null);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        verify(filterChain).doFilter(request, response);
        verifyZeroInteractions(runtimeResultHandler);
    }

    @Test
    public void shouldProcessMessageAndNotCallChainIfValidateRequestIsNotSUCCESS() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        ContextFactory contextFactory = mock(ContextFactory.class);
        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(contextFactory.getContext()).willReturn(serverAuthContext);
        given(runtimeResultHandler.handleValidateRequestResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<MessageInfo>anyObject(), Matchers.<AuditTrail>anyObject(), Matchers.<Subject>anyObject(),
                Matchers.<HttpServletResponse>anyObject())).willReturn(false);

        JaspiRuntime jaspiRuntime = new JaspiRuntime(contextFactory, runtimeResultHandler, auditApi, null);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        verify(filterChain, never()).doFilter(request, response);
        verify(runtimeResultHandler, never()).handleSecureResponseResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<HttpServletResponse>anyObject());
    }

    @Test
    public void shouldProcessMessage() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        ContextFactory contextFactory = mock(ContextFactory.class);
        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(contextFactory.getContext()).willReturn(serverAuthContext);
        given(runtimeResultHandler.handleValidateRequestResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<MessageInfo>anyObject(), Matchers.<AuditTrail>anyObject(), Matchers.<Subject>anyObject(),
                Matchers.<HttpServletResponse>anyObject())).willReturn(true);

        JaspiRuntime jaspiRuntime = new JaspiRuntime(contextFactory, runtimeResultHandler, auditApi, null);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        ArgumentCaptor<HttpServletResponse> responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
        verify(filterChain).doFilter(requestCaptor.capture(), responseCaptor.capture());
        verify(runtimeResultHandler).handleSecureResponseResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<HttpServletResponse>anyObject());
        verify(serverAuthContext).cleanSubject(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());

        assertEquals(((HttpServletRequestWrapper) requestCaptor.getValue()).getRequest(), request);
        assertEquals(((HttpServletResponseWrapper) responseCaptor.getValue()).getResponse(), response);
    }

    @Test
    public void shouldConvertAuthExceptionToCrestResponseWhenDoFilterFails() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        FailureResponseHandler failureResponseHandler = mock(FailureResponseHandler.class);

        ContextFactory contextFactory = mock(ContextFactory.class);
        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(contextFactory.getContext()).willReturn(serverAuthContext);
        doThrow(AuthException.class).when(serverAuthContext).validateRequest(Matchers.<MessageInfo>anyObject(),
                Matchers.<Subject>anyObject(), Matchers.<Subject>anyObject());

        JaspiRuntime jaspiRuntime = new JaspiRuntime(contextFactory, runtimeResultHandler, auditApi,
                failureResponseHandler);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(failureResponseHandler).handle(resourceExceptionCaptor.capture(), any(MessageInfo.class));
        assertEquals(resourceExceptionCaptor.getValue().getCode(), 500);
    }
}
