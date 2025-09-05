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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2024 3A Systems LLC.
 */

package org.forgerock.audit.handlers.jms;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Topic;
import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import org.assertj.core.api.ThrowableAssert;
import org.forgerock.audit.handlers.jms.JmsAuditEventHandlerConfiguration.JndiConfiguration;
import org.forgerock.json.resource.InternalServerErrorException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the {@link JndiJmsContextManager} class.
 */
@Test(groups = {"JndiJmsContextManagerTest"})
public class JndiJmsContextManagerTest {

    private InitialContextFactoryBuilder originalContextFactoryBuilder;
    private ClassLoader originalClassLoader;

    /**
     * Save the context builder and classloader to be restored after the test is completed.
     * @throws Exception
     */
    @BeforeMethod
    private void beforeTestContextLoading() throws Exception {
        originalContextFactoryBuilder = getInitialContextFactoryBuilder();
        originalClassLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Restore the context builder and classloader to their original state.
     *
     * @throws Exception
     */
    @AfterMethod
    private void afterTestContextLoading() throws Exception {
        setInitialContextFactoryBuilder(originalContextFactoryBuilder);
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    /**
     * Tests that the {@link JndiJmsContextManager} restores the context classloader after doing a lookup and
     * verifies that the "lookup logic that ensures you are getting the class you are asking for" is valid.
     *
     * @throws Exception
     */
    @Test
    public void testContextLoading() throws Exception {
        // Given

        // Setup known mocked classloader and context builder to be used for validation.
        InitialContextFactoryBuilder builder = mock(InitialContextFactoryBuilder.class);
        setInitialContextFactoryBuilder(builder);
        ClassLoader contextClassLoader = mock(ClassLoader.class);
        Thread.currentThread().setContextClassLoader(contextClassLoader);
        Context context = mock(Context.class);
        InitialContextFactory initialContextFactory = mock(InitialContextFactory.class);
        when(initialContextFactory.getInitialContext(any(Hashtable.class))).thenReturn(context);
        when(builder.createInitialContextFactory(any(Hashtable.class))).thenReturn(initialContextFactory);

        // Setup JMS specific components for this test.
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        JndiConfiguration configuration = new JndiConfiguration();
        configuration.setJmsConnectionFactoryName("ConnectionFactory");
        configuration.setJmsTopicName("audit");
        final JndiJmsContextManager manager = new JndiJmsContextManager(configuration);

        // When

        // 4 test iterations: a badValue, a null, then a good connectionFactory, then a good topic.
        when(context.lookup(anyString())).thenReturn("badValue", null, connectionFactory, topic);

        // Then

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                manager.getConnectionFactory();
            }
        }).isInstanceOf(InternalServerErrorException.class).hasMessageContaining("did not return a");

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                manager.getTopic();
            }
        }).isInstanceOf(InternalServerErrorException.class).hasMessageStartingWith("No Object");

        // check that the contextClassLoader is restored after looking up the connectionFactory
        ConnectionFactory contextConnectionFactory = manager.getConnectionFactory();
        assertThat(contextConnectionFactory).isSameAs(connectionFactory);
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(contextClassLoader);

        // check that the contextClassLoader is restored after looking up the topic.
        Topic contextTopic = manager.getTopic();
        assertThat(contextTopic).isSameAs(topic);
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(contextClassLoader);
    }

    /**
     * Calls {@link NamingManager#getInitialContextFactoryBuilder()} to retrieve the current context builder.
     *
     * @return the current context builder.
     * @throws Exception
     */
    private InitialContextFactoryBuilder getInitialContextFactoryBuilder() throws Exception {
        // NamingManager.getInitialContextFactoryBuilder has private access, so we need to set it accessible to use it.
        Method builderMethod = NamingManager.class.getDeclaredMethod("getInitialContextFactoryBuilder");
        builderMethod.setAccessible(true);
        return (InitialContextFactoryBuilder) builderMethod.invoke(null);
    }

    /**
     * Calls {@link NamingManager#setInitialContextFactoryBuilder(InitialContextFactoryBuilder)} to set the context
     * builder to the one being passed in.
     *
     * @param builder builder to set as the NamingManagers context factory builder.
     * @throws Exception
     */
    private void setInitialContextFactoryBuilder(InitialContextFactoryBuilder builder) throws Exception {
        // NamingManager.setInitialContextFactoryBuilder has logic that only allows the builder to be set once.
        // We must set it to null, to allow us to restore the builder to the original.
        Field factoryBuilderField = NamingManager.class.getDeclaredField("initctx_factory_builder");
        factoryBuilderField.setAccessible(true);
        factoryBuilderField.set(null, null);
        NamingManager.setInitialContextFactoryBuilder(builder);
    }
}
