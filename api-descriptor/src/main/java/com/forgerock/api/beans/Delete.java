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

import com.forgerock.api.ApiValidationException;

/**
 * Class that represents the Delete operation type in API descriptor.
 *
 */
public final class Delete extends Operation {

    private final Boolean mvccSupported;

    /**
     * Protected contstructor of the Delete.
     *
     * @param builder Delete Builder
     */
    private Delete(Builder builder) {
        super(builder);
        this.mvccSupported = builder.mvccSupported;

        if (mvccSupported == null) {
            throw new ApiValidationException("mvccSupported required");
        }
    }

    /**
     * Informs if MVCC is supported.
     *
     * @return {@code true} if MVCC is supported and {@code false} otherwise
     */
    public boolean getMvccSupported() {
        return mvccSupported;
    }

    /**
     * Creates a new builder for Delete.
     * @return New builder instance
     */
    public static final Builder delete() {
        return new Builder();
    }

    /**
     * Allocates the Delete operation type to the given Resource Builder.
     * @param resourceBuilder - Resource Builder to add the operation
     */
    @Override
    protected void allocateToResource(Resource.Builder resourceBuilder) {
        resourceBuilder.delete(this);
    }

    /**
     * Builder for the Delete.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private Boolean mvccSupported;

        private Builder() {
            super();
        }

        /**
         * Setter for MVCC-supported flag.
         *
         * @param mvccSupported Whether this resource supports MVCC
         */
        private Builder mvccSupported(boolean mvccSupported) {
            this.mvccSupported = mvccSupported;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Delete instace.
         *
         * @return Delete instace
         */
        public Delete build() {
            return new Delete(this);
        }
    }

}
