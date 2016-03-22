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
 * Class that represents the Error type in API descriptor.
 *
 */
public class Error {

    private final String name;
    private final String description;
    private final Integer code;
    private final Schema schema;

    /**
     * Private constructor called by the builder build method
     * @param builder
     */
    private Error(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.code = builder.code;
        this.schema = builder.schema;
    }

    /**
     * Getter of the error name
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the error description
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the error code
     * @return - Code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * Getter of the error schema
     * @return Schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * New error builder with the mandatory parameters
     * @param name Error name
     * @param code Error code
     * @return Builder
     */
    public static Builder newBuilder(String name, Integer code) {
        return new Builder(name, code);
    }

    public static final class Builder {

        private String name;
        private String description;
        private Integer code;
        private Schema schema;

        /**
         * Error builder instance with the 2 mandatory parameters
         * @param name - Error name
         * @param code - Error code
         */
        private Builder(String name, Integer code) {
            this.name = name;
            this.code = code;
        }

        /**
         * Set the error description
         * @param description
         * @return Builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the schema
         * @param schema
         * @return Builder
         */
        public Builder withSchema(Schema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Builds the Error
         * @return Error instance
         */
        public Error build() {
            return new Error(this);
        }
    }
}
