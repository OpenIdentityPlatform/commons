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
import static org.forgerock.json.resource.RoutingMode.EQUALS;
import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;
import static org.mockito.Mockito.mock;

import org.forgerock.json.resource.Route.RouteMatcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link Route}.
 */
@SuppressWarnings("javadoc")
public final class RouteTest {

    @DataProvider
    public Object[][] testData() {
        return new Object[][] {
            // @formatter:off
            /* mode, template, resourceName, remaining */
            { EQUALS, "test", "test", "" },
            { EQUALS, "test/", "test", "" },
            { EQUALS, "test", "testremaining", null },
            { EQUALS, "users/{id}", "users/1", "" },
            { EQUALS, "users/{id}", "users/1/devices/0", null },
            { STARTS_WITH, "users/{id}", "users/1", "" },
            { STARTS_WITH, "users/{id}", "users/1/devices/0", "devices/0" },
            { STARTS_WITH, "test/", "test/remaining", "remaining" },
            { STARTS_WITH, "test/", "testremaining", null },
            { STARTS_WITH, "test/", "test", "" },
            { STARTS_WITH, "test/", "test/", "" },
            { STARTS_WITH, "test", "test/remaining", "remaining" },
            { STARTS_WITH, "test", "testremaining", null },
            { STARTS_WITH, "test{suffix}", "testabc", "" },
            { STARTS_WITH, "test{suffix}", "testabc/", "" },
            { STARTS_WITH, "test{suffix}", "testabc/123", "123" },
            { STARTS_WITH, "test", "test", "" },
            { STARTS_WITH, "test", "test/", "" }
            // @formatter:on
        };
    }

    @Test(dataProvider = "testData")
    public void testGetRouteMatcher(final RoutingMode mode, final String template,
            final String resourceName, final String expectedRemaining) {
        final Route route = new Route(mode, template, null);
        final RouteMatcher matcher =
                route.getRouteMatcher(mock(ServerContext.class), newReadRequest(resourceName));
        if (expectedRemaining != null) {
            assertThat(matcher).isNotNull();
            assertThat(matcher.getRemaining()).isEqualTo(expectedRemaining);
        } else {
            assertThat(matcher).isNull();
        }
    }
}
