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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.validators;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;

@SuppressWarnings("javadoc")
public abstract class ValidatorTestBase {
    protected List<Object[]> getTestJSON(String instanceType, String testFilePath) throws IOException, ParseException {
        InputStream is = ArrayTypeValidatorTest.class.getResourceAsStream(testFilePath);
        Assert.assertNotNull(is);
        JSONParser parser = new JSONParser();
        Object o = parser.parse(new InputStreamReader(is));
        Assert.assertTrue(o instanceof List, "Expect JSON Array");
        List<Object[]> tests = new ArrayList<>();
        for (Object s : (List<?>) o) {
            Assert.assertTrue(s instanceof Map, "Expect JSON Object");
            @SuppressWarnings("unchecked")
            Validator v =
                    ObjectValidatorFactory.getTypeValidator((Map<String, Object>) ((Map<?, ?>) s)
                            .get("schema"));
            for (Object i : (List<?>) ((Map<?, ?>) s).get(instanceType)) {
                tests.add(new Object[]{v, i});
            }
        }
        return tests;
    }
}
