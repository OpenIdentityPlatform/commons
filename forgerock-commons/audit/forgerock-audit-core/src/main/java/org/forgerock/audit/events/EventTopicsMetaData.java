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
package org.forgerock.audit.events;

import org.forgerock.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates meta-data for event topics.
 */
public final class EventTopicsMetaData {

    private static final Logger logger = LoggerFactory.getLogger(EventTopicsMetaData.class);
    private final Map<String, JsonValue> eventTopicsMetaData;

    /**
     * Create a new EventTopicsMetaData.
     *
     * @param eventTopicsMetaData
     *          Event topic schemas mapped by event topic name.
     */
    public EventTopicsMetaData(Map<String, JsonValue> eventTopicsMetaData) {
        this.eventTopicsMetaData = eventTopicsMetaData;
    }

    /**
     * Returns <tt>true</tt> if this object has meta-data for the specified topic.
     *
     * @param topic
     *          The name of the topic to check.
     * @return <tt>true</tt> if this object has meta-data for the specified topic; <tt>false</tt> otherwise.
     */
    public boolean containsTopic(String topic) {
        return eventTopicsMetaData.containsKey(topic);
    }

    /**
     * Returns the JSON schema for the requested topic if this object has meta-data for that topic.
     * Otherwise, null is returned.
     *
     * @param topic
     *          The name of the topic to check.
     * @return JSON schema if this object has meta-data for the specified topic; <tt>null</tt> otherwise.
     */
    public JsonValue getSchema(String topic) {
        return eventTopicsMetaData.get(topic);
    }

    /**
     * Returns the names of the set of topics for which this object has meta-data.
     *
     * @return set of topic names.
     */
    public Set<String> getTopics() {
        return eventTopicsMetaData.keySet();
    }

    /**
     * Returns a new instance of <tt>EventTopicsMetaData</tt> containing only the meta-data for topics
     * held by this object that are named within provided <tt>topics</tt> parameter.
     * <p/>
     * Any entries within <tt>topics</tt> that are not known to this object will not be included in the resulting
     * <tt>EventTopicsMetaData</tt> object.
     *
     * @param topics
     *          The names of topics whose meta-data should be included.
     * @return a new instance of <tt>EventTopicsMetaData</tt>.
     */
    public EventTopicsMetaData filter(Set<String> topics) {
        Map<String, JsonValue> filteredTopicSchemas = new HashMap<>();
        for (String topic : topics) {
            if (!containsTopic(topic)) {
                logger.error("unknown audit event topic : {}", topic);
                continue;
            }
            filteredTopicSchemas.put(topic, getSchema(topic));
        }
        return new EventTopicsMetaData(filteredTopicSchemas);
    }
}
