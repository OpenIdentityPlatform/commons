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

import static com.forgerock.api.util.ValidationUtil.containsWhitespace;
import static com.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.forgerock.api.ApiValidationException;
import org.forgerock.util.Reject;

/**
 * Class that represents API descriptor {@link Error} errors.
 */
public final class Errors {

    private final Map<String, Error> errors;

    private Errors(Builder builder) {
        this.errors = builder.errors;

        if (errors.isEmpty()) {
            throw new ApiValidationException("Must have at least one error definition");
        }
    }

    /**
     * Gets a {@code Map} of error-names to {@link Error}s. This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of error-names to {@link Error}s.
     */
    @JsonValue
    protected Map<String, Error> getErrors() {
        return errors;
    }

    /**
     * Gets the {@link Error} for a given Error-name.
     *
     * @param name Error name
     * @return {@link Error} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public Error get(String name) {
        return errors.get(name);
    }

    /**
     * Returns all {@link Error} names.
     *
     * @return All {@link Error} names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return errors.keySet();
    }

    /**
     * Create a new Builder for Errors.
     *
     * @return Builder
     */
    public static Builder errors() {
        return new Builder();
    }

    /**
     * Builder to help construct the Errors.
     */
    public static final class Builder {

        private final Map<String, Error> errors = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a {@link Error}.
         *
         * @param name Error name
         * @param error {@link Error}
         * @return Builder
         */
        public Builder put(String name, Error error) {
            if (isEmpty(name) || containsWhitespace(name)) {
                throw new IllegalArgumentException("name required and may not contain whitespace");
            }
            if (errors.containsKey(name)) {
                throw new IllegalStateException("name not unique");
            }
            errors.put(name, Reject.checkNotNull(error));
            return this;
        }

        /**
         * Builds the Errors instance.
         *
         * @return Errors instance
         */
        public Errors build() {
            return new Errors(this);
        }
    }

}