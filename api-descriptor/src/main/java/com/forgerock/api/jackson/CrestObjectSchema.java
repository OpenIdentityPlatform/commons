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

package com.forgerock.api.jackson;

import static org.forgerock.json.JsonValue.json;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.validation.ValidationException;

import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code ObjectSchema} that includes the custom CREST JSON Schema attributes.
 */
public class CrestObjectSchema extends ObjectSchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema,
        ValidatableSchema {
    private WritePolicy writePolicy;
    private Boolean errorOnWritePolicyFailure;
    private Integer propertyOrder;

    @Override
    public WritePolicy getWritePolicy() {
        return writePolicy;
    }

    @Override
    public void setWritePolicy(WritePolicy policy) {
        this.writePolicy = policy;
    }

    @Override
    public Boolean getErrorOnWritePolicyFailure() {
        return errorOnWritePolicyFailure;
    }

    @Override
    public void setErrorOnWritePolicyFailure(Boolean errorOnWritePolicyFailure) {
        this.errorOnWritePolicyFailure = errorOnWritePolicyFailure;
    }

    @Override
    public Integer getPropertyOrder() {
        return propertyOrder;
    }

    @Override
    public void setPropertyOrder(Integer order) {
        this.propertyOrder = order;
    }

    @Override
    public void validate(JsonValue object) throws ValidationException {
        if (!object.isMap()) {
            throw new ValidationException("Object expected, but got: " + object.getObject());
        }
        Map<String, Object> propertyValues = new HashMap<>((Map<String, Object>) object.getObject());
        Iterator<Map.Entry<String, Object>> propertyIterator = propertyValues.entrySet().iterator();
        Map<Pattern, JsonSchema> patternProperties = new HashMap<>();
        if (getPatternProperties() != null) {
            for (Map.Entry<String, JsonSchema> pattern : getPatternProperties().entrySet()) {
                patternProperties.put(Pattern.compile(pattern.getKey()), pattern.getValue());
            }
        }
        while (propertyIterator.hasNext()) {
            Map.Entry<String, Object> property = propertyIterator.next();
            boolean validated = false;
            if (getProperties().containsKey(property.getKey())) {
                ((ValidatableSchema) getProperties().get(property.getKey())).validate(json(property.getValue()));
                validated = true;
            }
            for (Map.Entry<Pattern, JsonSchema> pattern : patternProperties.entrySet()) {
                if (pattern.getKey().matcher(property.getKey()).matches()) {
                    ((ValidatableSchema) pattern.getValue()).validate(json(property.getValue()));
                    validated = true;
                }
            }
            if (validated) {
                propertyIterator.remove();
            }
        }
        AdditionalProperties additionalProperties = getAdditionalProperties();
        if (additionalProperties != null) {
            if (additionalProperties instanceof NoAdditionalProperties && !propertyValues.isEmpty()) {
                throw new ValidationException("Did not expect additional properties, but got " + propertyValues);
            }
            SchemaAdditionalProperties schemaAdditionalProperties = (SchemaAdditionalProperties) additionalProperties;
            for (Object value : propertyValues.values()) {
                ((ValidatableSchema) schemaAdditionalProperties.getJsonSchema()).validate(json(object));
            }
        }
    }
}
