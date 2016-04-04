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

import static com.forgerock.api.util.ValidationUtil.isEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.forgerock.api.ApiValidationException;
import com.forgerock.api.enums.ParameterSource;

/**
 * Class that represents the Parameter type in API descriptor.
 */
public final class Parameter {

    private final String name;
    private final String type;
    private final String defaultValue; //Todo String?
    private final String description;
    private final ParameterSource source;
    private final boolean required;
    private final String[] enumValues;
    private final String[] enumTitles;

    // TODO "Other appropriate fields as described in the JSON Schema Validation spec may also be used."

    /**
     * Private Parameter constructor called by the builder.
     *
     * @param builder Builder that holds the values for setting the parameter properties
     */
    private Parameter(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.defaultValue = builder.defaultValue;
        this.description = builder.description;
        this.source = builder.source;
        this.required = builder.required;
        this.enumValues = builder.enumValues;
        this.enumTitles = builder.enumTitles;

        if (isEmpty(name) || isEmpty(type) || source == null) {
            throw new ApiValidationException("name, type, and source are required");
        }
        if (enumTitles != null) {
            if (enumValues == null) {
                throw new ApiValidationException("enum[] required when enum_values[] is defined");
            }
            if (enumTitles.length != enumValues.length) {
                throw new ApiValidationException("enum[] and enum_values[] must be the same length");
            }
        }
    }

    /**
     * Getter of the name of the parameter.
     *
     * @return Parameter name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the parameter type.
     *
     * @return Parameter type
     */
    public String getType() {
        return type;
    }

    /**
     * Getter of the parameter's default value.
     *
     * @return Parameter default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Getter of the parameter description.
     *
     * @return Parameter description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the parameter source.
     *
     * @return Parameter source enum
     */
    public ParameterSource getSource() {
        return source;
    }

    /**
     * Getter of the required property.
     *
     * @return Required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Getter of required enum-values.
     *
     * @return Required enum-values or {@code null}
     */
    @JsonProperty("enum")
    public String[] getEnumValues() {
        return enumValues;
    }

    /**
     * Getter of enum-titles.
     *
     * @return Enum-titles or {@code null}
     */
    @JsonProperty("options/enum_titles")
    public String[] getEnumTitles() {
        return enumTitles;
    }

    /**
     * New parameter builder.
     *
     * @return Builder
     */
    public static Builder parameter() {
        return new Builder();
    }

    /**
     * Builder to construct Parameter object.
     */
    public static final class Builder {

        private String name;
        private String type;
        private String defaultValue;
        private String description;
        private ParameterSource source;
        private boolean required;
        private String[] enumValues;
        private String[] enumTitles;

        private Builder() {
        }

        /**
         * Set the parameter name.
         *
         * @param name Parameter name
         * @return Builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets enum-values that must match.
         *
         * @param enumValues Enum-values
         * @return Builder
         */
        public Builder enumValues(String... enumValues) {
            this.enumValues = enumValues;
            return this;
        }

        /**
         * Sets enum-titles that <b>must</b> be the same length as {@link #enumValues(String[])}, if provided.
         *
         * @param enumTitles Enum-titles
         * @return Builder
         */
        public Builder enumTitles(String... enumTitles) {
            this.enumTitles = enumTitles;
            return this;
        }

        /**
         * Set the parameter type.
         *
         * @param type Parameter type
         * @return Builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the parameter default value.
         *
         * @param defaultValue If exists, the default value
         * @return builder
         */
        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Set the parameter description.
         *
         * @param description The description of the parameter
         * @return builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the parameter source.
         *
         * @param source Where the parameter comes from. May be: PATH or ADDITIONAL
         * @return builder
         */
        public Builder source(ParameterSource source) {
            this.source = source;
            return this;
        }

        /**
         * Set the required property.
         *
         * @param required Whether the parameter is required
         * @return builder
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Builds the Parameter.
         *
         * @return The parameter instance
         */
        public Parameter build() {
            return new Parameter(this);
        }
    }

}
