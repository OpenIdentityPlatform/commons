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

import static org.forgerock.api.util.ValidationUtil.*;
import static org.forgerock.util.Reject.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Class that represents API descriptor {@link Schema} definitions.
 */
@JsonDeserialize(builder = Definitions.Builder.class)
public final class Definitions {

    private final Map<String, Schema> definitions;

    private Definitions(Builder builder) {
        this.definitions = builder.definitions;
    }

    /**
     * Gets a {@code Map} of schema-names to {@link Schema}s. This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of schema-names to {@link Schema}s.
     */
    @JsonValue
    protected Map<String, Schema> getDefinitions() {
        return definitions;
    }

    /**
     * Gets the {@link Schema} for a given Schema-name.
     *
     * @param name Schema name
     * @return {@link Schema} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public Schema get(String name) {
        return definitions.get(name);
    }

    /**
     * Returns all {@link Schema} names.
     *
     * @return All {@link Schema} names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return definitions.keySet();
    }

    /**
     * Create a new Builder for Definitions.
     *
     * @return Builder
     */
    public static Builder definitions() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Definitions that = (Definitions) o;
        return Objects.equals(definitions, that.definitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitions);
    }

    /**
     * Builder to help construct the Definitions.
     */
    public static final class Builder {

        private final Map<String, Schema> definitions = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a {@link Schema}.
         *
         * @param name Schema name
         * @param schema {@link Schema}
         * @return Builder
         */
        @JsonAnySetter
        public Builder put(String name, Schema schema) {
            if (isEmpty(name) || containsWhitespace(name)) {
                throw new IllegalArgumentException(
                        "Schema name required and may not contain whitespace, current value: '" + name + "'");
            }
            if (definitions.containsKey(name) && !definitions.get(name).equals(schema)) {
                throw new IllegalStateException("The given Schema name"
                        + " '" + name + "' already exists but the Schema objects are not equal");
            }

            definitions.put(name, checkNotNull(schema));
            return this;
        }

        /**
         * Builds the Definitions instance.
         *
         * @return Definitions instance
         */
        public Definitions build() {
            return new Definitions(this);
        }
    }

}