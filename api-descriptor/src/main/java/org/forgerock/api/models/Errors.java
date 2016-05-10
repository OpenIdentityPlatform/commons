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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Class that represents API descriptor {@link ApiError} errors.
 */
public final class Errors {

    /**
     * {@link ApiError} {@code Map}-entry {@link Comparator}, which sorts by code and description.
     */
    public static final ErrorEntryComparator ERROR_ENTRY_COMPARATOR = new ErrorEntryComparator();

    private final Map<String, ApiError> errors;

    private Errors(Builder builder) {
        this.errors = builder.errors;
    }

    /**
     * Gets a {@code Map} of error-names to {@link ApiError}s.
     *
     * @return {@code Map} of error-names to {@link ApiError}s.
     */
    @JsonValue
    public Map<String, ApiError> getErrors() {
        return errors;
    }

    /**
     * Gets the {@link ApiError} for a given ApiError-name.
     *
     * @param name ApiError name
     * @return {@link ApiError} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public ApiError get(String name) {
        return errors.get(name);
    }

    /**
     * Returns all {@link ApiError} names.
     *
     * @return All {@link ApiError} names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return errors.keySet();
    }

    /**
     * This allows the models package to mutate the errors defined here. This is used when processing annotations on
     * resources that may reference errors using an id, so those errors need to be defined here rather than in-line in
     * the resource descriptions.
     *
     * @param id The error id.
     * @param apiError The error definition.
     * @see ApiError#fromAnnotation(org.forgerock.api.annotations.ApiError, ApiDescription, Class)
     */
    void addError(String id, ApiError apiError) {
        if (apiError.getReference() != null) {
            throw new IllegalArgumentException("Cannot define an apiError using a reference");
        }
        ApiError defined = errors.get(id);
        if (defined != null && !defined.equals(apiError)) {
            throw new IllegalArgumentException("Trying to redefine already defined apiError, " + id);
        }
        errors.put(id, apiError);
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

        private final Map<String, ApiError> errors = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a {@link ApiError}.
         *
         * @param name Error name
         * @param apiError {@link ApiError}
         * @return Builder
         */
        public Builder put(String name, ApiError apiError) {
            if (isEmpty(name) || containsWhitespace(name)) {
                throw new IllegalArgumentException("name required and may not contain whitespace");
            }
            if (errors.containsKey(name)) {
                throw new IllegalStateException("name not unique");
            }
            errors.put(name, Reject.checkNotNull(apiError));
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

    /**
     * {@link ApiError} {@code Map}-entry {@link Comparator}, which sorts by code and description.
     * This {@code Comparator} does not handle {@code null} values or duplicates,
     * because those conditions should never occur in practice.
     * <p>
     * This class is thread-safe.
     * </p>
     */
    private static class ErrorEntryComparator implements Comparator<Map.Entry<String, ApiError>> {
        @Override
        public int compare(final Map.Entry<String, ApiError> o1, final Map.Entry<String, ApiError> o2) {
            final int codeCompare = Integer.compare(o1.getValue().getCode(), o2.getValue().getCode());
            if (codeCompare == 0) {
                return o1.getValue().getDescription().compareTo(o2.getValue().getDescription());
            }
            return codeCompare;
        }
    }

}