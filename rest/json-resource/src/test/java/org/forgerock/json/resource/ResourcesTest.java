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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.resource.TestUtils.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.resource.core.RootContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

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

    @DataProvider
    public Object[][] testCollectionResourceProviderData() {
        // @formatter:off
        return new Object[][] {
            { "test", "test" },
            { "test%2fuser", "test/user" },
            { "test user", "test user" },
            { "test%20user", "test user" },
            { "test+%2buser", "test++user" }
        };
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    @Test(dataProvider = "testCollectionResourceProviderData")
    public void testCollectionResourceProvider(String resourceName, String expectedId)
            throws Exception {
        CollectionResourceProvider collection = mock(CollectionResourceProvider.class);
        RequestHandler handler = Resources.newCollection(collection);
        Connection connection = Resources.newInternalConnection(handler);
        ReadRequest read = Requests.newReadRequest(resourceName);
        connection.readAsync(new RootContext(), read);
        ArgumentCaptor<ReadRequest> captor = ArgumentCaptor.forClass(ReadRequest.class);
        verify(collection).readInstance(any(ServerContext.class), eq(expectedId), captor.capture(),
                any(ResultHandler.class));
        assertThat(captor.getValue().getResourceName()).isEqualTo("");
    }
}
