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
import static org.mockito.Mockito.*;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
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
import org.testng.annotations.Test;

/**
 * Tests the functionality of the JMS Audit event handler.
 */
public class JmsAuditEventHandlerTest {

    private static final String RESOURCE_PATH = "/org/forgerock/audit/handlers/jms/";
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Tests the JmsAuditEventHandler which publishes JMS messages for all audit events.
     *
     * @throws Exception
     */
    @Test
    public void testJmsAuditEventHandlerPublish() throws Exception {
        // given
        Session session = mock(Session.class);
        when(session.createTextMessage(anyString())).thenReturn(mock(TextMessage.class));

        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(any(Destination.class))).thenReturn(producer);

        JmsAuditEventHandler jmsAuditEventHandler = new JmsAuditEventHandler(
                parseAuditEventHandlerConfiguration(JmsAuditEventHandlerConfiguration.class, getAuditConfig()),
                EventTopicsMetaDataBuilder.coreTopicSchemas().build());
        jmsAuditEventHandler.startup();
        when(jmsAuditEventHandler.getConnection().createSession(anyBoolean(), anyInt())).thenReturn(session);

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
        AuditJsonConfig.registerHandlerToService(getAuditConfig(), auditServiceBuilder);
        AuditService auditService = auditServiceBuilder.build();
        try {
            auditService.startup();
            AuditEventHandler registeredHandler = auditService.getRegisteredHandler("jms");
            assertThat(registeredHandler).isNotNull().isInstanceOf(JmsAuditEventHandler.class);
        } finally {
            auditService.shutdown();
        }
    }

    static JsonValue getAuditConfig() throws AuditException {
        return AuditJsonConfig.getJson(
                JmsAuditEventHandlerTest.class.getResourceAsStream(RESOURCE_PATH + "event-handler-config.json"));
    }

}
