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

package org.forgerock.jaspi.runtime;

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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JaspiRuntimeTest {

    private RuntimeResultHandler runtimeResultHandler;

    @BeforeMethod
    public void setUp() {

        runtimeResultHandler = mock(RuntimeResultHandler.class);
    }

    @Test
    public void shouldProcessMessageAndPassStraightToChainWhenServerAuthContextNotConfigured() throws AuthException,
            IOException, ServletException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        ServerAuthContext serverAuthContext = null;

        JaspiRuntime jaspiRuntime = new JaspiRuntime(serverAuthContext, runtimeResultHandler);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        verify(filterChain).doFilter(request, response);
        verifyZeroInteractions(runtimeResultHandler);
    }

    @Test
    public void shouldProcessMessageAndNotCallChainIfValidateRequestIsNotSUCCESS() throws AuthException, IOException,
            ServletException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(runtimeResultHandler.handleValidateRequestResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<HttpServletResponse>anyObject())).willReturn(false);

        JaspiRuntime jaspiRuntime = new JaspiRuntime(serverAuthContext, runtimeResultHandler);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        verify(filterChain, never()).doFilter(request, response);
        verify(runtimeResultHandler, never()).handleSecureResponseResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<HttpServletResponse>anyObject());
    }

    @Test
    public void shouldProcessMessage() throws AuthException, IOException, ServletException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(runtimeResultHandler.handleValidateRequestResult(Matchers.<AuthStatus>anyObject(),
                Matchers.<HttpServletResponse>anyObject())).willReturn(true);

        JaspiRuntime jaspiRuntime = new JaspiRuntime(serverAuthContext, runtimeResultHandler);

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
    public void shouldConvertAuthExceptionToCrestResponseWhenDoFilterFails() throws ServletException, IOException,
            AuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        PrintWriter writer = mock(PrintWriter.class);

        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        doThrow(AuthException.class).when(serverAuthContext).validateRequest(Matchers.<MessageInfo>anyObject(),
                Matchers.<Subject>anyObject(), Matchers.<Subject>anyObject());

        given(response.getWriter()).willReturn(writer);

        JaspiRuntime jaspiRuntime = new JaspiRuntime(serverAuthContext, runtimeResultHandler);

        //When
        jaspiRuntime.processMessage(request, response, filterChain);

        //Then
        verify(response).setStatus(500);
        ArgumentCaptor<String> writerArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(writerArgumentCaptor.capture(), anyInt(), anyInt());
        assertTrue(writerArgumentCaptor.getValue().contains("500"));
        verify(response).setContentType("application/json");
    }
}
