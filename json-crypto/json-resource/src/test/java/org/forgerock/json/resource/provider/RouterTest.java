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

package org.forgerock.json.resource.provider;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.resource.Connections.newInternalConnection;
import static org.forgerock.json.resource.Context.newRootContext;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.provider.Router.URI_TEMPLATE_VARIABLES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.exception.NotFoundException;
import org.forgerock.json.resource.exception.ResourceException;
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

    @Test(expectedExceptions = NotFoundException.class)
    public void testZeroRoutes() throws ResourceException {
        Router router = new Router();
        Context c = newRootContext();
        ReadRequest r = newReadRequest("/object");
        newInternalConnection(router).read(c, r);
    }

    @Test
    public void testDefaultRouteWithZero() throws ResourceException {
        Router router = new Router();
        RequestHandler h = mock(RequestHandler.class);
        router.setDefaultRoute(h);

        Context c = newRootContext();
        ReadRequest r = newReadRequest("/object");
        router.handleRead(c, r, null);
        verify(h).handleRead(c, r, null);
    }

    @Test
    public void testDefaultRouteWithOne() throws ResourceException {
        Router router = new Router();
        RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/users", h1);
        RequestHandler h2 = mock(RequestHandler.class);
        router.setDefaultRoute(h2);

        Context c = newRootContext();
        ReadRequest r = newReadRequest("/object");
        router.handleRead(c, r, null);
        verify(h2).handleRead(c, r, null);
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

    @Test(dataProvider = "absoluteRouteHitTestData")
    public void testAbsoluteRouteHit(String resourceName) throws ResourceException {
        Router router = new Router();
        RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, resourceName, h);
        Context c = newRootContext();
        ReadRequest r = newReadRequest(resourceName);
        router.handleRead(c, r, null);
        verify(h).handleRead(c, r, null);
        assertThat(URI_TEMPLATE_VARIABLES.get(c)).isEmpty();
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

    @Test(dataProvider = "routeMissTestData", expectedExceptions = NotFoundException.class)
    public void testRouteMiss(String template, String resourceName) throws ResourceException {
        Router router = new Router();
        RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
        Context c = newRootContext();
        ReadRequest r = newReadRequest(resourceName);
        try {
            newInternalConnection(router).read(c, r);
        } finally {
            // Ensure that routing failure does not pollute the context.
            verifyZeroInteractions(h);
            assertThat(URI_TEMPLATE_VARIABLES.get(c)).isEmpty();
        }
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

    @Test(dataProvider = "variableRouteHitTestData")
    public void testVariableRouteHit(String template, String resourceName, String[] expectedVars)
            throws ResourceException {
        Router router = new Router();
        RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
        Context c = newRootContext();
        ReadRequest r = newReadRequest(resourceName);
        router.handleRead(c, r, null);
        verify(h).handleRead(c, r, null);
        Map<String, String> expectedMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < expectedVars.length; i += 2) {
            expectedMap.put(expectedVars[i], expectedVars[i + 1]);
        }
        assertThat(URI_TEMPLATE_VARIABLES.get(c)).isEqualTo(expectedMap);
    }

    @Test
    public void testMultipleRoutes() throws ResourceException {
        Router router = new Router();
        RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/users", h1);
        RequestHandler h2 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/groups", h2);

        Context c = newRootContext();
        ReadRequest r1 = newReadRequest("/users");
        router.handleRead(c, r1, null);
        verify(h1).handleRead(c, r1, null);

        ReadRequest r2 = newReadRequest("/groups");
        router.handleRead(c, r2, null);
        verify(h2).handleRead(c, r2, null);
    }

    @Test
    public void testMultipleRoutePrecedence() throws ResourceException {
        Router router = new Router();
        RequestHandler h1 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/object", h1);
        RequestHandler h2 = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, "/{objectId}", h2);

        Context c = newRootContext();
        ReadRequest r1 = newReadRequest("/object");
        router.handleRead(c, r1, null);
        verify(h1).handleRead(c, r1, null);
        ReadRequest r2 = newReadRequest("/thing");
        router.handleRead(c, r2, null);
        verify(h2).handleRead(c, r2, null);
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

    @Test(dataProvider = "invalidTemplatesTestData",
            expectedExceptions = IllegalArgumentException.class)
    public void testInvalidTemplates(String template) throws ResourceException {
        Router router = new Router();
        RequestHandler h = mock(RequestHandler.class);
        router.addRoute(RoutingMode.EQUALS, template, h);
    }

}
