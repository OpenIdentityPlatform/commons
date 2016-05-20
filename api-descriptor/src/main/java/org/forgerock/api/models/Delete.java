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

package org.forgerock.api.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Class that represents the Delete operation type in API descriptor.
 *
 */
@JsonDeserialize(builder = Delete.Builder.class)
public final class Delete extends Operation {

    /**
     * Protected contstructor of the Delete.
     *
     * @param builder Delete Builder
     */
    private Delete(Builder builder) {
        super(builder);
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
     * Builds a Delete object from the data in the Delete annotation.
     * @param delete Delete annotation where the data stored
     * @param descriptor The root descriptor to add definitions to.
     * @param relativeType The type relative to which schema resources should be resolved.
     * @return Delete instance
     */
    public static Delete fromAnnotation(org.forgerock.api.annotations.Delete delete, ApiDescription descriptor,
            Class<?> relativeType) {
        return delete()
                .detailsFromAnnotation(delete.operationDescription(), descriptor, relativeType)
                .build();
    }

    /**
     * Builder for the Delete.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private Builder() {
            super();
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Delete instance.
         *
         * @return Delete instance
         */
        public Delete build() {
            return new Delete(this);
        }
    }

}
