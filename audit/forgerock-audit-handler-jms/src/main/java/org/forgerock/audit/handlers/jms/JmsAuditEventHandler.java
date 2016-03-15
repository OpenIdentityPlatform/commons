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

import static org.forgerock.audit.util.ResourceExceptionsUtil.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes Audit events on a JMS Topic.
 */
public class JmsAuditEventHandler extends AuditEventHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(JmsAuditEventHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final JmsAuditEventHandlerConfiguration configuration;
    private final JndiContextManager jndiContextManager;
    private Connection connection;

    /**
     * Creates a new AuditEventHandler instance that publishes JMS messages on a JMS Topic for each Audit event.
     *
     * @param configuration Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData Meta-data for all audit event topics.
     */
    @Inject
    public JmsAuditEventHandler(
            final JmsAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData) throws ResourceException {

        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        Reject.ifNull(configuration.getProviderUrl(), "JMS providerUrl is required");
        Reject.ifNull(configuration.getJmsTopic(), "JMS publish topic is required");
        Reject.ifNull(configuration.getInitialContextFactory(), "JMS provider connection context factory is required.");
        Reject.ifNull(configuration.getDeliveryMode(), "JMS Delivery Mode is required");
        Reject.ifNull(configuration.getSessionAcknowledgement(), "JMS Session Acknowledgement is required");

        this.configuration = configuration;
        this.jndiContextManager = new JndiContextManager(configuration);

        logger.debug("Successfully configured JMS audit event handler.");
    }

    /**
     * Creates the JMS Topic and ConnectionFactory from the context configuration settings and opens the JMS connection.
     */
    @Override
    public void startup() throws ResourceException {
        try {
            ConnectionFactory connectionFactory = jndiContextManager.getConnectionFactory();
            connection = connectionFactory.createConnection();

            connection.start();
            logger.debug("JMS audit event handler connection is started.");
        } catch (Exception e) {
            throw new InternalServerErrorException("Unable to start JMS connection", e);
        }
    }

    /**
     * Closes the JMS connection.
     */
    @Override
    public void shutdown() throws ResourceException {
        try {
            connection.close();
            connection = null;
            logger.debug("JMS audit event handler is shutdown.");
        } catch (JMSException e) {
            throw new InternalServerErrorException("Unable to close JMS connection", e);
        }
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

        Session session = null;
        try {
            // Create a JMS Session for this thread.
            session = connection.createSession(false, configuration.getSessionAcknowledgement().getAcknowledge());

            // Publish the created message.
            MessageProducer producer = session.createProducer(jndiContextManager.getTopic());
            producer.setDeliveryMode(configuration.getDeliveryMode().getMode());

            TextMessage textMessage = session.createTextMessage(buildMessageText(auditTopic, auditEvent));
            producer.send(textMessage);

            // Return the auditEvent as the response.
            return newResourceResponse(
                    auditEvent.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                    null,
                    auditEvent).asPromise();

        } catch (Exception ex) {
            return adapt(ex).asPromise();
        } finally {
            if (null != session) {
                try {
                    session.close();
                } catch (JMSException e) {
                    logger.warn("Unable to close JMS session", e);
                }
            }
        }
    }

    private String buildMessageText(String auditTopic, JsonValue event) {
        try {
            return mapper.writeValueAsString(
                    object(
                            field("auditTopic", auditTopic),
                            field("event", event.getObject())
                    ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unable to convert auditEvent into jms message", e);
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
     * Returns the current connection if it is connected.
     *
     * @return the current connection, null if not connected.
     */
    Connection getConnection() {
        return connection;
    }
}
