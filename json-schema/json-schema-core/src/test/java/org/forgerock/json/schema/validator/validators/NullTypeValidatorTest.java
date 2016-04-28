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

import static org.testng.Assert.*;

import java.util.Map;

import org.forgerock.json.schema.validator.CollectErrorsHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class NullTypeValidatorTest {
    private String schema2 = "{"
            + "\"type\": \"null\","
            + "\"required\": true"
            + "}";
    private String schema3 = "{"
            + "\"type\": \"null\","
            + "\"required\": \"true\""
            + "}";

    @Test
    public void valueIsNull() throws Exception  {
        JSONParser parser = new JSONParser();
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) parser.parse(schema2);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        assertTrue(v.isRequired(), "Required MUST be true");
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        v.validate(null, null, errorHandler);
        assertFalse(errorHandler.hasError());
    }

    @Test
    public void valueIsNotNull()  throws Exception {
        JSONParser parser = new JSONParser();
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) parser.parse(schema3);
        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
        assertTrue(v.isRequired(), "Required MUST be true");
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        v.validate(Boolean.TRUE, null, errorHandler);
        assertTrue(errorHandler.hasError());
    }
}
