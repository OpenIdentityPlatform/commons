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

import javax.validation.ValidationException;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import org.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code BooleanSchema} that includes the custom CREST JSON Schema attributes.
 */
public class CrestBooleanSchema extends BooleanSchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema,
        ValidatableSchema, WithExampleSchema<Boolean> {
    private WritePolicy writePolicy;
    private ReadPolicy readPolicy;
    private Boolean errorOnWritePolicyFailure;
    private Boolean returnOnDemand;
    private Integer propertyOrder;
    private Boolean example;

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
        if (!object.isBoolean()) {
            throw new ValidationException("Expected boolean, but got " + object.getObject());
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

    @Override
    public Boolean getExample() {
        return example;
    }

    @Override
    public void setExample(String example) {
        this.example = Boolean.valueOf(example);
    }
}
