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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.validators;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.FailFastErrorHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.schema.validator.Constants.*;

/**
 * Union Types  An array of two or more simple validators definitions.  Each
 * item in the array MUST be a simple validators definition or a schema.
 * The instance value is valid if it is of the same validators as one of
 * the simple validators definitions, or valid by one of the schemas, in
 * the array.
 * <p/>
 * <p>
 * For example, a schema that defines if an instance can be a string or
 * a number would be:</p>
 * <p/>
 * <code>{"type":["string","number"]}</code>
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class UnionTypeValidator extends Validator {

    private final List<Validator> validators;

    /**
     * Create a union type validator.
     * @param schema The schema.
     * @param jsonPointer The pointers.
     */
    public UnionTypeValidator(Map<String, Object> schema, List<String> jsonPointer) {
        super(schema, jsonPointer);
        final List<?> unionTypes = (List<?>) schema.get(TYPE);
        this.validators = new ArrayList<>(unionTypes.size());
        for (Object o : unionTypes) {
            if (o instanceof String) {
                validators.add(ObjectValidatorFactory.getTypeValidator((String) o, schema, jsonPointer));
            } else if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Validator v = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) o, jsonPointer);
                validators.add(v);
                if (v instanceof NullTypeValidator) {
                    required = false;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Object node, JsonPointer at, ErrorHandler handler) throws SchemaException {
        for (Validator v : validators) {
            try {
                v.validate(node, at, new FailFastErrorHandler());
                return;
            } catch (ValidationException e) {
                //Only one helpers should success to be overall success.
            }
        }
        handler.error(new ValidationException("Invalid union validators.", getPath(at, null)));
    }
}
