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

import com.forgerock.api.enums.WritePolicyEnum;
import org.forgerock.util.Reject;

import java.util.Map;

/**
 * Class that represents the Schema type in API descriptor
 *
 */
public class Schema {
    //Todo clarify

    private final Reference reference;
    private final Integer partyOrder;
    private final WritePolicyEnum writePolicyEnum;
    private final Boolean errorOnWritePolicyFailure;
    private final Map<String, String> enumKeysValues; //TODO clarify

    /**
     * private contstructor of the Schema
     *
     * @param builder Operation Builder
     */
    private Schema (Builder builder) {
        this.reference = builder.reference;
        this.partyOrder = builder.partyOrder;
        this.writePolicyEnum = builder.writePolicyEnum;
        this.errorOnWritePolicyFailure = builder.errorOnWritePolicyFailure;
        this.enumKeysValues = builder.enumKeysValues;
    }

    /**
     * Getter for reference
     * @return Reference
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Getter for party order
     * @return PartyOrder
     */
    public Integer getPartyOrder() {
        return partyOrder;
    }

    /**
     * Getter for write policy
     * @return WritePolicyEnum
     */
    public WritePolicyEnum getWritePolicyEnum() {
        return writePolicyEnum;
    }

    /**
     * Getter for error on write policy failure flag
     * @return true if the error on write policy is set
     */
    public Boolean isErrorOnWritePolicyFailure() {
        return errorOnWritePolicyFailure;
    }

    /**
     * Getter for enum keys and values as a map used in the JSON
     * @return EnumKeysValues
     */
    public Map<String, String> getEnumKeysValues() {
        return enumKeysValues;
    }

    /**
     * Create a new Builder for Schema using the reference parameter only
     *
     * @return Builder
     */
    private static Builder newBuilder(Reference reference) {
        return new Builder(reference);
    }

    /**
     * Create a new Builder for Schema using JSON schema and Forgerock extensions
     *
     * @return Builder
     */
    private static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Reference reference;
        private Integer partyOrder;
        private WritePolicyEnum writePolicyEnum;
        private Boolean errorOnWritePolicyFailure;
        private Map<String, String> enumKeysValues; //TODO clarify

        /**
         * Private default constructor with the mandatory fields
         */
        private Builder(Reference reference) {
            this.reference = reference;
        }

        /**
         * Private default constructor with the mandatory fields
         */
        private Builder() { }

        /**
         * Set the party order
         * @param partyOrder
         * @return Builder
         */
        public Builder withPartyOrder(Integer partyOrder) {
            this.partyOrder = partyOrder;
            return this;
        }

        /**
         * Set the write policy
         * @param writePolicyEnum
         * @return WritePolicyEnum
         */
        public Builder withWritePolicyEnum(WritePolicyEnum writePolicyEnum) {
            this.writePolicyEnum = writePolicyEnum;
            return this;
        }

        /**
         * Set the error on write policy failure flag
         * @param errorOnWritePolicyFailure
         * @return Builder
         */
        public Builder withErrorOnWritePolicyFailure(Boolean errorOnWritePolicyFailure) {
            Reject.ifNull(errorOnWritePolicyFailure);
            this.errorOnWritePolicyFailure = errorOnWritePolicyFailure;
            return this;
        }

        /**
         * Set the enum values and titles as a map
         * @param enumKeysValues
         * @return Builder
         */
        public Builder withEnumKeysValues(Map<String, String> enumKeysValues) {
            this.enumKeysValues = enumKeysValues;
            return this;
        }

        /**
         * Builds the Schema instace
         *
         * @return Schema instace
         */
        public Schema build() {
            return new Schema(this);
        }
    }

}
