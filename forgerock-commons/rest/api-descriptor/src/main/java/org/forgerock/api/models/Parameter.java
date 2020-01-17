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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.ApiValidationException;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.util.i18n.LocalizableString;

/**
 * Class that represents the Parameter type in API descriptor.
 */
@JsonDeserialize(builder = Parameter.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class Parameter {

    private final String name;
    private final String type;
    private final String defaultValue; //Todo String?
    private final LocalizableString description;
    private final ParameterSource source;
    private final Boolean required;
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
    public LocalizableString getDescription() {
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
    public Boolean isRequired() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Parameter parameter = (Parameter) o;
        return required == parameter.required
                && Objects.equals(name, parameter.name)
                && Objects.equals(type, parameter.type)
                && Objects.equals(defaultValue, parameter.defaultValue)
                && Objects.equals(description, parameter.description)
                && source == parameter.source
                && Arrays.equals(enumValues, parameter.enumValues)
                && Arrays.equals(enumTitles, parameter.enumTitles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, defaultValue, description, source, required, enumValues, enumTitles);
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
     * Builds a Parameter object from the data in the annotation.
     * @param type The type to resolve {@link LocalizableString}s from.
     * @param parameter The annotation that holds the data
     * @return Parameter instance
     */
    public static Parameter fromAnnotation(Class<?> type, org.forgerock.api.annotations.Parameter parameter) {
        return parameter()
                .description(new LocalizableString(parameter.description(), type))
                .defaultValue(parameter.defaultValue())
                .enumValues(parameter.enumValues())
                .enumTitles(parameter.enumTitles())
                .required(parameter.required())
                .name(parameter.name())
                .source(parameter.source())
                .type(parameter.type())
                .build();
    }

    /**
     * Builder to construct Parameter object.
     */
    public static final class Builder {

        private String name;
        private String type;
        private String defaultValue;
        private LocalizableString description;
        private ParameterSource source;
        private Boolean required;
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
        @JsonProperty("name")
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
        @JsonProperty("enum")
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
        @JsonProperty("options/enum_titles")
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
        @JsonProperty("type")
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
        @JsonProperty("defaultValue")
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
        public Builder description(LocalizableString description) {
            this.description = description;
            return this;
        }

        /**
         * Set the parameter description.
         *
         * @param description The description of the parameter
         * @return builder
         */
        @JsonProperty("description")
        public Builder description(String description) {
            this.description = new LocalizableString(description);
            return this;
        }

        /**
         * Set the parameter source.
         *
         * @param source Where the parameter comes from. May be: PATH or ADDITIONAL
         * @return builder
         */
        @JsonProperty("source")
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
        @JsonProperty("required")
        public Builder required(Boolean required) {
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
