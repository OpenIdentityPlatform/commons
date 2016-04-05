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

package com.forgerock.api.models;

import static com.forgerock.api.util.ValidationUtil.isEmpty;

import com.forgerock.api.ApiValidationException;

/**
 * Class that represents the Error type in API descriptor.
 */
public final class Error {

    // Must be an Integer, because 0 is not a valid default
    private final Integer code;
    private final String description;
    private final Schema schema;

    private Error(Builder builder) {
        this.code = builder.code;
        this.description = builder.description;
        this.schema = builder.schema;

        if (code == null || isEmpty(description)) {
            throw new ApiValidationException("code and description are required");
        }
    }

    /**
     * Getter of the error code.
     *
     * @return Code
     */
    public int getCode() {
        return code;
    }

    /**
     * Getter of the error description.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the error schema.
     *
     * @return Schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * New error builder.
     *
     * @return Builder
     */
    public static Builder error() {
        return new Builder();
    }

    /**
     * Builds an Error object from the data in the annotation.
     * @param error The annotation that holds the data
     * @return Error instance
     */
    public static Error fromAnnotation(com.forgerock.api.annotations.Error error) {
        return error()
                .description(error.description())
                .code(error.code())
                .schema(Schema.fromAnnotation(error.detailSchema()))
                .build();
    }

    /**
     * Builder for the Error.
     */
    public static final class Builder {

        // Must be an Integer, because 0 is not a valid default.
        private Integer code;
        private String description;
        private Schema schema;

        private Builder() {
        }

        /**
         * Set the error code.
         *
         * @param code Error code
         * @return Builder
         */
        public Builder code(Integer code) {
            this.code = code;
            return this;
        }

        /**
         * Set the error description.
         *
         * @param description Error description
         * @return Builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the schema.
         *
         * @param schema Error schema
         * @return Builder
         */
        public Builder schema(Schema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Builds the Error.
         *
         * @return Error instance
         */
        public Error build() {
            return new Error(this);
        }
    }
}
