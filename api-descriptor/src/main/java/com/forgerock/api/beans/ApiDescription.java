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
public class ApiDescription {

    private String _id; //TODO fix the type to frURI?
    private Schema[] definitions;
    private Paths paths;
    private String[] protocolVersions;

    private ApiDescription(Builder builder){
        this._id = builder._id;
        this.definitions = builder.definitions;
        this.paths = builder.paths;
        this.protocolVersions = builder.protocolVersions;
    }

    /**
     * Getter of _id
     * @return _id
     */
    public String get_id() {
        return _id;
    }

    /**
     * Getter of definitions
     * @return Definition array
     */
    public Schema[] getDefinitions() {
        return definitions;
    }

    /**
     * Getter of paths
     * @return Paths
     */
    public Paths getPaths() {
        return paths;
    }

    /**
     * Getter of protocol versions
     * @return ProtocolVersion array
     */
    public String[] getProtocolVersions() {
        return protocolVersions;
    }

    /**
     * Create a new Builder for ApiDescription
     *
     * @return Builder
     */
    private static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String _id; //TODO fix the type to frURI?
        private Schema[] definitions;
        private Paths paths;
        private String[] protocolVersions;

        /**
         * Private default constructor with the mandatory fields
         */
        private Builder() {
        }

        /**
         * Set the _id
         * @param _id
         * @return Builder
         */
        public Builder with_id(String _id) {
            this._id = _id;
            return this;
        }

        /**
         * Set the definitions
         * @param definitions
         * @return Builder
         */
        public Builder withDefinitions(Schema[] definitions) {
            this.definitions = definitions;
            return this;
        }

        /**
         * Set the paths
         * @param paths
         * @return Builder
         */
        public Builder withPaths(Paths paths) {
            this.paths = paths;
            return this;
        }

        /**
         * Set the protocol versions
         * @param protocolVersions
         * @return ProtocolVersions
         */
        public Builder withProtocolVersions(String[] protocolVersions) {
            this.protocolVersions = protocolVersions;
            return this;
        }

        /**
         * Builds the ApiDescription instace
         *
         * @return ApiDescription instace
         */
        public ApiDescription build() {
            return new ApiDescription(this);
        }
    }

}