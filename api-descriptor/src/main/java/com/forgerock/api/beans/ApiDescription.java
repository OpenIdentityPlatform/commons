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

import com.forgerock.api.ApiValidationException;

/**
 * Class that represents the ApiDescription type in API descriptor.
 *
 * @param <T> Type implements {@link PathNode}
 */
public final class ApiDescription<T extends PathNode> {

    private String id;
    private String description;
    private Definitions definitions;
    private Errors errors;
    private Paths<T> paths;

    private ApiDescription(Builder<T> builder) {
        this.id = builder.id;
        this.description = builder.description;
        this.definitions = builder.definitions;
        this.errors = builder.errors;
        this.paths = builder.paths;

        if (id == null || id.trim().isEmpty()) {
            throw new ApiValidationException("id required");
        }
        if (definitions == null && errors == null && paths == null) {
            throw new ApiValidationException("At least one of {definitions, errors, paths} required");
        }
    }

    /**
     * Getter of id.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets description of API Descriptor.
     *
     * @return Description of API Descriptor
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the ApiDescription.
     *
     * @param description Description of the API
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter of definitions.
     *
     * @return Definitions map
     */
    public Definitions getDefinitions() {
        return definitions;
    }

    /**
     * Getter of errors.
     *
     * @return Errors map
     */
    public Errors getErrors() {
        return errors;
    }

    /**
     * Getter of paths.
     *
     * @return Paths
     */
    public Paths<T> getPaths() {
        return paths;
    }

    /**
     * Create a new Builder for ApiDescription.
     *
     * @return Builder
     */
    public static Builder<VersionedPath> apiDescriptionWithVersionedPaths() {
        return new Builder<>();
    }

    /**
     * Create a new Builder for ApiDescription.
     *
     * @return Builder
     */
    public static Builder<Resource> apiDescription() {
        return new Builder<>();
    }

    /**
     * Builder for the ApiDescription.
     *
     * @param <T2> Type implements {@link PathNode}
     */
    public static final class Builder<T2 extends PathNode> {

        private String id;
        private String description;
        private Definitions definitions;
        private Errors errors;
        private Paths<T2> paths;

        /**
         * Private default constructor with the mandatory fields.
         */
        private Builder() {
        }

        /**
         * Set the id.
         *
         * @param id ApiDescription id
         * @return Builder
         */
        public Builder<T2> id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description Description of API Description
         * @return Builder
         */
        public Builder<T2> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the definitions.
         *
         * @param definitions Definitions for this API Description
         * @return Builder
         */
        public Builder<T2> definitions(Definitions definitions) {
            this.definitions = definitions;
            return this;
        }

        /**
         * Set the errors.
         *
         * @param errors Errors for this API Description
         * @return Builder
         */
        public Builder<T2> errors(Errors errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Set the paths.
         *
         * @param paths Paths
         * @return Builder
         */
        public Builder<T2> paths(Paths<T2> paths) {
            this.paths = paths;
            return this;
        }

        /**
         * Builds the ApiDescription instance.
         *
         * @return ApiDescription instance
         */
        public ApiDescription<T2> build() {
            return new ApiDescription<>(this);
        }
    }

}