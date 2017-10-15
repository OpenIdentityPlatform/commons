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

import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.Comparator;
import java.util.Objects;

import org.forgerock.api.ApiValidationException;
import com.google.common.base.Strings;
import org.forgerock.util.i18n.LocalizableString;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Class that represents the ApiError type in API descriptor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ApiError.Builder.class)
public final class ApiError {

    /**
     * {@link ApiError} {@link Comparator}, which sorts by code and description.
     */
    public static final ErrorComparator ERROR_COMPARATOR = new ErrorComparator();

    // Must be an Integer, because 0 is not a valid default
    private final Integer code;
    private final LocalizableString description;
    private final Schema schema;
    @JsonProperty("$ref")
    private final Reference reference;

    private ApiError(Builder builder) {
        this.code = builder.code;
        this.description = builder.description;
        this.schema = builder.schema;
        this.reference = builder.reference;

        if (reference == null && (code == null || description == null || isEmpty(description.toString()))) {
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
    public Integer getCode() {
        return code;
    }

    /**
     * Getter of the error description.
     *
     * @return Description
     */
    public LocalizableString getDescription() {
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
        ApiError apiError = (ApiError) o;
        return Objects.equals(code, apiError.code)
                && Objects.equals(description, apiError.description)
                && Objects.equals(schema, apiError.schema)
                && Objects.equals(reference, apiError.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, description, schema, reference);
    }

    /**
     * New apiError builder.
     *
     * @return Builder
     */
    public static Builder apiError() {
        return new Builder();
    }

    /**
     * Builds an ApiError object from the data in the annotation. If the {@code ApiError} has an {@code id} defined, the
     * ApiError will be defined in the top-level {@code descriptor}, and a reference to that definition will be
     * returned.
     *
     * @param apiError The annotation that holds the data
     * @param descriptor The root descriptor, for adding definitions to.
     * @param relativeType The type relative to which schema resources should be resolved.
     * @return ApiError instance
     */
    public static ApiError fromAnnotation(org.forgerock.api.annotations.ApiError apiError,
                                          ApiDescription descriptor, Class<?> relativeType) {
        ApiError apiErrorDefinition = apiError()
                .description(new LocalizableString(apiError.description(), relativeType))
                .code(apiError.code())
                .schema(Schema.fromAnnotation(apiError.detailSchema(), descriptor, relativeType))
                .build();
        if (!Strings.isNullOrEmpty(apiError.id())) {
            // we've got an id for this apiApiError, so define it at the top level and return a reference.
            descriptor.addError(apiError.id(), apiErrorDefinition);
            return apiError().reference(Reference.reference().value("#/errors/" + apiError.id()).build()).build();
        } else {
            return apiErrorDefinition;
        }
    }

    /**
     * Builder for the ApiError.
     */
    public static final class Builder {

        // Must be an Integer, because 0 is not a valid default.
        private Integer code;
        private LocalizableString description;
        private Schema schema;
        private Reference reference;

        private Builder() {
        }

        /**
         * Set the error code.
         *
         * @param code The apiError code.
         * @return This builder.
         */
        @JsonProperty("code")
        public Builder code(Integer code) {
            this.code = code;
            return this;
        }

        /**
         * Set the error description.
         *
         * @param description ApiError description
         * @return This builder.
         */
        public Builder description(LocalizableString description) {
            this.description = description;
            return this;
        }

        /**
         * Set the error description.
         *
         * @param description ApiError description
         * @return This builder.
         */
        @JsonProperty("description")
        public Builder description(String description) {
            this.description = new LocalizableString(description);
            return this;
        }

        /**
         * Set the schema.
         *
         * @param schema ApiError schema
         * @return This builder.
         */
        @JsonProperty("schema")
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
        @JsonProperty("$ref")
        public Builder reference(Reference reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Builds the ApiError.
         *
         * @return ApiError instance
         */
        public ApiError build() {
            return new ApiError(this);
        }
    }

    /**
     * {@link ApiError} {@link Comparator}, which sorts by code and description. This {@code Comparator} does not handle
     * {@code null} values or duplicates, because those conditions should never occur in practice.
     * <p>
     * This class is thread-safe.
     * </p>
     */
    private static class ErrorComparator implements Comparator<ApiError> {
        @Override
        public int compare(final ApiError o1, final ApiError o2) {
            if (o1.getReference() != null) {
                return o2.getReference() != null
                        ? o1.getReference().getValue().compareTo(o2.getReference().getValue())
                        : 1;
            }
            if (o2.getReference() != null) {
                return -1;
            }
            final int codeCompare = o1.code.compareTo(o2.code);
            if (codeCompare == 0) {
                return o1.description.toString().compareTo(o2.description.toString());
            }
            return codeCompare;
        }
    }
}
