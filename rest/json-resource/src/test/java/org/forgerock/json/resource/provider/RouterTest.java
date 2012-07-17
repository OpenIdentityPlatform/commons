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

import static org.forgerock.json.resource.Context.*;
import static org.forgerock.json.resource.Requests.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.NotFoundException;
import org.forgerock.json.resource.exception.ResourceException;
import org.testng.annotations.Test;

/**
 * Tests {@code Router}.
 */
public final class RouterTest {

    @Test
    public void testActionSingletonSuccess() throws ResourceException {
        // Request parameters.
        Context c = newRootContext();
        ActionRequest r = newActionRequest("/object", "test");
        @SuppressWarnings("unchecked")
        ResultHandler<JsonValue> h = mock(ResultHandler.class);

        // Provider / router.
        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
        RoutingStrategy s = mock(RoutingStrategy.class);
        when(s.routeRequest(c, r)).thenReturn(new RoutingResult(p));
        Router router = new Router(s);
        router.action(c, r, h);

        // Check that request was routed.
        verify(p).actionInstance(c, r, h);
    }

    @Test
    public void testActionSingletonRedundantID() throws ResourceException {
        // Request parameters.
        Context c = newRootContext();
        ActionRequest r = newActionRequest("/object", "id", "test");
        @SuppressWarnings("unchecked")
        ResultHandler<JsonValue> h = mock(ResultHandler.class);

        // Provider / router.
        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
        RoutingStrategy s = mock(RoutingStrategy.class);
        when(s.routeRequest(c, r)).thenReturn(new RoutingResult(p));
        Router router = new Router(s);
        router.action(c, r, h);

        // Check that request was never routed but the handler was invoked.
        verifyZeroInteractions(p);
        verify(h).handleError(isA(BadRequestException.class));
    }

    @Test
    public void testActionCollection() throws ResourceException {
        // Request parameters.
        Context c = newRootContext();
        ActionRequest r = newActionRequest("/object", "test");
        @SuppressWarnings("unchecked")
        ResultHandler<JsonValue> h = mock(ResultHandler.class);

        // Provider / router.
        CollectionResourceProvider p = mock(CollectionResourceProvider.class);
        RoutingStrategy s = mock(RoutingStrategy.class);
        when(s.routeRequest(c, r)).thenReturn(new RoutingResult(p));
        Router router = new Router(s);
        router.action(c, r, h);

        // Check that request was routed.
        verify(p).actionCollection(c, r, h);
    }

    @Test
    public void testActionCollectionInstance() throws ResourceException {
        // Request parameters.
        Context c = newRootContext();
        ActionRequest r = newActionRequest("/object", "id", "test");
        @SuppressWarnings("unchecked")
        ResultHandler<JsonValue> h = mock(ResultHandler.class);

        // Provider / router.
        CollectionResourceProvider p = mock(CollectionResourceProvider.class);
        RoutingStrategy s = mock(RoutingStrategy.class);
        when(s.routeRequest(c, r)).thenReturn(new RoutingResult(p));
        Router router = new Router(s);
        router.action(c, r, h);

        // Check that request was routed.
        verify(p).actionInstance(c, r, h);
    }

    @Test
    public void testActionNotFound() throws ResourceException {
        // Request parameters.
        Context c = newRootContext();
        ActionRequest r = newActionRequest("/object", "id", "test");
        @SuppressWarnings("unchecked")
        ResultHandler<JsonValue> h = mock(ResultHandler.class);

        // Provider / router.
        SingletonResourceProvider p = mock(SingletonResourceProvider.class);
        RoutingStrategy s = mock(RoutingStrategy.class);
        when(s.routeRequest(c, r)).thenThrow(new NotFoundException());
        Router router = new Router(s);
        router.action(c, r, h);

        // Check that request was never routed but the handler was invoked.
        verifyZeroInteractions(p);
        verify(h).handleError(isA(NotFoundException.class));
    }

    // TODO: complete test suite once APIs are stabilized.
}
