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

import com.fasterxml.jackson.module.jsonSchema.factories.JsonSchemaFactory;
import com.fasterxml.jackson.module.jsonSchema.types.AnySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

/**
 * A {@code JsonSchemaFactory} that returns the extension schema objects rather than the default Jackson
 * implementations.
 */
public class CrestJsonSchemaFactory extends JsonSchemaFactory {
    @Override
    public ObjectSchema objectSchema() {
        return new CrestObjectSchema();
    }

    @Override
    public AnySchema anySchema() {
        return new CrestAnySchema();
    }

    @Override
    public ArraySchema arraySchema() {
        return new CrestArraySchema();
    }

    @Override
    public BooleanSchema booleanSchema() {
        return new CrestBooleanSchema();
    }

    @Override
    public IntegerSchema integerSchema() {
        return new CrestIntegerSchema();
    }

    @Override
    public NumberSchema numberSchema() {
        return new CrestNumberSchema();
    }

    @Override
    public StringSchema stringSchema() {
        return new CrestStringSchema();
    }
}
