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
 * Class that represents the Read Operation type in API descriptor
 *
 */
public class Read extends Operation{

    /**
     * Protected contstructor of the Operation
     *
     * @param builder Operation Builder
     */
    private Read(Builder builder) {
        super(builder);
    }

    /**
     * Creates a new builder for Operation
     * @return New builder instance
     */
    public static final Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder extends Operation.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Create instace
         *
         * @return Create instace
         */
        public Read build() {
            return new Read(this);
        }
    }

}
