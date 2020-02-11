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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.forgerock.api.ApiValidationException;
import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ApiErrorTest {

    @DataProvider(name = "buildValidationData")
    public Object[][] buildValidationData() {
        return new Object[][]{
                {null, null, ApiValidationException.class},
                {500, null, ApiValidationException.class},
                {null, new LocalizableString("my description"), ApiValidationException.class},
                {500, new LocalizableString("my description"), null},
        };
    }

    @Test(dataProvider = "buildValidationData")
    public void testBuildValidation(final Integer code, final LocalizableString description,
            final Class<? extends Throwable> expectedException) {
        final ApiError.Builder builder = ApiError.apiError();
        final ApiError apiError;
        try {
            if (code != null) {
                builder.code(code);
            }
            if (description != null) {
                builder.description(description);
            }
            apiError = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(apiError.getCode()).isEqualTo(code);
        assertThat(apiError.getDescription()).isEqualTo(description);
        assertThat(apiError.getSchema()).isNull();
    }

    @Test
    public void testSchemaField() {
        final Schema schema = Schema.schema()
                .type(Object.class)
                .build();

        final ApiError apiError = ApiError.apiError()
                .code(500)
                .description("my description")
                .schema(schema)
                .build();

        assertThat(apiError.getSchema()).isNotNull();
    }

}
