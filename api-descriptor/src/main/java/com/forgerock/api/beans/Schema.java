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

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Class that represents the Schema type in API descriptor.
 */
public final class Schema {

    private final Reference reference;
    private final JsonValue schema;

    /**
     * Private contstructor of the Schema.
     *
     * @param builder Builder.
     */
    private Schema(Builder builder) {
        this.reference = builder.reference;
        this.schema = builder.schema;
    }

    /**
     * Getter for reference. May be null if the schema is specified here.
     * @return The reference.
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Obtain the schema definition if it is not a reference.
     * @return The schema.
     */
    public JsonValue getSchema() {
        return schema;
    }

    /**
     * Create a new Builder for Schema.
     * @return The builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create a new Builder for Schema. A synonym for {@link #newBuilder()} that is useful for static imports.
     * @return The builder.
     */
    public static Builder schema() {
        return newBuilder();
    }

    /**
     * A builder class for {@code Schema} instances.
     */
    public static final class Builder {

        private JsonValue schema;
        private Reference reference;

        /**
         * Private default constructor.
         */
        private Builder() { }

        /**
         * Sets the schema reference.
         * @param reference The reference.
         * @return This builder.
         */
        public Builder reference(Reference reference) {
            Reject.ifNull(reference);
            this.reference = reference;
            return this;
        }

        /**
         * Sets the schema.
         * @param schema The schema.
         * @return This builder.
         */
        public Builder schema(JsonValue schema) {
            Reject.ifNull(schema);
            this.schema = schema;
            return this;
        }

        /**
         * Builds the Schema instace.
         *
         * @return Schema instace.
         */
        public Schema build() {
            Reject.ifTrue(schema == null && reference == null, "Must provide either a reference or a schema");
            return new Schema(this);
        }
    }

}
