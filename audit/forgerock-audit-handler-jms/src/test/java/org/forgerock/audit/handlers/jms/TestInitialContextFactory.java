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

import static org.forgerock.audit.handlers.jms.JmsAuditEventHandlerTest.getAuditConfig;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

import org.forgerock.json.JsonPointer;

/**
 * This ContextFactory is instantiated by the JmsAuditEventHandlerTest through the JNDI JMS config it uses.
 */
public class TestInitialContextFactory implements InitialContextFactory {

    private static final JsonPointer TOPIC_CONFIG_POINTER = new JsonPointer("/config/jmsTopic");

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        try {
            String testTopic = getAuditConfig().get(TOPIC_CONFIG_POINTER).asString();

            Context context = mock(Context.class);
            when(context.lookup(eq(JndiContextManager.TOPIC_JNDI_CONFIG_PREFIX + testTopic)))
                    .thenReturn(mock(Topic.class));

            ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
            when(context.lookup(eq("ConnectionFactory"))).thenReturn(connectionFactory);

            TopicConnection testConnection = mock(TopicConnection.class);
            when(connectionFactory.createConnection()).thenReturn(testConnection);
            when(connectionFactory.createConnection(any(String.class), any(String.class))).thenReturn(testConnection);

            return context;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}