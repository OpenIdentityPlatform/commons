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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Requests.newReadRequest;

import org.forgerock.http.ResourcePath;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link Requests}.
 */
@SuppressWarnings("javadoc")
public final class RequestsTest {

    @DataProvider
    public Object[][] containerPaths() {
        return new Object[][] {
            // @formatter:off
            { "", "test", "test" },
            { "/", "test", "test" },
            { "users", "test", "users/test" },
            { "users/", "test", "users/test" },
            { "/users", "test", "users/test" },
            { "/users/", "test", "users/test" },
            { "users", "test user", "users/test%20user" },
            { "users", "test/user", "users/test%2Fuser" },
            { "users", "test+user", "users/test+user" }
            // @formatter:on
        };
    }

    @Test(dataProvider = "containerPaths")
    public void testResourcePathConcatenation(final String container,
            final String id, final String expectedResourcePath) {
        final Request request = newReadRequest(container, id);
        assertThat(request.getResourcePath()).isEqualTo(expectedResourcePath);
    }

    // test the contract that resource name must not be null
    @Test(expectedExceptions = NullPointerException.class)
    public void testNullResourcePath() {
        newReadRequest((String) null);
    }

    // test the contract that request.getResourcePath/getResourcePathObject
    // always returns non-null by creating a "legit" request and then
    // updating the ResourcePath to null using the setter
    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullResourcePath() {
        final Request request = newReadRequest("/hello");
        request.setResourcePath((ResourcePath) null);
    }

    @Test
    public void testResourceIdEscaping1() {
        Request r = Requests.newReadRequest("test/users/forward%2fslash");
        assertThat(r.getResourcePath()).isEqualTo("test/users/forward%2fslash");
        assertThat(r.getResourcePathObject().leaf()).isEqualTo("forward/slash");
        assertThat(r.getResourcePathObject()).hasSize(3);
    }

    @Test
    public void testResourceIdEscaping2() {
        Request r = Requests.newReadRequest("test/users", "forward/slash");
        assertThat(r.getResourcePath()).isEqualTo("test/users/forward%2Fslash");
        assertThat(r.getResourcePathObject().leaf()).isEqualTo("forward/slash");
        assertThat(r.getResourcePathObject()).hasSize(3);
    }

    @Test
    public void testNewResourceIdEscaping() {
        CreateRequest r =
                Requests.newCreateRequest("test/users", "forward/slash", new JsonValue(null));
        assertThat(r.getResourcePath()).isEqualTo("test/users");
        assertThat(r.getResourcePathObject().leaf()).isEqualTo("users");
        assertThat(r.getResourcePathObject()).hasSize(2);
        assertThat(r.getNewResourceId()).isEqualTo("forward/slash");
    }

}
