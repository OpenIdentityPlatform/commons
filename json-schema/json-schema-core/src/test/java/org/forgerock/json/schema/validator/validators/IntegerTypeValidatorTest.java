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
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;

@SuppressWarnings("javadoc")
public class IntegerTypeValidatorTest extends ValidatorTestBase{

    @DataProvider(name = "invalid-schema-objects")
    public Iterator<Object[]> invalidSchemaObject() throws Exception {
        List<Object[]> tests = getTestJSON("invalid","/integerTests.json");
        return tests.iterator();
    }

    @DataProvider(name = "valid-schema-objects")
    public Iterator<Object[]> validSchemaObject() throws Exception {
        List<Object[]> tests = getTestJSON("valid","/integerTests.json");
        return tests.iterator();
    }

    @Test(dataProvider = "valid-schema-objects")
    public void validateValidObjects(Validator validator, Object instance) throws Exception {
        Assert.assertNotNull(validator);
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        validator.validate(instance, null, errorHandler);
        assertFalse(errorHandler.hasError());
    }

    @Test(dataProvider = "invalid-schema-objects")
    public void validateInvalidObjects(Validator validator, Object instance) throws SchemaException  {
        Assert.assertNotNull(validator);
        CollectErrorsHandler errorHandler = new CollectErrorsHandler();
        validator.validate(instance, null, errorHandler);
        assertTrue(errorHandler.hasError());
    }
}
