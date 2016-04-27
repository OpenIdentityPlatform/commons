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

import org.forgerock.api.ApiValidationException;

/**
 * Class that represents the ApiDescription type in API descriptor.
 */
public final class ApiDescription {

    private final String id;
    private final String version;
    private final String description;
    private final Definitions definitions;
    private final Services services;
    private final Errors errors;
    private final Paths paths;

    private ApiDescription(Builder builder) {
        this.id = builder.id;
        this.version = builder.version;
        this.description = builder.description;
        this.definitions = builder.definitions == null ? Definitions.definitions().build() : builder.definitions;
        this.services = builder.services;
        this.errors = builder.errors == null ? Errors.errors().build() : builder.errors;
        this.paths = builder.paths;

        if (isEmpty(id) || isEmpty(version)) {
            throw new ApiValidationException("id and version required");
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
     * Getter of version.
     *
     * @return The version.
     */
    public String getVersion() {
        return version;
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
     * Getter of definitions.
     *
     * @return Definitions map
     */
    public Definitions getDefinitions() {
        return definitions;
    }

    /**
     * Getter of services.
     *
     * @return Services map
     */
    public Services getServices() {
        return services;
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
    public Paths getPaths() {
        return paths;
    }

    /**
     * Create a new Builder for ApiDescription.
     *
     * @return Builder
     */
    public static Builder apiDescription() {
        return new Builder();
    }

    /**
     * Builder for the ApiDescription.
     */
    public static final class Builder {

        private String id;
        private String description;
        private Definitions definitions;
        private Errors errors;
        private Services services;
        private Paths paths;
        private String version;

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
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description Description of API Description
         * @return Builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the definitions.
         *
         * @param definitions Definitions for this API Description
         * @return Builder
         */
        public Builder definitions(Definitions definitions) {
            this.definitions = definitions;
            return this;
        }


        /**
         * Set the services.
         *
         * @param services Services for this API Description
         * @return Builder
         */
        public Builder services(Services services) {
            this.services = services;
            return this;
        }

        /**
         * Set the errors.
         *
         * @param errors Errors for this API Description
         * @return Builder
         */
        public Builder errors(Errors errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Set the paths.
         *
         * @param paths Paths
         * @return Builder
         */
        public Builder paths(Paths paths) {
            this.paths = paths;
            return this;
        }

        /**
         * Set the version of the API.
         *
         * @param version The version.
         * @return This builder.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Builds the ApiDescription instance.
         *
         * @return ApiDescription instance
         */
        public ApiDescription build() {
            return new ApiDescription(this);
        }
    }

}