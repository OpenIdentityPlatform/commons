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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.filter.servlet;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertNotNull;

public class HttpServletAuthorizationFilterTest {

    private HttpServletAuthorizationFilter authorizationFilter;

    private InitParamClassConstructor initParamClassConstructor;

    private SuccessHandler successHandler;

    @BeforeMethod
    public void setUp() {

        initParamClassConstructor = mock(InitParamClassConstructor.class);
        ResultHandlerFactory resultHandlerFactory = mock(ResultHandlerFactory.class);

        authorizationFilter = new HttpServletAuthorizationFilter(initParamClassConstructor,
                resultHandlerFactory);

        successHandler = mock(SuccessHandler.class);
        given(resultHandlerFactory.newSuccessHandler(Matchers.<HttpServletRequest>anyObject(),
                Matchers.<HttpServletResponse>anyObject(), Matchers.<FilterChain>anyObject()))
                .willReturn(successHandler);
    }

    @Test
    public void shouldCreateNewInstanceWithDefaultConstructor() {

        //Given

        //When
        HttpServletAuthorizationFilter authorizationFilter = new HttpServletAuthorizationFilter();

        //Then
        assertNotNull(authorizationFilter);
    }

    @Test
    public void shouldInitialiseFilter() throws ServletException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        AuthorizationModuleFactory authorizationModuleFactory = mock(AuthorizationModuleFactory.class);

        given(initParamClassConstructor.construct(filterConfig, "authorization-module-factory-class",
                "authorization-module-factory-method", "getAuthorizationModuleFactory"))
                .willReturn(authorizationModuleFactory);
        given(filterConfig.getServletContext()).willReturn(servletContext);

        //When
        authorizationFilter.init(filterConfig);

        //Then
        verify(authorizationModuleFactory).getAuthorizationModule();
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldFailToInitialiseFilterWhenAuthorizationModuleFactoryNotConfigured() throws ServletException {

        //Given
        FilterConfig filterConfig = mock(FilterConfig.class);

        given(initParamClassConstructor.construct(eq(filterConfig), anyString(), anyString(), anyString()))
                .willReturn(null);

        //When
        authorizationFilter.init(filterConfig);

        //Then
        // Expect ServletException
    }

    private void initialiseFilter(HttpServletAuthorizationModule module) throws ServletException {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        AuthorizationModuleFactory authorizationModuleFactory = mock(AuthorizationModuleFactory.class);

        given(initParamClassConstructor.construct(filterConfig, "authorization-module-factory-class",
                "authorization-module-factory-method", "getAuthorizationModuleFactory"))
                .willReturn(authorizationModuleFactory);
        given(filterConfig.getServletContext()).willReturn(servletContext);
        given(authorizationModuleFactory.getAuthorizationModule()).willReturn(module);

        authorizationFilter.init(filterConfig);
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldThrowServletExceptionWhenAuthzModulesNotSet() throws IOException, ServletException {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
        // Expected ServletException
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldThrowServletExceptionWhenAuthzModulesAreEmpty() throws IOException, ServletException {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        initialiseFilter(null);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
        // Expected ServletException
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldThrowServletExceptionWhenRequestNotHttp() throws IOException, ServletException {

        //Given
        ServletRequest req = mock(ServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);

        initialiseFilter(module);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
        // Expected ServletException
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldThrowServletExceptionWhenResponseNotHttp() throws IOException, ServletException {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        ServletResponse resp = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);

        initialiseFilter(module);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
        // Expected ServletException
    }

    @Test (expectedExceptions = ServletException.class)
    public void doFilterShouldThrowServletExceptionWhenRequestAndResponseNotHttp() throws IOException,
            ServletException {

        //Given
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse resp = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);

        initialiseFilter(module);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
        // Expected ServletException
    }

    @Test
    public void shouldDoFilterWithoutException() throws Exception {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);
        Promise<AuthorizationResult, AuthorizationException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.success());
        Promise<Void, ServletException> resultHandlerPromise = Promises.newSuccessfulPromise(null);

        initialiseFilter(module);

        given(module.authorize(eq(req), Matchers.<AuthorizationContext>anyObject())).willReturn(authorizePromise);
        given(successHandler.apply(Matchers.<AuthorizationResult>anyObject())).willReturn(resultHandlerPromise);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
        verify(module).authorize(eq(req), Matchers.<AuthorizationContext>anyObject());
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldDoFilterWhenAuthorizePromiseFails() throws Exception {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);
        AuthorizationException exception = mock(AuthorizationException.class);
        Promise<AuthorizationResult, AuthorizationException> authorizePromise = Promises.newFailedPromise(exception);

        initialiseFilter(module);

        given(module.authorize(eq(req), Matchers.<AuthorizationContext>anyObject())).willReturn(authorizePromise);

        // Will force NPE if called
        given(successHandler.apply(Matchers.<AuthorizationResult>anyObject())).willReturn(null);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldDoFilterWhenResultHandlerPromiseFails() throws Exception {

        //Given
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);
        Promise<AuthorizationResult, AuthorizationException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.success());
        ServletException exception = mock(ServletException.class);
        Promise<Void, ServletException> resultHandlerPromise = Promises.newFailedPromise(exception);

        initialiseFilter(module);

        given(module.authorize(eq(req), Matchers.<AuthorizationContext>anyObject())).willReturn(authorizePromise);
        given(successHandler.apply(Matchers.<AuthorizationResult>anyObject())).willReturn(resultHandlerPromise);

        //When
        authorizationFilter.doFilter(req, resp, chain);

        //Then
    }

    @Test (expectedExceptions = ServletException.class)
    public void destroyShouldNullifyRuntimeAndServletApiAdapter() throws ServletException, IOException {

        //Given
        HttpServletAuthorizationModule module = mock(HttpServletAuthorizationModule.class);
        initialiseFilter(module);

        //When
        authorizationFilter.destroy();

        //Then
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        authorizationFilter.doFilter(req, resp, chain);
        // Expect ServletException
    }
}
