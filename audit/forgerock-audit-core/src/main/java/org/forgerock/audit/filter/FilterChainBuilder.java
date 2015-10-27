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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.filter.Filters.FilterNames;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * A builder that builds a filter chain.
 */
public class FilterChainBuilder {
    private Map<String, FilterPolicy> policies;
    private List<String> auditTopics;

    /**
     * Adds the topics this filter chain is for.
     * @param auditTopics The topics.
     * @return This FilterChainBuilder.
     */
    public FilterChainBuilder withAuditTopics(final Collection<String> auditTopics) {
        Reject.ifNull(auditTopics);
        this.auditTopics = new LinkedList<>(auditTopics);
        return this;
    }

    /**
     * Adds the policies to chain together. The expected input would be a map with the key being
     * the {@link FilterNames}, and the value the {@link FilterPolicy} for that given filter.
     * @param policies The policies.
     * @return This FilterChainBuilder.
     */
    public FilterChainBuilder withPolicies(final Map<String, FilterPolicy> policies) {
        Reject.ifNull(policies);
        this.policies = new LinkedHashMap<>(policies);
        return this;
    }

    /**
     * Builds the FilterChain.
     * @return The FilterChain as a {@link Filter}.
     */
    public Filter build() {
        final List<Filter> filters = new LinkedList<>();
        // create Filters
        if (policies != null && auditTopics != null) {
            for (final Map.Entry<String, FilterPolicy> policyEntry : policies.entrySet()) {
                try {
                    filters.add(Filters.newFilter(policyEntry.getKey(), auditTopics, policyEntry.getValue()));
                } catch (AuditException e) {
                    // Do nothing. The exception has been logged.
                }
            }
        }
        return new FilterChain(filters);
    }

    /**
     * Chains together multiple filters and runs them all.
     */
    public static class FilterChain implements Filter {
        private final List<Filter> filters;

        /**
         * Creates a filter chain from a given list of filters.
         * @param filters The list of filters to chain.
         */
        FilterChain(List<Filter> filters) {
            this.filters = new LinkedList<>(filters);
        }

        /**
         * Runs the filters in the filter chain.
         * {@inheritDoc}
         */
        @Override
        public void doFilter(String auditTopic, JsonValue auditEvent) {
            for (final Filter filter: filters) {
                filter.doFilter(auditTopic, auditEvent);
            }
        }
    }
}
