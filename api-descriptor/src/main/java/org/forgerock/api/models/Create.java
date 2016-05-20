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

import static org.forgerock.api.enums.CreateMode.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.ApiValidationException;
import org.forgerock.api.enums.CreateMode;

/**
 * Class that represents the Create Operation type in API descriptor.
 */
@JsonDeserialize(builder = Create.Builder.class)
public final class Create extends Operation {

    private final CreateMode mode;
    private final Boolean singleton;

    /**
     * Protected contstructor of the Create.
     *
     * @param builder Operation Builder
     */
    private Create(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.singleton = builder.singleton;

        if (mode == null) {
            throw new ApiValidationException("mode required");
        }
    }

    /**
     * Getter of the mode.
     *
     * @return Mode
     */
    public CreateMode getMode() {
        return mode;
    }

    /**
     * Informs if operation creates singleton resources.
     *
     * @return {@code true} if operation creates singleton resources and {@code false} otherwise
     */
    public Boolean isSingleton() {
        return singleton;
    }

    /**
     * Creates a new builder for Create.
     *
     * @return New builder instance
     */
    public static final Builder create() {
        return new Builder();
    }

    /**
     * Allocates the Create operation type to the given Resource Builder.
     *
     * @param resourceBuilder - Resource Builder to add the operation
     */
    @Override
    protected void allocateToResource(Resource.Builder resourceBuilder) {
        resourceBuilder.create(this);
    }

    /**
     * Builds a Create object from the data in the annotation.
     *
     * @param create Create annotation that holds the data
     * @param instanceOperations True if the resource is performing instance operations.
     * @param descriptor The root descriptor to add definitions to.
     * @param relativeType The type relative to which schema resources should be resolved.
     * @return Create instance
     */
    public static Create fromAnnotation(org.forgerock.api.annotations.Create create, boolean instanceOperations,
            ApiDescription descriptor, Class<?> relativeType) {
        List<CreateMode> modes = Arrays.asList(create.modes());
        if ((instanceOperations && !modes.contains(ID_FROM_CLIENT))
                || (!instanceOperations && !modes.contains(ID_FROM_SERVER))) {
            return null;
        }
        return create()
                .detailsFromAnnotation(create.operationDescription(), descriptor, relativeType)
                .mode(instanceOperations ? ID_FROM_CLIENT : ID_FROM_SERVER)
                .build();
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
        Create create = (Create) o;
        return mode == create.mode
                && Objects.equals(singleton, create.singleton);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mode, singleton);
    }

    /**
     * Builder for the Create.
     */
    public static final class Builder extends Operation.Builder<Builder> {

        private CreateMode mode;
        private Boolean singleton = false;

        private Builder() {
            super();
        }


        /**
         * Setter for create-mode.
         *
         * @param mode Create-mode
         * @return Builder
         */
        @JsonProperty("mode")
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
        @JsonProperty("singleton")
        public Builder singleton(Boolean singleton) {
            this.singleton = singleton;
            return this;
        }

        /**
         * Returns the builder so this.
         *
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
