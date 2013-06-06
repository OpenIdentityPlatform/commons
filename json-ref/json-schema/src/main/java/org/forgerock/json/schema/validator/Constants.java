/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.json.schema.validator;

import java.net.URI;

/**
 * Constants is the collection of all constant values used by the object validator implementation.
 */
public class Constants {

    // Prevents instantiation
    private Constants() {
    }

    //http://tools.ietf.org/html/draft-zyp-json-schema-03
    public static final URI JSON_SCHEMA_DRAFT03 = URI.create("http://json-schema.org/draft-03/schema");
    public static final URI JSON_HYPER_SCHEMA_DRAFT03 = URI.create("http://json-schema.org/draft-03/hyper-schema");
    public static final URI JSON_LINKS_DRAFT03 = URI.create("http://json-schema.org/draft-03/links");
    //http://tools.ietf.org/html/draft-zyp-json-schema-04
    public static final URI JSON_SCHEMA_DRAFT04 = URI.create("http://json-schema.org/draft-04/schema");
    public static final URI JSON_HYPER_SCHEMA_DRAFT04 = URI.create("http://json-schema.org/draft-04/hyper-schema");
    public static final URI JSON_LINKS_DRAFT04 = URI.create("http://json-schema.org/draft-04/links");
    //latest version
    public static final URI JSON_SCHEMA = URI.create("http://json-schema.org/schema#");
    public static final URI JSON_HYPER_SCHEMA = URI.create("http://json-schema.org/hyper-schema#");
    public static final URI JSON_LINKS = URI.create("http://json-schema.org/links#");
    //Default supported validators values
    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_NULL = "null";
    public static final String TYPE_ANY = "any";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">JSON schema draft 03 - type</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.5.2">JSON schema draft 04 - type</a>
     */
    public static final String TYPE = "type";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.2">JSON schema draft 03 - properties</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.4">JSON schema draft 04 - properties</a>
     */
    public static final String PROPERTIES = "properties";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.3">JSON schema draft 03 - patternProperties</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.4">JSON schema draft 04 - patternProperties</a>
     */
    public static final String PATTERNPROPERTIES = "patternProperties";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.4">JSON schema draft 03 - additionalProperties</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.4">JSON schema draft 04 - additionalProperties</a>
     */
    public static final String ADDITIONALPROPERTIES = "additionalProperties";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.5">JSON schema draft 03 - items</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.3.1">JSON schema draft 04 - items</a>
     */
    public static final String ITEMS = "items";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.6">JSON schema draft 03 - additionalItems</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.3.1">JSON schema draft 04 - additionalItems</a>
     */
    public static final String ADDITIONALITEMS = "additionalItems";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7">JSON schema draft 03 - required</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.3">JSON schema draft 04 - required</a>
     */
    public static final String REQUIRED = "required";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.8">JSON schema draft 03 - dependencies</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.5">JSON schema draft 04 - dependencies</a>
     */
    public static final String DEPENDENCIES = "dependencies";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.9">JSON schema draft 03 - minimum</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.1.3">JSON schema draft 04 - minimum</a>
     */
    public static final String MINIMUM = "minimum";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.10">JSON schema draft 03 - maximum</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.1.2">JSON schema draft 04 - maximum</a>
     */
    public static final String MAXIMUM = "maximum";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.11">JSON schema draft 03 - exclusiveMinimum</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.1.3">JSON schema draft 04 - exclusiveMinimum</a>
     */
    public static final String EXCLUSIVEMINIMUM = "exclusiveMinimum";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.12">JSON schema draft 03 - exclusiveMaximum</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.1.2">JSON schema draft 04 - exclusiveMaximum</a>
     */
    public static final String EXCLUSIVEMAXIMUM = "exclusiveMaximum";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.13">JSON schema draft 03 - minItems</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.3.3">JSON schema draft 04 - minItems</a>
     */
    public static final String MINITEMS = "minItems";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.14">JSON schema draft 03 - maxItems</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.3.2">JSON schema draft 04 - maxItems</a>
     */
    public static final String MAXITEMS = "maxItems";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.15">JSON schema draft 03 - uniqueItems</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.3.4">JSON schema draft 04 - maxItems</a>
     */
    public static final String UNIQUEITEMS = "uniqueItems";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.16">JSON schema draft 03 - pattern</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.2.3">JSON schema draft 04 - pattern</a>
     */
    public static final String PATTERN = "pattern";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.17">JSON schema draft 03 - minLength</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.2.2">JSON schema draft 04 - minLength</a>
     */
    public static final String MINLENGTH = "minLength";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.18">JSON schema draft 03 - maxLength</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.2.1">JSON schema draft 04 - maxLength</a>
     */
    public static final String MAXLENGTH = "maxLength";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.19">JSON schema draft 03 - enum</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.5.1">JSON schema draft 04 - enum</a>
     */
    public static final String ENUM = "enum";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.20">JSON schema draft 03 - default</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-6.2">JSON schema draft 04 - default</a>
     */
    public static final String DEFAULT = "default";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.23">JSON schema draft 03 - format</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-7">JSON schema draft 04 - format</a>
     */
    public static final String FORMAT = "format";
    /**
     * @deprecated renamed to multipleOf in JSON schema draft 04
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.24">JSON schema draft 03 - divisibleBy</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#appendix-A">JSON schema draft 04 - renamed</a>
     * @see #MULTIPLEOF
     */
    public static final String DIVISIBLEBY = "divisibleBy";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.2.3">JSON schema draft 04 - multipleOf</a>
     */
    public static final String MULTIPLEOF = "multipleOf";
    /**
     * @deprecated removed in JSON schema draft 04
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.25">JSON schema draft 03 - disallow</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#appendix-A">JSON schema draft 04 - removed</a>
     */
    public static final String DISALLOW = "disallow";
    /**
     * @deprecated removed in JSON schema draft 04
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.26">JSON schema draft 03 - extends</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#appendix-A">JSON schema draft 04 - removed</a>
     */
    public static final String EXTENDS = "extends";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.27">JSON schema draft 03 - id</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-7.2">JSON schema draft 04 - id</a>
     */
    public static final String ID = "id";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.28">JSON schema draft 03 - $ref</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-7.2.3">JSON schema draft 04 - $ref</a>
     */
    public static final String REF = "$ref";
    /**
     * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.29">JSON schema draft 03 - $schema</a>
     * @see <a href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-6">JSON schema draft 04 - $schema</a>
     */
    public static final String SCHEMA = "$schema";
    /**
     * date-time  This SHOULD be a date in ISO 8601 format of YYYY-MM-
     * DDThh:mm:ssZ in UTC time.  This is the recommended form of date/timestamp.
     */
    public static final String FORMAT_DATE_TIME = "date-time";
    /**
     * date  This SHOULD be a date in the format of YYYY-MM-DD.  It is
     * recommended that you use the "date-time" format instead of "date"
     * unless you need to transfer only the date part.
     */
    public static final String FORMAT_DATE = "date";
    /**
     * time  This SHOULD be a time in the format of hh:mm:ss.  It is
     * recommended that you use the "date-time" format instead of "time"
     * unless you need to transfer only the time part.
     */
    public static final String FORMAT_TIME = "time";
    /**
     * utc-millisec  This SHOULD be the difference, measured in
     * milliseconds, between the specified time and midnight, 00:00 of
     * January 1, 1970 UTC.  The value SHOULD be a number (integer or
     * float).
     */
    public static final String FORMAT_UTC_MILLISEC = "utc-millisec";
    /**
     * regex  A regular expression, following the regular expression
     * specification from ECMA 262/Perl 5.
     */
    public static final String FORMAT_REGEX = "regex";
    /**
     * color  This is a CSS color (like "#FF0000" or "red"), based on CSS
     * 2.1 [W3C.CR-CSS21-20070719].
     */
    public static final String FORMAT_COLOR = "color";
    /**
     * style  This is a CSS style definition (like "color: red; background-
     * color:#FFF"), based on CSS 2.1 [W3C.CR-CSS21-20070719].
     */
    public static final String FORMAT_STYLE = "style";
    /**
     * phone  This SHOULD be a phone number (format MAY follow E.123).
     */
    public static final String FORMAT_PHONE = "phone";
    /**
     * uri  This value SHOULD be a URI..
     */
    public static final String FORMAT_URI = "uri";
    /**
     * email  This SHOULD be an email address.
     */
    public static final String FORMAT_EMAIL = "email";
    /**
     * ip-address  This SHOULD be an ip version 4 address.
     */
    public static final String FORMAT_IP_ADDRESS = "ip-address";
    /**
     * ipv6  This SHOULD be an ip version 6 address.
     */
    public static final String FORMAT_IPV6 = "ipv6";
    /**
     * host-name  This SHOULD be a host-name.
     */
    public static final String FORMAT_HOST_NAME = "host-name";
    /*Additional custom formats MAY be created.  These custom formats MAY
    be expressed as an URI, and this URI MAY reference a schema of that
    format.
     */
    //ERROR MESSAGES
    /*
    Use parameterized messages in favour of SLF4J. The first parameter is always the "at".
    */
    /**
     * Type mismatch. Expected type: {} found: {}
     */
    public static final String ERROR_MSG_TYPE_MISMATCH = "Type mismatch at {}. Expected type {} found {}";
    /**
     * Required property violation at {}
     */
    public static final String ERROR_MSG_REQUIRED_PROPERTY = "Required property violation at {}";
    /**
     * Value at {} does not have a value in the enumeration.
     */
    public static final String ERROR_MSG_ENUM_VIOLATION = "Value at {} does not have a value in the enumeration.";
    /**
     * Value at {} MUST be null.
     */
    public static final String ERROR_MSG_NULL_TYPE = "Value at {} MUST be null.";

    /**
     * Value has additional properties
     */
    public static final String ERROR_MSG_ADDITIONAL_PROPERTIES = "Value at {} has additional properties.";

    /**
     * {}
     */
    public static final String ERROR_MSG_ = "{}";
}
