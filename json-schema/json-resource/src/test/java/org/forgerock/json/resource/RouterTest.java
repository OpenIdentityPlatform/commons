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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@code Router}.
 */
@SuppressWarnings("javadoc")
public final class RouterTest {

    // TODO: starts with match
    // TODO: more precedence tests
    // TODO: test collection/singleton
    // TODO: route registration/deregistation

    private static final class IsRouteContext extends ArgumentMatcher<ServerContext> {
        private final Map<String, String> expectedUriTemplateVariables;
        private final ServerContext parent;

        private IsRouteContext(final ServerContext parent,
                final Map<String, String> expectedUriTemplateVariables) {
            this.parent = parent;
            this.expectedUriTemplateVariables = expectedUriTemplateVariables;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(final Object argument) {
            if (argument instanceof RouterContext) {
                final RouterContext context = (RouterContext) argument;
                return context.getParent() == parent
                        && context.getUriTemplateVariables().equals(expectedUriTemplateVariables);
            } else {
                return false;
            }
        }
    }

    private static ArgumentMatcher<ServerContext> isRouteContext(final ServerContext parent) {
        return new IsRouteContext(parent, Collections.<String, String> emptyMap());
    }

    private static ArgumentMatcher<ServerContext> isRouteContext(final ServerContext parent,
            final Map<String, String> expectedUriTemplateVariables) {
        return new IsRouteContext(parent, expectedUriTemplateVariables);
    }

    private static ArgumentMatcher<ServerContext> isRouteContext(final ServerContext parent,
            final String key, final String value) {
        return new IsRouteContext(parent, Collections.singletonMap(key, value));
    }

    @DataProvider
    public Object[][] absoluteRouteHitTestData() {
        // @formatter:off
        return new Object[][] {
            { "" },
            { "/" },
            { "/a" },
            { "/a/" },
            { "/a/b" },
            { "/a/b/" },
            { "/a/b/c" },
            { "/a/b/c/" },
            { "/one/two/three" }, // Check multi-char path elements.
        };
        // @formatter:on
    }

    @DataProvider
    public Object[][] invalidTemplatesTestData() {
        // @formatter:off
        return new Object[][] {
            { "/{" },
            { "/{}" },
            { "/{a" },
            { "/{a/b" },
            { "/{a/{b}" }
        };
        // @formatter:on
    }

    @DataProvider
    public Object[][] routeMissTestData() {
        // @formatter:off
        return new Object[][] {
            { "/", "/a" },
            { "/a", "/" },
            { "/a", "/b" },
            { "/a/b", "/a" },
            { "/a", "/a/b" },
            { "/a/b", "/b/b" },
            { "/one/two", "/one/twox" },
            { "/one/twox", "/one/two" },
            { "/{a}", "/one/two" },
            { "/{a}/{b}", "/one/two/three" },
            { "/one/{a}/{b}", "/one/two" },
        };
        // @formatter:on
    }

    @Test(dataProvider = "absoluteRouteHitTestData")
    public void testAbsoluteRouteHit(final String resourceName) throws ResourceException {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, resourceName, h);
        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest(resourceName);
        router.handleRead(c, r, null);
        verify(h).handleRead(argThat(isRouteContext(c)), same(r),
                Matchers.<ResultHandler<Resource>> any());
    }

    @Test
    public void testDefaultRouteWithOne() throws ResourceException {
        final Router router = new Router();
        final RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/users", h1);
        final RequestHandler h2 = mock(RequestHandler.class);
        router.setDefaultRoute(h2);

        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest("/object");
        router.handleRead(c, r, null);
        verify(h2).handleRead(argThat(isRouteContext(c)), same(r),
                Matchers.<ResultHandler<Resource>> any());
    }

    @Test
    public void testDefaultRouteWithZero() throws ResourceException {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.setDefaultRoute(h);

        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest("/object");
        router.handleRead(c, r, null);
        verify(h).handleRead(argThat(isRouteContext(c)), same(r),
                Matchers.<ResultHandler<Resource>> any());
    }

    @Test(dataProvider = "invalidTemplatesTestData",
            expectedExceptions = IllegalArgumentException.class)
    public void testInvalidTemplates(final String template) throws ResourceException {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
    }

    @Test
    public void testMultipleRoutePrecedence() throws ResourceException {
        final Router router = new Router();
        final RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/object", h1);
        final RequestHandler h2 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/{objectId}", h2);

        final ServerContext c = newServerContext(router);
        final ReadRequest r1 = newReadRequest("/object");
        router.handleRead(c, r1, null);
        verify(h1).handleRead(argThat(isRouteContext(c)), same(r1),
                Matchers.<ResultHandler<Resource>> any());
        final ReadRequest r2 = newReadRequest("/thing");
        router.handleRead(c, r2, null);
        verify(h2).handleRead(argThat(isRouteContext(c, "objectId", "thing")), same(r2),
                Matchers.<ResultHandler<Resource>> any());
    }

    @Test
    public void testMultipleRoutes() throws ResourceException {
        final Router router = new Router();
        final RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/users", h1);
        final RequestHandler h2 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/groups", h2);

        final ServerContext c = newServerContext(router);
        final ReadRequest r1 = newReadRequest("/users");
        router.handleRead(c, r1, null);
        verify(h1).handleRead(argThat(isRouteContext(c)), same(r1),
                Matchers.<ResultHandler<Resource>> any());

        final ReadRequest r2 = newReadRequest("/groups");
        router.handleRead(c, r2, null);
        verify(h2).handleRead(argThat(isRouteContext(c)), same(r2),
                Matchers.<ResultHandler<Resource>> any());
    }

    @Test(dataProvider = "routeMissTestData", expectedExceptions = NotFoundException.class)
    public void testRouteMiss(final String template, final String resourceName)
            throws ResourceException {
        final Router router = new Router();
        final RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
        final ServerContext c = newServerContext(router);
        final ReadRequest r = newReadRequest(resourceName);
        try {
            newInternalConnection(router).read(c, r);
        } finally {
            // Ensure that routing failure does not pollute the context.
            verifyZeroInteractions(h);
        }
    }

    @Test(dataProvider = "variableRouteHitTestData")
    public void testVariableRouteHit(final String template, final String resourceName,
            final String[] expectedVars) throws ResourceException {
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
        verify(h).handleRead(argThat(isRouteContext(c, expectedMap)), same(r),
                Matchers.<ResultHandler<Resource>> any());
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testZeroRoutes() throws ResourceException {
        final Router router = new Router();
        final Context c = new RootContext();
        final ReadRequest r = newReadRequest("/object");
        newInternalConnection(router).read(c, r);
    }

    @DataProvider
    public Object[][] variableRouteHitTestData() {
        // @formatter:off
        return new Object[][] {
            { "/{userId}", "/a", new String[] {"userId", "a" }},
            { "/{userId}", "/a/", new String[] {"userId", "a" }},
            { "/{userId}", "/test", new String[] {"userId", "test" }},
            { "/{userId}", "/test/", new String[] {"userId", "test" }},
            { "/x{userId}", "/xtest", new String[] {"userId", "test" }},
            { "/{userId}/devices", "/test/devices", new String[] {"userId", "test" }},
            { "/{a}/{b}", "/aaa/bbb", new String[] {"a", "aaa", "b", "bbb" }},
            { "/{a}/b/{c}", "/aaa/b/ccc", new String[] {"a", "aaa", "c", "ccc" }},
        };
        // @formatter:on
    }

    private ServerContext newServerContext(final RequestHandler handler) {
        return new ServerContext(new RootContext(), newInternalConnection(handler));
    }

}
