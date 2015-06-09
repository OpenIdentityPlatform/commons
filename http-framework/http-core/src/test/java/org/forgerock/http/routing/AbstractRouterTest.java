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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.util.Pair;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractRouterTest {

    private TestAbstractRouter router;

    private Request request;
    @Mock
    private Context context;
    @Mock
    private Handler routeOneHandler;
    @Mock
    private Handler routeTwoHandler;
    @Mock
    private RouteMatcher<Request> routeOneMatcher;
    @Mock
    private RouteMatcher<Request> routeTwoMatcher;
    @Mock
    private RouteMatch routeOneRouteMatch;
    @Mock
    private RouteMatch routeTwoRouteMatch;

    @BeforeMethod
    public void setup() {
        router = new TestAbstractRouter();

        request = new Request();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldSelectRouteTwo() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, false);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, true);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeTwoHandler);
    }

    @Test
    public void shouldSelectRouteOne() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeOneHandler);
    }

    @Test
    public void shouldSelectRouteTwoWhenRouteOneDoesNotMatch() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, false);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, true);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeTwoHandler);
    }

    @Test
    public void shouldSelectRouteOneWhenRouteTwoDoesNotMatch() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeOneHandler);
    }

    @Test
    public void shouldSelectDefaultRouteWhenNoRouteMatches() throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeOneRouteMatch = null;
        RouteMatch routeTwoRouteMatch = null;

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(defaultRouteHandler);
    }

    @Test
    public void shouldReturnNullWhenNoRouteMatchesWithNoDefaultRouteSet() throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeOneRouteMatch = null;
        RouteMatch routeTwoRouteMatch = null;

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute).isNull();
    }

    @Test(expectedExceptions = IncomparableRouteMatchException.class)
    public void shouldThrowIncomparableRouteMatchExceptionWhenRouteMatchesCannotBeCompared()
            throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeOneRouteMatch = mock(RouteMatchB.class);
        RouteMatch routeTwoRouteMatch = mock(RouteMatchA.class);

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupIncomparableRouteMatch(routeOneRouteMatch, routeTwoRouteMatch);
        setupIncomparableRouteMatch(routeTwoRouteMatch, routeOneRouteMatch);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        router.getBestRoute(context, request);

        //Then
        failBecauseExceptionWasNotThrown(IncomparableRouteMatchException.class);
    }

    @Test
    public void creatingRouterFromExistingRouterShouldCopyAllRoutes() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        TestAbstractRouter newRouter = new TestAbstractRouter(router);

        //Then
        assertThat(newRouter.getRoutes()).contains(
                entry(routeOneMatcher, routeOneHandler),
                entry(routeTwoMatcher, routeTwoHandler));
        assertThat(newRouter.getDefaultRoute()).isEqualTo(defaultRouteHandler);
    }

    @Test
    public void shouldAddAllRoutes() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        TestAbstractRouter newRouter = new TestAbstractRouter();

        //When
        newRouter.addAllRoutes(router);

        //Then
        assertThat(newRouter.getRoutes()).contains(
                entry(routeOneMatcher, routeOneHandler),
                entry(routeTwoMatcher, routeTwoHandler));
    }

    @Test
    public void shouldRemoveAllRoutes() {

        //Given
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.removeAllRoutes();

        //Then
        assertThat(router.getRoutes()).isEmpty();
        assertThat(router.getDefaultRoute()).isEqualTo(defaultRouteHandler);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRemoveRoute() {

        //Given
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        boolean isModified = router.removeRoute(routeOneMatcher);

        //Then
        assertThat(isModified).isTrue();
        assertThat(router.getRoutes()).contains(
                entry(routeTwoMatcher, routeTwoHandler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotRemoveRouteIfNotRegistered() {

        //Given
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        boolean isModified = router.removeRoute(routeOneMatcher);

        //Then
        assertThat(isModified).isFalse();
        assertThat(router.getRoutes()).contains(
                entry(routeTwoMatcher, routeTwoHandler));
    }

    private void setupRouteMatch(RouteMatch thisRouteMatch, RouteMatch thatRouteMatch,
            boolean isThisRouteBetter) throws IncomparableRouteMatchException {
        if (thisRouteMatch != null) {
            given(thisRouteMatch.isBetterMatchThan(null)).willReturn(true);
            if (thatRouteMatch != null) {
                given(thisRouteMatch.isBetterMatchThan(thatRouteMatch)).willReturn(isThisRouteBetter);
            }
        }
    }

    private void setupIncomparableRouteMatch(RouteMatch thisRouteMatch, RouteMatch thatRouteMatch)
            throws IncomparableRouteMatchException {
        if (thisRouteMatch != null) {
            given(thisRouteMatch.isBetterMatchThan(null)).willReturn(true);
            if (thatRouteMatch != null) {
                doThrow(IncomparableRouteMatchException.class).when(thisRouteMatch).isBetterMatchThan(thatRouteMatch);
            }
        }
    }

    private static final class TestAbstractRouter extends AbstractRouter<TestAbstractRouter, Request, Handler> {

        protected TestAbstractRouter() {
            super();
        }

        protected TestAbstractRouter(AbstractRouter<TestAbstractRouter, Request, Handler> router) {
            super(router);
        }

        @Override
        protected TestAbstractRouter getThis() {
            return this;
        }
    }

    private static abstract class RouteMatchA implements RouteMatch {
    }

    private static abstract class RouteMatchB implements RouteMatch {
    }
}
