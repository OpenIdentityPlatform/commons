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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.json.AuditJsonConfig.parseAuditEventHandlerConfiguration;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Tests the functionality of the JMS Audit event handler.
 */
@Test(dependsOnGroups = {"JndiJmsContextManagerTest"})
public class JmsAuditEventHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(JmsAuditEventHandlerTest.class);
    private static final String RESOURCE_PATH = "/org/forgerock/audit/handlers/jms/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EventTopicsMetaData CORE_EVENT_TOPICS =
            EventTopicsMetaDataBuilder.coreTopicSchemas().build();
    private int sessionCount = 0;

    /**
     * Tests the JmsAuditEventHandler which publishes JMS messages for all audit events.
     *
     * @throws Exception
     */
    @Test
    public void testJmsAuditEventHandlerPublish() throws Exception {
        // given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        MessageProducer producer = mock(MessageProducer.class);

        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(session.createProducer(topic)).thenReturn(producer);
        when(session.createTextMessage(anyString())).thenReturn(mock(TextMessage.class));
        doNothing().when(producer).send(any(Message.class));
        AuditEventHandler jmsAuditEventHandler =
                new JmsAuditEventHandler(
                        new DefaultJmsContextManager(connectionFactory, topic),
                        getDefaultConfiguration(),
                        CORE_EVENT_TOPICS);
        jmsAuditEventHandler.startup();

        // then
        jmsAuditEventHandler.publishEvent(null, "TEST_AUDIT", json(object(field("name", "TestEvent"))));

        // verify the results.
        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(producer).send(textMessageCaptor.capture());

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).createTextMessage(stringCaptor.capture());

        String text = stringCaptor.getValue();
        JsonValue jsonValue = new JsonValue(MAPPER.readValue(text, Map.class));

        assertThat(jsonValue).hasString("auditTopic");
        assertThat(jsonValue).stringAt("auditTopic").isEqualTo("TEST_AUDIT");
        assertThat(jsonValue).stringAt("event/name").isEqualTo("TestEvent");
    }

    /**
     * Validates that the JMS batch publisher functions as expected.
     *
     * @throws Exception
     */
    @Test
    public void testBatchJmsAuditEventHandler() throws Exception {
        // given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        final Session session = mock(Session.class);
        MessageProducer producer = mock(MessageProducer.class);
        final TextMessage textMessage = mock(TextMessage.class);

        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenAnswer(new Answer<Session>() {
            @Override
            public Session answer(InvocationOnMock invocation) throws Throwable {
                sessionCount++;
                logger.info("session created: {}", sessionCount);
                return session;
            }
        });
        when(session.createProducer(any(Destination.class))).thenReturn(producer);

        final ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createTextMessage(textCaptor.capture())).thenAnswer(new Answer<TextMessage>() {
            @Override
            public TextMessage answer(InvocationOnMock invocation) throws Throwable {
                when(textMessage.getText()).thenReturn(textCaptor.getValue());
                return textMessage;
            }
        });

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(100L); // small delay to simulate time to send message.
                logger.info("message sent by session {}: {}",
                        sessionCount,
                        invocation.getArgumentAt(0, TextMessage.class).getText());
                return null;
            }
        }).when(producer).send(textMessage);

        JmsAuditEventHandlerConfiguration configuration = getBufferedConfiguration();
        assertThat(configuration.getBatch().isBatchEnabled()).isTrue();   // make sure we are testing batch.

        AuditEventHandler jmsAuditEventHandler =
                new JmsAuditEventHandler(
                        new DefaultJmsContextManager(connectionFactory, mock(Topic.class)),
                        configuration,
                        CORE_EVENT_TOPICS);
        jmsAuditEventHandler.startup();

        // then
        int messagesToSend = configuration.getBatch().getMaxBatchedEvents() + 2;
        for (int i = 0; i < messagesToSend; i++) {
            jmsAuditEventHandler.publishEvent(
                    null,
                    "TEST_AUDIT",
                    json(object(
                            field("name", "TestBatchedEvent"),
                            field("index", i))
                    ));
        }
        // shutdown to clear out the queue.
        jmsAuditEventHandler.shutdown();

        // verify the results.

        // The first message gets picked up immediately, then the rest should be delivered in a 2 new batches as
        // the events remaining will be 1 more than the maxBatchedEvents.
        // We should then expect 3 sessions, unless the threads are really fast and only 2 are needed.
        verify(connection, atMost(3)).createSession(anyBoolean(), anyInt());
        // verify the total count of messages sent.
        verify(producer, times(messagesToSend)).send(any(TextMessage.class));
    }

    /**
     * Validates that the JMS Audit Event Handler configuration can be loaded by the auditServiceBuilder.
     *
     * @throws Exception
     */
    @Test
    public void testHandlerRegistration() throws Exception {
        final AuditServiceBuilder auditServiceBuilder = newAuditService();
        final Client client = new Client(mock(Handler.class));
        DependencyProviderBase dependencyProvider = new DependencyProviderBase();
        dependencyProvider.register(Client.class, client);
        auditServiceBuilder.withDependencyProvider(dependencyProvider);

        // register and startup the audit service.
        AuditJsonConfig.registerHandlerToService(getAuditConfig("event-handler-config.json"), auditServiceBuilder);
        AuditService auditService = auditServiceBuilder.build();
        try {
            auditService.startup();
            AuditEventHandler registeredHandler = auditService.getRegisteredHandler("jms");
            assertThat(registeredHandler).isNotNull().isInstanceOf(JmsAuditEventHandler.class);
        } finally {
            auditService.shutdown();
        }
    }

    @Test
    public void testJmsAuditEventHandlerPublishWithSuccessfulRetry() throws Exception {
        // given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        MessageProducer producer = mock(MessageProducer.class);

        when(connectionFactory.createConnection()).thenReturn(connection);

        // fail first time , return session second time
        when(connection.createSession(anyBoolean(), anyInt()))
                .thenThrow(mock(JMSException.class))
                .thenReturn(session);
        when(session.createProducer(topic)).thenReturn(producer);
        when(session.createTextMessage(anyString())).thenReturn(mock(TextMessage.class));
        doNothing().when(producer).send(any(Message.class));
        JmsAuditEventHandler jmsAuditEventHandler =
                new JmsAuditEventHandler(
                        new DefaultJmsContextManager(connectionFactory, topic),
                        getDefaultConfiguration(),
                        CORE_EVENT_TOPICS);
        EventTopicsMetaDataBuilder.coreTopicSchemas().build();
        jmsAuditEventHandler.startup();

        // when
        jmsAuditEventHandler.publishEvent(null, "TEST_AUDIT", json(object(field("name", "TestEvent"))));

        // then
        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(producer).send(textMessageCaptor.capture());

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).createTextMessage(stringCaptor.capture());

        String text = stringCaptor.getValue();
        JsonValue jsonValue = new JsonValue(MAPPER.readValue(text, Map.class));

        assertThat(jsonValue).hasString("auditTopic");
        assertThat(jsonValue).stringAt("auditTopic").isEqualTo("TEST_AUDIT");
        assertThat(jsonValue).stringAt("event/name").isEqualTo("TestEvent");
    }

    @Test
    public void testJmsAuditEventHandlerPublishWithFailedRetry() throws Exception {
        // given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        MessageProducer producer = mock(MessageProducer.class);

        when(connectionFactory.createConnection()).thenReturn(connection);

        // fail with exception, then fail with exception agian to fail completely
        when(connection.createSession(anyBoolean(), anyInt()))
                .thenThrow(mock(JMSException.class), mock(JMSException.class));
        when(session.createProducer(topic)).thenReturn(producer);
        when(session.createTextMessage(anyString())).thenReturn(mock(TextMessage.class));
        doNothing().when(producer).send(any(Message.class));
        JmsAuditEventHandler jmsAuditEventHandler =
                new JmsAuditEventHandler(
                        new DefaultJmsContextManager(connectionFactory, topic),
                        getDefaultConfiguration(),
                        CORE_EVENT_TOPICS);
        jmsAuditEventHandler.startup();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                jmsAuditEventHandler.publishEvent(null, "TEST_AUDIT", json(object(field("name", "TestEvent"))));

        // then
        assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);

    }

    @Test
    public void testQueryNotSupported() throws Exception {

        // given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        Connection connection = mock(Connection.class);

        when(connectionFactory.createConnection()).thenReturn(connection);
        JmsAuditEventHandler jmsAuditEventHandler =
                new JmsAuditEventHandler(
                        new DefaultJmsContextManager(connectionFactory, topic),
                        getDefaultConfiguration(),
                        CORE_EVENT_TOPICS);
        jmsAuditEventHandler.startup();

        // then
        Promise<QueryResponse, ResourceException> response =
                jmsAuditEventHandler.queryEvents(null, "TEST_AUDIT", Requests.newQueryRequest(""), null);

        assertThat(response).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testReadNotSupported() throws Exception {

        // given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Topic topic = mock(Topic.class);
        Connection connection = mock(Connection.class);

        when(connectionFactory.createConnection()).thenReturn(connection);
        JmsAuditEventHandler jmsAuditEventHandler =
                new JmsAuditEventHandler(
                        new DefaultJmsContextManager(connectionFactory, topic),
                        getDefaultConfiguration(),
                        CORE_EVENT_TOPICS);
        jmsAuditEventHandler.startup();

        // when
        Promise<ResourceResponse, ResourceException> response =
                jmsAuditEventHandler.readEvent(null, "TEST_AUDIT", "id");


        // then
        assertThat(response).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    private JmsAuditEventHandlerConfiguration getDefaultConfiguration() throws Exception {
        return parseAuditEventHandlerConfiguration(
                JmsAuditEventHandlerConfiguration.class,
                getAuditConfig("event-handler-config.json"));
    }

    private JmsAuditEventHandlerConfiguration getBufferedConfiguration() throws Exception {
        return parseAuditEventHandlerConfiguration(
                JmsAuditEventHandlerConfiguration.class,
                getAuditConfig("batch-handler-config.json"));
    }

    static JsonValue getAuditConfig(String testConfigFile) throws AuditException {
        return AuditJsonConfig.getJson(
                JmsAuditEventHandlerTest.class.getResourceAsStream(RESOURCE_PATH + testConfigFile));
    }

    private static class DefaultJmsContextManager implements JmsContextManager {

        private final ConnectionFactory connectionFactory;
        private final Topic topic;

        public DefaultJmsContextManager(ConnectionFactory connectionFactory, Topic topic) {
            this.connectionFactory = connectionFactory;
            this.topic = topic;
        }

        @Override
        public Topic getTopic() throws InternalServerErrorException {
            return topic;
        }

        @Override
        public ConnectionFactory getConnectionFactory() throws InternalServerErrorException {
            return connectionFactory;
        }
    }
}
