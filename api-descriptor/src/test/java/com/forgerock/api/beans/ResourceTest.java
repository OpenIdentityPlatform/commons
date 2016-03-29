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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.forgerock.api.ApiValidationException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ResourceTest {

    private final String description = "My Description";
    private Schema schema;
    private Action action1;
    private Action action2;
    // TODO add all other operations to the tests below

    @BeforeClass
    public void beforeClass() {
        schema = Schema.schema()
                .schema(json(object()))
                .build();
        action1 = Action.action()
                .name("action1")
                .response(schema)
                .build();
        action2 = Action.action()
                .name("action2")
                .response(schema)
                .build();

    }

    /**
     * Test the {@Resource} builder with builder-methods that do <em>not</em> take arrays as arguments.
     */
    @Test
    public void testBuilderWithNonArrayMethods() {

        final Resource.Builder builder = Resource.resource();
        builder.description(description);
        builder.action(action1);
        builder.action(action2);

        final Resource resource = builder.build();

        assertTestBuilder(resource);
    }

    /**
     * Test the {@Resource} builder with builder-methods that take arrays as arguments.
     */
    @Test
    public void testBuilderWithArrayMethods() {

        final Resource.Builder builder = Resource.resource();
        builder.description(description);
        builder.actions(asList(action1, action2));

        final Resource resource = builder.build();

        assertTestBuilder(resource);
    }

    private void assertTestBuilder(final Resource resource) {
        assertThat(resource.getDescription()).isEqualTo(description);
        assertThat(resource.getActions()).contains(action1, action2);
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyResource() {
        Resource.resource().build();
    }

}
