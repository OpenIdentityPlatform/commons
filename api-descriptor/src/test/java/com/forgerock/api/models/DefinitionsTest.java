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

package com.forgerock.api.models;

import static org.assertj.core.api.Assertions.*;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DefinitionsTest {

    private Schema objectSchema;

    @BeforeClass
    public void beforeClass() {
        objectSchema = Schema.schema().type(Object.class).build();
    }

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, objectSchema, IllegalArgumentException.class},
                {"", objectSchema, IllegalArgumentException.class},
                {"\t", objectSchema, IllegalArgumentException.class},
                {"contains space", objectSchema, IllegalArgumentException.class},
                {"uniqueName", null, NullPointerException.class},
                {"notUniqueName", objectSchema, IllegalStateException.class},
                {"uniqueName", objectSchema, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final Schema schema, final Class<? extends Throwable> expectedException) {
        final Definitions.Builder builder = Definitions.definitions();

        // add an entry, so that we can test for name-uniqueness
        builder.put("notUniqueName", objectSchema);

        final Definitions definitions;
        try {
            builder.put(name, schema);
            definitions = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(definitions.get("uniqueName")).isNotNull();
        assertThat(definitions.getNames()).contains("uniqueName", "notUniqueName");
        assertThat(definitions.getDefinitions()).isNotEmpty();
    }

    @Test
    public void testValidWithEmptyDefinitions() {
        Definitions.definitions().build();
    }

}
