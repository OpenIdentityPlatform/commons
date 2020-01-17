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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.json.JsonPointer;

/**
 * Represents a FilterPolicy which contains the includeIf and excludeIf values for the filter. The includeIf property
 * lists fields/values in {@link JsonPointer} syntax to include for the audit event. By default all audit event fields
 * are included. The excludeIf property lists fields/values in {@link JsonPointer} syntax to exclude for the audit
 * event.
 *
 * The listed fields/values should be prefixed with the topic the {@link JsonPointer} applies to. For example, the
 * following excludeIf value:
 * <pre>
 *     "/access/exclude/field"
 * </pre>
 *
 * would exclude the field "/exclude/field" for the access audit topic.
 *
 * The following is an example FilterPolicy in json format.
 * <pre>
 *     {
 *         "excludeIf" : [
 *              "/access/exclude/field"
 *         ],
 *         "includeIf" : [
 *              "/access/include/field"
 *         ]
 *     }
 * </pre>
 */
public class FilterPolicy {
    @JsonPropertyDescription("audit.events.filter.policies.include")
    private List<String> includeIf;

    @JsonPropertyDescription("audit.events.filter.policies.exclude")
    private List<String> excludeIf;

    /**
     * Gets the includeIf list. The includeIf is a list of values to include in the audit event.
     * @return The list of includeIfs.
     */
    public List<String> getIncludeIf() {
        return includeIf == null ? Collections.<String>emptyList() : includeIf;
    }

    /**
     * Sets the includeIf list. The includeIf is a list of values to include in the audit event.
     * @param includeIf The list of includeIfs.
     */
    public void setIncludeIf(Collection<String> includeIf) {
        this.includeIf = new LinkedList<>(includeIf);
    }

    /**
     * Gets the excludeIf list. The excludeIf is a list of values to exclude from the audit event.
     * @return The list of excludeIfs.
     */
    public List<String> getExcludeIf() {
        return excludeIf == null ? Collections.<String>emptyList() : excludeIf;
    }

    /**
     * Sets the excludeIf list. The excludeIf is a list of values to exclude from the audit event.
     * @param excludeIf The list of excludeIfs.
     */
    public void setExcludeIf(Collection<String> excludeIf) {
        this.excludeIf = new LinkedList<>(excludeIf);
    }
}
