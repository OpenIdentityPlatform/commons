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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.audit.events.handlers;

import java.util.List;
import java.util.Map;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract AuditEventHandler class.
 *
 * @param <CFG>
 *            type of the configuration
 */
public abstract class AuditEventHandlerBase<CFG extends EventHandlerConfiguration>
    implements AuditEventHandler<CFG> {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventHandlerBase.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAuditEventsMetaData(final Map<String, JsonValue> auditEvents) {
        // do nothing by default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencyProvider(DependencyProvider dependencyProvider) {
        // do nothing by default
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation publishes each event in order.
     * Implementing classes should override this method to optimize the publication.
     */
    @Override
    public synchronized void publishEvents(List<AuditEventTopicState> events) {
        try {
            for (AuditEventTopicState event : events) {
                publishEvent(event.getContext(), event.getTopic(), event.getEvent());
            }
        } catch (Exception e) {
            logger.error(
                    String.format("Could not process buffered events. Size of events: %d, first event: %s",
                            events.size(), events.get(0)), e);
        }
    }

}
