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

import java.util.Iterator;

import javax.validation.ValidationException;

import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import org.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code ArraySchema} that includes the custom CREST JSON Schema attributes.
 */
public class CrestArraySchema extends ArraySchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema,
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
            ValidatableSchema itemSchema = (ValidatableSchema) items.asSingleItems().getSchema();
            for (JsonValue item : object) {
                itemSchema.validate(item);
            }
        } else {
            Iterator<JsonValue> arrayItems = object.iterator();
            for (ValidatableSchema itemSchema : (ValidatableSchema[]) items.asArrayItems().getJsonSchemas()) {
                if (!arrayItems.hasNext()) {
                    throw new ValidationException("Not enough items. Expecting " + itemSchema);
                }
                itemSchema.validate(arrayItems.next());
            }
        }
    }
}
