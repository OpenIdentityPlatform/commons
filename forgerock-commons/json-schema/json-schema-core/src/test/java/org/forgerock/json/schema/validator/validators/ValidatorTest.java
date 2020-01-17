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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.validators;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("javadoc")
public class ValidatorTest {

    @Test
    public void getPathTest()  throws SchemaException {
        Map<String, Object> schema =  new HashMap<>();
        schema.put(Constants.REQUIRED, Boolean.TRUE);
        Validator validator = new Validator(schema, null) {
            @Override
            public void validate(Object node, JsonPointer at, ErrorHandler handler) throws SchemaException {
            }
        };
        Assert.assertTrue(validator.required, "Required MUST be True");
        Assert.assertEquals(validator.getPath(null, null), new JsonPointer());
        Assert.assertEquals(validator.getPath(new JsonPointer("/path"), null), new JsonPointer("path"));
        Assert.assertEquals(validator.getPath(null, "path"), new JsonPointer("path"));
    }
}
