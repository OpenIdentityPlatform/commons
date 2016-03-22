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

import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.List;

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
        this.actions = builder.actions.toArray(new Action[builder.actions.size()]);
        this.queries = builder.queries.toArray(new Query[builder.queries.size()]);
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
    private static Builder resource() {
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
        private List<Action> actions;
        private List<Query> queries;
        private String deprecatedSince;

        /**
         * Private default constructor
         */
        protected Builder() {
            actions = new ArrayList<Action>();
            queries = new ArrayList<Query>();
        }

        /**
         * Set the resource schema
         * @param resourceSchema
         * @return Builder
         */
        public Builder resourceSchema(Schema resourceSchema) {
            this.resourceSchema = resourceSchema;
            return this;
        }

        /**
         * Set the title
         * @param title
         * @return Builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the description
         * @param description
         * @return Builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set create
         * @param create
         * @return Builder
         */
        public Builder create(Create create) {
            this.create = create;
            return this;
        }

        /**
         * Set Read
         * @param read
         * @return Builder
         */
        public Builder read(Read read) {
            this.read = read;
            return this;
        }

        /**
         * Set Update
         * @param update
         * @return Builder
         */
        public Builder update(Update update) {
            this.update = update;
            return this;
        }

        /**
         * Set Delete
         * @param delete
         * @return Builder
         */
        public Builder delete(Delete delete) {
            this.delete = delete;
            return this;
        }

        /**
         * Set Patch
         * @param patch
         * @return Builder
         */
        public Builder patch(Patch patch) {
            this.patch = patch;
            return this;
        }

        /**
         * Set Actions
         * @param actions
         * @return Builder
         */
        public Builder actions(List<Action> actions) {
            this.actions = actions;
            return this;
        }

        /**
         * Adds one Action to the list of Actions.
         * @param action Action to be added to the list
         * @return Builder
         */
        public Builder action(Action action) {
            this.actions.add(action);
            return this;
        }

        /**
         * Set Queries
         * @param queries
         * @return Builder
         */
        public Builder queries(List<Query> queries) {
            this.queries = queries;
            return this;
        }

        /**
         * Adds one Query to the list of queries.
         * @param query Query to be added to the list
         * @return
         */
        public Builder query(Query query) {
            this.queries.add(query);
            return this;
        }

        /**
         * Set deprecated since
         * @param deprecatedSince
         * @return Builder
         */
        public Builder deprecatedSince(String deprecatedSince) {
            this.deprecatedSince = deprecatedSince;
            return this;
        }

        /**
         * Allocates the operations given in the parameter by their type
         * @param operations One or more Operations
         * @return Builder
         */
        public Builder operations(Operation... operations) {
            Reject.ifNull(operations);
            for (Operation operation : operations){
                operation.allocateToResource(this);
            }
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
