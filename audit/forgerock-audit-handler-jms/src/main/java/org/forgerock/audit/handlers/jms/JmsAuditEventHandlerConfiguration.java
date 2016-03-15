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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

/**
 * Configuration object for the {@link JmsAuditEventHandler}.
 * <p/>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <p/>
 * <pre>
 * {
 *     "name" : "jms",
 *     "topics": [ "access", "activity", "config", "authentication" ],
 *     "providerUrl" : "tcp://localhost:61616",
 *     "jmsTopic" : "audit",
 *     "initialContextFactory" : "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
 *     "deliveryMode" : "NON_PERSISTENT",
 *     "sessionAcknowledgement" : "AUTO"
 * }
 * </pre>
 */
public class JmsAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.providerUrl")
    private String providerUrl;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.jmsTopic")
    private String jmsTopic;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.initialContextFactory")
    private String initialContextFactory;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.deliveryMode")
    private DeliveryModeConfig deliveryMode;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.jms.sessionAcknowledgement")
    private SessionAcknowledgementConfig sessionAcknowledgement;

    /**
     * Returns the JMS provider connection url utilized to connect to the JMS broker.
     *
     * @return the JMS provider connection url utilized to connect to the JMS broker.
     */
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * Sets the JMS provider connection url utilized to connect to the JMS broker.
     *
     * @param providerUrl the connection url
     */
    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    /**
     * Returns the JMS topic to which messages will be published.  Do not confuse this with Audit Topics.
     *
     * @return The JMS topic to which messages will be published.
     */
    public String getJmsTopic() {
        return jmsTopic;
    }

    /**
     * Sets the JMS topic for which the messages will be published on.
     *
     * @param jmsTopic the JMS topic
     */
    public void setJmsTopic(String jmsTopic) {
        this.jmsTopic = jmsTopic;
    }

    /**
     * Returns the classname that implements the JMS connection Factory.
     *
     * @return the classname that implements the JMS connection Factory.
     */
    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    /**
     * Sets the classname that implements the JMS Connection Factory.
     *
     * @param initialContextFactory the JMS Connection Factory classname
     */
    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }

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
     * Returns the acknowledgement mode that the JMS session should use when publishing the JMS messages.
     *
     * @return the session's acknowledgement mode.
     */
    public SessionAcknowledgementConfig getSessionAcknowledgement() {
        return sessionAcknowledgement;
    }

    /**
     * Sets  the acknowledgement mode that the JMS session should use when publishing the JMS messages.
     *
     * @param sessionAcknowledgement the session's acknowledgement mode.
     */
    public void setSessionAcknowledgement(SessionAcknowledgementConfig sessionAcknowledgement) {
        this.sessionAcknowledgement = sessionAcknowledgement;
    }
}
