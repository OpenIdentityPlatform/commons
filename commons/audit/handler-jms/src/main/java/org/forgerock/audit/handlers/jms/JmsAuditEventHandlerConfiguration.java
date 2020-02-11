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

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.util.Reject;

/**
 * Configuration object for the {@link JmsAuditEventHandler}.
 * <p/>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <p/>
 * <pre>
 * {
 *     "name" : "jms",
 *     "topics": [ "access", "activity", "config", "authentication" ],
 *     "deliveryMode" : "NON_PERSISTENT",
 *     "sessionMode" : "AUTO",
 *     "jndi" :  {
 *          "contextProperties" : {
 *              "initialContextFactory" : "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
 *              "providerUrl" : "tcp://localhost:61616"
 *          },
 *          "topicName" : "audit",
 *          "connectionFactoryName" : "connectionFactory"
 *     }
 * }
 * </pre>
 */
public class JmsAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.deliveryMode")
    private DeliveryModeConfig deliveryMode;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.sessionMode")
    private SessionModeConfig sessionMode;

    @JsonPropertyDescription("audit.handlers.jms.batch")
    private BatchPublisherConfiguration batch = new BatchPublisherConfiguration();

    @JsonPropertyDescription("audit.handlers.jms.jndi")
    private JndiConfiguration jndi = new JndiConfiguration();

    /**
     * Returns the delivery mode configuration that should be used when publishing the JMS messages.
     *
     * @return the delivery mode.
     */
    public DeliveryModeConfig getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * Sets the delivery mode configuration that should be used when publishing the JMS messages.
     *
     * @param deliveryMode the delivery mode
     */
    public void setDeliveryMode(DeliveryModeConfig deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    /**
     * Returns the mode that the JMS session should use when publishing the JMS messages.
     *
     * @return the session's mode.
     * @see Session#getAcknowledgeMode()
     */
    public SessionModeConfig getSessionMode() {
        return sessionMode;
    }

    /**
     * Sets the session mode that the JMS session should use when publishing the JMS messages.
     *
     * @param sessionMode the session's acknowledgement mode.
     * @see Session#getAcknowledgeMode()
     */
    public void setSessionMode(SessionModeConfig sessionMode) {
        this.sessionMode = sessionMode;
    }

    /**
     * Returns the configuration used to initialize the batch publisher.
     *
     * @return the configuration used to initialize the batch publisher.
     */
    public BatchPublisherConfiguration getBatch() {
        return batch;
    }

    /**
     * Sets the configuration used to initialize the batch publisher.
     *
     * @param batch the configuration used to initialize the batch publisher.
     */
    public void setBatch(BatchPublisherConfiguration batch) {
        this.batch = batch;
    }

    /**
     * Gets the {@link JndiConfiguration}.
     * @return The {@link JndiConfiguration}.
     */
    public JndiConfiguration getJndi() {
        return jndi;
    }

    /**
     * Sets the {@link JndiConfiguration}.
     * @param jndi The {@link JndiConfiguration}
     */
    public void setJndi(JndiConfiguration jndi) {
        this.jndi = jndi;
    }

    @Override
    public boolean isUsableForQueries() {
        return false;
    }


    /**
     * Stores the JNDI context properties and lookup names.
     */
    public static class JndiConfiguration {

        @JsonPropertyDescription("audit.handlers.jms.contextProperties")
        private Map<String, String> contextProperties = Collections.emptyMap();

        @JsonPropertyDescription("audit.handlers.jms.topicName")
        private String topicName = "audit";

        @JsonPropertyDescription("audit.handlers.jms.connectionFactoryName")
        private String connectionFactoryName = "connectionFactory";

        /**
         * Gets the Jndi {@link InitialContext} properties.
         * @return The {@link InitialContext} properties.
         */
        public Map<String, String> getContextProperties() {
            return contextProperties;
        }

        /**
         * Sets the Jndi {@link InitialContext} properties.
         * @param contextProperties The {@link InitialContext} properties.
         */
        public void setContextProperties(Map<String, String> contextProperties) {
            Reject.ifNull(contextProperties, "The jndi context properties can't be null");
            this.contextProperties = Collections.unmodifiableMap(contextProperties);
        }

        /**
         * Returns the jndi lookup name for the JMS {@link Topic} to which messages will be published.
         * Do not confuse this with Audit Topics.
         * @see InitialContext#lookup(String)
         *
         * @return The jndi lookup name for the JMS {@link Topic} to which messages will be published.
         */
        public String getTopicName() {
            return topicName;
        }

        /**
         * Sets the jndi lookup name for the JMS {@link Topic} for which the messages will be published on.
         * @see InitialContext#lookup(String)
         *
         * @param jmsTopicName The jndi lookup name for the JMS {@link Topic}.
         */
        public void setJmsTopicName(String jmsTopicName) {
            this.topicName = jmsTopicName;
        }

        /**
         * Returns the jndi lookup name for the JMS {@link ConnectionFactory} to which messages will be published.
         * Do not confuse this with Audit Topics.
         * @see InitialContext#lookup(String)
         *
         * @return The jndi lookup name for the JMS {@link ConnectionFactory} to which messages will be published.
         */
        public String getConnectionFactoryName() {
            return connectionFactoryName;
        }

        /**
         * Sets the jndi lookup name for the JMS {@link ConnectionFactory} for which the messages will be published on.
         * @see InitialContext#lookup(String)
         *
         * @param jmsConnectionFactoryName The jndi lookup name for the JMS {@link ConnectionFactory}.
         */
        public void setJmsConnectionFactoryName(String jmsConnectionFactoryName) {
            this.connectionFactoryName = jmsConnectionFactoryName;
        }
    }
}
