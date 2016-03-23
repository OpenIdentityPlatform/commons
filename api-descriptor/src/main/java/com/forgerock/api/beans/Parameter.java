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

import com.forgerock.api.enums.ParameterSource;

/**
 * Class that represents the Parameter type in API descriptor.
 *
 */
public final class Parameter {

    private final String name;
    private final String type;
    private final String defaultValue; //Todo String?
    private final String description;
    private final ParameterSource parameterSource;
    private final boolean required;

    /**
     * Private Parameter constructor called by the builder.
     * @param builder Builder that holds the values for setting the parameter properties
     */
    private Parameter(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.defaultValue = builder.defaultValue;
        this.description = builder.description;
        this.parameterSource = builder.parameterSource;
        this.required = builder.required;
    }

    /**
     * Getter of the name of the parameter.
     * @return Parameter name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the parameter type.
     * @return Parameter type
     */
    public String getType() {
        return type;
    }

    /**
     * Getter of the parameter's default value.
     * @return Parameter default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Getter of the parameter description.
     * @return Parameter description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the parameter source.
     * @return Parameter source enum
     */
    public ParameterSource getParameterSource() {
        return parameterSource;
    }

    /**
     * Getter of the required property.
     * @return Required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * New parameter builder.
     * @param name - The name of the parameter
     * @param type - The type of the parameter: string, number, boolean, enum, and array variants
     * @return Builder
     */
    public static Builder parameter(String name, String type) {
        return new Builder(name, type);
    }

    /**
     * Builder to construct Parameter object.
     */
    public static final class Builder {

        private final String name;
        private final String type;
        private String defaultValue;
        private String description;
        private ParameterSource parameterSource;
        private boolean required;

        /**
         * Default builder contstructor with 2 mandatory parameters.
         * @param name - The name of the parameter
         * @param type - The type of the parameter: string, number, boolean, enum, and array variants
         */
        private Builder(String name, String type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Set the parameter default value.
         * @param defaultValue If exists, the default value
         * @return builder
         */
        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Set the parameter description.
         * @param description The description of the parameter
         * @return builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the parameter source.
         * @param parameterSource Where the parameter comes from. May be: PATH or ADDITIONAL
         * @return builder
         */
        public Builder parameterSourceEnum(ParameterSource parameterSource) {
            this.parameterSource = parameterSource;
            return this;
        }

        /**
         * Set the required property.
         * @param required Whether the parameter is required
         * @return builder
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Builds the Parameter.
         * @return The parameter instance
         */
        public Parameter build() {
            return new Parameter(this);
        }
    }

}
