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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.enums.Stability;
import org.forgerock.util.i18n.LocalizableString;

/**
 * Class that represents the Operation type in API descriptor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Operation {

    private final LocalizableString description;
    private final String[] supportedLocales;
    @JsonProperty("errors")
    private final ApiError[] apiErrors;
    private final Parameter[] parameters;
    private final Stability stability;

    /**
     * Protected constructor of the Operation.
     *
     * @param builder Operation Builder
     */
    protected Operation(Builder builder) {
        this.description = builder.description;
        this.supportedLocales = builder.supportedLocales;
        this.stability = builder.stability;

        final List<ApiError> apiErrors = builder.apiErrors;
        this.apiErrors = apiErrors.toArray(new ApiError[apiErrors.size()]);

        final List<Parameter> parameters = builder.parameters;
        this.parameters = parameters.toArray(new Parameter[parameters.size()]);
    }

    /**
     * Getter of the description.
     *
     * @return Description
     */
    public LocalizableString getDescription() {
        return description;
    }

    /**
     * Getter of the supported locales array.
     *
     * @return Supported locales
     */
    public String[] getSupportedLocales() {
        return supportedLocales;
    }

    /**
     * Getter of the error array.
     *
     * @return ApiError array
     */
    public ApiError[] getApiErrors() {
        return apiErrors.length == 0 ? null : apiErrors;
    }

    /**
     * Getter of the parameters array.
     *
     * @return Parameters
     */
    public Parameter[] getParameters() {
        return parameters.length == 0 ? null : parameters;
    }

    /**
     * Getter of Operation stability.
     *
     * @return Stability or {@code null} which suggests {@link Stability#STABLE} (default).
     */
    public Stability getStability() {
        return stability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Operation operation = (Operation) o;
        return Objects.equals(description, operation.description)
                && Arrays.equals(supportedLocales, operation.supportedLocales)
                && Arrays.equals(apiErrors, operation.apiErrors)
                && Arrays.equals(parameters, operation.parameters)
                && stability == operation.stability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, supportedLocales, apiErrors, parameters, stability);
    }

    /**
     * Allocates the operation by operation type to the given Resource Builder
     * by calling the corresonding method by type.
     *
     * @param resourceBuilder - Resource Builder to add the operation
     */
    protected abstract void allocateToResource(Resource.Builder resourceBuilder);

    /**
     * Builder to help construct the Operation.
     */
    public abstract static class Builder<T extends Builder<T>> {

        private LocalizableString description;
        private String[] supportedLocales;
        private final List<ApiError> apiErrors;
        private final List<Parameter> parameters;
        private Stability stability;

        /**
         * Creates a new Builder.
         */
        protected Builder() {
            apiErrors = new ArrayList<>();
            parameters = new ArrayList<>();
        }

        /**
         * Abstract method that returns the instantiated Builder itself.
         *
         * @return Builder
         */
        protected abstract T self();

        /**
         * Set the description.
         *
         * @param description A description of the endpoint
         * @return Builder
         */
        public T description(LocalizableString description) {
            this.description = description;
            return self();
        }

        /**
         * Set the description.
         *
         * @param description A description of the endpoint
         * @return Builder
         */
        @JsonProperty("description")
        public T description(String description) {
            this.description = new LocalizableString(description);
            return self();
        }

        /**
         * Set the supported locale.
         *
         * @param supportedlocales Locales codes supported by the operation
         * @return Builder
         */
        @JsonProperty("supportedLocales")
        public T supportedLocales(String... supportedlocales) {
            this.supportedLocales = supportedlocales;
            return self();
        }

        /**
         * Set multiple supported errors.
         *
         * @param apiErrors What errors may be returned by this operation
         * @return Builder
         */
        @JsonProperty("errors")
        public T errors(List<ApiError> apiErrors) {
            this.apiErrors.addAll(apiErrors);
            return self();
        }

        /**
         * Sets a single supported error.
         *
         * @param apiError An error that may be returned by this operation
         * @return Builder
         */
        public T error(ApiError apiError) {
            this.apiErrors.add(apiError);
            return self();
        }

        /**
         * Set multiple supported parameters.
         *
         * @param parameters Extra parameters supported by the operation
         * @return Builder
         */
        @JsonProperty("parameters")
        public T parameters(List<Parameter> parameters) {
            this.parameters.addAll(parameters);
            return self();
        }

        /**
         * Sets a single supported parameters.
         *
         * @param parameter Extra parameter supported by the operation
         * @return Builder
         */
        public T parameter(Parameter parameter) {
            this.parameters.add(parameter);
            return self();
        }

        /**
         * Sets stability of Operation.
         *
         * @param stability Stability
         * @return Builder
         */
        @JsonProperty("stability")
        public T stability(Stability stability) {
            this.stability = stability;
            return self();
        }

        /**
         * Set all properties in the Builder using the data in the annotation.
         * @param operation The annotation that holds the data
         * @param descriptor The root descriptor
         * @param relativeType The type relative to which schema resources should be resolved.
         * @return Builder
         */
        public T detailsFromAnnotation(org.forgerock.api.annotations.Operation operation,
                ApiDescription descriptor, Class<?> relativeType) {
            for (org.forgerock.api.annotations.ApiError apiApiError : operation.errors()) {
                error(ApiError.fromAnnotation(apiApiError, descriptor, relativeType));
            }
            for (org.forgerock.api.annotations.Parameter parameter : operation.parameters()) {
                parameter(Parameter.fromAnnotation(relativeType, parameter));
            }
            return description(new LocalizableString(operation.description(), relativeType))
                    .supportedLocales(operation.locales())
                    .stability(operation.stability());
        }
    }

}
