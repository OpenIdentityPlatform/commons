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

import org.forgerock.json.schema.validator.validators.*;
import org.forgerock.json.schema.validator.validators.Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.schema.validator.Constants.*;

/**
 * ObjectValidatorFactory initialises the validator instances for given schemas.
 */
public class ObjectValidatorFactory {

    private static final Map<String, Class<? extends Validator>> validators;

    static {
        validators = new HashMap<String, Class<? extends Validator>>(8);
        validators.put(TYPE_STRING, StringTypeValidator.class);
        validators.put(TYPE_NUMBER, NumberTypeValidator.class);
        validators.put(TYPE_INTEGER, IntegerTypeValidator.class);
        validators.put(TYPE_BOOLEAN, BooleanTypeValidator.class);
        validators.put(TYPE_OBJECT, ObjectTypeValidator.class);
        validators.put(TYPE_ARRAY, ArrayTypeValidator.class);
        validators.put(TYPE_NULL, NullTypeValidator.class);
        validators.put(TYPE_ANY, AnyTypeValidator.class);
    }

    /**
     * @param schema JSON Schema Draft-03 object
     * @return Pre-configured {@link Validator} instance.
     * @throws NullPointerException when the <code>schema</code> is null.
     * @throws RuntimeException     when the validators in the <code>schema</code> is not supported.
     */
    public static Validator getTypeValidator(Map<String, Object> schema) {
        return getTypeValidator(schema, Collections.<String>emptyList());
    }

    /**
     * Returns a validator validating the schema.
     *
     * @param schema JSON Schema Draft-03 object
     * @param jsonPointer the list of tokens representing the JSON pointer leading to this validator 
     * @return Pre-configured {@link Validator} instance.
     * @throws NullPointerException when the <code>schema</code> is null.
     * @throws RuntimeException     when the validators in the <code>schema</code> is not supported.
     */
    public static Validator getTypeValidator(Map<String, Object> schema, List<String> jsonPointer) {
        Object typeValue = schema.get(TYPE);
        if (null == typeValue) {
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
        return validators.get(type);
    }
}
