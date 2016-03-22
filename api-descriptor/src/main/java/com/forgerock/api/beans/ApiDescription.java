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

import java.util.Map;

import com.forgerock.api.ApiValidationException;

/**
 * Class that represents the ApiDescription type in API descriptor.
 */
public final class ApiDescription<T extends PathNode> {

    private String id;
    private String description;
    private Map<String, Schema> definitions;
    private Map<String, Error> errors;
    private Map<String, T> paths;
    private String[] protocolVersions;

    private ApiDescription(Builder builder) {
        this.id = builder.id;
        this.description = builder.description;
        this.definitions = builder.definitions;
        this.errors = builder.errors;
        this.paths = builder.paths;
        this.protocolVersions = builder.protocolVersions;

        if (id == null || id.trim().isEmpty()) {
            throw new ApiValidationException("id required");
        }
        if ((definitions == null || definitions.isEmpty())
                && (errors == null || errors.isEmpty())
                && (paths == null || paths.isEmpty())) {
            throw new ApiValidationException("At least one of {definitions, errors, paths} required to be non-empty");
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

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter of definitions.
     *
     * @return Definitions map
     */
    public Map<String, Schema> getDefinitions() {
        return definitions;
    }

    /**
     * Getter of errors.
     *
     * @return Errors map
     */
    public Map<String, Error> getErrors() {
        return errors;
    }

    /**
     * Getter of paths.
     *
     * @return Paths
     */
    public Map<String, T> getPaths() {
        return paths;
    }

    /**
     * Getter of protocol versions.
     *
     * @return ProtocolVersion array
     */
    public String[] getProtocolVersions() {
        return protocolVersions;
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
     */
    public static final class Builder<T extends PathNode> {

        private String id;
        private String description;
        private Map<String, Schema> definitions;
        private Map<String, Error> errors;
        private Map<String, T> paths;
        private String[] protocolVersions;

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
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description Description of API Description
         * @return Builder
         */
        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the definitions.
         *
         * @param definitions Definitions for this API Description
         * @return Builder
         */
        public Builder<T> definitions(Map<String, Schema> definitions) {
            this.definitions = definitions;
            return this;
        }

        /**
         * Set the errors.
         *
         * @param errors Errors for this API Description
         * @return Builder
         */
        public Builder<T> errors(Map<String, Error> errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Set the paths.
         *
         * @param paths Paths
         * @return Builder
         */
        public Builder<T> paths(Map<String, T> paths) {
            this.paths = paths;
            return this;
        }

        /**
         * Set the protocol versions.
         *
         * @param protocolVersions Protocol version
         * @return ProtocolVersions
         */
        public Builder<T> protocolVersions(String[] protocolVersions) {
            this.protocolVersions = protocolVersions;
            return this;
        }

        /**
         * Builds the ApiDescription instace.
         *
         * @return ApiDescription instace
         */
        public ApiDescription build() {
            return new ApiDescription(this);
        }
    }

}