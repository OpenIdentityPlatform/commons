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

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Stores the state of the details sent to {@link AuditEventHandler#publishEvent(Context, String, JsonValue)}.
 * The state contains the context, topic, and event content.
 */
public final class AuditEventTopicState {

    private final Context context;
    private final String topic;
    private final JsonValue event;

    /**
     * Creates a (topic,event) pair.
     *
     * @param context
     *         The context that triggered the audit event.
     * @param topic
     *         The topic.
     * @param event
     *         The event content.
     */
    public AuditEventTopicState(Context context, String topic, JsonValue event) {
        Reject.ifNull(topic, event);
        this.context = context;
        this.topic = topic;
        this.event = event;
    }

    /**
     * Returns the topic of the event.
     *
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Returns the event content.
     *
     * @return the event
     */
    public JsonValue getEvent() {
        return event;
    }

    /**
     * Returns the context that triggered the event.
     *
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + topic.hashCode();
        result = prime * result + event.asMap().hashCode();
        result = prime * result + context.toJsonValue().asMap().hashCode();
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AuditEventTopicState) {
            AuditEventTopicState other = (AuditEventTopicState) obj;
            if (topic.equals(other.topic)
                    && event.asMap().equals(other.event.asMap())
                    && context.toJsonValue().asMap().equals(other.context.toJsonValue().asMap())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("AuditEventTopicState [topic=%s, event=%s, contextId=%s]", topic, event, context.getId());
    }
}
