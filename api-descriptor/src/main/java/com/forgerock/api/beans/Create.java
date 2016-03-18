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

import com.forgerock.api.enums.CreateModeEnum;
import org.forgerock.util.Reject;

/**
 * Class that represents the Create Operation type in API descriptor
 *
 */
public class Create extends Operation{

    private final CreateModeEnum mode;
    private final Boolean mvccSupported;

    /**
     * Protected contstructor of the Create
     *
     * @param builder Operation Builder
     */
    private Create(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.mvccSupported = builder.mvccSupported;
    }

    /**
     * Getter of the mode
     * @return Mode
     */
    public CreateModeEnum getMode() {
        return mode;
    }

    /**
     * Getter of mvcc supported
     * @return true if mvcc is supported
     */
    public Boolean getMvccSupported() {
        return mvccSupported;
    }

    /**
     * Creates a new builder for Create
     * @return New builder instance
     */
    public static final Builder newBuilder() {
        return new Builder();
    }

    private static final class Builder extends Operation.Builder<Builder> {

        private CreateModeEnum mode;
        private Boolean mvccSupported;

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Set the mode
         * @param mode
         * @return Builder
         */
        public Builder withMode(CreateModeEnum mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Set the if mvccSupported
         * @param mvccSupported
         * @return Builder
         */
        public Builder withMvccSupported(Boolean mvccSupported) {
            Reject.ifNull(mvccSupported);
            this.mvccSupported = mvccSupported;
            return this;
        }

        /**
         * Builds the Create instace
         *
         * @return Create instace
         */
        public Create build() {
            return new Create(this);
        }
    }

}
