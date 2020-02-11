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

package org.forgerock.json.schema.validator;

import static org.forgerock.json.schema.validator.Constants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.schema.validator.validators.AnyTypeValidator;
import org.forgerock.json.schema.validator.validators.ArrayTypeValidator;
import org.forgerock.json.schema.validator.validators.BooleanTypeValidator;
import org.forgerock.json.schema.validator.validators.IntegerTypeValidator;
import org.forgerock.json.schema.validator.validators.NullTypeValidator;
import org.forgerock.json.schema.validator.validators.NumberTypeValidator;
import org.forgerock.json.schema.validator.validators.ObjectTypeValidator;
import org.forgerock.json.schema.validator.validators.ReferenceTypeValidator;
import org.forgerock.json.schema.validator.validators.StringTypeValidator;
import org.forgerock.json.schema.validator.validators.UnionTypeValidator;
import org.forgerock.json.schema.validator.validators.Validator;

/**
 * ObjectValidatorFactory initialises the validator instances for given schemas.
 */
@SuppressWarnings("deprecation")
public final class ObjectValidatorFactory {

    private static final Map<String, Class<? extends Validator>> VALIDATORS;

    static {
        VALIDATORS = new HashMap<>(8);
        VALIDATORS.put(TYPE_STRING, StringTypeValidator.class);
        VALIDATORS.put(TYPE_NUMBER, NumberTypeValidator.class);
        VALIDATORS.put(TYPE_INTEGER, IntegerTypeValidator.class);
        VALIDATORS.put(TYPE_BOOLEAN, BooleanTypeValidator.class);
        VALIDATORS.put(TYPE_OBJECT, ObjectTypeValidator.class);
        VALIDATORS.put(TYPE_ARRAY, ArrayTypeValidator.class);
        VALIDATORS.put(TYPE_NULL, NullTypeValidator.class);
        VALIDATORS.put(TYPE_ANY, AnyTypeValidator.class);
    }

    private ObjectValidatorFactory() {
        // hide ctor of utility class
    }

    /**
     * Returns a validator validating the schema.
     *
     * @param schema JSON Schema Draft-03 object
     * @return Pre-configured {@link Validator} instance.
     * @throws NullPointerException when the <code>schema</code> is null.
     * @throws RuntimeException     when the validators in the <code>schema</code> is not supported.
     */
    public static Validator getTypeValidator(Map<String, Object> schema) {
        return getTypeValidator(schema, Collections.<String>emptyList());
    }

    /**
     * Returns a validator validating the schema. It uses the passed in JSON pointer as a relative location of the
     * validator in the schema.
     *
     * @param schema JSON Schema Draft-03 object
     * @param jsonPointer the list of tokens representing the JSON pointer leading to this validator
     * @return Pre-configured {@link Validator} instance.
     * @throws NullPointerException when the <code>schema</code> is null.
     * @throws RuntimeException     when the validators in the <code>schema</code> is not supported.
     */
    public static Validator getTypeValidator(Map<String, Object> schema, List<String> jsonPointer) {
        Validator v = getTypeValidatorInner(schema, jsonPointer);
        if (jsonPointer.isEmpty()) {
            v.resolveSchemaReferences();
        }
        return v;
    }

    private static Validator getTypeValidatorInner(Map<String, Object> schema, List<String> jsonPointer) {
        Object typeValue = schema.get(TYPE);
        if (null == typeValue) {
            Object refValue = schema.get(REF);
            if (refValue instanceof String) {
                // the $ref actually is a reference to another yet unknown schema
                return new ReferenceTypeValidator(schema, (String) refValue, jsonPointer);
            }
            // type "any" is invalid for JSON schema draft 04
            return getTypeValidator(TYPE_ANY, schema, jsonPointer);
        } else if (typeValue instanceof String) {
            return getTypeValidator((String) typeValue, schema, jsonPointer);
        } else if (typeValue instanceof List) {
            return new UnionTypeValidator(schema, jsonPointer);
        }
        throw new RuntimeException("Unsupported validators exception {}");
    }

    /**
     * Instantiates a validator of the passed in type with the given schema.
     *
     * @param type the type of Validator to instantiate
     * @param schema the schema that the instantiated validator will validate
     * @param jsonPointer the list of tokens representing the JSON pointer leading to this validator
     * @return the instantiated validator. Cannot be null.
     * @throws RuntimeException when the validators in the <code>schema</code> is not supported.
     */
    public static Validator getTypeValidator(String type, Map<String, Object> schema, List<String> jsonPointer) {
        Class<? extends Validator> clazz = findClass(type);
        if (null != clazz) {
            try {
                return clazz.getConstructor(Map.class, List.class).newInstance(schema, jsonPointer);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to initialize the new Validator instance");
            }
        }
        throw new RuntimeException("Unsupported validators exception {}");
    }

    private static Class<? extends Validator> findClass(String type) {
        return VALIDATORS.get(type);
    }
}
