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

package org.forgerock.api.models;

import static org.forgerock.api.util.ValidationUtil.*;

import java.util.Comparator;

import org.forgerock.guava.common.base.Strings;

import org.forgerock.api.ApiValidationException;

/**
 * Class that represents the Error type in API descriptor.
 */
public final class Error {

    /**
     * {@link Error} {@link Comparator}, which sorts by code and description.
     */
    public static final ErrorComparator ERROR_COMPARATOR = new ErrorComparator();

    // Must be an Integer, because 0 is not a valid default
    private final Integer code;
    private final String description;
    private final Schema schema;
    private final Reference reference;

    private Error(Builder builder) {
        this.code = builder.code;
        this.description = builder.description;
        this.schema = builder.schema;
        this.reference = builder.reference;

        if (reference == null && (code == null || isEmpty(description))) {
            throw new ApiValidationException("code and description are required");
        }
        if (reference != null && (code != null || description != null || schema != null)) {
            throw new ApiValidationException("Cannot set code, description or schema when using a reference");
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
     * Getter of the reference.
     *
     * @return The reference.
     */
    public Reference getReference() {
        return reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Error error = (Error) o;

        if (code != null ? !code.equals(error.code) : error.code != null) {
            return false;
        }
        if (description != null ? !description.equals(error.description) : error.description != null) {
            return false;
        }
        if (schema != null ? !schema.equals(error.schema) : error.schema != null) {
            return false;
        }
        return reference != null ? reference.equals(error.reference) : error.reference == null;

    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (reference != null ? reference.hashCode() : 0);
        return result;
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
     * Builds an Error object from the data in the annotation. If the {@code error} has an {@code id} defined, the
     * error will be defined in the top-level {@code descriptor}, and a reference to that definition will be returned.
     *
     * @param error The annotation that holds the data
     * @param descriptor The root descriptor, for adding definitions to.
     * @param relativeType The type relative to which schema resources should be resolved.
     * @return Error instance
     */
    public static Error fromAnnotation(org.forgerock.api.annotations.Error error, ApiDescription descriptor,
            Class<?> relativeType) {
        Error errorDefinition = error()
                .description(error.description())
                .code(error.code())
                .schema(Schema.fromAnnotation(error.detailSchema(), descriptor, relativeType))
                .build();
        if (!Strings.isNullOrEmpty(error.id())) {
            // we've got an id for this error, so define it at the top level and return a reference.
            descriptor.getErrors().addError(error.id(), errorDefinition);
            return error().reference(Reference.reference().value("#/errors/" + error.id()).build()).build();
        } else {
            return errorDefinition;
        }
    }

    /**
     * Builder for the Error.
     */
    public static final class Builder {

        // Must be an Integer, because 0 is not a valid default.
        private Integer code;
        private String description;
        private Schema schema;
        private Reference reference;

        private Builder() {
        }

        /**
         * Set the error code.
         *
         * @param code The error code.
         * @return This builder.
         */
        public Builder code(Integer code) {
            this.code = code;
            return this;
        }

        /**
         * Set the error description.
         *
         * @param description Error description
         * @return This builder.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the schema.
         *
         * @param schema Error schema
         * @return This builder.
         */
        public Builder schema(Schema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Set the error as a reference to another definition.
         *
         * @param reference The reference.
         * @return This builder.
         */
        public Builder reference(Reference reference) {
            this.reference = reference;
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

    /**
     * {@link Error} {@link Comparator}, which sorts by code and description. This {@code Comparator} does not handle
     * {@code null} values or duplicates, because those conditions should never occur in practice.
     * <p>
     * This class is thread-safe.
     * </p>
     */
    private static class ErrorComparator implements Comparator<Error> {
        @Override
        public int compare(final Error o1, final Error o2) {
            final int codeCompare = o1.code.compareTo(o2.code);
            if (codeCompare == 0) {
                return o1.description.compareTo(o2.description);
            }
            return codeCompare;
        }
    }
}
