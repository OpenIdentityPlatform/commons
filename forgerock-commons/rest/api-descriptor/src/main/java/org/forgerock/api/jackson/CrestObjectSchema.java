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
import static org.forgerock.json.JsonValue.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.validation.ValidationException;

import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.api.enums.WritePolicy;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;

/**
 * An extension to the Jackson {@code ObjectSchema} that includes the custom CREST JSON Schema attributes.
 */
public class CrestObjectSchema extends ObjectSchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema,
        ValidatableSchema, RequiredFieldsSchema, WithExampleSchema<Map<String, Object>> {
    private static final JavaType EXAMPLE_VALUE_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructParametrizedType(HashMap.class, Map.class, String.class, Object.class);
    private WritePolicy writePolicy;
    private ReadPolicy readPolicy;
    private Boolean errorOnWritePolicyFailure;
    private Boolean returnOnDemand;
    private Integer propertyOrder;
    private Set<String> requiredFields;
    private Map<String, Object> example;

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
                final JsonSchema schema = getProperties().get(property.getKey());
                if (schema instanceof ValidatableSchema && !"reference".equals(property.getKey())) {
                    ((ValidatableSchema) schema).validate(json(property.getValue()));
                }
                validated = true;
            }
            for (Map.Entry<Pattern, JsonSchema> pattern : patternProperties.entrySet()) {
                if (pattern.getKey().matcher(property.getKey()).matches()) {
                    if (pattern.getValue() instanceof ValidatableSchema) {
                        ((ValidatableSchema) pattern.getValue()).validate(json(property.getValue()));
                    }
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
            if (schemaAdditionalProperties.getJsonSchema() instanceof ValidatableSchema) {
                final ValidatableSchema schema = (ValidatableSchema) schemaAdditionalProperties.getJsonSchema();
                for (Object value : propertyValues.values()) {
                    schema.validate(json(value));
                }
            }
        }
    }

    /**
     * Gets read-only property. This method overrides the superclass' definition of "readOnly" being all lower-case,
     * via the {@code JsonProperty} annotation.
     *
     * @return {@code true} if property is read-only, otherwise {@code false} or {@code null}
     */
    @JsonProperty("readOnly")
    @Override
    public Boolean getReadonly() {
        return super.getReadonly();
    }

    // This method overrides the superclass' definition of "required" via JsonProperty annotation
    @JsonProperty("required")
    @Override
    public Set<String> getRequiredFields() {
        return requiredFields;
    }

    @Override
    public void setRequiredFields(Set<String> requiredFields) {
        this.requiredFields = requiredFields;
    }

    @Override
    public Map<String, Object> getExample() {
        Map<String, Object> example = this.example;
        if (example == null) {
            example = new HashMap<>();
            for (Map.Entry<String, JsonSchema> property : getProperties().entrySet()) {
                if (property.getValue() instanceof WithExampleSchema) {
                    Object propertyExample = ((WithExampleSchema) property.getValue()).getExample();
                    if (propertyExample != null) {
                        example.put(property.getKey(), propertyExample);
                    }
                }
            }
            if (example.isEmpty()) {
                example = null;
            }
        }
        return example;
    }

    @Override
    public void setExample(String example) throws IOException {
        this.example = OBJECT_MAPPER.readValue(example, EXAMPLE_VALUE_TYPE);
    }
}
