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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.filter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonPointer;
import org.forgerock.util.Reject;
import org.forgerock.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for creating audit event filters. */
public final class Filters {
    private static final Logger logger = LoggerFactory.getLogger(Filters.class);

    /** The filter types. */
    protected enum FilterNames {
        /** Value event filter type. */
        VALUE,
        /** Field event filter type. */
        FIELD
    }

    /**
     * Create a new filter for values and fields.
     *
     * @param auditTopics The topics to filter.
     * @param policy The policy to apply.
     * @return The new filter.
     */
    public static ValueOrFieldFilter newValueOrFieldFilter(final List<String> auditTopics, final FilterPolicy policy) {
        return new ValueOrFieldFilter(exclusionListPerTopic(auditTopics, policy));
    }

    /**
     * Create a new filter for of the type specified.
     *
     * @param name The name of the filter type.
     * @param auditTopics The topics to filter.
     * @param policy The policy to apply.
     * @return The filter.
     * @throws AuditException If the type is unknown, or cannot be created.
     */
    public static ValueOrFieldFilter newFilter(final String name, final List<String> auditTopics,
            final FilterPolicy policy) throws AuditException {
        Reject.ifNull(name);
        FilterNames filterName = Utils.asEnum(name, FilterNames.class);
        switch (filterName) {
        case VALUE:
        case FIELD:
            return newValueOrFieldFilter(auditTopics, policy);
        default:
            final String error = String.format("Unknown filter policy name: %s", name);
            logger.error(error);
            throw new AuditException(error);
        }
    }

    private static Map<String, List<JsonPointer>> exclusionListPerTopic(final List<String> auditTopicsList,
            final FilterPolicy policy) {
        final Map<String, List<JsonPointer>> topicMap = initializeTopicMap(auditTopicsList);
        for (final String value : policy.getExcludeIf()) {
            addToTopicMap(value, topicMap);
        }
        return topicMap;
    }

    private static void addToTopicMap(final String value, final Map<String, List<JsonPointer>> topicMap) {
        final JsonPointer pointer = new JsonPointer(value);
        final String topic = pointer.get(0);
        final JsonPointer exclusionPointer =
                new JsonPointer(Arrays.copyOfRange(pointer.toArray(), 1, pointer.size()));
        final List<JsonPointer> topicPolicies = topicMap.get(topic);
        if (topicPolicies != null) {
            topicPolicies.add(exclusionPointer);
        } else {
            logger.error(String.format("Attempting to create a policy for an audit topic not registered: %s", value));
        }
    }

    private static Map<String, List<JsonPointer>> initializeTopicMap(final List<String> auditTopicsList) {
        Map<String, List<JsonPointer>> topicMap = new LinkedHashMap<>(auditTopicsList.size());
        for (final String auditTopic : auditTopicsList) {
            topicMap.put(auditTopic, new LinkedList<JsonPointer>());
        }
        return topicMap;
    }

    private Filters() {
        // utility class
    }

}
