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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.http.handler;

import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public final class HandlersTest {

    private final Handler terminal = spy(new Handler() {
        @Override
        public Promise<Response, NeverThrowsException> handle(final Context context, final Request request) {
            return newResponsePromise(new Response(Status.OK));
        }
    });

    private final Handler terminal2 = spy(new Handler() {
        @Override
        public Promise<Response, NeverThrowsException> handle(final Context context, final Request request) {
            return newResponsePromise(new Response(Status.NOT_FOUND));
        }
    });

    private final Filter first = spy(new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(final Context context, final Request request,
                final Handler next) {
            return next.handle(context, request);
        }
    });

    private final Filter second = spy(new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(final Context context, final Request request,
                                                              final Handler next) {
            return next.handle(context, request);
        }
    });

    private final Filter doubler = spy(new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(final Context context, final Request request,
                                                              final Handler next) {
            next.handle(context, request);
            next.handle(context, request);
            return newResponsePromise(new Response(Status.NOT_FOUND));
        }
    });

    private final Filter shortcut = spy(new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(final Context context, final Request request,
                                                              final Handler next) {
            return newResponsePromise(new Response(Status.TEAPOT));
        }
    });

    private final ChainProducer producer;

    private RootContext context;

    private Request request;

    @DataProvider
    public static Object[][] chainOfProducers() {
        return new Object[][]{
                {new HandlersChainProducer()},
        };
    }

    @Factory(dataProvider = "chainOfProducers")
    public HandlersTest(ChainProducer producer) {
        this.producer = producer;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        context = new RootContext();
        request = new Request();
    }

    @Test
    public void shouldCreateChainWithNoFilter() throws Exception {
        Handler chain = producer.chainOf(terminal);

        chain.handle(context, request);

        verify(terminal).handle(context, request);
    }

    @Test
    public void shouldCreateChainWithOneFilter() throws Exception {
        Handler chain = producer.chainOf(terminal, first);

        chain.handle(context, request);

        InOrder order = Mockito.inOrder(first, terminal);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal).handle(context, request);
    }

    @Test
    public void shouldCreateChainWithTwoFilters() throws Exception {
        Handler chain = producer.chainOf(terminal, first, second);

        chain.handle(context, request);

        InOrder order = Mockito.inOrder(first, second, terminal);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(second).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal).handle(context, request);
    }

    /**
     * Tests that a filter can call next handler multiple times. If there is a cursoring
     * mechanism that increments the index for each call then the first invocation
     * will iterate to the target causing the second invocation to go directly to
     * the target rather than the next filter.
     */
    @Test
    public void shouldCallNextHandler2Times() throws Exception {
        Handler chain = producer.chainOf(terminal, doubler);

        chain.handle(context, request);

        InOrder order = Mockito.inOrder(doubler, terminal);
        order.verify(doubler).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal, times(2)).handle(context, request);
    }

    @Test
    public void shouldAllowFiltersToBeSharedInDifferentChains() throws Exception {
        Handler chain1 = producer.chainOf(terminal, first);
        Handler chain2 = producer.chainOf(terminal2, first);

        chain1.handle(context, request);
        chain2.handle(context, request);

        InOrder order = Mockito.inOrder(terminal, terminal2, first);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal).handle(context, request);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal2).handle(context, request);
    }

    @Test
    public void shouldAllowFilterToShortcutExecution() throws Exception {
        Handler chain = producer.chainOf(terminal, shortcut);

        chain.handle(context, request);

        verify(shortcut).filter(same(context), same(request), any(Handler.class));
        verifyZeroInteractions(terminal);
    }

    private interface ChainProducer {
        Handler chainOf(Handler terminal, Filter... filters);
    }

    private static class HandlersChainProducer implements ChainProducer {
        @Override
        public Handler chainOf(Handler terminal, Filter... filters) {
            return Handlers.chainOf(terminal, filters);
        }
    }
}
