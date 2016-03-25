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

import static org.forgerock.audit.util.ResourceExceptionsUtil.adapt;
import static org.forgerock.audit.util.ResourceExceptionsUtil.notSupported;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes Audit events on a JMS Topic.
 */
public class JmsAuditEventHandler extends AuditEventHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(JmsAuditEventHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final JmsResourceManager jmsResourceManager;
    private final Publisher<JsonValue> publisher;

    /**
     * Creates a new AuditEventHandler instance that publishes JMS messages on a JMS Topic for each Audit event.
     *
     * @param jmsContextManager optional injected {@link JmsContextManager}.
     * @param configuration Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData Meta-data for all audit event topics.
     */
    @Inject
    public JmsAuditEventHandler(
            @Audit final JmsContextManager jmsContextManager,
            final JmsAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData) throws ResourceException {

        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());

        publisher = buildPublisher(configuration);
        this.jmsResourceManager =
                jmsContextManager == null
                        ? new JmsResourceManager(
                                configuration, new JndiJmsContextManager(configuration.getJndi()))
                        : new JmsResourceManager(configuration, jmsContextManager);
        logger.debug("Successfully configured JMS audit event handler.");
    }

    /**
     * Factory method for publisher.
     *
     * @param configuration used to determine if a batched publisher is needed or not.
     * @return the constructed publisher.
     */
    Publisher<JsonValue> buildPublisher(JmsAuditEventHandlerConfiguration configuration) {
        return configuration.getBatch().isBatchEnabled()
                ? new JmsBatchPublisher(configuration.getBatch())
                : new JmsPublisher();
    }

    /**
     * Creates the JMS Topic and ConnectionFactory from the context configuration settings and opens the JMS connection.
     */
    @Override
    public void startup() throws ResourceException {
        publisher.startup();
        logger.debug("JMS audit event handler is started.");
    }

    /**
     * Closes the JMS connection.
     */
    @Override
    public void shutdown() throws ResourceException {
        publisher.shutdown();
        logger.debug("JMS audit event handler is shutdown.");
    }

    @Override
    public boolean canBeUsedForQueries() {
        // JMS does not support Query or Read.
        return false;
    }

    /**
     * Converts the audit event into a JMS TextMessage and then publishes the message on the configured jmsTopic.
     *
     * @param context The context chain that initiated the event.
     * @param auditTopic The Audit Topic for which the auditEvent was created for. (Not to be confused with a JMS Topic)
     * @param auditEvent The event to convert to a JMS TextMessage and publish on the JMS Topic.
     * @return a promise with either a response or an exception
     */
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String auditTopic,
            JsonValue auditEvent) {
        try {
            publisher.publish(json(object(
                    field("auditTopic", auditTopic),
                    field("event", auditEvent.getObject())
            )));

            // Return the auditEvent as the response.
            return newResourceResponse(
                    auditEvent.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                    null,
                    auditEvent).asPromise();

        } catch (Exception ex) {
            return adapt(ex).asPromise();
        }
    }


    /**
     * Publishes the list of messages using a single producer.
     *
     * @param messages the messages to send.
     * @throws InternalServerErrorException if unable to publish jms messages.
     */
    private void publishJmsMessagesWithRetry(List<JsonValue> messages) throws InternalServerErrorException {
        try {
            publishJmsMessages(messages);
        } catch (JMSException e) {
            logger.debug("Retrying publish", e);
            try {
                resetConnection();
                publishJmsMessages(messages);
            } catch (JMSException|ResourceException ex) {
                final String message = "Unable to publish JMS messages, messages are likely lost";
                logger.error(message, e);
                throw new InternalServerErrorException(message, e);
            }
        }
    }

    /**
     * Publishes the list of messages using a single producer.
     *
     * @param messages the messages to send.
     * @throws JMSException if unable to publish jms messages and a retry is possible.
     *         InternalServerErrorException if unable to publish jms messages and a retry is not possible.
     */
    private void publishJmsMessages(List<JsonValue> messages) throws JMSException, InternalServerErrorException {
        try (Session session = jmsResourceManager.createSession()) {
            try (MessageProducer producer = jmsResourceManager.createProducer(session)) {
                for (JsonValue message : messages) {
                    String text = mapper.writeValueAsString(message.getObject());
                    producer.send(session.createTextMessage(text));
                }
            }
        } catch (JMSException e) {
            logger.debug("Failed to publish messages", e);
            throw e;
        } catch (JsonProcessingException e) {
            final String message = "Unable to publish JMS messages, messages are likely lost";
            logger.error(message, e);
            throw new InternalServerErrorException(message, e);
        }
    }

    /**
     * Returns NotSupportedException as query is not implemented for JMS.
     * <br/>
     * {@inheritDoc}
     * @return NotSupportedException as query is not implemented for JMS.
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(
            Context context,
            String topic,
            QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler) {
        return notSupported(queryRequest).asPromise();
    }

    /**
     * Returns NotSupportedException as read is not implemented for JMS.
     * <br/>
     * {@inheritDoc}
     * @return NotSupportedException as read is not implemented for JMS.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        return new NotSupportedException("read operations are not supported").asPromise();
    }

    /**
     * Implementation of the BatchPublisher to handle publishing groups of audit event data to JMS.
     */
    private class JmsBatchPublisher extends BatchPublisher<JsonValue> {

        /**
         * Constructor that passes the configuration to {@link BatchPublisher}
         *
         * @param configuration config of the publisher.
         */
        public JmsBatchPublisher(BatchPublisherConfiguration configuration) {
            super("JmsBatchPublisher", configuration);
        }

        @Override
        public void startupPublisher() throws ResourceException {
            openJmsConnection();
        }

        @Override
        public void shutdownPublisher() throws ResourceException {
            closeJmsConnection();
        }

        @Override
        protected void publishMessages(List<JsonValue> messages) {
            try {
                publishJmsMessagesWithRetry(messages);
            } catch (InternalServerErrorException e) {
                // do nothing
            }
        }
    }

    /**
     * Implementation of the Publisher to handle publishing singleton audit event data to JMS.
     */
    private class JmsPublisher implements Publisher<JsonValue> {

        @Override
        public void startup() throws ResourceException {
            openJmsConnection();
        }

        @Override
        public void shutdown() throws ResourceException {
            closeJmsConnection();
        }

        @Override
        public void publish(JsonValue message) throws ResourceException {
            publishJmsMessagesWithRetry(Collections.singletonList(message));
        }
    }

    private void openJmsConnection() throws InternalServerErrorException {
        try {
            jmsResourceManager.openConnection();
        } catch (JMSException e) {
            throw new InternalServerErrorException("trouble opening connection", e);
        }
    }

    private void closeJmsConnection() throws InternalServerErrorException {
        try {
            jmsResourceManager.closeConnection();
        } catch (JMSException e) {
            throw new InternalServerErrorException("trouble closing connection", e);
        }
    }

    private void resetConnection() throws InternalServerErrorException {
        closeJmsConnection();
        openJmsConnection();
    }
}
