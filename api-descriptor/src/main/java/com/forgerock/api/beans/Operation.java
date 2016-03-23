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

import java.util.List;

/**
 * Class that represents the Operation type in API descriptor.
 *
 */
public abstract class Operation {

    private final Context supportedContext;
    private final String[] supportedLocales;
    private final String[] fields;
    private final List<Error> errors;
    private final Parameter[] parameters;

    /**
     * Protected contstructor of the Operation.
     * @param builder Operation Builder
     */
    protected Operation(Builder builder) {
        this.supportedContext = builder.supportedContext;
        this.supportedLocales = builder.supportedLocales;
        this.fields = builder.fields;
        this.errors = builder.errors;
        this.parameters = builder.parameters;
    }

    /**
     * Getter of the supported context.
     * @return Supported context
     */
    public Context getSupportedContext() {
        return supportedContext;
    }

    /**
     * Getter of the supported locales array.
     * @return Supported locales
     */
    public String[] getSupportedLocales() {
        return supportedLocales;
    }

    /**
     * Getter of the fields array.
     * @return Fields
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * Getter of the errors array.
     * @return Errors
     */
    public List<Error> getErrors() {
        return errors;
    }

    /**
     * Getter of the parameters array.
     * @return Parameters
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * Allocates the operation by operation type to the given Resource Builder
     * by calling the corresonding method by type.
     * @param resourceBuilder - Resource Builder to add the operation
     */
    protected abstract void allocateToResource(Resource.Builder resourceBuilder);

    /**
     * Builder to help construct the Operation.
     */
    public abstract static class Builder<T extends Builder<T>> {

        private Context supportedContext;
        private String[] supportedLocales;
        private String[] fields;
        private List<Error> errors;
        private Parameter[] parameters;

        /**
         * Default protected constructor.
         */
        protected Builder() {
            //default private constructor
        }

        /**
         * Abstract method that returns the instantiated Builder itself.
         * @return Builder
         */
        protected abstract T self();

        /**
         * Set the supported context.
         * @param supportedContext The supported contexts
         * @return Builder
         */
        public T supportedContext(Context supportedContext) {
            this.supportedContext = supportedContext;
            return self();
        }

        /**
         * Set the supported context.
         * @param supportedlocales Locales codes supported by the operation
         * @return Builder
         */
        public T supportedLocales(String[] supportedlocales) {
            this.supportedLocales = supportedlocales;
            return self();
        }

        /**
         * Set the supported context.
         * @param fields The fields that can be selected for returning in the response payload
         * @return Builder
         */
        public T fields(String[] fields) {
            this.fields = fields;
            return self();
        }

        /**
         * Set the supported context.
         * @param errors What errors may be returned by this operation
         * @return Builder
         */
        public T errors(List<Error> errors) {
            this.errors = errors;
            return self();
        }

        /**
         * Set the supported context.
         * @param parameters Extra parameters supported by the operation
         * @return Builder
         */
        public T parameters(Parameter[] parameters) {
            this.parameters = parameters;
            return self();
        }

    }

}
