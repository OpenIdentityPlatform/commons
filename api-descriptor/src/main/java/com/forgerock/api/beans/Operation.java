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

import java.util.ArrayList;
import java.util.List;

import com.forgerock.api.enums.Stability;

/**
 * Class that represents the Operation type in API descriptor.
 */
public abstract class Operation {

    private final String description;
    private final Context[] supportedContexts;
    private final String[] supportedLocales;
    private final Error[] errors;
    private final Parameter[] parameters;
    private final Stability stability;

    /**
     * Protected constructor of the Operation.
     *
     * @param builder Operation Builder
     */
    protected Operation(Builder builder) {
        this.description = builder.description;
        this.supportedLocales = builder.supportedLocales;
        this.stability = builder.stability;

        // NOTE: had to use local variables for lists or else type-inference was getting confused
        final List<Context> supportedContexts = builder.supportedContexts;
        this.supportedContexts = supportedContexts.toArray(new Context[supportedContexts.size()]);

        final List<Error> errors = builder.errors;
        this.errors = errors.toArray(new Error[errors.size()]);

        final List<Parameter> parameters = builder.parameters;
        this.parameters = parameters.toArray(new Parameter[parameters.size()]);
    }

    /**
     * Getter of the description.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter of the supported contexts.
     *
     * @return Supported contexts
     */
    public Context[] getSupportedContexts() {
        return supportedContexts;
    }

    /**
     * Getter of the supported locales array.
     *
     * @return Supported locales
     */
    public String[] getSupportedLocales() {
        return supportedLocales;
    }

    /**
     * Getter of the errors array.
     *
     * @return Errors
     */
    public Error[] getErrors() {
        return errors;
    }

    /**
     * Getter of the parameters array.
     *
     * @return Parameters
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * Getter of Operation stability.
     *
     * @return Stability or {@code null} which suggests {@link Stability#STABLE} (default).
     */
    public Stability getStability() {
        return stability;
    }

    /**
     * Allocates the operation by operation type to the given Resource Builder
     * by calling the corresonding method by type.
     *
     * @param resourceBuilder - Resource Builder to add the operation
     */
    protected abstract void allocateToResource(Resource.Builder resourceBuilder);

    /**
     * Builder to help construct the Operation.
     */
    public abstract static class Builder<T extends Builder<T>> {

        private String description;
        private final List<Context> supportedContexts;
        private String[] supportedLocales;
        private final List<Error> errors;
        private final List<Parameter> parameters;
        private Stability stability;

        /**
         * Creates a new Builder.
         */
        protected Builder() {
            supportedContexts = new ArrayList<>();
            errors = new ArrayList<>();
            parameters = new ArrayList<>();
        }

        /**
         * Abstract method that returns the instantiated Builder itself.
         *
         * @return Builder
         */
        protected abstract T self();

        /**
         * Set the description.
         *
         * @param description A description of the endpoint
         * @return Builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets multiple supported contexts.
         *
         * @param supportedContexts The supported contexts
         * @return Builder
         */
        public T supportedContexts(List<Context> supportedContexts) {
            this.supportedContexts.addAll(supportedContexts);
            return self();
        }

        /**
         * Set a single supported context.
         *
         * @param supportedContext The supported context
         * @return Builder
         */
        public T supportedContext(Context supportedContext) {
            this.supportedContexts.add(supportedContext);
            return self();
        }

        /**
         * Set the supported locale.
         *
         * @param supportedlocales Locales codes supported by the operation
         * @return Builder
         */
        public T supportedLocales(String[] supportedlocales) {
            this.supportedLocales = supportedlocales;
            return self();
        }

        /**
         * Set multiple supported errors.
         *
         * @param errors What errors may be returned by this operation
         * @return Builder
         */
        public T errors(List<Error> errors) {
            this.errors.addAll(errors);
            return self();
        }

        /**
         * Sets a single supported error.
         *
         * @param error An error that may be returned by this operation
         * @return Builder
         */
        public T error(Error error) {
            this.errors.add(error);
            return self();
        }

        /**
         * Set multiple supported parameters.
         *
         * @param parameters Extra parameters supported by the operation
         * @return Builder
         */
        public T parameters(List<Parameter> parameters) {
            this.parameters.addAll(parameters);
            return self();
        }

        /**
         * Sets a single supported parameters.
         *
         * @param parameter Extra parameter supported by the operation
         * @return Builder
         */
        public T parameter(Parameter parameter) {
            this.parameters.add(parameter);
            return self();
        }

        /**
         * Sets stability of Operation.
         *
         * @param stability Stability
         * @return Builder
         */
        public T stability(Stability stability) {
            this.stability = stability;
            return self();
        }
    }

}
