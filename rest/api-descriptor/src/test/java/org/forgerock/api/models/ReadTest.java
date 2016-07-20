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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Arrays;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.Stability;
import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for {@link Read}, but also serves as a coverage test for {@link Operation} itself, because {@code Read}
 * doesn't define any fields of its own.
 */
public class ReadTest {

    private final LocalizableString description = new LocalizableString("My Description");
    private final String[] supportedLocales = new String[]{"en"};
    private ApiError internalServerApiError;
    private ApiError notFoundApiError;
    private Parameter parameter1;
    private Parameter parameter2;

    @BeforeClass
    public void beforeClass() {
        final Schema schema = Schema.schema()
                .schema(json(object()))
                .build();
        notFoundApiError = ApiError.apiError()
                .code(404)
                .description("Not Found")
                .build();
        internalServerApiError = ApiError.apiError()
                .code(500)
                .description("Internal Service ApiError")
                .build();
        parameter1 = Parameter.parameter()
                .name("param1")
                .type("string")
                .source(ParameterSource.ADDITIONAL)
                .build();
        parameter2 = Parameter.parameter()
                .name("param2")
                .type("string")
                .source(ParameterSource.ADDITIONAL)
                .build();
    }

    @Test
    public void testAllOperationFields() {
        final Read read = Read.read()
                .description(description)
                .supportedLocales(supportedLocales)
                .stability(Stability.STABLE)
                .error(notFoundApiError)
                .errors(Arrays.asList(internalServerApiError))
                .parameter(parameter1)
                .parameters(Arrays.asList(parameter2))
                .build();

        // invoke allocateToResource method
        final Resource resource = Resource.resource().operations(read).mvccSupported(true).build();

        assertThat(resource.getRead()).isEqualTo(read);
        assertThat(resource.isMvccSupported()).isTrue();

        assertThat(read.getDescription()).isEqualTo(description);
        assertThat(read.getSupportedLocales()).contains(supportedLocales);
        assertThat(read.getStability()).isEqualTo(Stability.STABLE);
        assertThat(read.getApiErrors()).hasSize(2).contains(internalServerApiError, notFoundApiError);
        assertThat(read.getParameters()).hasSize(2).contains(parameter1, parameter2);
    }

}
