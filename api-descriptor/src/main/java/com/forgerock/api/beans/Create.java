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

import com.forgerock.api.enums.CreateMode;

/**
 * Class that represents the Create Operation type in API descriptor.
 *
 */
public final class Create extends Operation {

    private final CreateMode mode;
    private final boolean mvccSupported;

    /**
     * Protected contstructor of the Create.
     *
     * @param builder Operation Builder
     */
    private Create(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.mvccSupported = builder.mvccSupported;
    }

    /**
     * Getter of the mode.
     * @return Mode
     */
    public CreateMode getMode() {
        return mode;
    }

    /**
     * Getter of mvcc supported.
     * @return true if mvcc is supported
     */
    public boolean getMvccSupported() {
        return mvccSupported;
    }

    /**
     * Creates a new builder for Create.
     * @param mode Create mode
     * @param mvccSupported Multiversion concurrency control supported
     * @return New builder instance
     */
    public static final Builder create(CreateMode mode, boolean mvccSupported) {
        return new Builder(mode, mvccSupported);
    }

    /**
     * Allocates the Create operation type to the given Resource Builder.
     * @param resourceBuilder - Resource Builder to add the operation
     */
    @Override
    protected void allocateToResource(Resource.Builder resourceBuilder) {
        resourceBuilder.create(this);
    }

    /**
     * Builder for the Create.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private CreateMode mode;
        private boolean mvccSupported;

        /**
         * Private constructor with the required parameter.
         * @param mode Create mode
         * @param mvccSupported Multiversion concurrency control supported
         */
        private Builder(CreateMode mode, boolean mvccSupported) {
            super();
            this.mode = mode;
            this.mvccSupported = mvccSupported;
        }

        /**
         * Returns the builder so this.
         * @return this
         */
        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Create instace.
         *
         * @return Create instace
         */
        public Create build() {
            return new Create(this);
        }
    }

}
