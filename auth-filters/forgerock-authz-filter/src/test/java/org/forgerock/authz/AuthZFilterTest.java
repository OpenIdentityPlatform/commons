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

package org.forgerock.authz;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.auth.common.LoggingConfigurator;
import org.forgerock.auth.common.DebugLogger;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

public class AuthZFilterTest {

    private AuthZFilter authZFilter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private PrintWriter writer;
    private AuthorizationFilter authorizationFilter;
    private AuditLogger<HttpServletRequest> auditLogger;
    private DebugLogger debugLogger;
    private AuthorizationConfigurator authorizationConfigurator;

    @BeforeMethod
    public void setUp() throws ServletException, IOException {
        authZFilter = new AuthZFilter();

        FilterConfig filterConfig = mock(FilterConfig.class);
        given(filterConfig.getInitParameter("configurator")).willReturn(TestConfigurator.class.getCanonicalName());

        authZFilter.init(filterConfig);


        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        writer = mock(PrintWriter.class);

        authorizationFilter = mock(AuthorizationFilter.class);
        auditLogger = mock(AuditLogger.class);
        debugLogger = mock(DebugLogger.class);

        authorizationConfigurator = mock(AuthorizationConfigurator.class);

        given(response.getWriter()).willReturn(writer);

        given(authorizationConfigurator.getAuthorizationFilter()).willReturn(authorizationFilter);
        given(authorizationConfigurator.getAuditLogger()).willReturn(auditLogger);
        given(authorizationConfigurator.getDebugLogger()).willReturn(debugLogger);

        AuthorizationConfiguratorFactory.setAuthorizationConfigurator(authorizationConfigurator);
    }

    @Test
    public void shouldCallInitOnFirstDoFilter() throws IOException, ServletException {

        //Given
        given(authorizationFilter.authorize(request, response)).willReturn(true);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        verify(authorizationConfigurator).getAuditLogger();
        verify(authorizationConfigurator).getDebugLogger();
        verify(authorizationConfigurator).getAuthorizationFilter();
        verify(authorizationFilter).initialise(Matchers.<LoggingConfigurator>anyObject(), eq(auditLogger), eq(debugLogger));
    }

    @Test
    public void shouldNotCallInitOnSecondDoFilter() throws IOException, ServletException {

        //Given
        given(authorizationFilter.authorize(request, response)).willReturn(true);

        authZFilter.doFilter(request, response, filterChain);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        verify(authorizationConfigurator).getAuditLogger();
        verify(authorizationConfigurator).getDebugLogger();
        verify(authorizationConfigurator).getAuthorizationFilter();
        verify(authorizationFilter).initialise(Matchers.<LoggingConfigurator>anyObject(), eq(auditLogger), eq(debugLogger));
    }

    @Test
    public void shouldAuditSuccessWhenAuthorizationPassed() throws IOException, ServletException {

        //Given
        given(authorizationFilter.authorize(request, response)).willReturn(true);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        assertEquals(auditRecordCaptor.getValue().getAuthResult(), AuthResult.SUCCESS);
        assertEquals(auditRecordCaptor.getValue().getAuditObject(), request);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldAuditFailureWhenAuthorizationFailed() throws IOException, ServletException {

        //Given
        given(authorizationFilter.authorize(request, response)).willReturn(false);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        assertEquals(auditRecordCaptor.getValue().getAuthResult(), AuthResult.FAILURE);
        assertEquals(auditRecordCaptor.getValue().getAuditObject(), request);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void shouldReturn403WhenAuthorizationFailed() throws IOException, ServletException {

        //Given
        given(authorizationFilter.authorize(request, response)).willReturn(false);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        verify(response).setStatus(403);
        verify(writer).write(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }
}
