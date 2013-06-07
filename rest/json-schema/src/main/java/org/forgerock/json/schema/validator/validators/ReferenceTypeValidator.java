/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.json.schema.validator.validators;

import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;

/**
 * ReferenceTypeValidator holds a reference to another validator. The reference
 * must be resolved by calling {@link Validator#resolveSchemaReferences()} after
 * building all the validators.
 *
 * @see org.forgerock.json.schema.validator.Constants#REF
 */
public class ReferenceTypeValidator extends Validator {

    private final String reference;
    private Validator referenceValidator;

    /**
     * Default ctor.
     *
     * @param schema the schema holding the reference to this validator
     * @param reference the reference to the pointed schema
     * @param jsonPointer the JSON pointer locating where this validator was defined in the schema.
     */
    public ReferenceTypeValidator(Map<String, Object> schema, String reference, List<String> jsonPointer) {
        super(schema, jsonPointer);
        this.reference = reference;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(Object node, JsonPointer at, ErrorHandler handler) {
        // referenceValidator should not be null, it is validated in Validator#resolveSchemaReferences()
        if (referenceValidator == null) {
            throw new SchemaException(new JsonValue(null, getJsonPointer()),
                    "Could not dereference JSON reference " + reference);
        }
        referenceValidator.validate(node, getPath(at, null), handler);
    }

    /**
     * Returns the reference to the pointed schema.
     *
     * @return the reference to the pointed schema
     */
    public String getReference() {
        return reference;
    }

    /**
     * Sets the validator being referenced by this object.
     *
     * @param validator the validator being referenced by this object
     */
    public void setReferencedValidator(Validator validator) {
        this.referenceValidator = validator;
    }
}
