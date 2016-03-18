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
 * Class that represents the Reference type in API descriptor
 */
public class Reference {

    private final Resource[] resources;
    private final VersionedPath versionedPath;

    private Reference(Builder builder){
        this.resources = builder.resources;
        this.versionedPath = builder.versionedPath;
    }

    /**
     * Getter of the resources
     * @return resources
     */
    public Resource[] getResources() {
        return resources;
    }

    /**
     * Getter of the versionedPath
     * @return VersionedPath
     */
    public VersionedPath getVersionedPath() {
        return versionedPath;
    }

    /**
     * Create a new Builder for Reference with the resources parameter
     *
     * @return Builder
     */
    private static Builder newBuilder(Resource... resources) {
        return new Builder(resources);
    }

    /**
     * Create a new Builder for Reference with versionedPath parameter
     *
     * @return Builder
     */
    private static Builder newBuilder(VersionedPath versionedPath) {
        return new Builder(versionedPath);
    }


    public static class Builder {

        private Resource[] resources;
        private VersionedPath versionedPath;

        /**
         * Private default constructor with the mandatory field
         */
        private Builder(Resource... resources) {
            this.resources = resources;
        }

        /**
         * Private default constructor with the mandatory field
         */
        private Builder(VersionedPath versionedPath) {
            this.versionedPath = versionedPath;
        }

        /**
         * Builds the Reference instace
         *
         * @return Reference instace
         */
        public Reference build() {
            return new Reference(this);
        }
    }

}