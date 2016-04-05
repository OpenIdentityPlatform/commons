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

package com.forgerock.api.models;

import static com.forgerock.api.enums.CreateMode.*;

import java.util.Arrays;
import java.util.List;

import com.forgerock.api.ApiValidationException;
import com.forgerock.api.enums.CreateMode;

/**
 * Class that represents the Create Operation type in API descriptor.
 */
public final class Create extends Operation {

    private final CreateMode mode;
    private final Boolean mvccSupported;

    /**
     * Protected contstructor of the Create.
     *
     * @param builder Operation Builder
     */
    private Create(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.mvccSupported = builder.mvccSupported;

        if (mode == null || mvccSupported == null) {
            throw new ApiValidationException("mode and mvccSupported required");
        }
    }

    /**
     * Getter of the mode.
     * @return Mode
     */
    public CreateMode getMode() {
        return mode;
    }

    /**
     * Informs if MVCC is supported.
     *
     * @return {@code true} if MVCC is supported and {@code false} otherwise
     */
    public boolean isMvccSupported() {
        return mvccSupported;
    }

    /**
     * Creates a new builder for Create.
     * @return New builder instance
     */
    public static final Builder create() {
        return new Builder();
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
     * Builds a Create object from the data in the annotation.
     * @param create Create annotation that holds the data
     * @param singleton True if singleton create //TODO fix this
     * @return Create instance
     */
    public static Create fromAnnotation(com.forgerock.api.annotations.Create create, boolean singleton) {
        List<CreateMode> modes = Arrays.asList(create.modes());
        if ((singleton && !modes.contains(ID_FROM_CLIENT)) || (!singleton && !modes.contains(ID_FROM_SERVER))) {
            return null;
        }
        return create()
                .detailsFromAnnotation(create.operationDescription())
                .mode(singleton ? ID_FROM_CLIENT : ID_FROM_SERVER)
                .mvccSupported(create.mvccSupported())
                .build();
    }

    /**
     * Builder for the Create.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private CreateMode mode;
        private Boolean mvccSupported;
        private Boolean singleton;

        private Builder() {
            super();
        }

        /**
         * Setter for MVCC-supported flag.
         *
         * @param mvccSupported Whether this resource supports MVCC
         * @return Builder
         */
        public Builder mvccSupported(boolean mvccSupported) {
            this.mvccSupported = mvccSupported;
            return this;
        }

        /**
         * Setter for create-mode.
         *
         * @param mode Create-mode
         * @return Builder
         */
        public Builder mode(CreateMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Setter for singleton.
         *
         * @param singleton Specifies that create operates on a singleton as opposed to a collection.
         * @return Builder
         */
        public Builder singleton(boolean singleton) {
            this.singleton = singleton;
            return this;
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
         * Builds the Create instance.
         *
         * @return Create instance
         */
        public Create build() {
            return new Create(this);
        }
    }

}
