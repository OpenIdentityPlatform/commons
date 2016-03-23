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
package com.forgerock.api.beans;

import com.forgerock.api.enums.PagingMode;
import com.forgerock.api.enums.QueryType;
import com.forgerock.api.enums.SupportedPagingModePolicy;

/**
 * Class that represents the Create Operation type in API descriptor.
 *
 */
public final class Query extends Operation {

    private final QueryType queryType;
    private final PagingMode pagingMode;
    private final SupportedPagingModePolicy[] supportedPagingModePolicies;
    private final String queryId;
    private final String[] queryableFields;
    private final String description;
    private final String[] supportedSortKeys;

    /**
     * Protected contstructor of the Query.
     *
     * @param builder Operation Builder
     */
    private Query(Builder builder) {
        super(builder);
        this.queryType = builder.queryType;
        this.pagingMode = builder.pagingMode;
        this.supportedPagingModePolicies = builder.supportedPagingModePolicies;
        this.queryId = builder.queryId;
        this.queryableFields = builder.queryableFields;
        this.description = builder.description;
        this.supportedSortKeys = builder.supportedSortKeys;
    }

    /**
     * Getter of the query type.
     * @return Query type num
     */
    public QueryType getQueryType() {
        return queryType;
    }

    /**
     * Getter of the paging mode.
     * @return Paging mode enum
     */
    public PagingMode getPagingMode() {
        return pagingMode;
    }

    /**
     * Getter of the supported paging policies.
     * @return Supported paging policy enums
     */
    public SupportedPagingModePolicy[] getSupportedPagingModePolicies() {
        return supportedPagingModePolicies;
    }

    /**
     * Getter of the query id.
     * @return Query id
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Getter of the queryable fields.
     * @return Queryable fields
     */
    public String[] getQueryableFields() {
        return queryableFields;
    }

    /**
     * Getter of the description.
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the supported sort keys.
     * @return Supported sort keys
     */
    public String[] getSupportedSortKeys() {
        return supportedSortKeys;
    }

    /**
     * Creates a new builder for Query.
     * @return New builder instance
     */
    public static final Builder query() {
        return new Builder();
    }

    /**
     * Allocates the Query operation type to the given Resource Builder.
     * @param resourceBuilder - Resource Builder to add the operation
     */
    @Override
    protected void allocateToResource(Resource.Builder resourceBuilder) {
        resourceBuilder.query(this);
    }

    /**
     * Builder to help construct the Read.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private QueryType queryType;
        private PagingMode pagingMode;
        private SupportedPagingModePolicy[] supportedPagingModePolicies;
        private String queryId;
        private String[] queryableFields;
        private String description;
        private String[] supportedSortKeys;

        /**
         * Returns the builder instance.
         * @return Builder
         */
        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Set the query type.
         * @param queryType query type enum
         * @return Builder
         */
        public Builder queryType(QueryType queryType) {
            this.queryType = queryType;
            return this;
        }

        /**
         * Set the paging mode.
         * @param pagingMode Query paging mode enum
         * @return Builder
         */
        public Builder pagingMode(PagingMode pagingMode) {
            this.pagingMode = pagingMode;
            return this;
        }

        /**
         * Set the supported paging mode policies.
         * @param supportedPagingModePolicies Array of supported paging mode policies
         * @return Builder
         */
        public Builder supportedPagingModePolicies(SupportedPagingModePolicy[] supportedPagingModePolicies) {
            this.supportedPagingModePolicies = supportedPagingModePolicies;
            return this;
        }

        /**
         * Set the query id. Required if “type” is ID.
         * @param queryId Query id
         * @return Builder
         */
        public Builder queryId(String queryId) {
            this.queryId = queryId;
            return this;
        }

        /**
         * Set the queryable fields.
         * @param queryableFields Array of the fileds that are queryable
         * @return Builder
         */
        public Builder queryableFields(String[] queryableFields) {
            this.queryableFields = queryableFields;
            return this;
        }

        /**
         * Set the description.
         * @param description Read operation description
         * @return Builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the supported sort keys.
         * @param supportedSortKeys Array of supported sort keys
         * @return Builder
         */
        public Builder supportedSortKeys(String[] supportedSortKeys) {
            this.supportedSortKeys = supportedSortKeys;
            return this;
        }

        /**
         * Builds the Query instace.
         *
         * @return Query instace
         */
        public Query build() {
            return new Query(this);
        }
    }

}
