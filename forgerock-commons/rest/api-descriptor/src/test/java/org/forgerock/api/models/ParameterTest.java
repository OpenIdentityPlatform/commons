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

import static org.forgerock.api.enums.ParameterSource.ADDITIONAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.forgerock.api.ApiValidationException;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ParameterTest {

    @DataProvider(name = "buildValidationData")
    public Object[][] buildValidationData() {
        return new Object[][]{
                // invalid
                {null, null, null, null, null, ApiValidationException.class},
                {null, "string", ADDITIONAL, null, null, ApiValidationException.class},
                {"name", null, ADDITIONAL, null, null, ApiValidationException.class},
                {"name", "string", null, null, null, ApiValidationException.class},
                {"name", "string", ADDITIONAL, null, new String[]{"t"}, ApiValidationException.class},

                // valid
                {"name", "string", ADDITIONAL, new String[]{"v"}, new String[]{"t", "tt"}, null},
                {"name", "string", ADDITIONAL, new String[]{"v"}, new String[]{"t"}, null},
                {"name", "string", ADDITIONAL, new String[]{"v"}, null, null},
                {"name", "string", ADDITIONAL, null, null, null},
        };
    }

    @Test(dataProvider = "buildValidationData")
    public void testBuildValidation(final String name, final String type, final ParameterSource source,
            final String[] enumValues, final String[] enumTitles, final Class<? extends Throwable> expectedException) {
        final Parameter.Builder builder = Parameter.parameter();
        final Parameter parameter;
        try {
            if (name != null) {
                builder.name(name);
            }
            if (type != null) {
                builder.type(type);
            }
            if (source != null) {
                builder.source(source);
            }
            if (enumValues != null) {
                builder.enumValues(enumValues);
            }
            if (enumTitles != null) {
                builder.enumTitles(enumTitles);
            }
            parameter = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(parameter.getName()).isEqualTo(name);
        assertThat(parameter.getType()).isEqualTo(type);
        assertThat(parameter.getSource()).isEqualTo(source);
    }

    @Test
    public void testAllFields() {
        final String name = "param1";
        final String type = "string";
        final String defaultValue = "None";
        final String description = "My Description";
        final ParameterSource source = ADDITIONAL;
        final boolean required = true;
        final String[] enumValues = new String[]{"None", "All", "Some"};
        final String[] enumTitles = new String[]{"No Results", "All Results", "Some Results"};

        final Parameter parameter = Parameter.parameter()
                .name(name)
                .type(type)
                .defaultValue(defaultValue)
                .description(description)
                .source(source)
                .required(required)
                .enumValues(enumValues)
                .enumTitles(enumTitles)
                .build();

        assertThat(parameter.getName()).isEqualTo(name);
        assertThat(parameter.getType()).isEqualTo(type);
        assertThat(parameter.getDefaultValue()).isEqualTo(defaultValue);
        assertThat(parameter.getDescription().toTranslatedString(new PreferredLocales())).isEqualTo(description);
        assertThat(parameter.getSource()).isEqualTo(source);
        assertThat(parameter.isRequired()).isEqualTo(required);
        assertThat(parameter.getEnumValues()).isEqualTo(enumValues);
        assertThat(parameter.getEnumTitles()).isEqualTo(enumTitles);
    }

}
