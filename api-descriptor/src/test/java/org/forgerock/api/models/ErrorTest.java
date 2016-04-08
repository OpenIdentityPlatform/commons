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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ErrorTest {

    @DataProvider(name = "buildValidationData")
    public Object[][] buildValidationData() {
        return new Object[][]{
                {null, null, ApiValidationException.class},
                {500, null, ApiValidationException.class},
                {null, "my description", ApiValidationException.class},
                {500, "my description", null},
        };
    }

    @Test(dataProvider = "buildValidationData")
    public void testBuildValidation(final Integer code, final String description,
            final Class<? extends Throwable> expectedException) {
        final Error.Builder builder = Error.error();
        final Error error;
        try {
            if (code != null) {
                builder.code(code);
            }
            if (description != null) {
                builder.description(description);
            }
            error = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getDescription()).isEqualTo(description);
        assertThat(error.getSchema()).isNull();
    }

    @Test
    public void testSchemaField() {
        final Schema schema = Schema.schema()
                .type(Object.class)
                .build();

        final Error error = Error.error()
                .code(500)
                .description("my description")
                .schema(schema)
                .build();

        assertThat(error.getSchema()).isNotNull();
    }

}
