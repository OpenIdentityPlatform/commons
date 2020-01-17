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

package org.forgerock.json.schema.validator.helpers;

import org.forgerock.json.schema.validator.CollectErrorsHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class MinimumHelperTest {

    @Test
    public void testValidateFloat() throws Exception {
        MinimumHelper instance = new MinimumHelper(new Float("99.03"), true);
        CollectErrorsHandler handler = new CollectErrorsHandler();
        instance.validate(new Float("100.01"), null, handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(new Double("100.02"), null, handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(100, null, handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        //Exceptions
        instance.validate(new Float("95.01"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 1);
        instance.validate(new Double("95.02"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 2);
        instance.validate(95, null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 3);
        instance.validate(new Float("99.03"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 4);
        instance.validate(new Double("99.03"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 5);
    }

    @Test
    public void testValidateDouble() throws Exception {
        MinimumHelper instance = new MinimumHelper(new Double("99.03"), false);
        CollectErrorsHandler handler = new CollectErrorsHandler();
        instance.validate(new Float("100.01"), null, handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(new Double("100.02"), null, handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(100, null, handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        //Exceptions
        instance.validate(new Float("95.01"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 1);
        instance.validate(new Double("95.02"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 2);
        instance.validate(95, null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 3);
//        1 == new Double("99.03").compareTo((new Float("99.03")).doubleValue());
//        instance.validate(new Float("99.03"), null, handler);
//        Assert.assertEquals(handler.getExceptions().size(), 3);
        instance.validate(new Double("99.03"), null, handler);
        Assert.assertEquals(handler.getExceptions().size(), 3);
    }
}
