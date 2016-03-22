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

import com.forgerock.api.enums.PagingModeEnum;
import com.forgerock.api.enums.QueryTypeEnum;
import com.forgerock.api.enums.SupportedPagingModePolicyEnum;

/**
 * Class that represents the Create Operation type in API descriptor.
 *
 */
public class Query extends Operation{

    private final QueryTypeEnum queryType;
    private final PagingModeEnum pagingMode;
    private final SupportedPagingModePolicyEnum[] supportedPagingModePolicies;
    private final String queryId;
    private final String[] queryableFields;
    private final String description;
    private final String[] supportedSortKeys;

    /**
     * Protected contstructor of the Query
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
     * Getter of the query type
     * @return Query type num
     */
    public QueryTypeEnum getQueryType() {
        return queryType;
    }

    /**
     * Getter of the paging mode
     * @return Paging mode enum
     */
    public PagingModeEnum getPagingMode() {
        return pagingMode;
    }

    /**
     * Getter of the supported paging policies
     * @return Supported paging policy enums
     */
    public SupportedPagingModePolicyEnum[] getSupportedPagingModePolicies() {
        return supportedPagingModePolicies;
    }

    /**
     * Getter of the query id
     * @return Query id
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Getter of the queryable fields
     * @return Queryable fields
     */
    public String[] getQueryableFields() {
        return queryableFields;
    }

    /**
     * Getter of the description
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the supported sort keys
     * @return Supported sort keys
     */
    public String[] getSupportedSortKeys() {
        return supportedSortKeys;
    }

    /**
     * Creates a new builder for Query
     * @return New builder instance
     */
    public static final Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder extends Operation.Builder<Builder> {

        private QueryTypeEnum queryType;
        private PagingModeEnum pagingMode;
        private SupportedPagingModePolicyEnum[] supportedPagingModePolicies;
        private String queryId;
        private String[] queryableFields;
        private String description;
        private String[] supportedSortKeys;

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Set the query type
         * @param queryType
         * @return Builder
         */
        public Builder withQueryType(QueryTypeEnum queryType) {
            this.queryType = queryType;
            return this;
        }

        /**
         * Set the paging mode
         * @param pagingMode
         * @return Builder
         */
        public Builder withPagingMode(PagingModeEnum pagingMode) {
            this.pagingMode = pagingMode;
            return this;
        }

        /**
         * Set the supported paging mode policies
         * @param supportedPagingModePolicies
         * @return Builder
         */
        public Builder withSupportedPagingModePolicies(SupportedPagingModePolicyEnum[] supportedPagingModePolicies) {
            this.supportedPagingModePolicies = supportedPagingModePolicies;
            return this;
        }

        /**
         * Set the query id
         * @param queryId
         * @return Builder
         */
        public Builder withQueryId(String queryId) {
            this.queryId = queryId;
            return this;
        }

        /**
         * Set the queryable fields
         * @param queryableFields
         * @return Builder
         */
        public Builder withQueryableFields(String[] queryableFields) {
            this.queryableFields = queryableFields;
            return this;
        }

        /**
         * Set the description
         * @param description
         * @return Builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the supported sort keys
         * @param supportedSortKeys
         * @return Builder
         */
        public Builder withSupportedSortKeys(String[] supportedSortKeys) {
            this.supportedSortKeys = supportedSortKeys;
            return this;
        }

        /**
         * Builds the Query instace
         *
         * @return Query instace
         */
        public Query build() {
            return new Query(this);
        }
    }

}
