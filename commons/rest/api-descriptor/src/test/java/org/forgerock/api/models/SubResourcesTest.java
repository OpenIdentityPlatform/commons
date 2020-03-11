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
import static org.forgerock.json.JsonValue.*;

import org.forgerock.api.util.PathUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SubResourcesTest {

    private Resource simpleResource;

    @BeforeClass
    public void before() {
        final Schema schema = Schema.schema()
                .schema(json(object()))
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .response(schema)
                .build();
        simpleResource = Resource.resource().action(action1).mvccSupported(true).build();
    }

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, simpleResource, IllegalArgumentException.class},
                {"", simpleResource, null},
                {"\t", simpleResource, IllegalArgumentException.class},
                {"/contains space", simpleResource, IllegalArgumentException.class},
                {"/uniqueName", null, NullPointerException.class},
                {"/notUniqueName", simpleResource, IllegalStateException.class},
                {"/uniqueName", simpleResource, null},
                {"noLeadingSlash", simpleResource, null},
                {"trailingSlash/", simpleResource, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final Resource resource,
            final Class<? extends Throwable> expectedException) throws Exception {
        final SubResources.Builder builder = SubResources.subresources();

        // add an entry, so that we can test for name-uniqueness
        builder.put("/notUniqueName", simpleResource);

        final SubResources subResources;
        try {
            builder.put(name, resource);
            subResources = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
                return;
            } else {
                throw e;
            }
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        final String normalizedName = name.isEmpty() ? name : PathUtil.buildPath(name);

        assertThat(subResources.get(normalizedName)).isNotNull();
        assertThat(subResources.getNames()).contains(normalizedName, "/notUniqueName");
        assertThat(subResources.getSubResources()).isNotEmpty();
    }

    @Test
    public void testEmptySubResources() {
        final SubResources subResources = SubResources.subresources().build();
        assertThat(subResources.getSubResources()).isEmpty();
    }

}
