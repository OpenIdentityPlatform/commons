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

import javax.validation.ValidationException;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import org.forgerock.json.JsonValue;

/**
 * A {@link JsonSchema} implementation that supports validation rules.
 */
interface ValidatableSchema {

    /**
     * Validates {@link JsonSchema} according to some set of validation rules.
     *
     * @param object JSON to validate
     * @throws ValidationException Indicates that JSON does not conform to a known JSON Schema format.
     */
    void validate(JsonValue object) throws ValidationException;
}
