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
 * Portions copyright 2024 3A Systems LLC.
 */

package org.forgerock.api.jackson;

import static org.forgerock.api.jackson.JacksonUtils.*;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import javax.validation.ValidationException;
import jakarta.xml.bind.DatatypeConverter;

import org.forgerock.api.enums.ReadPolicy;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import org.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code StringSchema} that includes the custom CREST JSON Schema attributes.
 */
class CrestStringSchema extends StringSchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema, EnumSchema,
        ValidatableSchema, PropertyFormatSchema, WithExampleSchema<String> {
    private WritePolicy writePolicy;
    private ReadPolicy readPolicy;
    private Boolean errorOnWritePolicyFailure;
    private Boolean returnOnDemand;
    private Integer propertyOrder;
    private String propertyFormat;
    @JsonProperty
    private Map<String, List<String>> options;
    private String example;

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
        if (!object.isString()) {
            throw new ValidationException("Expected string but got: " + object.getObject());
        }
        String s = object.asString();
        validateEnum(enums, s);
        if (!isEmpty(propertyFormat)) {
            validateFormat(s);
        }
    }

    /**
     * Validates a subset of known {@code format} values, but allows unknown values to pass-through, according to
     * JSON Schema v4 spec.
     *
     * @param s Value to validate
     * @throws ValidationException Indicates that {@code s} does not conform to a known JSON Schema format.
     */
    private void validateFormat(final String s) throws ValidationException {
        switch (propertyFormat) {
        case "date-time":
            // http://tools.ietf.org/html/rfc3339#section-5.6
            try {
                DatatypeConverter.parseDateTime(s);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Expected date-time format, but got " + s, e);
            }
            return;
        case "date":
        case "full-date":
            // NOTE: supported by OpenAPI, but not defined by JSON Schema v4 spec
            // http://tools.ietf.org/html/rfc3339#section-5.6
            try {
                DatatypeConverter.parseDate(s);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Expected date/full-date format, but got " + s, e);
            }
            return;
        case "email":
            // http://tools.ietf.org/html/rfc5322#section-3.4.1
            try {
                new InternetAddress(s).validate();
            } catch (AddressException e) {
                throw new ValidationException("Expected email, but got " + s, e);
            }
            return;
        case "hostname":
            // http://tools.ietf.org/html/rfc1034#section-3.1
            if (!InternetDomainName.isValid(s)) {
                throw new ValidationException("Expected host-name, but got " + s);
            }
            return;
        case "ipv4":
            // http://tools.ietf.org/html/rfc2673#section-3.2
            if (!InetAddresses.isInetAddress(s) || s.indexOf(':') != -1) {
                throw new ValidationException("Expected ipv4, but got " + s);
            }
            return;
        case "ipv6":
            // http://tools.ietf.org/html/rfc2373#section-2.2
            if (!InetAddresses.isInetAddress(s) || s.indexOf(':') == -1) {
                throw new ValidationException("Expected ipv6, but got " + s);
            }
            return;
        case "uri":
            // http://tools.ietf.org/html/rfc3986
            try {
                URI.create(s);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Expected URI format, but got " + s, e);
            }
            return;
        }
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

    @Override
    public String getExample() {
        return example;
    }

    @Override
    public void setExample(String example) {
        this.example = example;
    }
}
