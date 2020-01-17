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

import org.forgerock.json.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DefinitionsTest {

    private static final Schema OBJECT_SCHEMA = Schema.schema().type(Object.class).build();
    private static final Schema OTHER_EQUAL_SCHEMA = Schema.schema().type(Object.class).build();
    private static final Schema OTHER_NON_EQUAL_SCHEMA = Schema.schema().type(JsonValue.class).build();


    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, OBJECT_SCHEMA, IllegalArgumentException.class},
                {"", OBJECT_SCHEMA, IllegalArgumentException.class},
                {"\t", OBJECT_SCHEMA, IllegalArgumentException.class},
                {"contains space", OBJECT_SCHEMA, IllegalArgumentException.class},
                {"uniqueName", null, NullPointerException.class},
                {"notUniqueName", OBJECT_SCHEMA, null},
                {"notUniqueName", OTHER_EQUAL_SCHEMA, null},
                {"notUniqueName", OTHER_NON_EQUAL_SCHEMA, IllegalStateException.class},
                {"uniqueName", OBJECT_SCHEMA, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final Schema schema, final Class<? extends Throwable> expectedException) {
        final Definitions.Builder builder = Definitions.definitions();

        // add an entry, so that we can test for name-uniqueness
        builder.put("notUniqueName", OBJECT_SCHEMA);

        final Definitions definitions;
        try {
            builder.put(name, schema);
            definitions = builder.build();
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

        assertThat(definitions.get(name)).isNotNull();
        assertThat(definitions.getNames()).contains(name, "notUniqueName");
        assertThat(definitions.getDefinitions()).isNotEmpty();
    }

    @Test
    public void testValidWithEmptyDefinitions() {
        Definitions.definitions().build();
    }

}
