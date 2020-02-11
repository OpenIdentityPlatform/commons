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

import static org.forgerock.api.jackson.JacksonUtils.*;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.ValidationException;

import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import org.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code IntegerSchema} that includes the custom CREST JSON Schema attributes.
 */
class CrestIntegerSchema extends IntegerSchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema, EnumSchema,
        ValidatableSchema, MultipleOfSchema, PropertyFormatSchema, MinimumMaximumSchema, WithExampleSchema<Long> {
    private WritePolicy writePolicy;
    private ReadPolicy readPolicy;
    private Boolean errorOnWritePolicyFailure;
    private Boolean returnOnDemand;
    private Integer propertyOrder;
    private Double multipleOf;
    private String propertyFormat;
    private BigDecimal propertyMinimum;
    private BigDecimal propertyMaximum;
    @JsonProperty
    private Map<String, List<String>> options;
    private Long example;

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
    public List<String> getEnumTitles() {
        return options == null ? null : options.get(ENUM_TITLES);
    }

    @Override
    public void setEnumTitles(List<String> titles) {
        this.options = Collections.singletonMap(ENUM_TITLES, titles);
    }

    @Override
    public void validate(JsonValue object) throws ValidationException {
        if (!object.isNumber()) {
            throw new ValidationException("Expected integer, but got " + object.getObject());
        }
        Number number = object.asNumber();
        if (!(number instanceof Integer || number instanceof Long)) {
            throw new ValidationException("Expected integer, but got " + object.getObject());
        }
        validateMaximumAndMinimum(number, getMaximum(), getExclusiveMaximum(), getMinimum(), getExclusiveMinimum());
        validateFormatForNumber(format);
        validateEnum(enums, number.toString());
    }

    @Override
    public Double getMultipleOf() {
        return multipleOf;
    }

    @Override
    public void setMultipleOf(Double multipleOf) {
        this.multipleOf = multipleOf;
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

    // This method overrides the superclass' definition of "format" via JsonProperty annotation
    @JsonProperty("format")
    @Override
    public String getPropertyFormat() {
        if (!isEmpty(propertyFormat)) {
            return propertyFormat;
        }
        // fallback to old behavior
        return format == null ? null : format.toString();
    }

    @Override
    public void setPropertyFormat(String propertyFormat) {
        this.propertyFormat = propertyFormat;
    }

    // This method overrides the superclass' definition of "minimum" via JsonProperty annotation
    @JsonProperty("minimum")
    @Override
    public BigDecimal getPropertyMinimum() {
        return propertyMinimum;
    }

    @Override
    public void setPropertyMinimum(BigDecimal propertyMinimum) {
        this.propertyMinimum = propertyMinimum;
    }

    // This method overrides the superclass' definition of "maximum" via JsonProperty annotation
    @JsonProperty("maximum")
    @Override
    public BigDecimal getPropertyMaximum() {
        return propertyMaximum;
    }

    @Override
    public void setPropertyMaximum(BigDecimal propertyMaximum) {
        this.propertyMaximum = propertyMaximum;
    }

    @Override
    public Long getExample() {
        return example;
    }

    @Override
    public void setExample(String example) {
        this.example = Long.valueOf(example);
    }
}
