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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationFilterBuilder;
import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.testng.annotations.Test;

public class AuthenticationFilterTest {

    @Test(expectedExceptions = IllegalStateException.class)
    public void attemptingToBuildFilterWithNoAuditApiShouldThrowIllegalStateException() {

        //Given
        AuthenticationFilterBuilder builder = spy(AuthenticationFilter.builder());

        //When
        builder.build();

        //Then
        failBecauseExceptionWasNotThrown(IllegalStateException.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldBuildFilterWithAuditApiAndDefaultLogger() {

        //Given
        AuditApi auditApi = mock(AuditApi.class);
        AsyncServerAuthModule sessionAuthModule = null;

        AuthenticationFilterBuilder builder = spy(AuthenticationFilter.builder());

        //When
        builder.auditApi(auditApi)
                .build();

        //Then
        ArgumentCaptor<Logger> loggerCaptor = ArgumentCaptor.forClass(Logger.class);
        ArgumentCaptor<ResponseHandler> responseHandlerCaptor =
                ArgumentCaptor.forClass(ResponseHandler.class);
        ArgumentCaptor<Subject> serviceSubjectCaptor = ArgumentCaptor.forClass(Subject.class);
        ArgumentCaptor<List> authModulesCaptor = ArgumentCaptor.forClass(List.class);

        verify(builder).createFilter(loggerCaptor.capture(), eq(auditApi), responseHandlerCaptor.capture(),
                serviceSubjectCaptor.capture(), eq(sessionAuthModule), authModulesCaptor.capture(),
                Matchers.<Promise<List<Void>, AuthenticationException>>anyObject());

        assertThat(loggerCaptor.getValue()).isNotNull();
        assertThat(responseHandlerCaptor.getValue()).isNotNull();
        assertThat(serviceSubjectCaptor.getValue()).isNotNull();
        assertThat(authModulesCaptor.getValue()).isEmpty();
    }

    @Test
    public void shouldBuildFilterWithNamedLogger() {

        //Given
        AuditApi auditApi = mock(AuditApi.class);
        AsyncServerAuthModule sessionAuthModule = null;

        AuthenticationFilterBuilder builder = spy(AuthenticationFilter.builder());

        //When
        builder.auditApi(auditApi)
                .named("NAMED_LOGGER")
                .build();

        //Then
        ArgumentCaptor<Logger> loggerCaptor = ArgumentCaptor.forClass(Logger.class);

        verify(builder).createFilter(loggerCaptor.capture(), eq(auditApi), Matchers.<ResponseHandler>anyObject(),
                Matchers.<Subject>anyObject(), eq(sessionAuthModule), Matchers.<List<AsyncServerAuthModule>>anyObject(),
                Matchers.<Promise<List<Void>, AuthenticationException>>anyObject());

        assertThat(loggerCaptor.getValue()).isNotNull();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldBuildFullyConfiguredFilter() {

        //Given
        Logger logger = mock(Logger.class);
        AuditApi auditApi = mock(AuditApi.class);
        Subject serviceSubject = new Subject();
        ResponseWriter responseWriter = mock(ResponseWriter.class);

        AsyncServerAuthModule sessionAuthModule = mockAuthModule();
        MessagePolicy sessionAuthModuleRequestPolicy = mock(MessagePolicy.class);
        MessagePolicy sessionAuthModuleResponsePolicy = mock(MessagePolicy.class);
        CallbackHandler sessionAuthModuleHandler = mock(CallbackHandler.class);
        Map<String, Object> sessionAuthModuleSettings = new HashMap<String, Object>();

        AsyncServerAuthModule authModuleOne = mockAuthModule();
        MessagePolicy authModuleOneRequestPolicy = mock(MessagePolicy.class);
        MessagePolicy authModuleOneResponsePolicy = mock(MessagePolicy.class);
        CallbackHandler authModuleOneHandler = mock(CallbackHandler.class);
        Map<String, Object> authModuleOneSettings = new HashMap<String, Object>();

        AsyncServerAuthModule authModuleTwo = mockAuthModule();
        MessagePolicy authModuleTwoRequestPolicy = mock(MessagePolicy.class);
        MessagePolicy authModuleTwoResponsePolicy = mock(MessagePolicy.class);
        CallbackHandler authModuleTwoHandler = mock(CallbackHandler.class);
        Map<String, Object> authModuleTwoSettings = new HashMap<String, Object>();

        AuthenticationFilterBuilder builder = spy(AuthenticationFilter.builder());

        //When
        builder.logger(logger)
                .auditApi(auditApi)
                .serviceSubject(serviceSubject)
                .responseHandler(responseWriter)
                .sessionModule(
                        configureModule(sessionAuthModule)
                                .requestPolicy(sessionAuthModuleRequestPolicy)
                                .responsePolicy(sessionAuthModuleResponsePolicy)
                                .callbackHandler(sessionAuthModuleHandler)
                                .withSettings(sessionAuthModuleSettings))
                .authModules(
                        configureModule(authModuleOne)
                                .requestPolicy(authModuleOneRequestPolicy)
                                .responsePolicy(authModuleOneResponsePolicy)
                                .callbackHandler(authModuleOneHandler)
                                .withSettings(authModuleOneSettings),
                        configureModule(authModuleTwo)
                                .requestPolicy(authModuleTwoRequestPolicy)
                                .responsePolicy(authModuleTwoResponsePolicy)
                                .callbackHandler(authModuleTwoHandler)
                                .withSettings(authModuleTwoSettings))
                .build();

        //Then
        ArgumentCaptor<Logger> loggerCaptor = ArgumentCaptor.forClass(Logger.class);
        ArgumentCaptor<ResponseHandler> responseHandlerCaptor =
                ArgumentCaptor.forClass(ResponseHandler.class);
        ArgumentCaptor<List> authModulesCaptor = ArgumentCaptor.forClass(List.class);

        verify(builder).createFilter(loggerCaptor.capture(), eq(auditApi), responseHandlerCaptor.capture(),
                eq(serviceSubject), eq(sessionAuthModule), authModulesCaptor.capture(),
                Matchers.<Promise<List<Void>, AuthenticationException>>anyObject());

        assertThat(loggerCaptor.getValue()).isNotNull();
        assertThat(responseHandlerCaptor.getValue()).isNotNull();
        assertThat(authModulesCaptor.getValue()).hasSize(2).contains(authModuleOne, authModuleTwo);

        verify(sessionAuthModule).initialize(sessionAuthModuleRequestPolicy, sessionAuthModuleResponsePolicy,
                sessionAuthModuleHandler, sessionAuthModuleSettings);
        verify(authModuleOne).initialize(authModuleOneRequestPolicy, authModuleOneResponsePolicy, authModuleOneHandler,
                authModuleOneSettings);
        verify(authModuleTwo).initialize(authModuleTwoRequestPolicy, authModuleTwoResponsePolicy, authModuleTwoHandler,
                authModuleTwoSettings);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldBuildFilterWithAdaptedServerAuthContext() throws Exception {

        //Given
        AuditApi auditApi = mock(AuditApi.class);
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        MessagePolicy authModuleRequestPolicy = mock(MessagePolicy.class);
        MessagePolicy authModuleResponsePolicy = mock(MessagePolicy.class);
        CallbackHandler authModuleHandler = mock(CallbackHandler.class);
        Map<String, Object> authModuleSettings = new HashMap<String, Object>();

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[]{Request.class, Response.class});

        AuthenticationFilterBuilder builder = spy(AuthenticationFilter.builder());

        //When
        builder.auditApi(auditApi)
                .authModules(
                        configureModule(authModule)
                                .requestPolicy(authModuleRequestPolicy)
                                .responsePolicy(authModuleResponsePolicy)
                                .callbackHandler(authModuleHandler)
                                .withSettings(authModuleSettings))
                .build();

        //Then
        verify(builder).createFilter(Matchers.<Logger>anyObject(), eq(auditApi),
                Matchers.<ResponseHandler>anyObject(), Matchers.<Subject>anyObject(),
                Matchers.<AsyncServerAuthModule>anyObject(), anyListOf(AsyncServerAuthModule.class),
                Matchers.<Promise<List<Void>, AuthenticationException>>anyObject());

        verify(authModule).initialize(authModuleRequestPolicy, authModuleResponsePolicy,
                authModuleHandler, authModuleSettings);
    }

    private AsyncServerAuthModule mockAuthModule() {
        AsyncServerAuthModule authModule = mock(AsyncServerAuthModule.class);

        Collection<Class<?>> supportedMessageTypes = new HashSet<Class<?>>();
        supportedMessageTypes.add(Request.class);
        supportedMessageTypes.add(Response.class);
        given(authModule.getSupportedMessageTypes()).willReturn(supportedMessageTypes);

        given(authModule.initialize(Matchers.<MessagePolicy>anyObject(), Matchers.<MessagePolicy>anyObject(),
                Matchers.<CallbackHandler>anyObject(), anyMapOf(String.class, Object.class)))
                .willReturn(Promises.<Void, AuthenticationException>newSuccessfulPromise(null));

        return authModule;
    }
}
