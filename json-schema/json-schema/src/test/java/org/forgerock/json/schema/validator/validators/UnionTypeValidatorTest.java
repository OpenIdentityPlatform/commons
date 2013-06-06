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
package org.forgerock.json.schema.validator.validators;

import static org.testng.Assert.*;

import org.forgerock.json.schema.validator.validators.Validator;
import org.forgerock.json.schema.validator.CollectErrorsHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.Test;

import java.util.Map;

@SuppressWarnings("javadoc")
public class UnionTypeValidatorTest {

    private String schema1 = "{"
            + "\"type\": [\"string\",\"boolean\"],"
            + "\"pattern\": \".*\","
            + "\"required\": true"
            + "\"minLength\": 0"
            + "\"maxLength\": 11"
            + "\"enum\": [\"number1\",\"number2\"]"
            + "\"format\": \"date-time\""
            + "}";
    private String schema2 = "{"
            + "\"type\": [\"string\",\"any\"],"
            + "\"required\": true"
            + "}";
    private String schema3 = "{"
            + "\"type\": [\"string\",\"null\"],"
            + "\"minLength\": 0"
            + "\"maxLength\": 10"
            + "}";

    @Test
    public void unionWithNullType() throws Exception  {
        JSONParser parser = new JSONParser();
        Map<String, Object> schema = (Map<String, Object>) parser.parse(schema3);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        v.validate(null, null, errorHandler);
        assertFalse(errorHandler.hasError());
    }

    @Test
    public void unionWithBooleanType()  throws Exception {
        JSONParser parser = new JSONParser();
        Map<String, Object> schema = (Map<String, Object>) parser.parse(schema3);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        v.validate(Boolean.TRUE, null, errorHandler);
        assertTrue(errorHandler.hasError());
    }
}
