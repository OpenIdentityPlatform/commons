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
 * Class that represents the ApiDescription type in API descriptor.
 */
public final class ApiDescription {

    private String id; //TODO fix the type to frURI?
    private Schema[] definitions;
    private Paths paths;
    private String[] protocolVersions;

    private ApiDescription(Builder builder) {
        this.id = builder.id;
        this.definitions = builder.definitions;
        this.paths = builder.paths;
        this.protocolVersions = builder.protocolVersions;
    }

    /**
     * Getter of id.
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Getter of definitions.
     * @return Definition array
     */
    public Schema[] getDefinitions() {
        return definitions;
    }

    /**
     * Getter of paths.
     * @return Paths
     */
    public Paths getPaths() {
        return paths;
    }

    /**
     * Getter of protocol versions.
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
    private static Builder apiDescription() {
        return new Builder();
    }

    /**
     * Builder for the ApiDescription.
     */
    public static final class Builder {

        private String id; //TODO fix the type to frURI?
        private Schema[] definitions;
        private Paths paths;
        private String[] protocolVersions;

        /**
         * Private default constructor with the mandatory fields.
         */
        private Builder() {
        }

        /**
         * Set the id.
         * @param id ApiDescription id
         * @return Builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the definitions.
         * @param definitions Definitions or this APIdescription
         * @return Builder
         */
        public Builder definitions(Schema[] definitions) {
            this.definitions = definitions;
            return this;
        }

        /**
         * Set the paths.
         * @param paths Paths
         * @return Builder
         */
        public Builder paths(Paths paths) {
            this.paths = paths;
            return this;
        }

        /**
         * Set the protocol versions.
         * @param protocolVersions Protocol version
         * @return ProtocolVersions
         */
        public Builder protocolVersions(String[] protocolVersions) {
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