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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.*;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.json.JsonValue;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Tests the functionality of the JMS Audit event handler.
 */
public class JmsAuditEventHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(JmsAuditEventHandlerTest.class);
    private static final String RESOURCE_PATH = "/org/forgerock/audit/handlers/jms/";
    private static final ObjectMapper mapper = new ObjectMapper();
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
        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        MessageProducer producer = mock(MessageProducer.class);

        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(session.createProducer(any(Destination.class))).thenReturn(producer);
        when(session.createTextMessage(anyString())).thenReturn(mock(TextMessage.class));

        JmsAuditEventHandler jmsAuditEventHandler = new JmsAuditEventHandler(
                connectionFactory,
                mock(Topic.class),
                parseAuditEventHandlerConfiguration(
                        JmsAuditEventHandlerConfiguration.class,
                        getAuditConfig("event-handler-config.json")),
                EventTopicsMetaDataBuilder.coreTopicSchemas().build());
        jmsAuditEventHandler.startup();

        // then
        jmsAuditEventHandler.publishEvent(null, "TEST_AUDIT", json(object(field("name", "TestEvent"))));

        // verify the results.
        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(producer).send(textMessageCaptor.capture());

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).createTextMessage(stringCaptor.capture());

        String text = stringCaptor.getValue();
        JsonValue jsonValue = new JsonValue(mapper.readValue(text, Map.class));

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
                logger.info("message sent by session {}: {}",
                        sessionCount,
                        invocation.getArgumentAt(0, TextMessage.class).getText());
                return null;
            }
        }).when(producer).send(textMessage);

        JmsAuditEventHandlerConfiguration configuration = parseAuditEventHandlerConfiguration(
                JmsAuditEventHandlerConfiguration.class,
                getAuditConfig("batch-handler-config.json"));
        assertThat(configuration.isBatchEnabled()).isTrue();   // make sure we are testing batch.

        JmsAuditEventHandler jmsAuditEventHandler = new JmsAuditEventHandler(
                connectionFactory,
                mock(Topic.class),
                configuration,
                EventTopicsMetaDataBuilder.coreTopicSchemas().build());
        jmsAuditEventHandler.startup();

        // then
        int messagesToSend = configuration.getBatchConfiguration().getMaxBatchedEvents() + 2;
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
        // We should then expect only 3 sessions.
        verify(connection, times(3)).createSession(anyBoolean(), anyInt());
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

    static JsonValue getAuditConfig(String testConfigFile) throws AuditException {
        return AuditJsonConfig.getJson(
                JmsAuditEventHandlerTest.class.getResourceAsStream(RESOURCE_PATH + testConfigFile));
    }
}
