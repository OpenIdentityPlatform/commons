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

/**
 * Class that represents the Resource type in API descriptor.
 */
public class Resource {
    private final Schema resourceSchema;
    private final String title;
    private final String description;
    private final Create create;
    private final Read read;
    private final Update update;
    private final Delete delete;
    private final Patch patch;
    private final Action[] actions;
    private final Query[] queries;
    private final String deprecatedSince;

    private Resource(Builder builder) {
        this.resourceSchema = builder.resourceSchema;
        this.title = builder.title;
        this.description = builder.description;
        this.create = builder.create;
        this.read = builder.read;
        this.update = builder.update;
        this.delete = builder.delete;
        this.patch = builder.patch;
        this.actions = builder.actions;
        this.queries = builder.queries;
        this.deprecatedSince = builder.deprecatedSince;
    }

    /**
     * Getter of resoruce schema
     * @return Resource schema
     */
    public Schema getResourceSchema() {
        return resourceSchema;
    }

    /**
     * Getter of title
     * @return Title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter of description
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of Create
     * @return Create
     */
    public Create getCreate() {
        return create;
    }

    /**
     * Getter of Read
     * @return Read
     */
    public Read getRead() {
        return read;
    }

    /**
     * Getter of Update
     * @return Update
     */
    public Update getUpdate() {
        return update;
    }

    /**
     * Getter of Delete
     * @return Delete
     */
    public Delete getDelete() {
        return delete;
    }

    /**
     * Getter of Patch
     * @return Patch
     */
    public Patch getPatch() {
        return patch;
    }

    /**
     * Getter of actions
     * @return Actions
     */
    public Action[] getActions() {
        return actions;
    }

    /**
     * Getter of queries
     * @return Queries
     */
    public Query[] getQueries() {
        return queries;
    }

    /**
     * Getter of deprecated since
     * @return Deprecated since
     */
    public String getDeprecatedSince() {
        return deprecatedSince;
    }

    /**
     * Create a new Builder for Resoruce
     * @return Builder
     */
    private static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Schema resourceSchema;
        private String title;
        private String description;
        private Create create;
        private Read read;
        private Update update;
        private Delete delete;
        private Patch patch;
        private Action[] actions;
        private Query[] queries;
        private String deprecatedSince;

        /**
         * Private default constructor
         */
        protected Builder() {}

        /**
         * Set the resource schema
         * @param resourceSchema
         * @return Builder
         */
        public Builder withResourceSchema(Schema resourceSchema) {
            this.resourceSchema = resourceSchema;
            return this;
        }

        /**
         * Set the title
         * @param title
         * @return Builder
         */
        public Builder withTitle(String title) {
            this.title = title;
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
         * Set create
         * @param create
         * @return Builder
         */
        public Builder withCreate(Create create) {
            this.create = create;
            return this;
        }

        /**
         * Set Read
         * @param read
         * @return Builder
         */
        public Builder withRead(Read read) {
            this.read = read;
            return this;
        }

        /**
         * Set Update
         * @param update
         * @return Builder
         */
        public Builder withUpdate(Update update) {
            this.update = update;
            return this;
        }

        /**
         * Set Delete
         * @param delete
         * @return Builder
         */
        public Builder withDelete(Delete delete) {
            this.delete = delete;
            return this;
        }

        /**
         * Set Patch
         * @param patch
         * @return Builder
         */
        public Builder withPatch(Patch patch) {
            this.patch = patch;
            return this;
        }

        /**
         * Set Actions
         * @param actions
         * @return Builder
         */
        public Builder withActions(Action[] actions) {
            this.actions = actions;
            return this;
        }

        /**
         * Set Queries
         * @param queries
         * @return Builder
         */
        public Builder withQueries(Query[] queries) {
            this.queries = queries;
            return this;
        }

        /**
         * Set deprecated since
         * @param deprecatedSince
         * @return Builder
         */
        public Builder withDeprecatedSince(String deprecatedSince) {
            this.deprecatedSince = deprecatedSince;
            return this;
        }

        /**
         * Construct a new instance of Resource
         * @return Resource instance
         */
        public Resource build() {
            return new Resource(this);
        }

    }
    
}
