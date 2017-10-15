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

package org.forgerock.api.jackson;

import static org.forgerock.json.JsonValue.*;

import java.io.IOException;
import java.util.Set;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

/**
 * Some utilities for dealing with Jackson schemas.
 */
public final class JacksonUtils {

    /**
     * A public static {@code ObjectMapper} instance, so that they do not have to be instantiated all over the place,
     * as they are expensive to construct. Note that the {@code SerializationFeature.WRITE_DATES_AS_TIMESTAMPS}
     * option is <em>disabled</em>, so that dates will be in JSON Schema v4 format (e.g., "type":"string",
     * "format":"date-time").
     */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Validate that the provided JSON conforms to the schema.
     *
     * @param json JSON content.
     * @param schema The schema. Must be an instance of one of the extended schema classes in this package.
     * @return {@code true} if schema implements {@link ValidatableSchema} and was validated and {@code false} otherwise
     * @throws ValidationException If the JSON does not conform to the schema.
     */
    public static boolean validateJsonToSchema(String json, JsonSchema schema) throws ValidationException {
        if (schema instanceof ValidatableSchema) {
            try {
                ((ValidatableSchema) schema).validate(json(OBJECT_MAPPER.readValue(json, Object.class)));
                return true;
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot parse JSON", e);
            }
        }
        return false;
    }

    /**
     * Obtain the JsonSchema for a type, using the extended schema classes that are in this package.
     *
     * @param type The class to get a schema for.
     * @return The schema.
     * @throws JsonMappingException If the type cannot be mapped to a schema by Jackson.
     */
    public static JsonSchema schemaFor(Class<?> type) throws JsonMappingException {
        CrestPropertyDetailsSchemaFactoryWrapper visitor = new CrestPropertyDetailsSchemaFactoryWrapper();
        OBJECT_MAPPER.acceptJsonFormatVisitor(type, visitor);
        return visitor.finalSchema();
    }

    /**
     * Validate that a value falls within the enums specified.
     * @param enums The enums (may be empty).
     * @param value The value.
     * @throws ValidationException When the value is not one of the specified enums.
     */
    static void validateEnum(Set<String> enums, String value) throws ValidationException {
        if (enums != null && !enums.isEmpty() && !enums.contains(value)) {
            throw new ValidationException("Value " + value + " is not in enums " + enums);
        }
    }

    /**
     * Validates the the format is valid for a number value.
     *
     * @param format The format, if specified.
     * @throws ValidationException When the format is not valid for a number value.
     */
    static void validateFormatForNumber(JsonValueFormat format) throws ValidationException {
        if (format != null && format != JsonValueFormat.UTC_MILLISEC) {
            throw new ValidationException("Expected format " + format + " but got a number");
        }
    }

    /**
     * Validate the maximum and minimum values of a number.
     *
     * @param number The number.
     * @param maximum The maximum, if set.
     * @param exclusiveMaximum Whether the maximum is exclusive.
     * @param minimum The minimum, if set.
     * @param exclusiveMinimum Whether the minimum is exclusive.
     * @throws ValidationException When the number does not fall within the restrictions specified.
     */
    static void validateMaximumAndMinimum(Number number, Double maximum, Boolean exclusiveMaximum,
            Double minimum, Boolean exclusiveMinimum) throws ValidationException {
        double value = number.doubleValue();
        if (maximum != null) {
            if (exclusiveMaximum != null && exclusiveMaximum && value >= maximum) {
                throw new ValidationException("Number value is too large - exclusive maximum is " + maximum
                        + ", but got " + value);
            } else if ((exclusiveMaximum == null || !exclusiveMaximum) && value > maximum) {
                throw new ValidationException("Number value is too large - maximum is " + maximum
                        + ", but got " + value);
            }
        }
        if (minimum != null) {
            if (exclusiveMinimum != null && exclusiveMinimum && value >= minimum) {
                throw new ValidationException("Number value is too small - exclusive minimum is " + minimum
                        + ", but got " + value);
            } else if ((exclusiveMinimum == null || !exclusiveMinimum) && value > minimum) {
                throw new ValidationException("Number value is too small - minimum is " + minimum
                        + ", but got " + value);
            }
        }
    }

    private JacksonUtils() {
        // utils class.
    }
}
