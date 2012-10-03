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
import static org.forgerock.json.resource.Context.newRootContext;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.fail;

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.exception.NotFoundException;
import org.forgerock.json.resource.exception.ResourceException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@code UriTemplateRoutingStrategy}.
 */
public final class UriTemplateRoutingStrategyTest {

//    @Test(expectedExceptions = NotFoundException.class)
//    public void testZeroRoutes() throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        Context c = newRootContext();
//        Request r = newReadRequest("/object");
//        s.routeRequest(c, r);
//    }
//
//    @DataProvider
//    public Object[][] absoluteRouteHitTestData() {
//        // @formatter:off
//        return new Object[][] {
//            { "" },
//            { "/" },
//            { "/a" },
//            { "/a/" },
//            { "/a/b" },
//            { "/a/b/" },
//            { "/a/b/c" },
//            { "/a/b/c/" },
//            { "/one/two/three" }, // Check multi-char path elements.
//        };
//        // @formatter:on
//    }
//
//    @Test(dataProvider = "absoluteRouteHitTestData")
//    public void testAbsoluteRouteHit(String component) throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
//        s.register(component, p);
//        Context c = newRootContext();
//        Request r = newReadRequest(component);
//        assertThat(s.routeRequest(c, r).asSingleton()).isSameAs(p);
//        assertThat(URI_TEMPLATE_VARIABLES.get(c)).isEmpty();
//    }
//
//    @DataProvider
//    public Object[][] routeMissTestData() {
//        // @formatter:off
//        return new Object[][] {
//            { "/", "/a" },
//            { "/a", "/" },
//            { "/a", "/b" },
//            { "/a/b", "/a" },
//            { "/a", "/a/b" },
//            { "/a/b", "/b/b" },
//            { "/one/two", "/one/twox" },
//            { "/one/twox", "/one/two" },
//            { "/{a}", "/one/two" },
//            { "/{a}/{b}", "/one/two/three" },
//            { "/one/{a}/{b}", "/one/two" },
//        };
//        // @formatter:on
//    }
//
//    @Test(dataProvider = "routeMissTestData", expectedExceptions = NotFoundException.class)
//    public void testRouteMiss(String template, String component) throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
//        s.register(template, p);
//        Context c = newRootContext();
//        Request r = newReadRequest(component);
//        try {
//            s.routeRequest(c, r);
//        } finally {
//            // Ensure that routing failure does not pollute the context.
//            assertThat(URI_TEMPLATE_VARIABLES.get(c)).isEmpty();
//        }
//    }
//
//    @DataProvider
//    public Object[][] variableRouteHitTestData() {
//        // @formatter:off
//        return new Object[][] {
//            { "/{userId}", "//", new String[] {"userId", "" }},
//            { "/{userId}", "/a", new String[] {"userId", "a" }},
//            { "/{userId}", "/a/", new String[] {"userId", "a" }},
//            { "/{userId}", "/test", new String[] {"userId", "test" }},
//            { "/{userId}", "/test/", new String[] {"userId", "test" }},
//            { "/x{userId}", "/xtest", new String[] {"userId", "test" }},
//            { "/{userId}/devices", "/test/devices", new String[] {"userId", "test" }},
//            { "/{a}/{b}", "/aaa/bbb", new String[] {"a", "aaa", "b", "bbb" }},
//            { "/{a}/b/{c}", "/aaa/b/ccc", new String[] {"a", "aaa", "c", "ccc" }},
//        };
//        // @formatter:on
//    }
//
//    @Test(dataProvider = "variableRouteHitTestData")
//    public void testVariableRouteHit(String template, String component, String[] expectedVars)
//            throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
//        s.register(template, p);
//        Context c = newRootContext();
//        Request r = newReadRequest(component);
//        assertThat(s.routeRequest(c, r).asSingleton()).isSameAs(p);
//        Map<String, String> expectedMap = new LinkedHashMap<String, String>();
//        for (int i = 0; i < expectedVars.length; i += 2) {
//            expectedMap.put(expectedVars[i], expectedVars[i + 1]);
//        }
//        assertThat(URI_TEMPLATE_VARIABLES.get(c)).isEqualTo(expectedMap);
//    }
//
//    @Test
//    public void testMultipleRoutes() throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p1 = mock(SingletonResourceProvider.class);
//        SingletonResourceProvider p2 = mock(SingletonResourceProvider.class);
//        s.register("/users", p1);
//        s.register("/groups", p2);
//
//        Context c = newRootContext();
//        Request r1 = newReadRequest("/users");
//        assertThat(s.routeRequest(c, r1).asSingleton()).isSameAs(p1);
//        Request r2 = newReadRequest("/groups");
//        assertThat(s.routeRequest(c, r2).asSingleton()).isSameAs(p2);
//    }
//
//    @Test
//    public void testMultipleRoutePrecedence() throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p1 = mock(SingletonResourceProvider.class);
//        SingletonResourceProvider p2 = mock(SingletonResourceProvider.class);
//        s.register("/object", p1);
//        s.register("/{objectId}", p2);
//
//        Context c = newRootContext();
//        Request r1 = newReadRequest("/object");
//        assertThat(s.routeRequest(c, r1).asSingleton()).isSameAs(p1);
//        Request r2 = newReadRequest("/thing");
//        assertThat(s.routeRequest(c, r2).asSingleton()).isSameAs(p2);
//    }
//
//    @Test
//    public void testRouteReregistration1() throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p1 = mock(SingletonResourceProvider.class);
//        SingletonResourceProvider p2 = mock(SingletonResourceProvider.class);
//        s.register("/users", p1);
//        Context c = newRootContext();
//        Request r = newReadRequest("/users");
//        assertThat(s.routeRequest(c, r).asSingleton()).isSameAs(p1);
//        s.register("/users", p2);
//        assertThat(s.routeRequest(c, r).asSingleton()).isSameAs(p2);
//    }
//
//    @Test
//    public void testRouteReregistration2() throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        CollectionResourceProvider p1 = mock(CollectionResourceProvider.class);
//        CollectionResourceProvider p2 = mock(CollectionResourceProvider.class);
//        s.register("/users", p1);
//        Context c = newRootContext();
//        Request r = newReadRequest("/users");
//        assertThat(s.routeRequest(c, r).asCollection()).isSameAs(p1);
//        s.register("/users", p2);
//        assertThat(s.routeRequest(c, r).asCollection()).isSameAs(p2);
//    }
//
//    @Test
//    public void testRouteDeregistration1() throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p1 = mock(SingletonResourceProvider.class);
//        SingletonResourceProvider p2 = mock(SingletonResourceProvider.class);
//        s.register("/users", p1);
//        s.register("/groups", p2);
//        Context c = newRootContext();
//        Request r1 = newReadRequest("/users");
//        Request r2 = newReadRequest("/groups");
//        assertThat(s.routeRequest(c, r1).asSingleton()).isSameAs(p1);
//        assertThat(s.routeRequest(c, r2).asSingleton()).isSameAs(p2);
//        s.deregister("/users");
//        try {
//            s.routeRequest(c, r1);
//            fail("Routing unexpected succeeded");
//        } catch (NotFoundException e) {
//            // Expected.
//        }
//        assertThat(s.routeRequest(c, r2).asSingleton()).isSameAs(p2);
//    }
//
//    @DataProvider
//    public Object[][] invalidTemplatesTestData() {
//        // @formatter:off
//        return new Object[][] {
//            { "/{" },
//            { "/{}" },
//            { "/{a" },
//            { "/{a/b" },
//            { "/{a/{b}" },
//            { "/{a}{b}" }
//        };
//        // @formatter:on
//    }
//
//    @Test(dataProvider = "invalidTemplatesTestData",
//            expectedExceptions = IllegalArgumentException.class)
//    public void testInvalidTemplates(String template) throws ResourceException {
//        UriTemplateRoutingStrategy s = new UriTemplateRoutingStrategy();
//        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
//        s.register(template, p);
//    }
}
