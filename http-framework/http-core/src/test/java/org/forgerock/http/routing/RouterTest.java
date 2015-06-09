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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RouterTest {

    private Router router;

    private Context context;
    private Request request;

    @BeforeMethod
    public void setup() {
        router = new Router();

        context = mock(Context.class);
        request = new Request();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void creatingRouterFromExistingRouterShouldCopyAllRoutes() {

        //Given
        RouteMatcher<Request> routeOneMatcher = mock(RouteMatcher.class);
        RouteMatcher<Request> routeTwoMatcher = mock(RouteMatcher.class);
        Handler routeOneHandler = mock(Handler.class);
        Handler routeTwoHandler = mock(Handler.class);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        Router newRouter = new Router(router);

        //Then
        assertThat(newRouter.getRoutes()).contains(
                entry(routeOneMatcher, routeOneHandler),
                entry(routeTwoMatcher, routeTwoHandler));
        assertThat(newRouter.getDefaultRoute()).isEqualTo(defaultRouteHandler);
    }

    @Test
    public void handleShouldCallBestRoute() {

        //Given
        Handler defaultRouteHandler = mock(Handler.class);

        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handle(context, request);

        //Then
        verify(defaultRouteHandler).handle(any(Context.class), eq(request));
    }

    @Test
    public void handleShouldReturn404ResponseExceptionIfNoRouteFound() throws Exception {

        //Given
        request.setUri("http://example.com:8080/json/users");

        //When
        Promise<Response, NeverThrowsException> promise = router.handle(context, request);

        //Then
        Response response = promise.getOrThrowUninterruptibly();
        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void handleShouldReturn500ResponseExceptionIfIncompatibleRouteMatchExceptionThrown() throws Exception {

        //Given
        RouteMatcher<Request> routeMatcher = mock(RouteMatcher.class);
        RouteMatch routeMatch = mock(RouteMatch.class);
        Handler routeHandler = mock(Handler.class);
        router.addRoute(routeMatcher, routeHandler);
        request.setUri("http://example.com:8080/json/users");

        given(routeMatcher.evaluate(any(Context.class), any(Request.class))).willReturn(routeMatch);
        doThrow(IncomparableRouteMatchException.class).when(routeMatch).isBetterMatchThan(any(RouteMatch.class));

        //When
        Promise<Response, NeverThrowsException> promise = router.handle(context, request);

        //Then
        Response response = promise.getOrThrowUninterruptibly();
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }
}
