/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.json.schema.validator.validators;

import java.util.Map;

import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class BooleanTypeValidatorTest {

    private static final String SCHEMA = "{"
            + "\"type\": \"boolean\","
            + "\"required\": true"
            + "}";

    @Test(expectedExceptions = ValidationException.class, expectedExceptionsMessageRegExp = "WrongType")
    public void requiredValueNotBoolean() throws Exception {
        JSONParser parser = new JSONParser();
        Map<String, Object> schema = (Map<String, Object>) parser.parse(SCHEMA);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        v.validate("test", null, new ErrorHandler() {

            @Override
            public void error(ValidationException exception) throws ValidationException {
                throw new ValidationException("WrongType");
            }

            @Override
            public void assembleException() throws ValidationException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    @Test(expectedExceptions = ValidationException.class)
    public void requiredValueNull() throws Exception {
        JSONParser parser = new JSONParser();
        Map<String, Object> schema = (Map<String, Object>) parser.parse(SCHEMA);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        v.validate(null, null, new ErrorHandler() {

            @Override
            public void error(ValidationException exception) throws ValidationException {
                throw new ValidationException("requiredValueNull");
            }

            @Override
            public void assembleException() throws ValidationException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    @Test
    public void requiredValueNotNull() throws Exception {
        JSONParser parser = new JSONParser();
        Map<String, Object> schema = (Map<String, Object>) parser.parse(SCHEMA);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        v.validate(false, null, new ErrorHandler() {

            @Override
            public void error(ValidationException exception) throws SchemaException {
                throw new ValidationException("requiredValueNotNull");
            }

            @Override
            public void assembleException() throws ValidationException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

}
