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
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests {@code VersionRouter}.
 */
@SuppressWarnings("javadoc")
public final class RouterTest {

    // TODO: starts with match
    // TODO: more precedence tests
    // TODO: test collection/singleton
    // TODO: route registration/deregistation

    @DataProvider
    public Object[][] absoluteRouteHitTestData() {
        // @formatter:off
        return new Object[][] {
            /* resource name */
            { "" },
            { "a" },
            { "a/b" },
            { "a/b/c" },
            { "one/two/three" }, // Check multi-char path elements.
        };
        // @formatter:on
    }

    @Test(dataProvider = "absoluteRouteHitTestData")
    public void testAbsoluteRouteHit(final String resourceName) {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, resourceName, h);
        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest(resourceName);
        router.handleRead(c, r, null);
        final ArgumentCaptor<RouterContext> rc = ArgumentCaptor.forClass(RouterContext.class);
        verify(h).handleRead(rc.capture(), Matchers.<ReadRequest> any(),
                Matchers.<ResultHandler<Resource>> any());
        checkRouterContext(rc, c, resourceName);
    }

    private void checkRouterContext(ArgumentCaptor<RouterContext> rc, final ServerContext c,
            final String expectedMatchedUri, final String key, final String value) {
        checkRouterContext(rc, c, expectedMatchedUri, Collections.singletonMap(key, value));
    }

    private void checkRouterContext(ArgumentCaptor<RouterContext> rc, final ServerContext c,
            final String expectedMatchedUri) {
        checkRouterContext(rc, c, expectedMatchedUri, Collections.<String, String>emptyMap());
    }

    private void checkRouterContext(ArgumentCaptor<RouterContext> rc, final ServerContext c,
            final String expectedMatchedUri, final Map<String, String> expectedUriTemplateVariables) {
        assertThat(rc.getValue().getParent()).isSameAs(c);
        assertThat(rc.getValue().getMatchedUri()).isEqualTo(expectedMatchedUri);
        assertThat(rc.getValue().getBaseUri()).isEqualTo(expectedMatchedUri);
        assertThat(rc.getValue().getUriTemplateVariables()).isEqualTo(expectedUriTemplateVariables);
    }

    @DataProvider
    public Object[][] invalidTemplatesTestData() {
        // @formatter:off
        return new Object[][] {
            /* invalid template */
            { "{" },
            { "{}" },
            { "{a" },
            { "{a/b" },
            { "{a/{b}" }
        };
        // @formatter:on
    }

    @Test(dataProvider = "invalidTemplatesTestData",
            expectedExceptions = IllegalArgumentException.class)
    public void testInvalidTemplates(final String template) {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
    }

    @Test
    public void testMultipleRoutePrecedence() {
        final Router router = new Router();
        final RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "object", h1);
        final RequestHandler h2 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "{objectId}", h2);

        final ServerContext c = newServerContext(router);
        final ReadRequest r1 = newReadRequest("object");
        router.handleRead(c, r1, null);
        final ArgumentCaptor<RouterContext> rc1 = ArgumentCaptor.forClass(RouterContext.class);
        verify(h1).handleRead(rc1.capture(), Matchers.<ReadRequest> any(),
                Matchers.<ResultHandler<Resource>> any());
        checkRouterContext(rc1, c, "object");

        final ReadRequest r2 = newReadRequest("thing");
        router.handleRead(c, r2, null);
        final ArgumentCaptor<RouterContext> rc2 = ArgumentCaptor.forClass(RouterContext.class);
        verify(h2).handleRead(rc2.capture(), Matchers.<ReadRequest> any(),
                Matchers.<ResultHandler<Resource>> any());
        checkRouterContext(rc2, c, "thing", "objectId", "thing");
    }

    @Test
    public void testMultipleRoutes() {
        final Router router = new Router();
        final RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "users", h1);
        final RequestHandler h2 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "groups", h2);

        final ServerContext c = newServerContext(router);
        final ReadRequest r1 = newReadRequest("users");
        router.handleRead(c, r1, null);
        final ArgumentCaptor<RouterContext> rc1 = ArgumentCaptor.forClass(RouterContext.class);
        verify(h1).handleRead(rc1.capture(), Matchers.<ReadRequest> any(),
                Matchers.<ResultHandler<Resource>> any());
        checkRouterContext(rc1, c, "users");

        final ReadRequest r2 = newReadRequest("groups");
        router.handleRead(c, r2, null);
        final ArgumentCaptor<RouterContext> rc2 = ArgumentCaptor.forClass(RouterContext.class);
        verify(h2).handleRead(rc2.capture(), Matchers.<ReadRequest> any(),
                Matchers.<ResultHandler<Resource>> any());
        checkRouterContext(rc2, c, "groups");
    }

    @DataProvider
    public Object[][] routeMissTestData() {
        // @formatter:off
        return new Object[][] {
            /* template - resource name */
            { "", "a" },
            { "a", "" },
            { "a", "b" },
            { "a/b", "a" },
            { "a", "a/b" },
            { "a/b", "b/b" },
            { "one/two", "one/twox" },
            { "one/twox", "one/two" },
            { "{a}", "one/two" },
            { "{a}/{b}", "one/two/three" },
            { "one/{a}/{b}", "one/two" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "routeMissTestData", expectedExceptions = NotFoundException.class)
    public void testRouteMiss(final String template, final String resourceName)
            throws ResourceException {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template)
                .addVersion("1.0", h);
        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest(resourceName);
        try {
            newInternalConnection(router).read(c, r);
        } finally {
            // Ensure that routing failure does not pollute the context.
            verifyZeroInteractions(h);
        }
    }

    @DataProvider
    public Object[][] variableRouteHitTestData() {
        // @formatter:off
        return new Object[][] {
            /* template - resource name - variables */
            { "{userId}", "a", new String[] {"userId", "a" }},
            { "{userId}", "test", new String[] {"userId", "test" }},
            { "x{userId}", "xtest", new String[] {"userId", "test" }},
            { "{userId}/devices", "test/devices", new String[] {"userId", "test" }},
            { "{a}/{b}", "aaa/bbb", new String[] {"a", "aaa", "b", "bbb" }},
            { "{a}/b/{c}", "aaa/b/ccc", new String[] {"a", "aaa", "c", "ccc" }},
            { "users/{id}/devices", "users/test%20user/devices", new String[] {"id", "test user" }},
            { "users/{id}/devices", "users/test+%2buser/devices", new String[] {"id", "test++user" }},
            { "users/{id}/devices", "users/test%2fdevices/devices", new String[] {"id", "test/devices" }},
        };
        // @formatter:on
    }

    @Test(dataProvider = "variableRouteHitTestData")
    public void testVariableRouteHit(final String template, final String resourceName,
            final String[] expectedVars) {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest(resourceName);
        router.handleRead(c, r, null);
        final Map<String, String> expectedMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < expectedVars.length; i += 2) {
            expectedMap.put(expectedVars[i], expectedVars[i + 1]);
        }
        final ArgumentCaptor<RouterContext> rc = ArgumentCaptor.forClass(RouterContext.class);
        verify(h).handleRead(rc.capture(), Matchers.<ReadRequest> any(),
                Matchers.<ResultHandler<Resource>> any());
        checkRouterContext(rc, c, resourceName, expectedMap);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testZeroRoutes() throws ResourceException {
        final Router router = new Router();
        final Context c = new RootContext();
        final ReadRequest r = newReadRequest("object");
        newInternalConnection(router).read(c, r);
    }

    private ServerContext newServerContext(final RequestHandler handler) {
        return new ServerContext(new RootContext());
    }

}
