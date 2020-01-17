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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.api.util.PathUtil;
import org.forgerock.http.routing.Version;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PathsTest {

    private VersionedPath simplePathNode;

    @BeforeClass
    public void before() {
        final Schema schema = Schema.schema()
                .schema(json(object()))
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .response(schema)
                .build();
        simplePathNode = VersionedPath.versionedPath()
                .put(Version.version(1), Resource.resource().action(action1).mvccSupported(true).build())
                .build();
    }

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, simplePathNode, IllegalArgumentException.class},
                {"", simplePathNode, null},
                {"\t", simplePathNode, IllegalArgumentException.class},
                {"/contains space", simplePathNode, IllegalArgumentException.class},
                {"/uniqueName", null, NullPointerException.class},
                {"/notUniqueName", simplePathNode, IllegalStateException.class},
                {"/uniqueName", simplePathNode, null},
                {"noLeadingSlash", simplePathNode, null},
                {"trailingSlash/", simplePathNode, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final VersionedPath pathNode,
            final Class<? extends Throwable> expectedException) throws Exception {
        final Paths.Builder builder = Paths.paths();

        // add an entry, so that we can test for name-uniqueness
        builder.put("/notUniqueName", simplePathNode);

        final Paths paths;
        try {
            builder.put(name, pathNode);
            paths = builder.build();
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

        assertThat(paths.get(normalizedName)).isNotNull();
        assertThat(paths.getNames()).contains(normalizedName, "/notUniqueName");
        assertThat(paths.getPaths()).isNotEmpty();
    }

    @Test
    public void testEmptyPaths() {
        final Paths paths = Paths.paths().build();
        assertThat(paths.getPaths()).isEmpty();
    }

}
