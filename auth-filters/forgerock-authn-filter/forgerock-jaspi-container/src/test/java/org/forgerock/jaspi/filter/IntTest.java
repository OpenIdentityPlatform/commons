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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.filter;

import org.forgerock.jaspi.container.AuthHttpServletRequestWrapper;
import org.forgerock.jaspi.container.config.Configuration;
import org.forgerock.jaspi.container.config.ConfigurationManager;
import org.forgerock.jaspi.filter.modules.SecureSendContinueAuthModule;
import org.forgerock.jaspi.filter.modules.SecureSendFailureAuthModule;
import org.forgerock.jaspi.filter.modules.ValidateSendContinueAuthModule;
import org.forgerock.jaspi.filter.modules.ValidateSendContinueSessionModule;
import org.forgerock.jaspi.filter.modules.ValidateSendFailureAuthModule;
import org.forgerock.jaspi.filter.modules.ValidateSendFailureSessionModule;
import org.forgerock.jaspi.filter.modules.ValidateSendSuccessAuthModule;
import org.forgerock.jaspi.filter.modules.ValidateSendSuccessSessionModule;
import org.forgerock.jaspi.filter.modules.ValidateSuccessAuthModule;
import org.forgerock.jaspi.filter.modules.ValidateSuccessSessionModule;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.security.auth.message.AuthException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class IntTest {

    @AfterMethod
    public void setUp() {
        ConfigurationManager.unconfigure();
    }

    @Test
    public void shouldThrowServletExceptionWhenModuleConfigurationNotSpecified() throws IOException, ServletException {

        //Given

        //When
        FilterRunner filterRunner = new FilterRunner();
        boolean exceptionCaught = false;
        Exception exception = null;
        try {
            filterRunner.run();
        } catch (ServletException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertTrue(ServletException.class.isAssignableFrom(exception.getClass()));
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
    }

    @Test
    public void shouldThrowServletExceptionWhenNotConfigured() throws IOException, ServletException {

        //Given

        //When
        FilterRunner filterRunner = new FilterRunner();
        boolean exceptionCaught = false;
        Exception exception = null;
        try {
            filterRunner.run("default");
        } catch (ServletException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertTrue(ServletException.class.isAssignableFrom(exception.getClass()));
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
    }

    @Test
    public void shouldThrowAuthExceptionWhenConfigurationEmpty() throws IOException, AuthException, ServletException {

        //Given
        Configuration configuration = new Configuration();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        boolean exceptionCaught = false;
        Exception exception = null;
        try {
            filterRunner.run("none");
        } catch (ServletException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertTrue(AuthException.class.isAssignableFrom(exception.getCause().getClass()));
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
    }

    @Test
    public void shouldAuthenticateAndCallSecureResponseWithValidateSuccessSessionModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-only")
                .setSessionModule(ValidateSuccessSessionModule.class, sessionModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-only");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse()).addHeader("session", ValidateSuccessSessionModule.class.getSimpleName());
        verifyNoMoreInteractions(filterRunner.getResponse());
    }

    @Test
    public void shouldNotAuthenticateWithValidateSendFailureSessionModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-only")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-only");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldAuthenticateAndNotCallSecureResponseWithValidateSendSuccessSessionModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-only")
                .setSessionModule(ValidateSendSuccessSessionModule.class, sessionModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-only");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldNotAuthenticateAndNotCallSecureResponseWithValidateSendContinueSessionModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-only")
                .setSessionModule(ValidateSendContinueSessionModule.class, sessionModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-only");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldAuthenticateAndNotCallSecureResponseWithValidateSuccessAuthModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("auth-only")
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("auth-only");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse()).addHeader(eq("AUTH_SUCCESS"), anyString());
    }

    @Test
    public void shouldNotAuthenticateWithValidateSendFailureAuthModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("auth-only")
                .addAuthenticationModule(ValidateSendFailureAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("auth-only");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldAuthenticateAndNotCallSecureResponseWithValidateSendSuccessAuthModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("auth-only")
                .addAuthenticationModule(ValidateSendSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("auth-only");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldNotAuthenticateAndNotCallSecureResponseWithValidateSendContinueAuthModule() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("auth-only")
                .addAuthenticationModule(ValidateSendContinueAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("auth-only");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldAuthenticateJustUsingValidateSuccessSessionModuleAndCallAnySecureResponse() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSuccessSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSendFailureAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse()).addHeader("session", ValidateSuccessSessionModule.class.getSimpleName());
        verifyNoMoreInteractions(filterRunner.getResponse());
    }

    @Test
    public void shouldNotAuthenticateUsingValidateSendSuccessSessionModuleAndNotCallAnySecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendSuccessSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldNotAuthenticateUsingValidateSendContinueSessionModuleAndNotCallAnySecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendContinueSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldAuthenticateUsingValidateSuccessAuthModuleAndCallBothSecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse()).addHeader(eq("session"), anyString());
        verify(filterRunner.getResponse()).addHeader(eq("AUTH_SUCCESS"), anyString());
    }

    @Test
    public void shouldNotAuthenticateUsingValidateSendSuccessAuthModuleAndCallSessionSecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSendSuccessAuthModule.class, authModuleProps)
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth-auth");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse()).addHeader(eq("session"), anyString());
        verify(filterRunner.getResponse(), never()).addHeader(eq("AUTH_SEND_SUCCESS"), anyString());
        verify(filterRunner.getResponse(), never()).addHeader(eq("AUTH_SUCCESS"), anyString());
    }

    @Test
    public void shouldNotAuthenticateUsingValidateSendFailureAuthModuleAndNotCallAnySecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSendFailureAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
        verify(filterRunner.getResponse()).setStatus(401);
    }

    @Test
    public void shouldNotAuthenticateUsingValidateSendContinueAuthModuleAndNotCallAnySecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSendContinueAuthModule.class, authModuleProps)
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        verify(filterRunner.getFilterChain(), never()).doFilter((HttpServletRequest) anyObject(),
                (HttpServletResponse) anyObject());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
    }

    @Test
    public void shouldAuthenticateUsingSecondValidateSuccessAuthModuleAndCallSecondAndSessionSecureResponse()
            throws IOException, ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(ValidateSendFailureAuthModule.class, authModuleProps)
                .addAuthenticationModule(ValidateSuccessAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth-auth");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse()).addHeader(eq("session"), anyString());
        verify(filterRunner.getResponse()).addHeader(eq("AUTH_SUCCESS"), anyString());
        verify(filterRunner.getResponse(), never()).addHeader(eq("AUTH_SEND_FAILURE"), anyString());
    }

    @Test
    public void shouldAuthenticateAndNotCallSessionSecureResponseWithSecureSendContinue() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(SecureSendContinueAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
        verify(filterRunner.getResponse()).setStatus(100);
    }

    @Test
    public void shouldAuthenticateAndNotCallSessionSecureResponseWithSecureSendFailure() throws IOException,
            ServletException, AuthException {

        //Given
        Map<String, Object> sessionModuleProps = new HashMap<String, Object>();
        Map<String, Object> authModuleProps = new HashMap<String, Object>();
        Configuration configuration = new Configuration()
                .addAuthContext("session-auth")
                .setSessionModule(ValidateSendFailureSessionModule.class, sessionModuleProps)
                .addAuthenticationModule(SecureSendFailureAuthModule.class, authModuleProps)
                .done();
        ConfigurationManager.configure(configuration);

        //When
        FilterRunner filterRunner = new FilterRunner();
        filterRunner.run("session-auth");

        //Then
        ArgumentCaptor<HttpServletRequestWrapper> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletRequestWrapper.class);
        ArgumentCaptor<HttpServletResponseWrapper> responseArgumentCaptor =
                ArgumentCaptor.forClass(HttpServletResponseWrapper.class);

        verify(filterRunner.getFilterChain()).doFilter(requestArgumentCaptor.capture(),
                responseArgumentCaptor.capture());
        assertEquals(responseArgumentCaptor.getValue().getResponse(), filterRunner.getResponse());
        verify(filterRunner.getResponse(), never()).addHeader(anyString(), anyString());
        verify(filterRunner.getResponse()).setStatus(500);
    }
}
