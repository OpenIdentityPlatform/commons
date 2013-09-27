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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.resource.Requests.newReadRequest;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link Requests}.
 */
@SuppressWarnings("javadoc")
public final class RequestsTest {

    @DataProvider
    public Object[][] containerNames() {
        return new Object[][] {
            // @formatter:off
            { "", "test" },
            { "/", "test" },
            { "users", "users/test" },
            { "users/", "users/test" },
            { "/users", "users/test" },
            { "/users/", "users/test" },
            // @formatter:on
        };
    }

    @Test(dataProvider = "containerNames")
    public void testResourceNameConcatenation(final String container,
            final String expectedResourceName) {
        final Request request = newReadRequest(container, "test");
        assertThat(request.getResourceName()).isEqualTo(expectedResourceName);
    }

}
