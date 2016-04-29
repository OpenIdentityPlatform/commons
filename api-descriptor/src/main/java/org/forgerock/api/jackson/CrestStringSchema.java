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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.validation.ValidationException;
import javax.xml.bind.DatatypeConverter;

import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.guava.common.net.InetAddresses;
import org.forgerock.guava.common.net.InternetDomainName;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import org.forgerock.api.enums.WritePolicy;

/**
 * An extension to the Jackson {@code StringSchema} that includes the custom CREST JSON Schema attributes.
 */
class CrestStringSchema extends StringSchema implements CrestReadWritePoliciesSchema, OrderedFieldSchema, EnumSchema,
        ValidatableSchema {
    private WritePolicy writePolicy;
    private ReadPolicy readPolicy;
    private Boolean errorOnWritePolicyFailure;
    private Boolean returnOnDemand;
    private Integer propertyOrder;
    @JsonProperty
    private Map<String, List<String>> options;

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
        if (format != null) {
            validateFormat(s);
        }
    }

    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d$");

    private void validateFormat(String s) {
        // @Checkstyle:off
        switch (format) {
            case DATE_TIME:
                try {
                    DatatypeConverter.parseDateTime(s);
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Expected date-time format, but got " + s, e);
                }
                return;
            case REGEX:
                if (!s.matches(getPattern())) {
                    throw new ValidationException("Expected " + getPattern() + " format, but got " + s);
                }
                return;
            case EMAIL:
                try {
                    new InternetAddress(s).validate();
                } catch (AddressException e) {
                    throw new ValidationException("Expected email, but got " + s, e);
                }
                return;
            case HOST_NAME:
                if (!InternetDomainName.isValid(s)) {
                    throw new ValidationException("Expected host-name, but got " + s);
                }
                return;
            case IP_ADDRESS:
                if (!InetAddresses.isInetAddress(s) || s.indexOf(':') != -1) {
                    throw new ValidationException("Expected ipv4, but got " + s);
                }
                return;
            case IPV6:
                if (!InetAddresses.isInetAddress(s) || s.indexOf(':') == -1) {
                    throw new ValidationException("Expected ipv6, but got " + s);
                }
                return;
            case TIME:
                if (!TIME_PATTERN.matcher(s).matches()) {
                    throw new ValidationException("Expected time format, but got " + s);
                }
                return;
            case DATE:
                try {
                    DatatypeConverter.parseDate(s);
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Expected date-time format, but got " + s, e);
                }
                return;
            case URI:
                try {
                    URI.create(s);
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Expected URI format, but got " + s, e);
                }
                return;
            case UTC_MILLISEC:
                throw new ValidationException("String cannot be of format " + format);
            case PHONE:
            case STYLE:
            case COLOR:
                // phone, style and color no longer supported by JSON Schema Validation, so we don't try and validate
                // them - see http://json-schema.org/latest/json-schema-validation.html#anchor145
        }
        // @Checkstyle:on
    }
}
