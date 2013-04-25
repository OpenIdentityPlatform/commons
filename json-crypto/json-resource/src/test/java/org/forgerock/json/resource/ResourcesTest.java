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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.resource.TestUtils.content;
import static org.forgerock.json.resource.TestUtils.expected;
import static org.forgerock.json.resource.TestUtils.filter;

import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link Resources}.
 */
@SuppressWarnings("javadoc")
public final class ResourcesTest {

    @DataProvider
    public Object[][] testFilterData() {
        // @formatter:off
        return new Object[][] {

            // Null content
            {
                filter(),
                content(null),
                expected(null)
            },

            {
                filter("/"),
                content(null),
                expected(null)
            },

            {
                filter("/a/b"),
                content(null),
                expected(null)
            },

            {
                filter("/1"),
                content(null),
                expected(null)
            },

            // Empty object
            {
                filter(),
                content(object()),
                expected(object())
            },

            {
                filter("/"),
                content(object()),
                expected(object())
            },

            {
                filter("/a/b"),
                content(object()),
                expected(object())
            },

            {
                filter("/1"),
                content(object()),
                expected(object())
            },

            // Miscellaneous
            {
                filter(),
                content(object(field("a", "1"), field("b", "2"))),
                expected(object(field("a", "1"), field("b", "2")))
            },

            {
                filter("/"),
                content(object(field("a", "1"), field("b", "2"))),
                expected(object(field("a", "1"), field("b", "2")))
            },

            {
                filter("/a"),
                content(object(field("a", "1"), field("b", "2"))),
                expected(object(field("a", "1")))
            },

            {
                filter("/a/b"),
                content(object(field("a", "1"), field("b", "2"))),
                expected(object())
            },

            {
                filter("/a"),
                content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                expected(object(field("a", object(field("b", "1"), field("c", "2")))))
            },

            {
                filter("/a/b"),
                content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                expected(object(field("b", "1")))
            },

            {
                filter("/a/b", "/d"),
                content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                expected(object(field("b", "1"), field("d", "3")))
            },

            {
                filter("/a/b", "/a"),
                content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                expected(object(field("b", "1"), field("a", object(field("b", "1"), field("c", "2")))))
            },

            {
                filter("/a", "/a/b"),
                content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                expected(object(field("a", object(field("b", "1"), field("c", "2"))), field("b", "1")))
            },

        };
        // @formatter:on
    }

    @Test(dataProvider = "testFilterData")
    public void testFilter(List<JsonPointer> filter, JsonValue content, JsonValue expected) {
        assertThat(Resources.filterResource(content, filter).getObject()).isEqualTo(
                expected.getObject());
    }
}
