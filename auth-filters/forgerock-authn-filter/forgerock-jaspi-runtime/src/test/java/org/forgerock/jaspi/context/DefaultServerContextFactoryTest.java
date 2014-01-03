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

package org.forgerock.jaspi.context;

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.runtime.config.ServerContextFactory;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.jaspi.utils.FilterConfiguration;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DefaultServerContextFactoryTest {

    private DefaultServerContextFactory defaultServerContextFactory;

    private FilterConfig config;
    private FilterConfiguration filterConfiguration;
    private ServerAuthModuleInstanceCreator moduleInstanceCreator;

    @BeforeMethod
    public void setUp() {

        config = mock(FilterConfig.class);
        filterConfiguration = mock(FilterConfiguration.class);
        moduleInstanceCreator = mock(ServerAuthModuleInstanceCreator.class);

        defaultServerContextFactory = new DefaultServerContextFactory(config, filterConfiguration,
                moduleInstanceCreator);
    }

    @Test
    public void shouldCreateServerContextFactoryUsingStaticFactoryMethod() {

        //Given

        //When
        ServerContextFactory serverContextFactory = DefaultServerContextFactory.getServerContextFactory(config);

        //Then
        assertNotNull(serverContextFactory);
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
        ServerAuthContext authContext = defaultServerContextFactory.getServerAuthContext(messageInfoUtils, handler,
                contextHandler);

        //Then
        verifyZeroInteractions(moduleInstanceCreator);
        assertTrue(authContext.getClass().isAssignableFrom(FallbackServerAuthContext.class));
    }

    @Test (expectedExceptions = JsonValueException.class)
    public void shouldThrowJsonValueExceptionWhenServerAuthModuleClassNameNotSet() throws AuthException,
            ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.AUTH_MODULES_KEY, JsonValue.array(
                    JsonValue.object(
                        JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                            JsonValue.field("PROP", "AUTH_ONE_PROP")
                        ))
                    )
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        //When
        defaultServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        fail();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void shouldGetServerAuthContextWithAuthModules() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);
        ServerAuthModule authModuleTwo = mock(ServerAuthModule.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.AUTH_MODULES_KEY, JsonValue.array(
                    JsonValue.object(
                        JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY, "AUTH_MODULE_ONE"),
                        JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                            JsonValue.field("PROP", "AUTH_ONE_PROP")
                        ))
                    ),
                    JsonValue.object(
                        JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY, "AUTH_MODULE_TWO"),
                        JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                            JsonValue.field("PROP", "AUTH_TWO_PROP")
                        ))
                    )
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        given(moduleInstanceCreator.construct(eq("AUTH_MODULE_ONE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler))).willReturn(authModuleOne);
        given(moduleInstanceCreator.construct(eq("AUTH_MODULE_TWO"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler))).willReturn(authModuleTwo);

        //When
        defaultServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        ArgumentCaptor<Map> authOnePropsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> authTwoPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(moduleInstanceCreator).construct(eq("AUTH_MODULE_ONE"), authOnePropsCaptor.capture(),
                Matchers.<MessagePolicy>anyObject(), eq(handler));
        verify(moduleInstanceCreator).construct(eq("AUTH_MODULE_TWO"), authTwoPropsCaptor.capture(),
                Matchers.<MessagePolicy>anyObject(), eq(handler));
        verifyNoMoreInteractions(moduleInstanceCreator);

        Map authOneProps = authOnePropsCaptor.getValue();
        assertEquals(authOneProps.size(), 1);
        assertTrue(authOneProps.containsKey("PROP"));
        assertTrue(authOneProps.containsValue("AUTH_ONE_PROP"));

        Map authTwoProps = authTwoPropsCaptor.getValue();
        assertEquals(authTwoProps.size(), 1);
        assertTrue(authTwoProps.containsKey("PROP"));
        assertTrue(authTwoProps.containsValue("AUTH_TWO_PROP"));
    }

    @Test
    public void shouldGetServerAuthContextWithAuthModuleWithNoProperties() throws AuthException, ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        ServerAuthModule authModuleOne = mock(ServerAuthModule.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.AUTH_MODULES_KEY, JsonValue.array(
                    JsonValue.object(
                        JsonValue.field(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY, "AUTH_MODULE_ONE")
                    )
                ))
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        given(moduleInstanceCreator.construct(eq("AUTH_MODULE_ONE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler))).willReturn(authModuleOne);

        //When
        defaultServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        verify(moduleInstanceCreator).construct(eq("AUTH_MODULE_ONE"), anyMapOf(String.class, Object.class),
                Matchers.<MessagePolicy>anyObject(), eq(handler));
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void shouldThrowJaspiAuthExceptionWhenGivenSingleAuthModuleNotInArray() throws AuthException,
            ServletException {

        //Given
        MessageInfoUtils messageInfoUtils = mock(MessageInfoUtils.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        ContextHandler contextHandler = mock(ContextHandler.class);
        ModuleConfigurationFactory configurationFactory = mock(ModuleConfigurationFactory.class);

        JsonValue configuration = JsonValue.json(JsonValue.object(
            JsonValue.field(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                JsonValue.field(ModuleConfigurationFactory.AUTH_MODULES_KEY, JsonValue.object())
            ))
        ));

        given(filterConfiguration.get(eq(config), anyString(), anyString(), anyString()))
                .willReturn(configurationFactory);
        given(configurationFactory.getConfiguration()).willReturn(configuration);

        //When
        defaultServerContextFactory.getServerAuthContext(messageInfoUtils, handler, contextHandler);

        //Then
        fail();
    }
}
