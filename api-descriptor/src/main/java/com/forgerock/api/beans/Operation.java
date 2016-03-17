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

/**
 * Class that represents the Operation type in API descriptor
 *
 * @since 14.0.0
 */
public class Operation {

    private final Context supportedContext;
    private final String[] supportedLocals;
    private final String[] fields;
    private final Error[] errors;
    private final Parameter[] parameters;

    /**
     * Protected contstructor of the Operation
     * @param builder Operation Builder
     */
    protected Operation(Builder builder) {
        this.supportedContext = builder.supportedContext;
        this.supportedLocals = builder.supportedLocals;
        this.fields = builder.fields;
        this.errors = builder.errors;
        this.parameters = builder.parameters;
    }

    /**
     * Getter of the supported context
     * @return Supported context
     */
    public Context getSupportedContext() {
        return supportedContext;
    }

    /**
     * Getter of the supported locals array
     * @return Supported locals
     */
    public String[] getSupportedLocals() {
        return supportedLocals;
    }

    /**
     * Getter of the fields array
     * @return Fields
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * Getter of the errors array
     * @return Errors
     */
    public Error[] getErrors() {
        return errors;
    }

    /**
     * Getter of the parameters array
     * @return Parameters
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * Builder to help construct the Operation
     */
    protected abstract static class Builder<T extends Builder<T>> {

        private Context supportedContext;
        private String[] supportedLocals;
        private String[] fields;
        private Error[] errors;
        private Parameter[] parameters;

        protected Builder() {
            //default private constructor
        }

        protected abstract T self();

        /**
         * Set the supported context
         * @param supportedContext
         * @return Builder
         */
        public Builder withSupportedContext(Context supportedContext) {
            this.supportedContext = supportedContext;
            return self();
        }

        /**
         * Set the supported context
         * @param supportedLocals
         * @return Builder
         */
        public Builder withSupportedLocals(String[] supportedLocals) {
            this.supportedLocals = supportedLocals;
            return self();
        }

        /**
         * Set the supported context
         * @param fields
         * @return Builder
         */
        public Builder withFields(String[] fields) {
            this.fields = fields;
            return self();
        }

        /**
         * Set the supported context
         * @param errors
         * @return Builder
         */
        public Builder withErrors(Error[] errors) {
            this.errors = errors;
            return self();
        }

        /**
         * Set the supported context
         * @param parameters
         * @return Builder
         */
        public Builder withParameters(Parameter[] parameters) {
            this.parameters = parameters;
            return self();
        }


    }

}
