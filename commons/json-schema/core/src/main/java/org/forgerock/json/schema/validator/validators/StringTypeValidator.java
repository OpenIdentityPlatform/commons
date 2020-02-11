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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;
import org.forgerock.json.schema.validator.helpers.EnumHelper;
import org.forgerock.json.schema.validator.helpers.FormatHelper;

import java.util.List;
import java.util.Map;

import static org.forgerock.json.schema.validator.Constants.*;

/**
 * StringTypeValidator applies all the constraints of a <code>string</code> type.
 * <p/>
 * Sample JSON Schema:
 * <code>
 * {
 * "type"        : "string",
 * "required"    : true,
 * "minLength"   : 1,
 * "maxLength"   : 8,
 * "enum" : [
 * " ",
 * "number1",
 * "number2",
 * "123456789"
 * ],
 * "pattern-fix" : ".*",
 * "format-fix"  : "date"
 * }
 * </code>
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class StringTypeValidator extends Validator {

    /**
     * When the instance value is a string, this provides a regular
     * expression that a string instance MUST match in order to be valid.
     * Regular expressions SHOULD follow the regular expression
     * specification from ECMA 262/Perl 5
     */
    private Pattern p = null;
    /**
     * When the instance value is a string, this defines the minimum length
     * of the string.
     */
    private int minLength = -1;
    /**
     * When the instance value is a string, this defines the maximum length
     * of the string.
     */
    private int maxLength = -1;
    /**
     * This provides an enumeration of all possible values that are valid
     * for the instance property.  This MUST be an array, and each item in
     * the array represents a possible value for the instance value.  If
     * this attribute is defined, the instance value MUST be one of the
     * values in the array in order for the schema to be valid.
     */
    private EnumHelper enumHelper = null;
    /**
     * This property defines the validators of data, content validators, or microformat
     * to be expected in the instance property values.  A format attribute
     * MAY be one of the values listed below, and if so, SHOULD adhere to
     * the semantics describing for the format.  A format SHOULD only be
     * used to give meaning to primitive types (string, integer, number, or
     * boolean).  Validators MAY (but are not required to) validate that the
     * instance values conform to a format
     */
    private FormatHelper formatHelper = null;

    /**
     * Create a string type validator.
     * @param schema The schema.
     * @param jsonPointer The pointers.
     */
    @SuppressWarnings("unchecked")
    public StringTypeValidator(Map<String, Object> schema, List<String> jsonPointer) {
        super(schema, jsonPointer);
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            if (PATTERN.equals(e.getKey())) {
                if (e.getValue() instanceof String) {
                    String pattern = (String) e.getValue();
                    try {
                        p = Pattern.compile(pattern, Pattern.UNICODE_CASE);
                    } catch (PatternSyntaxException pse) {
                        //LOG.error("Failed to apply pattern on " + at + ": Invalid RE syntax [" + pattern + "]", pse);
                    }
                }
            } else if (REQUIRED.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean) {
                    required = ((Boolean) e.getValue());
                } else if (e.getValue() instanceof String) {
                    required = Boolean.parseBoolean((String) e.getValue());
                }
            } else if (MINLENGTH.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    minLength = ((Number) e.getValue()).intValue();
                }
            } else if (MAXLENGTH.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    maxLength = ((Number) e.getValue()).intValue();
                }
            } else if (ENUM.equals(e.getKey())) {
                if (e.getValue() instanceof List) {
                    enumHelper = new EnumHelper((List<Object>) e.getValue());
                }
            } else if (FORMAT.equals(e.getKey())) {
                if (e.getValue() instanceof String) {
                    formatHelper = new FormatHelper((String) e.getValue());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Object node, JsonPointer at, ErrorHandler handler) throws SchemaException {
        if (node instanceof String) {
            String nodeValue = (String) node;
            if (minLength > -1 && nodeValue.length() < minLength) {
                handler.error(new ValidationException("minLength error", getPath(at, null)));
            }
            if (maxLength > -1 && nodeValue.length() > maxLength) {
                handler.error(new ValidationException("maxLength error", getPath(at, null)));
            }
            if (null != p) {
                Matcher m = p.matcher(nodeValue);
                if (!m.matches()) {
                    handler.error(new ValidationException(getPath(at, null) + ": does not match the regex pattern "
                            + p.pattern(), getPath(at, null)));
                }
            }
            if (null != enumHelper) {
                enumHelper.validate(node, at, handler);
            }
            if (null != formatHelper) {
                formatHelper.validate(node, at, handler);
            }
        } else if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
        } else if (required) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }
    }
}
