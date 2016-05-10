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
import java.util.List;

import org.forgerock.api.enums.Stability;

/**
 * Class that represents the Operation type in API descriptor.
 */
public abstract class Operation {

    private final String description;
    private final String[] supportedLocales;
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
    public String getDescription() {
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
        return apiErrors;
    }

    /**
     * Getter of the parameters array.
     *
     * @return Parameters
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * Getter of Operation stability.
     *
     * @return Stability or {@code null} which suggests {@link Stability#STABLE} (default).
     */
    public Stability getStability() {
        return stability;
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

        private String description;
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
        public T description(String description) {
            this.description = description;
            return self();
        }

        /**
         * Set the supported locale.
         *
         * @param supportedlocales Locales codes supported by the operation
         * @return Builder
         */
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
                parameter(Parameter.fromAnnotation(parameter));
            }
            return description(operation.description())
                    .supportedLocales(operation.locales())
                    .stability(operation.stability());
        }
    }

}
