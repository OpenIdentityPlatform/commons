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
 */

package org.forgerock.audit.handlers.jms;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;

import org.assertj.core.api.ThrowableAssert;
import org.forgerock.audit.handlers.jms.JmsAuditEventHandlerConfiguration.JndiConfiguration;
import org.forgerock.json.resource.InternalServerErrorException;
import org.testng.annotations.Test;

/**
 * Tests the {@link JndiJmsContextManager} class.
 */
public class JndiJmsContextManagerTest {

    /**
     * Tests that the {@link JndiJmsContextManager} restores the context classloader after doing a lookup.
     * @throws Exception
     */
    @Test
    public void testContextLoading() throws Exception {
        // Given
        Context context = mock(Context.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        InitialContextFactory initialContextFactory = mock(InitialContextFactory.class);
        InitialContextFactoryBuilder builder = mock(InitialContextFactoryBuilder.class);
        NamingManager.setInitialContextFactoryBuilder(builder);
        ClassLoader contextClassLoader = mock(ClassLoader.class);
        Thread.currentThread().setContextClassLoader(contextClassLoader);
        JndiConfiguration configuration = new JndiConfiguration();
        configuration.setJmsConnectionFactoryName("ConnectionFactory");
        configuration.setJmsTopicName("audit");
        final JndiJmsContextManager manager = new JndiJmsContextManager(configuration);

        // When

        // 4 test iterations: a badValue, a null, then a good connectionFactory, then a good topic.
        when(context.lookup(anyString())).thenReturn("badValue", null, connectionFactory, topic);
        when(initialContextFactory.getInitialContext(any(Hashtable.class))).thenReturn(context);
        when(builder.createInitialContextFactory(any(Hashtable.class))).thenReturn(initialContextFactory);

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
}
