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

import static org.forgerock.api.util.ValidationUtil.containsWhitespace;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.Objects;

import org.forgerock.api.ApiValidationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Class that represents the Reference type in API descriptor.
 */
@JsonDeserialize(builder = Reference.Builder.class)
public final class Reference {

    private final String value;

    private Reference(Builder builder) {
        this.value = builder.value;

        if (isEmpty(value)) {
            throw new ApiValidationException("Reference-value is required");
        }
        if (containsWhitespace(value)) {
            throw new ApiValidationException("Reference-value may not contain whitespace");
        }
    }

    /**
     * Getter of the JSON reference.
     *
     * @return value
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reference reference = (Reference) o;
        return Objects.equals(value, reference.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Create a new Builder for Reference.
     *
     * @return Builder
     */
    public static Builder reference() {
        return new Builder();
    }

    /**
     * Builder to help construct the Reference.
     */
    public static final class Builder {

        private String value;

        private Builder() {
        }

        @JsonCreator
        private Builder(String value) {
            this.value = value;
        }

        /**
         * Setter for Reference-value.
         *
         * @param value Reference-value
         * @return Builder
         */
        @JsonCreator
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds the Reference instance.
         *
         * @return Reference instance
         */
        public Reference build() {
            return new Reference(this);
        }
    }

}