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
package org.forgerock.audit;

import static java.util.Collections.emptyList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.audit.filter.FilterPolicy;

/**
 * Configuration of the audit service.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <pre>
 *   {
 *     "handlerForQueries" : "csv",
 *     "availableAuditEventHandlers" : [
 *          "org.forgerock.audit.events.handler.MyHandler",
 *          "org.forgerock.audit.events.handler.AnotherHandler"
 *     ],
 *     "filterPolicies" : {
 *         "field" : {
 *             "excludeIf" : [],
 *             "includeIf" : [
 *                  "/access/filter/field"
 *             ]
 *         },
 *         "value" : {
 *             "excludeIf" : [],
 *             "includeIf" : [
 *                  "/access/filter/value"
 *             ]
 *         }
 *     }
 *   }
 * </pre>
 */
public class AuditServiceConfiguration {

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.service.handlerForQueries")
    private String handlerForQueries;

    @JsonPropertyDescription("audit.service.availableAuditEventHandlers")
    private List<String> availableAuditEventHandlers;

    @JsonPropertyDescription("audit.service.filter.policies")
    private Map<String, FilterPolicy> filterPolicies = new LinkedHashMap<>();

    /**
     * Empty constructor.
     */
    public AuditServiceConfiguration() {
        // empty constructor
    }

    /**
     * Copy-constructor, in order to obtain a copy from an existing configuration.
     *
     * @param config an existing configuration
     */
    public AuditServiceConfiguration(AuditServiceConfiguration config) {
        handlerForQueries = config.getHandlerForQueries();
        availableAuditEventHandlers = config.availableAuditEventHandlers;
    }

    /**
     * Returns the name of the handler to use for querying the audit events.
     *
     * @return the name of the handler.
     */
    public String getHandlerForQueries() {
        return handlerForQueries;
    }

    /**
     * Sets the name of the handler to use for querying the audit events.
     *
     * @param name
     *            the name of the handler.
     */
    public void setHandlerForQueries(String name) {
        handlerForQueries = name;
    }

    /**
     * Returns a list of class names of available audit event handlers.
     *
     * @return the list of available audit event handlers.
     */
    public List<String> getAvailableAuditEventHandlers() {
        if (availableAuditEventHandlers == null) {
            return emptyList();
        } else {
            return availableAuditEventHandlers;
        }
    }

    /**
     * Sets the list of available audit event handlers.
     *
     * @param availableAuditEventHandlers the list of available audit event handlers.
     */
    public void setAvailableAuditEventHandlers(List<String> availableAuditEventHandlers) {
        this.availableAuditEventHandlers = availableAuditEventHandlers;
    }

    /**
     * Get the filter policy mappings.
     * @return The policies.
     */
    public Map<String, FilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /**
     * Set the filter policy mappings.
     * @param filterPolicies The policies.
     */
    public void setFilterPolicies(Map<String, FilterPolicy> filterPolicies) {
        this.filterPolicies.putAll(filterPolicies);
    }
}
