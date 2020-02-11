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

package org.forgerock.api.models;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.api.models.ApiError.*;

import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ErrorsTest {

    private static final ApiError INTERNAL_SERVER_ERROR = apiError().code(500).description(
            new LocalizableString("Internal Service Error")).build();

    private static final ApiError OTHER_EQUAL_ERROR = apiError().code(500).description(
            new LocalizableString("Internal Service Error")).build();

    private static final ApiError OTHER_NON_EQUAL_ERROR = apiError().code(501).description(
            new LocalizableString("Not Supported Error")).build();

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, INTERNAL_SERVER_ERROR, IllegalArgumentException.class},
                {"", INTERNAL_SERVER_ERROR, IllegalArgumentException.class},
                {"\t", INTERNAL_SERVER_ERROR, IllegalArgumentException.class},
                {"contains space", INTERNAL_SERVER_ERROR, IllegalArgumentException.class},
                {"uniqueName", null, NullPointerException.class},
                {"notUniqueName", INTERNAL_SERVER_ERROR, null},
                {"notUniqueName", OTHER_EQUAL_ERROR, null},
                {"notUniqueName", OTHER_NON_EQUAL_ERROR, IllegalStateException.class},
                {"uniqueName", INTERNAL_SERVER_ERROR, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final ApiError apiError,
                        final Class<? extends Throwable> expectedException) {
        final Errors.Builder builder = Errors.errors();

        // add an apiError, so that we can test for name-uniqueness (apiError values do NOT need to be unique)
        builder.put("notUniqueName", INTERNAL_SERVER_ERROR);

        final Errors errors;
        try {
            builder.put(name, apiError);
            errors = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
                return;
            }
            throw e;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(errors.get(name)).isNotNull();
        assertThat(errors.getNames()).contains(name, "notUniqueName");
        assertThat(errors.getErrors()).isNotEmpty();
    }

    @Test
    public void testValidWithEmptyErrors() {
        Errors.errors().build();
    }

}
