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

import static javax.naming.Context.*;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;

/**
 * Manages the JNDI lookup and context needs for JMS services.
 */
class JndiContextManager {

    public static final String TOPIC_JNDI_CONFIG_PREFIX = "topic.";
    private final Topic topic;
    private final ConnectionFactory connectionFactory;

    /**
     * Given the configuration, this builds an JMS InitialContext.
     *
     * @param configuration The Audit Configuration.
     * @throws InternalServerErrorException
     */
    public JndiContextManager(JmsAuditEventHandlerConfiguration configuration) throws ResourceException {
        Properties props = new Properties();
        props.setProperty(INITIAL_CONTEXT_FACTORY, configuration.getInitialContextFactory());
        props.setProperty(PROVIDER_URL, configuration.getProviderUrl());
        props.setProperty(TOPIC_JNDI_CONFIG_PREFIX + configuration.getJmsTopic(), configuration.getJmsTopic());

        try {
            InitialContext context = new InitialContext(props);
            topic = (Topic) context.lookup(configuration.getJmsTopic());
            connectionFactory = (ConnectionFactory) context.lookup("ConnectionFactory");
        } catch (NamingException e) {
            throw new InternalServerErrorException(
                    "Encountered issue building inial JMS context for JMS Audit Handler", e);
        }
    }

    /**
     * Returns the topic to use for JMS publish/subscribe functionality.
     *
     * @return The topic to use for JMS publish/subscribe functionality.
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * Returns the connection factory to use to connect to JMS services.
     * @return the connection factory to use to connect to JMS services.
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
