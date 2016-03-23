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
 * Class that represents the Schema type in API descriptor.
 *
 */
public final class Schema {

    /**
     * private contstructor of the Schema.
     *
     * @param builder Operation Builder
     */
    private Schema(Builder builder) { }

    /**
     * Create a new Builder for Schema using JSON schema and Forgerock extensions.
     *
     * @return Builder
     */
    public static Builder schema() {
        return new Builder();
    }

    /**
     * Builder for the Schema.
     */
    public static final class Builder {

        /**
         * Private default constructor with the mandatory fields.
         */
        private Builder() { }

        /**
         * Builds the Schema instace.
         *
         * @return Schema instace
         */
        public Schema build() {
            return new Schema(this);
        }
    }

}
