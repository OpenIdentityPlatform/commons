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
 * Class that represents the Context type in API descriptor.
 *
 */
public final class Context {

    private String name;
    private Schema schema;
    private boolean required;

    private Context(Builder builder) {
        this.name = builder.name;
        this.schema = builder.schema;
        this.required = builder.required;
    }

    /**
     * Getter of the context name.
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the context schema.
     * @return Schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Getter of the required parameter.
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Creates a new Builder instance for building the Context.
     * @return New Builder instance
     */
    public static Builder context() {
        return new Builder();
    }

    /**
     * Context Builder.
     */
    public static final class Builder {
        private String name;
        private Schema schema;
        private boolean required;

        /**
         * Private default constructor.
         */
        protected Builder() { }

        /**
         * Set the name.
         * @param name Context name
         * @return Builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the Schema.
         * @param schema Context schema
         * @return Builder
         */
        public Builder schema(Schema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Set if required or not.
         * @param required true if required
         * @return Builder
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Creates a new Context instance.
         * @return Context instance
         */
        public Context build() {
            return new Context(this);
        }

    }

}
