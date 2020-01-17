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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.forgerock.json.schema.validator.CollectErrorsHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ObjectTypeValidatorTest extends ValidatorTestBase {

    @DataProvider(name = "invalid-schema-objects")
    public Iterator<Object[]> invalidSchemaObject() throws Exception {
        List<Object[]> tests = getTestJSON("invalid", "/objectTests.json");
        return tests.iterator();
    }

    @DataProvider(name = "valid-schema-objects")
    public Iterator<Object[]> validSchemaObject() throws Exception {
        List<Object[]> tests = getTestJSON("valid", "/objectTests.json");
        return tests.iterator();
    }

    @Test(dataProvider = "valid-schema-objects")
    public void validateValidObjects(Validator validator, Object instance) throws Exception {
        Assert.assertNotNull(validator);
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        validator.validate(instance, null, errorHandler);
        assertFalse(errorHandler.hasError(), errorHandler.getExceptions().toString());
    }

    @Test(dataProvider = "invalid-schema-objects")
    public void validateInvalidObjects(Validator validator, Object instance) throws Exception {
        Assert.assertNotNull(validator);
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        validator.validate(instance, null, errorHandler);
        assertTrue(errorHandler.hasError(), errorHandler.getExceptions().toString());
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = SchemaException.class)
    public void invalidReferenceInSchema() throws Exception {
        JSONParser parser = new JSONParser();
        Object o = parser.parse("{ \"$ref\" : \"#/definitions/unknown\" }");
        ObjectValidatorFactory.getTypeValidator((Map<String, Object>) o);
    }
}
