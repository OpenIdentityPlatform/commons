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
 * Class that represents the Reference type in API descriptor.
 */
public final class Reference {

    private final String reference;

    private Reference(Builder builder) {
        this.reference = builder.reference;
    }

    /**
     * Getter of the JSON reference.
     * @return reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * Create a new Builder for Reference with the JSON ref parameter.
     * @param reference JSON reference
     * @return Builder
     */
    public static Builder reference(String reference) {
        return new Builder(reference);
    }

    /**
     * Builder to help construct the Reference.
     */
    public static final class Builder {

        private String reference;
        private VersionedPath versionedPath;

        /**
         * Private default constructor with the mandatory field.
         * @param reference A JSON Reference to the required object. The URI should be an frURI type, or a URL
         */
        private Builder(String reference) {
            this.reference = reference;
        }

        /**
         * Builds the Reference instace.
         *
         * @return Reference instace
         */
        public Reference build() {
            return new Reference(this);
        }
    }

}