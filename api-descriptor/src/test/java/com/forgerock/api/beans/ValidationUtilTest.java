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

package com.forgerock.api.beans;

import static com.forgerock.api.beans.ValidationUtil.containsWhitespace;
import static com.forgerock.api.beans.ValidationUtil.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ValidationUtilTest {

    private enum Method {
        containsWhitespace,
        isEmptyString,
        isEmptyArray
    }

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][]{
                // containsWhitespace(String)
                {"", Method.containsWhitespace, false},
                {"noSpace", Method.containsWhitespace, false},
                {"has space", Method.containsWhitespace, true},

                // isEmpty(String)
                {null, Method.isEmptyString, true},
                {"", Method.isEmptyString, true},
                {"\t", Method.isEmptyString, true},
                {"noSpace", Method.isEmptyString, false},
                {"has space", Method.isEmptyString, false},

                // isEmpty(Object[])
                {null, Method.isEmptyArray, true},
                {new String[0], Method.isEmptyArray, true},
                {new String[]{"item"}, Method.isEmptyArray, false},
                {new String[]{null}, Method.isEmptyArray, false},
        };
    }

    @Test(dataProvider = "data")
    public void testUtilMethods(final Object input, final Method method, final boolean expectedValue) {
        switch (method) {
        case containsWhitespace:
            assertThat(containsWhitespace((String) input)).isEqualTo(expectedValue);
            break;
        case isEmptyString:
            assertThat(isEmpty((String) input)).isEqualTo(expectedValue);
            break;
        case isEmptyArray:
            assertThat(isEmpty((Object[]) input)).isEqualTo(expectedValue);
            break;
        default:
            throw new IllegalStateException("Unhandled method: " + method);
        }
    }

}
