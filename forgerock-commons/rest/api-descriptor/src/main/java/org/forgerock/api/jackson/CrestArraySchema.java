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

import static org.forgerock.api.jackson.JacksonUtils.OBJECT_MAPPER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.validation.ValidationException;

import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import org.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code ArraySchema} that includes the custom CREST JSON Schema attributes.
 */
public class CrestArraySchema extends ArraySchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema,
        ValidatableSchema, WithExampleSchema<List<Object>> {
    private static final JavaType EXAMPLE_VALUE_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructParametrizedType(ArrayList.class, List.class, Object.class);

    private WritePolicy writePolicy;
    private ReadPolicy readPolicy;
    private Boolean errorOnWritePolicyFailure;
    private Boolean returnOnDemand;
    private Integer propertyOrder;
    private List<Object> example;

    @Override
    public WritePolicy getWritePolicy() {
        return writePolicy;
    }

    @Override
    public void setWritePolicy(WritePolicy policy) {
        this.writePolicy = policy;
    }

    @Override
    public ReadPolicy getReadPolicy() {
        return readPolicy;
    }

    @Override
    public void setReadPolicy(ReadPolicy readPolicy) {
        this.readPolicy = readPolicy;
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
    public Boolean getReturnOnDemand() {
        return returnOnDemand;
    }

    @Override
    public void setReturnOnDemand(Boolean returnOnDemand) {
        this.returnOnDemand = returnOnDemand;
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
        if (!object.isCollection()) {
            throw new ValidationException("Array expected, but got: " + object.getObject());
        }
        if (maxItems != null && object.size() > maxItems) {
            throw new ValidationException("Array has too many items. Maximum permitted: " + maxItems);
        }
        if (minItems != null && object.size() < minItems) {
            throw new ValidationException("Array has too few items. Minimum permitted: " + minItems);
        }
        if (items.isSingleItems()) {
            if (items.asSingleItems().getSchema() instanceof ValidatableSchema) {
                ValidatableSchema itemSchema = (ValidatableSchema) items.asSingleItems().getSchema();
                for (JsonValue item : object) {
                    itemSchema.validate(item);
                }
            }
        } else {
            Iterator<JsonValue> arrayItems = object.iterator();
            for (JsonSchema itemSchema : items.asArrayItems().getJsonSchemas()) {
                if (!arrayItems.hasNext()) {
                    throw new ValidationException("Not enough items. Expecting " + itemSchema);
                }
                if (itemSchema instanceof ValidatableSchema) {
                    ((ValidatableSchema) itemSchema).validate(arrayItems.next());
                }
            }
        }
    }

    @Override
    public List<Object> getExample() {
        List<Object> example = this.example;
        if (example == null) {
            example = new ArrayList<>();
            boolean foundExample = items.isSingleItems()
                    ? applySingleSchemaExample(example)
                    : applyMultipleSchemasExamples(example);
            if (!foundExample) {
                example = null;
            }
        }
        return example;
    }

    private boolean applySingleSchemaExample(List<Object> example) {
        boolean foundExample = false;
        if (items.asSingleItems().getSchema() instanceof WithExampleSchema) {
            Object itemsExample = ((WithExampleSchema) items.asSingleItems().getSchema()).getExample();
            if (itemsExample != null) {
                int count = minItems != null && minItems > 1 ? minItems : 1;
                foundExample = true;
                for (int i = 0; i < count; i++) {
                    example.add(itemsExample);
                }
            }
        }
        return foundExample;
    }

    private boolean applyMultipleSchemasExamples(List<Object> example) {
        boolean foundExample = false;
        for (JsonSchema schema : items.asArrayItems().getJsonSchemas()) {
            if (schema instanceof WithExampleSchema) {
                Object propertyExample = ((WithExampleSchema) schema).getExample();
                if (propertyExample != null) {
                    foundExample = true;
                    example.add(propertyExample);
                } else {
                    example.add(new HashMap<>());
                }
            }
        }
        return foundExample;
    }

    @Override
    public void setExample(String example) throws IOException {
        this.example = OBJECT_MAPPER.readValue(example, EXAMPLE_VALUE_TYPE);
    }
}
