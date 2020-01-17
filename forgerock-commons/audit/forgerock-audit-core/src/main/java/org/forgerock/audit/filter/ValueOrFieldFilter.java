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
package org.forgerock.audit.filter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * A {@link Filter} implementation that filters values and fields from the audit event.
 */
class ValueOrFieldFilter implements Filter {
    private final Map<String, List<JsonPointer>> exclusions;

    /**
     * Builds a ValueOrFieldFilter given a list of exclusion fields per topic.
     * @param exclusions A map of exclusion pointers per topic.
     */
    public ValueOrFieldFilter(final Map<String, List<JsonPointer>> exclusions) {
        Reject.ifNull(exclusions);
        this.exclusions = new LinkedHashMap<>(exclusions);
    }

    /**
     * Excludes various values/fields from the given audit event.
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final String auditTopic, final JsonValue auditEvent) {
        final List<JsonPointer> exclusionList = exclusions.get(auditTopic);
        if (exclusionList == null || exclusionList.isEmpty()) {
            return;
        }
        for (final JsonPointer exclusionPointer : exclusionList) {
            auditEvent.remove(exclusionPointer);
        }
    }
}
