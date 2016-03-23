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
 * Class that represents the Patch operation type in API descriptor.
 *
 */
public final class Patch extends Operation {

    private final boolean mvccSupported;

    /**
     * Protected contstructor of the Patch operation.
     *
     * @param builder Patch Builder
     */
    private Patch(Builder builder) {
        super(builder);
        this.mvccSupported = builder.mvccSupported;
    }

    /**
     * Creates a new builder for Patch.
     * @param mvccSupported Whether this resource supports MVCC
     * @return New builder instance
     */
    public static final Builder patch(boolean mvccSupported) {
        return new Builder(mvccSupported);
    }

    /**
     * Allocates the Patch operation type to the given Resource Builder.
     * @param resourceBuilder - Resource Builder to add the operation
     */
    @Override
    protected void allocateToResource(Resource.Builder resourceBuilder) {
        resourceBuilder.patch(this);
    }

    /**
     * Builder to help construct the Patch.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private boolean mvccSupported;

        /**
         * Private constructor with the required parameter.
         * @param mvccSupported Whether this resource supports MVCC
         */
        private Builder(boolean mvccSupported) {
            super();
            this.mvccSupported = mvccSupported;
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Patch instace.
         *
         * @return Patch instace
         */
        public Patch build() {
            return new Patch(this);
        }
    }

}
