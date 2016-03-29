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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.forgerock.api.ApiValidationException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ErrorsTest {

    private Error internalServerError;

    @BeforeClass
    public void beforeClass() {
        internalServerError = Error.error().code(500).description("Internal Service Error").build();
    }

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, internalServerError, IllegalArgumentException.class},
                {"", internalServerError, IllegalArgumentException.class},
                {"\t", internalServerError, IllegalArgumentException.class},
                {"contains space", internalServerError, IllegalArgumentException.class},
                {"uniqueName", null, NullPointerException.class},
                {"notUniqueName", internalServerError, IllegalStateException.class},
                {"uniqueName", internalServerError, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final Error error, final Class<? extends Throwable> expectedException) {
        final Errors.Builder builder = Errors.errors();

        // add an error, so that we can test for name-uniqueness (error values do NOT need to be unique)
        builder.put("notUniqueName", internalServerError);

        final Errors errors;
        try {
            builder.put(name, error);
            errors = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(errors.get("uniqueName")).isNotNull();
        assertThat(errors.getNames()).contains("uniqueName", "notUniqueName");
        assertThat(errors.getErrors()).isNotEmpty();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyErrors() {
        Errors.errors().build();
    }

}
