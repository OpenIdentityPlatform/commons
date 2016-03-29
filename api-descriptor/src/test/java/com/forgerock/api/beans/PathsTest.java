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

import static org.assertj.core.api.Assertions.*;

import com.forgerock.api.ApiValidationException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PathsTest {

    private final SimplePathNode simplePathNode = new SimplePathNode();

    @DataProvider(name = "putValidationData")
    public Object[][] putValidationData() {
        return new Object[][]{
                {null, null, Exception.class},
                {null, simplePathNode, IllegalArgumentException.class},
                {"", simplePathNode, IllegalArgumentException.class},
                {"\t", simplePathNode, IllegalArgumentException.class},
                {"/contains space", simplePathNode, IllegalArgumentException.class},
                {"/uniqueName", null, NullPointerException.class},
                {"/notUniqueName", simplePathNode, IllegalStateException.class},
                {"/uniqueName", simplePathNode, null},
        };
    }

    @Test(dataProvider = "putValidationData")
    public void testPut(final String name, final SimplePathNode pathNode,
            final Class<? extends Throwable> expectedException) {
        final Paths.Builder<SimplePathNode> builder = Paths.paths(SimplePathNode.class);

        // add an entry, so that we can test for name-uniqueness
        builder.put("/notUniqueName", simplePathNode);

        final Paths<SimplePathNode> paths;
        try {
            builder.put(name, pathNode);
            paths = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(paths.get("/uniqueName")).isNotNull();
        assertThat(paths.getNames()).contains("/uniqueName", "/notUniqueName");
        assertThat(paths.getPaths()).isNotEmpty();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyPaths() {
        Paths.paths(SimplePathNode.class).build();
    }

    /**
     * Simple {@link PathNode} class used for unit tests.
     */
    private static class SimplePathNode implements PathNode {
        // empty
    }

}
