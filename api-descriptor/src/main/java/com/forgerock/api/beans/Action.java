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
 * Class that represents the Action operation type in API descriptor
 *
 */
public class Action extends Operation{

    private final String id;
    private final String description;
    private final Schema request;
    private final Schema response;

    /**
     * Protected contstructor of the Action
     *
     * @param builder Action Builder
     */
    private Action(Builder builder) {
        super(builder);
        this.id = builder.id;
        this.description = builder.description;
        this.request = builder.request;
        this.response = builder.response;
    }

    /**
     * Getter of the ID
     * @return Id
     */
    public String getId() {
        return id;
    }

    /**
     * Getter of the description
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the request
     * @return Request
     */
    public Schema getRequest() {
        return request;
    }

    /**
     * Getter of the response
     * @return Response
     */
    public Schema getResponse() {
        return response;
    }

    /**
     * Creates a new builder for Action
     * @return New builder instance
     */
    public static final Builder newBuilder() {
        return new Builder();
    }

    private static final class Builder extends Operation.Builder<Builder> {

        private String id;
        private String description;
        private Schema request;
        private Schema response;

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Set the Id
         * @param id
         * @return Builder
         */
        public Builder withId(String id) {
            this.id = id;
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
         * Set the request
         * @param request
         * @return Builder
         */
        public Builder withRequest(Schema request) {
            this.request = request;
            return this;
        }

        /**
         * Set the response
         * @param response
         * @return Builder
         */
        public Builder withResponse(Schema response) {
            this.response = response;
            return this;
        }

        /**
         * Builds the Action instace
         *
         * @return Action instace
         */
        public Action build() {
            return new Action(this);
        }
    }

}
