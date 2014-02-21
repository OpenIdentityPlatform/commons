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

package org.forgerock.jaspi.runtime.config;

import org.forgerock.jaspi.context.ServerAuthModuleInstanceCreator;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class SessionServerContextFactoryTest {

    private SessionServerContextFactory sessionServerContextFactory;

    private FilterConfig config;
    private FilterConfiguration filterConfiguration;
    private ServerAuthModuleInstanceCreator moduleInstanceCreator;

    @BeforeMethod
    public void setUp() {

        config = mock(FilterConfig.class);
        filterConfiguration = mock(FilterConfiguration.class);
        moduleInstanceCreator = mock(ServerAuthModuleInstanceCreator.class);

        sessionServerContextFactory = new TestSessionServerContextFactory(config, filterConfiguration,
                moduleInstanceCreator);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void shouldThrowJaspiAuthExceptionWhenFailsToGetModuleConfigurationFactory() throws AuthException,
            ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);

        doThrow(ServletException.class).when(filterConfiguration)
                .get(eq(config), anyString(), anyString(), anyString());

        //When
        sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        fail();
    }

    @Test
    public void shouldGetServerAuthContextWithNoModules() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object())
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        //When
        ServerAuthContext authContext = sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler,
                contextHandler);

        //Then
        verifyZeroInteractions(moduleInstanceCreator);
        assertTrue(authContext.getClass().isAssignableFrom(TestServerAuthContext.class));
    }

    @Test (expectedExceptions = JsonValueException.class)
    public void shouldThrowJsonValueExceptionWhenSessionModuleClassNameNotSet() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.SESSION_MODULE_KEY, JsonValue.object(
                    JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                        JsonValue.field("PROP", "SESSION_PROP")
                    ))
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        //When
        sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        fail();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldGetServerAuthContextWithOnlySessionModule() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.SESSION_MODULE_KEY, JsonValue.object(
                    JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY, "SESSION_MODULE"),
                    JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                        JsonValue.field("PROP", "SESSION_PROP")
                    ))
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        given(moduleInstanceCreator.construct(eq("SESSION_MODULE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler))).willReturn(sessionAuthModule);

        //When
        sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        ArgumentCaptor<Map> sessionPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(moduleInstanceCreator).construct(eq("SESSION_MODULE"), sessionPropsCaptor.capture(),
                Matchers.<MessagePolicy>anyObject(), eq(handler));
        verifyNoMoreInteractions(moduleInstanceCreator);

        Map sessionProps = sessionPropsCaptor.getValue();
        assertEquals(sessionProps.size(), 1);
        assertTrue(sessionProps.containsKey("PROP"));
        assertTrue(sessionProps.containsValue("SESSION_PROP"));
    }

    @Test
    public void shouldGetServerAuthContextWithOnlySessionModuleWithNoProperties() throws AuthException,
            ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.SESSION_MODULE_KEY, JsonValue.object(
                    JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY, "SESSION_MODULE")
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        given(moduleInstanceCreator.construct(eq("SESSION_MODULE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler))).willReturn(sessionAuthModule);

        //When
        sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        verify(moduleInstanceCreator).construct(eq("SESSION_MODULE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler));
        verifyNoMoreInteractions(moduleInstanceCreator);
    }

    @Test
    public void shouldGetServerAuthContextAndSetMessagePolicy() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        ServerAuthModule sessionAuthModule = mock(ServerAuthModule.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.SESSION_MODULE_KEY, JsonValue.object(
                    JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY, "SESSION_MODULE"),
                    JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                        JsonValue.field("PROP", "SESSION_PROP")
                    ))
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        given(moduleInstanceCreator.construct(eq("SESSION_MODULE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler))).willReturn(sessionAuthModule);

        //When
        sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        ArgumentCaptor<MessagePolicy> messagePolicyCaptor = ArgumentCaptor.forClass(MessagePolicy.class);
        verify(moduleInstanceCreator).construct(eq("SESSION_MODULE"), anyMapOf(String.class, Object.class),
                messagePolicyCaptor.capture(), eq(handler));

        MessagePolicy.TargetPolicy[] targetPolicies = messagePolicyCaptor.getValue().getTargetPolicies();
        assertEquals(targetPolicies.length, 1);
        assertEquals(targetPolicies[0].getProtectionPolicy().getID(),
                MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void shouldThrowJaspiAuthExceptionWhenGivenArrayOfSessionModules() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.SESSION_MODULE_KEY, JsonValue.array(
                    JsonValue.object(),
                    JsonValue.object()
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        //When
        sessionServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        fail();
    }

    private static class TestSessionServerContextFactory extends SessionServerContextFactory {

        protected TestSessionServerContextFactory(final FilterConfig config,
                final FilterConfiguration filterConfiguration,
                final ServerAuthModuleInstanceCreator moduleInstanceCreator) {
            super(config, filterConfiguration, moduleInstanceCreator);
        }

        @Override
        protected ServerAuthContext getServerAuthContext(MessageInfoUtils messageInfoUtils, CallbackHandler handler,
                ContextHandler contextHandler, JsonValue configuration, ServerAuthModule sessionAuthModule)
                throws AuthException {
            return new TestServerAuthContext();
        }
    }

    private static class TestServerAuthContext implements ServerAuthContext {

        @Override
        public AuthStatus validateRequest(MessageInfo messageInfo, Subject subject, Subject subject2)
                throws AuthException {
            return null;
        }

        @Override
        public AuthStatus secureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
            return null;
        }

        @Override
        public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        }
    }
}
