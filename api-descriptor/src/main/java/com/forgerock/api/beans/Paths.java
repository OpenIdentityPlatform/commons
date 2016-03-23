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
 * Class that represents the Paths type in API descriptor.
 */
public final class Paths {

    private final Resource[] resources;

    private Paths(Builder builder) {
        this.resources = builder.resources;
    }

    /**
     * Getter of the resources.
     *
     * @return Resources
     */
    public Resource[] getResources() {
        return resources;
    }

    /**
     * Create a new Builder for Path.
     * @param resources One or more resources
     * @return Builder
     */
    public static Builder paths(Resource... resources) {
        return new Builder(resources);
    }

    /**
     * Builder that help construct the Paths.
     */
    public static final class Builder {

        private Resource[] resources;

        /**
         * Private default constructor with the mandatory fields.
         */
        private Builder(Resource... resources) {
            this.resources = resources;
        }

        /**
         * Builds the Paths instace.
         *
         * @return Paths instace
         */
        public Paths build() {
            return new Paths(this);
        }
    }

}