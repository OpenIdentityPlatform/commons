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

import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.ApiValidationException;
import org.forgerock.api.enums.PatchOperation;

/**
 * Class that represents the Patch operation type in API descriptor.
 */
@JsonDeserialize(builder = Patch.Builder.class)
public final class Patch extends Operation {

    private final PatchOperation[] operations;

    /**
     * Protected contstructor of the Patch operation.
     *
     * @param builder Patch Builder
     */
    private Patch(Builder builder) {
        super(builder);
        this.operations = builder.operations;

        if (isEmpty(operations)) {
            throw new ApiValidationException("operations required");
        }
    }

    /**
     * Getter for supported Patch operations.
     *
     * @return Supported Patch operations
     */
    public PatchOperation[] getOperations() {
        return operations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Patch patch = (Patch) o;
        return Arrays.equals(operations, patch.operations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), operations);
    }

    /**
     * Creates a new builder for Patch.
     *
     * @return New builder instance
     */
    public static final Builder patch() {
        return new Builder();
    }

    /**
     * Allocates the Patch operation type to the given Resource Builder.
     *
     * @param resourceBuilder - Resource Builder to add the operation
     */
    @Override
    protected void allocateToResource(Resource.Builder resourceBuilder) {
        resourceBuilder.patch(this);
    }

    /**
     * Builds a Patch object from the data stored in the annotation.
     * @param patch Patch annotation that holds the data
     * @param descriptor The root descriptor to add definitions to.
     * @param relativeType The type relative to which schema resources should be resolved.
     * @return Patch instance
     */
    public static Patch fromAnnotation(org.forgerock.api.annotations.Patch patch, ApiDescription descriptor,
            Class<?> relativeType) {
        return patch()
                .detailsFromAnnotation(patch.operationDescription(), descriptor, relativeType)
                .operations(patch.operations())
                .build();
    }

    /**
     * Builder to help construct the Patch.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private PatchOperation[] operations;

        private Builder() {
            super();
        }

        /**
         * Setter for supported Patch-operations.
         *
         * @param operations Supported Patch-operations
         * @return Builder
         */
        @JsonProperty("operations")
        public Builder operations(PatchOperation... operations) {
            this.operations = operations;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Patch instance.
         *
         * @return Patch instance
         */
        public Patch build() {
            return new Patch(this);
        }
    }

}
