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

import org.forgerock.util.Reject;

/**
 * Class that represents the Delete operation type in API descriptor
 *
 */
public class Delete extends Operation{

    private final Boolean mvccSupported;

    /**
     * Protected contstructor of the Delete
     *
     * @param builder Delete Builder
     */
    private Delete(Builder builder) {
        super(builder);
        this.mvccSupported = builder.mvccSupported;
    }

    /**
     * Getter of the mvcc supported parameter
     * @return true if mvcc is supported
     */
    public Boolean getMvccSupported() {
        return mvccSupported;
    }

    /**
     * Creates a new builder for Delete
     * @return New builder instance
     */
    public static final Builder newBuilder(Boolean mvccSupported) {
        Reject.ifNull(mvccSupported);
        return new Builder(mvccSupported);
    }

    public static final class Builder extends Operation.Builder<Builder> {

        private Boolean mvccSupported;

        /**
         * Private constructor with the required parameter
         * @param mvccSupported
         */
        private Builder(Boolean mvccSupported) {
            super();
            this.mvccSupported = mvccSupported;
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the Delete instace
         *
         * @return Delete instace
         */
        public Delete build() {
            return new Delete(this);
        }
    }

}
