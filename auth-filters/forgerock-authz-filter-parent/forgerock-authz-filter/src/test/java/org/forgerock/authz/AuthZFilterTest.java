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

package org.forgerock.authz;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.json.fluent.JsonValue;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class AuthZFilterTest {

    private AuthZFilter authZFilter;

    private static AuthorizationModule authorizationModule;
    private static AuditLogger<HttpServletRequest> auditLogger;

    private InstanceCreator instanceCreator;
    private FilterConfiguration filterConfiguration;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setUp() throws ServletException, IOException {

        instanceCreator = mock(InstanceCreator.class);
        filterConfiguration = mock(FilterConfiguration.class);

        authZFilter = new AuthZFilter(instanceCreator, filterConfiguration);

        authorizationModule = mock(AuthorizationModule.class);
        auditLogger = mock(AuditLogger.class);
    }

    @Test (expectedExceptions = ServletException.class)
    public void initShouldThrowServletExceptionWhenLoggingConfiguratorNotSet() throws ServletException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        //When
        authZFilter.init(filterConfig);

        //Then
        fail();
    }

    @Test (expectedExceptions = ServletException.class)
    public void initShouldThrowServletExceptionWhenAuditLoggerNull() throws ServletException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        given(filterConfiguration.get(eq(filterConfig), eq("logging-configurator-factory-class"), anyString(),
                anyString())) .willReturn(new NullAuditLoggingConfigurator());

        //When
        authZFilter.init(filterConfig);

        //Then
        fail();
    }

    @Test (expectedExceptions = ServletException.class)
    public void initShouldThrowServletExceptionWhenAuthzModuleNotSet() throws ServletException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        given(filterConfiguration.get(eq(filterConfig), eq("logging-configurator-factory-class"), anyString(),
                anyString())).willReturn(new AuditLoggingConfigurator());

        //When
        authZFilter.init(filterConfig);

        //Then
        fail();
    }

    @Test
    public void initShouldGetAuthzModuleFromConfigurator() throws ServletException, AuthorizationException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        given(filterConfiguration.get(eq(filterConfig), eq("logging-configurator-factory-class"), anyString(),
                anyString())).willReturn(new AuditLoggingConfigurator());
        given(filterConfiguration.get(eq(filterConfig), eq("module-configurator-factory-class"), anyString(),
                anyString())).willReturn(new TestAuthorizationModuleConfigurator());

        //When
        authZFilter.init(filterConfig);

        //Then
        verify(authorizationModule).initialise(null);
    }

    @Test
    public void initShouldCreateAuthzModuleFromClassName() throws ServletException, IllegalAccessException,
            InstantiationException, ClassNotFoundException, AuthorizationException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        given(filterConfiguration.get(eq(filterConfig), eq("logging-configurator-factory-class"), anyString(),
                anyString())).willReturn(new AuditLoggingConfigurator());
        given(filterConfig.getInitParameter("module-class")).willReturn("MODULE_CLASS_NAME");

        given(instanceCreator.createInstance("MODULE_CLASS_NAME", AuthorizationModule.class))
                .willReturn(authorizationModule);

        //When
        authZFilter.init(filterConfig);

        //Then
        verify(authorizationModule).initialise(Matchers.<JsonValue>anyObject());
    }

    @Test
    public void initShouldGetAuthzModuleFromConfiguratorOverClassName() throws ServletException, IllegalAccessException,
            InstantiationException, ClassNotFoundException, AuthorizationException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        given(filterConfiguration.get(eq(filterConfig), eq("logging-configurator-factory-class"), anyString(),
                anyString())).willReturn(new AuditLoggingConfigurator());
        given(filterConfiguration.get(eq(filterConfig), eq("module-configurator-factory-class"), anyString(),
                anyString())).willReturn(new TestAuthorizationModuleConfigurator());

        //When
        authZFilter.init(filterConfig);

        //Then
        verifyZeroInteractions(instanceCreator);
        verify(authorizationModule).initialise(null);
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldNotAllowNonHttpServletRequest() throws IOException, ServletException {

        //Given
        ServletRequest req = mock(ServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        //When
        authZFilter.doFilter(req, resp, chain);

        //Then
        fail();
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldNotAllowNonHttpServletResponse() throws IOException, ServletException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        fail();
    }

    private void initAuthzFilter() throws ServletException, IllegalAccessException, InstantiationException,
            ClassNotFoundException {

        FilterConfig filterConfig = mock(FilterConfig.class);

        given(filterConfiguration.get(eq(filterConfig), eq("logging-configurator-factory-class"), anyString(),
                anyString())).willReturn(new AuditLoggingConfigurator());
        given(filterConfiguration.get(eq(filterConfig), eq("module-configurator-factory-class"), anyString(),
                anyString())).willReturn(new TestAuthorizationModuleConfigurator());

        authZFilter.init(filterConfig);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditSuccessWhenAuthorizationPassed() throws IOException, ServletException,
            IllegalAccessException, ClassNotFoundException, InstantiationException, AuthorizationException {

        //Given
        initAuthzFilter();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        given(authorizationModule.authorize(eq(request), any(AuthorizationContext.class))).willReturn(true);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        ArgumentCaptor<AuditRecord> auditRecordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogger).audit(auditRecordCaptor.capture());
        assertEquals(auditRecordCaptor.getValue().getAuthResult(), AuthResult.SUCCESS);
        assertEquals(auditRecordCaptor.getValue().getAuditObject(), request);
        verify(filterChain).doFilter(request, response);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldAuditFailureWhenAuthorizationFailed() throws IOException, ServletException,
            IllegalAccessException, ClassNotFoundException, InstantiationException, AuthorizationException {

        //Given
        initAuthzFilter();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        PrintWriter writer = mock(PrintWriter.class);

        given(response.getWriter()).willReturn(writer);

        given(authorizationModule.authorize(eq(request), any(AuthorizationContext.class))).willReturn(false);

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
    public void shouldReturn403WhenAuthorizationFailed() throws IOException, ServletException, IllegalAccessException,
            ClassNotFoundException, InstantiationException, AuthorizationException {

        //Given
        initAuthzFilter();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        PrintWriter writer = mock(PrintWriter.class);

        given(response.getWriter()).willReturn(writer);

        given(authorizationModule.authorize(eq(request), any(AuthorizationContext.class))).willReturn(false);

        //When
        authZFilter.doFilter(request, response, filterChain);

        //Then
        verify(response).setStatus(403);
        verify(writer).write(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void shouldCreateNewAuthorizationContextIfNotAlreadyPresent() throws Exception {
        // Given
        initAuthzFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(authorizationModule.authorize(eq(request), any(AuthorizationContext.class))).willReturn(true);

        // When
        authZFilter.doFilter(request, mock(HttpServletResponse.class), mock(FilterChain.class));

        // Then
        verify(request).setAttribute(eq(AuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT),
                any(AuthorizationContext.class));
    }

    @Test
    public void shouldReuseExistingAuthorizationContextIfAlreadyPresent() throws Exception {
        // Given
        initAuthzFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthorizationContext context = new AuthorizationContext();
        context.setAttribute("test", "some value");
        given(authorizationModule.authorize(eq(request), any(AuthorizationContext.class))).willReturn(true);
        given(request.getAttribute(AuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT))
                .willReturn(context.getAttributes());

        // When
        authZFilter.doFilter(request, mock(HttpServletResponse.class), mock(FilterChain.class));

        // Then - ensure same context was passed to authz module
        verify(authorizationModule).authorize(request, context);
        // ensure context is not changed on the request
        verify(request, never()).setAttribute(eq(AuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT), any());
    }

    static class NullAuditLoggingConfigurator implements AuthorizationLoggingConfigurator {

        @Override
        public AuditLogger<HttpServletRequest> getAuditLogger() {
            return null;
        }
    }

    private static AuditLogger<HttpServletRequest> getMockAuditLogger() {
        return auditLogger;
    }

    static class AuditLoggingConfigurator implements AuthorizationLoggingConfigurator {

        @Override
        public AuditLogger<HttpServletRequest> getAuditLogger() {
            return getMockAuditLogger();
        }
    }

    private static AuthorizationModule getAuthorizationModule() {
        return authorizationModule;
    }

    static class TestAuthorizationModuleConfigurator implements AuthorizationModuleConfigurator {

        @Override
        public AuthorizationModule getModule() {
            return getAuthorizationModule();
        }

        @Override
        public JsonValue getConfiguration() {
            return null;
        }
    }
}
