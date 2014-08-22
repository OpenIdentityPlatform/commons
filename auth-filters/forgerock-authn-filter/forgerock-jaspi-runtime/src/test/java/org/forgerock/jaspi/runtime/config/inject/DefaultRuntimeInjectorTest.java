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

package org.forgerock.jaspi.runtime.config.inject;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.auth.common.LoggingConfigurator;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.runtime.config.ServerContextFactory;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class DefaultRuntimeInjectorTest {

    private FilterConfig config;
    private FilterConfiguration filterConfiguration;

    @BeforeMethod
    public void setUp() {
        config = mock(FilterConfig.class);
        filterConfiguration = mock(FilterConfiguration.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateAndAddJaspiRuntimeInstanceToInjectorOnInitialisation() throws ServletException,
            AuthException {

        //Given
        LoggingConfigurator<MessageInfo> loggingConfigurator = mock(LoggingConfigurator.class);

        DebugLogger debugLogger = mock(DebugLogger.class);
        AuditLogger<MessageInfo> auditLogger = mock(AuditLogger.class);

        given(loggingConfigurator.getDebugLogger()).willReturn(debugLogger);
        given(loggingConfigurator.getAuditLogger()).willReturn(auditLogger);

        ServerContextFactory serverContextFactory = mock(ServerContextFactory.class);
        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(filterConfiguration.get(eq(config), eq("context-factory-class"), anyString(), anyString()))
                .willReturn(serverContextFactory);

        given(filterConfiguration.get(eq(config), eq("logging-configurator-class"), anyString(), anyString()))
                .willReturn(loggingConfigurator);

        given(serverContextFactory.getServerAuthContext(Matchers.<MessageInfoUtils>anyObject(),
                Matchers.<CallbackHandler>anyObject(), Matchers.<ContextHandler>anyObject()))
                .willReturn(serverAuthContext);

        //When
        RuntimeInjector injector = new DefaultRuntimeInjector(config, filterConfiguration);

        //Then
        JaspiRuntime jaspiRuntime = injector.getInstance(JaspiRuntime.class);
        assertNotNull(jaspiRuntime);
    }

    @Test
    public void shouldCreateAndAddJaspiRuntimeInstanceToInjectorOnInitialisationWithEmptyLoggingConfigurator()
            throws ServletException, AuthException {

        //Given
        ServerContextFactory serverContextFactory = mock(ServerContextFactory.class);
        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(filterConfiguration.get(eq(config), eq("context-factory-class"), anyString(), anyString()))
                .willReturn(serverContextFactory);

        given(serverContextFactory.getServerAuthContext(Matchers.<MessageInfoUtils>anyObject(),
                Matchers.<CallbackHandler>anyObject(), Matchers.<ContextHandler>anyObject()))
                .willReturn(serverAuthContext);

        //When
        RuntimeInjector injector = new DefaultRuntimeInjector(config, filterConfiguration);

        //Then
        JaspiRuntime jaspiRuntime = injector.getInstance(JaspiRuntime.class);
        assertNotNull(jaspiRuntime);
    }

    @Test (expectedExceptions = RuntimeException.class)
    public void getInstanceShouldThrowRuntimeExceptionIfTypeNotRegistered() throws AuthException, ServletException {

        //Given
        ServerContextFactory serverContextFactory = mock(ServerContextFactory.class);
        ServerAuthContext serverAuthContext = mock(ServerAuthContext.class);

        given(filterConfiguration.get(eq(config), eq("context-factory-class"), anyString(), anyString()))
                .willReturn(serverContextFactory);

        given(serverContextFactory.getServerAuthContext(Matchers.<MessageInfoUtils>anyObject(),
                Matchers.<CallbackHandler>anyObject(), Matchers.<ContextHandler>anyObject()))
                .willReturn(serverAuthContext);

        RuntimeInjector injector = new DefaultRuntimeInjector(config, filterConfiguration);

        //When
        injector.getInstance(Object.class);

        //Then
        fail();
    }
}
