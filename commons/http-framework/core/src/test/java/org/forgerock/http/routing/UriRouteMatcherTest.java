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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.routing;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.mockito.Mockito.mock;

import java.net.URI;

import org.forgerock.services.context.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UriRouteMatcherTest {

    @DataProvider
    public Object[][] testData() {
        return new Object[][] {
            // @formatter:off
            /* mode,       template,       resourceName,        remaining */
            { EQUALS,      "",             "/test",             null },
            { EQUALS,      "test",         "test",              "" },
            { EQUALS,      "test/",        "test",              "" },
            { EQUALS,      "test",         "testremaining",     null },
            { EQUALS,      "users/{id}",   "users/1",           "" },
            { EQUALS,      "users/{id}",   "users/1/devices/0", null },
            { STARTS_WITH, "users/{id}",   "users/1",           "" },
            { STARTS_WITH, "users/{id}",   "users/1/devices/0", "devices/0" },
            { STARTS_WITH, "test/",        "test/remaining",    "remaining" },
            { STARTS_WITH, "test/",        "testremaining",     null },
            { STARTS_WITH, "test/",        "test",              "" },
            { STARTS_WITH, "test/",        "test/",             "" },
            { STARTS_WITH, "test/",        "test//",            "/" },
            { STARTS_WITH, "test",         "test/remaining",    "remaining" },
            { STARTS_WITH, "test",         "testremaining",     null },
            { STARTS_WITH, "test{suffix}", "testabc",           "" },
            { STARTS_WITH, "test{suffix}", "testabc/",          "" },
            { STARTS_WITH, "test{suffix}", "testabc/123",       "123" },
            { STARTS_WITH, "test",         "test",              "" },
            { STARTS_WITH, "test",         "test/",             "" },
            { STARTS_WITH, "",             "",                  "" },
            { STARTS_WITH, "",             "123",               "123" },
            { STARTS_WITH, "",             "123/456",           "123/456" },
            { STARTS_WITH, "",             "/123/456",           "123/456" },
            // @formatter:on
        };
    }

    @Test(dataProvider = "testData")
    public void shouldEvaluateRoute(RoutingMode mode, String template, String resourceName, String expectedRemaining) {
        RouteMatcher<Request> routeMatcher = newUriRouteMatcher(mode, template);
        Context context = mock(Context.class);
        Request request = mockRequest(resourceName);
        RouteMatch result = routeMatcher.evaluate(mock(Context.class), request);
        if (expectedRemaining != null) {
            assertThat(result).isNotNull();
            UriRouterContext uriRouterContext = result.decorateContext(context).asContext(UriRouterContext.class);
            assertThat(uriRouterContext.getRemainingUri()).isEqualTo(expectedRemaining);
        } else {
            assertThat(result).isNull();
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfTemplateVariableIsEmpty() {

        //Given
        RoutingMode mode = EQUALS;
        String template = "users/{}";

        //When
        newUriRouteMatcher(mode, template);

        //Then
        failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfTemplateVariableContainsIllegalCharacter() {

        //Given
        RoutingMode mode = EQUALS;
        String template = "users/{&}";

        //When
        newUriRouteMatcher(mode, template);

        //Then
        failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfTemplateVariableIsNotClosed() {

        //Given
        RoutingMode mode = EQUALS;
        String template = "users/{";

        //When
        newUriRouteMatcher(mode, template);

        //Then
        failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    }

    @Test
    public void shouldEvaluateRouteWithMultipleTemplateVariables() {

        //Given
        RoutingMode mode = EQUALS;
        String template = "users/{user}/roles/{role}";
        String resourceName = "users/demo/roles/admin";
        RouteMatcher<Request> routeMatcher = newUriRouteMatcher(mode, template);
        Context context = mock(Context.class);
        Request request = mockRequest(resourceName);

        //When
        RouteMatch result = routeMatcher.evaluate(mock(Context.class), request);

        //Then
        assertThat(result).isNotNull();
        UriRouterContext uriRouterContext = result.decorateContext(context).asContext(UriRouterContext.class);
        assertThat(uriRouterContext.getRemainingUri()).isEqualTo("");
        assertThat(uriRouterContext.getUriTemplateVariables())
                .hasSize(2)
                .contains(entry("user", "demo"), entry("role", "admin"));
    }

    @DataProvider
    private Object[][] toStringData() {
        return new Object[][]{
            {EQUALS, "equals(users/{id})"},
            {STARTS_WITH, "startsWith(users/{id})"},
        };
    }

    @Test(dataProvider = "toStringData")
    public void shouldToStringUriRouteMatcher(RoutingMode mode, String expectedString) {

        //Given
        RouteMatcher<Request> routeMatcher = newUriRouteMatcher(mode, "users/{id}");

        //When
        String toString = routeMatcher.toString();

        //Then
        assertThat(toString).isEqualTo(expectedString);
    }

    @SuppressWarnings("unchecked")
    @DataProvider
    private Object[][] equalsData() {
        RouteMatcher<Request> routeMatcher = newUriRouteMatcher(EQUALS, "users/{id}");
        RouteMatcher<Object> differentTypeRouteMatcher = mock(RouteMatcher.class);
        return new Object[][]{
            {routeMatcher, routeMatcher, true},
            {routeMatcher, differentTypeRouteMatcher, false},
            {routeMatcher, newUriRouteMatcher(STARTS_WITH, "users/{id}"), false},
            {routeMatcher, newUriRouteMatcher(EQUALS, "users/{user}"), false},
            {routeMatcher, newUriRouteMatcher(EQUALS, "users/{id}"), true},
        };
    }

    @Test(dataProvider = "equalsData")
    public void shouldPerformEquals(RouteMatcher<Request> routeMatcherOne,
            RouteMatcher<Object> routeMatcherTwo, boolean expectedToBeEqual) {

        //When
        boolean isEqual = routeMatcherOne.equals(routeMatcherTwo);

        //Then
        if (expectedToBeEqual) {
            assertThat(isEqual).isTrue();
        } else {
            assertThat(isEqual).isFalse();
        }
    }

    @Test(dataProvider = "equalsData")
    public void shouldPerformHashcode(RouteMatcher<Request> routeMatcherOne,
            RouteMatcher<Object> routeMatcherTwo, boolean expectedToBeEqual) {

        //When
        int hashCodeOne = routeMatcherOne.hashCode();
        int hashCodeTwo = routeMatcherTwo.hashCode();

        //Then
        if (expectedToBeEqual) {
            assertThat(hashCodeOne).isEqualTo(hashCodeTwo);
        } else {
            assertThat(hashCodeOne).isNotEqualTo(hashCodeTwo);
        }
    }

    @DataProvider
    private Object[][] isBetterMatchData() {
        RouteMatch routeMatch = newRouteMatch(EQUALS, "users/{id}", "users/demo");
        return new Object[][]{
            {routeMatch, null, true},
            {routeMatch, newRouteMatch(EQUALS, "users/{id}", "users/demos"), false},
            {routeMatch, newRouteMatch(STARTS_WITH, "users/{id}", "users/demo"), true},
            {routeMatch, newRouteMatch(EQUALS, "{type}/{id}", "users/demo"), true},
        };
    }

    @Test(dataProvider = "isBetterMatchData")
    public void shouldDetermineIfIsBetterMatch(RouteMatch routeMatchOne, RouteMatch routeMatchTwo,
            boolean isExpectedToBeBetterMatch) throws IncomparableRouteMatchException {

        //When
        boolean isBetterMatchThan = routeMatchOne.isBetterMatchThan(routeMatchTwo);

        //Then
        assertThat(isBetterMatchThan).isEqualTo(isExpectedToBeBetterMatch);
    }

    @Test(expectedExceptions = IncomparableRouteMatchException.class)
    public void ifIsBetterMatchShouldThrowIncomparableRouteMatchExceptionWhenRouteMatchIsOfDifferentType()
            throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeMatch = newRouteMatch(EQUALS, "users/{id}", "users/demo");
        RouteMatch differentRouteMatch = newDifferentRouteMatchType();

        //When
        routeMatch.isBetterMatchThan(differentRouteMatch);

        //Then
        failBecauseExceptionWasNotThrown(IncomparableRouteMatchException.class);
    }

    private Request mockRequest(String resourceName) {
        return new Request().setUri(URI.create(resourceName));
    }

    private RouteMatcher<Request> newUriRouteMatcher(RoutingMode mode, String uriTemplate) {
        return RouteMatchers.requestUriMatcher(mode, uriTemplate);
    }

    private RouteMatch newRouteMatch(RoutingMode mode, String uriTemplate, String resourceName) {
        Context context = mock(Context.class);
        Request request = mockRequest(resourceName);
        return newUriRouteMatcher(mode, uriTemplate).evaluate(context, request);
    }

    private RouteMatch newDifferentRouteMatchType() {
        return mock(RouteMatch.class);
    }
}
