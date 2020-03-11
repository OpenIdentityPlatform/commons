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

import static org.forgerock.api.jackson.JacksonUtils.OBJECT_MAPPER;
import static org.forgerock.api.jackson.JacksonUtils.schemaFor;
import static org.forgerock.json.JsonValue.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.forgerock.api.jackson.JacksonUtils;
import com.google.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

/**
 * Class that represents the Schema type in API descriptor.
 */
@JsonDeserialize(builder = Schema.Builder.class)
public abstract class Schema {

    private Schema() {
        // This class only has two private inner sub-classes, so constructor is private.
    }

    /**
     * Getter for reference. May be null if the schema is specified here.
     * @return The reference.
     */
    public abstract Reference getReference();

    /**
     * Obtain the schema definition if it is not a reference.
     * @return The schema.
     */
    public abstract JsonValue getSchema();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Schema schema1 = (Schema) o;
        return Objects.equals(getReference(), schema1.getReference())
                && isSchemaPropertyMatches(schema1);

    }

    private boolean isSchemaPropertyMatches(Schema schema1) {
        return getSchema() != null && schema1.getSchema() != null
                ? getSchema().isEqualTo(schema1.getSchema())
                : schema1.getSchema() == getSchema();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReference(), getSchema());
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
     * Builds Schema object from the data in the annotation parameter. If the {@code schema} has an {@code id} defined,
     * or if the type being used for the schema definition has an {@code id} defined, the schema will be defined in the
     * top-level {@code descriptor}, and a reference to that definition will be returned.
     *
     * @param schema The annotation that holds the data
     * @param descriptor The root descriptor to add definitions to.
     * @param relativeType The type relative to which schema resources should be resolved.
     * @return Schema instance
     */
    public static Schema fromAnnotation(org.forgerock.api.annotations.Schema schema, ApiDescription descriptor,
            final Class<?> relativeType) {
        Class<?> type = schema.fromType();
        if (type.equals(Void.class) && Strings.isNullOrEmpty(schema.schemaResource())) {
            return null;
        }
        Builder builder = schema();
        String id = schema.id();
        if (!type.equals(Void.class)) {
            // the annotation declares a type to use as the schema
            builder.type(type);
            if (Strings.isNullOrEmpty(id)) {
                // if the schema annotation passed to this method does not have an id, check to see if the type being
                // used to generate the schema is annotated with an id.
                org.forgerock.api.annotations.Schema typeSchema =
                        type.getAnnotation(org.forgerock.api.annotations.Schema.class);
                if (typeSchema != null && !Strings.isNullOrEmpty(typeSchema.id())) {
                    id = typeSchema.id();
                }
            }
        } else {
            // not using a type, so must be using a resource file containing JSON Schema json.
            InputStream resource = relativeType.getResourceAsStream(schema.schemaResource());
            try {
                JsonValue json = json(JacksonUtils.OBJECT_MAPPER.readValue(resource, Object.class))
                        .as(new TranslateJsonSchema(relativeType.getClassLoader()));
                builder.schema(json);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read declared resource " + schema.schemaResource(), e);
            }
        }
        if (!Strings.isNullOrEmpty(id)) {
            // we've got an id for this schema, so define it at the top level and return a reference.
            descriptor.addDefinition(id, builder.build());
            return schema().reference(Reference.reference().value("#/definitions/" + id).build()).build();
        } else {
            return builder.build();
        }
    }

    /**
     * A builder class for {@code Schema} instances.
     */
    public static final class Builder {

        private JsonValue schema;
        private Reference reference;
        private Map<String, Object> jsonValueObject = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() { }

        /**
         * Sets the schema reference.
         * @param reference The reference.
         * @return This builder.
         */
        @JsonProperty("$ref")
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
         * Sets the schema by json key-value pairs.
         * @param key json parameter name
         * @param value json parametr value
         * @return This builder.
         */
        @JsonAnySetter
        public Builder schema(String key, Object value) {
            jsonValueObject.put(key, value);
            return this;
        }

        /**
         * Sets the schema.
         * @param type The type to derive the schema from.
         * @return This builder.
         */
        public Builder type(Class<?> type) {
            Reject.ifNull(type);
            try {
                JsonSchema jsonSchema = schemaFor(type);
                String schemaString = OBJECT_MAPPER.writer().writeValueAsString(jsonSchema);
                this.schema = json(OBJECT_MAPPER.readValue(schemaString, Object.class))
                        .as(new TranslateJsonSchema(type.getClassLoader()));
            } catch (JsonMappingException e) {
                throw new IllegalArgumentException(e);
            } catch (IOException e) {
                throw new IllegalStateException("Jackson cannot read its own JSON", e);
            }
            return this;
        }

        /**
         * Builds the Schema instance.
         *
         * @return Schema instance.
         */
        public Schema build() {
            if (!jsonValueObject.isEmpty()) {
                schema(json(jsonValueObject));
            }
            return reference == null ? new SchemaSchema(schema) : new ReferenceSchema(reference);
        }
    }

    private static final class ReferenceSchema extends Schema {

        private final Reference reference;

        private ReferenceSchema(Reference reference) {
            this.reference = reference;
        }

        @Override
        @JsonProperty("$ref")
        public Reference getReference() {
            return reference;
        }

        @Override
        @JsonIgnore
        public JsonValue getSchema() {
            return null;
        }
    }

    private static final class SchemaSchema extends Schema {

        private final JsonValue schema;

        private SchemaSchema(JsonValue schema) {
            this.schema = schema;
        }

        @Override
        @JsonIgnore
        public Reference getReference() {
            return null;
        }

        @Override
        @com.fasterxml.jackson.annotation.JsonValue
        public JsonValue getSchema() {
            return schema;
        }
    }
}
