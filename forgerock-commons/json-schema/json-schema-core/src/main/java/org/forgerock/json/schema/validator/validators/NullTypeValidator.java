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

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;

import java.util.List;
import java.util.Map;

import static org.forgerock.json.schema.validator.Constants.*;

/**
 * null  Value MUST be null.  Note this is mainly for purpose of
 * being able use union types to define nullability.  If this validators
 * is not included in a union, null values are not allowed (the
 * primitives do not allow nulls on their own.
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class NullTypeValidator extends Validator {

    /**
     * Construct a null type validator.
     * @param schema The schema.
     * @param jsonPointer The pointer.
     */
    public NullTypeValidator(Map<String, Object> schema, List<String> jsonPointer) {
        super(schema, jsonPointer);
    }

    @Override
    public void validate(Object node, JsonPointer at, ErrorHandler handler) throws SchemaException {
        if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_NULL_TYPE, getPath(at, null)));
        }
    }
}
