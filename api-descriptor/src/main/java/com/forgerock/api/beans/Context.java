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

/**
 * Class that represents the Context type in API descriptor
 *
 */
public class Context {

    private String name;
    private Schema schema;
    private Boolean required;

    /**
     *
     * @param builder
     */
    private Context(Builder builder) {
        this.name = builder.name;
        this.schema = builder.schema;
        this.required = builder.required;
    }

    /**
     * Getter of the context name
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the context schema
     * @return Schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Getter of the required parameter
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    private static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private Schema schema;
        private Boolean required;

        /**
         * Private default constructor
         */
        protected Builder() {}

        /**
         * Set the name
         * @param name
         * @return Builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the Schema
         * @param schema
         * @return Builder
         */
        public Builder withSchema(Schema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Set if required or not
         * @param required
         * @return Builder
         */
        public Builder withRequired(Boolean required) {
            Reject.ifNull(required);
            this.required = required;
            return this;
        }

        public Context build() {
            return new Context(this);
        }

    }

}
