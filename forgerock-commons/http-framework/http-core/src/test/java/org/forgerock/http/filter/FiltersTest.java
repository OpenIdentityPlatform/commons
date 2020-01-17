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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.http.filter;

import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.CountDownLatch;

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
public class FiltersTest {

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
    private Context context;
    private Request request;

    @DataProvider
    public static Object[][] chainOfs() {
        //@Checkstyle:off
        return new Object[][] {
                { new FiltersChainProducer() },
        };
        //@Checkstyle:on
    }

    @Factory(dataProvider = "chainOfs")
    public FiltersTest(ChainProducer producer) {
        this.producer = producer;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        context = new RootContext();
        request = new Request();
    }

    @Test
    public void shouldCreateChainWithNoFilter() throws Exception {
        Filter chain = producer.chainOf();

        chain.filter(context, request, terminal);

        verify(terminal).handle(context, request);
    }

    @Test
    public void shouldCreateChainWithOneFilter() throws Exception {
        Filter chain = producer.chainOf(first);

        chain.filter(context, request, terminal);

        InOrder order = Mockito.inOrder(first, terminal);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal).handle(context, request);
    }

    @Test
    public void shouldCreateChainWithTwoFilters() throws Exception {
        Filter chain = producer.chainOf(first, second);

        chain.filter(context, request, terminal);

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
        Filter chain = producer.chainOf(doubler);

        chain.filter(context, request, terminal);

        InOrder order = Mockito.inOrder(doubler, terminal);
        order.verify(doubler).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal, times(2)).handle(context, request);
    }

    @Test
    public void shouldAllowFiltersToBeSharedInDifferentChains() throws Exception {
        Filter chain1 = producer.chainOf(first);
        Filter chain2 = producer.chainOf(first);

        chain1.filter(context, request, terminal);
        chain2.filter(context, request, terminal2);

        InOrder order = Mockito.inOrder(terminal, terminal2, first);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal).handle(context, request);
        order.verify(first).filter(same(context), same(request), any(Handler.class));
        order.verify(terminal2).handle(context, request);
    }

    /**
     * This test is an attempt to showcase a use case where a chain would be used by 2 concurrent threads,
     * but with different target handler.
     */
    @Test
    public void shouldBeStateless() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        Filter a = new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                try {
                    latch.await();
                    return next.handle(context, request);
                } catch (InterruptedException e) {
                    return newResponsePromise(new Response(Status.INTERNAL_SERVER_ERROR));
                }
            }
        };

        // Need at least 2 filters for the demonstration
        Filter b = new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                return next.handle(context, request);
            }
        };

        final Filter chain = producer.chainOf(a, b);

        // Launch a first execution with 'terminal' as ending handler
        // will block in the latch
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                chain.filter(context, request, terminal);
            }
        };
        thread1.start();

        // Execute the same chain, with a different ending handler
        // will block in the latch as well (but will change handler.next for all executions)
        Thread thread2 = new Thread() {
            @Override
            public void run() {
                // unblock the main thread
                latch2.countDown();
                chain.filter(context, request, terminal2);
            }
        };
        thread2.start();

        // wait for the 2nd thread to start
        latch2.await();
        // release the latch
        latch.countDown();

        thread1.join();
        thread2.join();

        verify(terminal).handle(context, request);
        verify(terminal2).handle(context, request);
    }

    @Test
    public void shouldAllowFilterToShortcutExecution() throws Exception {
        Filter chain = producer.chainOf(shortcut);

        chain.filter(context, request, terminal);

        verify(shortcut).filter(same(context), same(request), any(Handler.class));
        verifyZeroInteractions(terminal);
    }

    private interface ChainProducer {
        Filter chainOf(Filter... filters);
    }

    private static class FiltersChainProducer implements ChainProducer {
        @Override
        public Filter chainOf(Filter... filters) {
            return Filters.chainOf(filters);
        }
    }

    @Override
    public String toString() {
        // Helps to identify which provider failed the test (only appears in the text report on the console AFAICT)
        return String.format("%s.with{ %s }", getClass().getSimpleName(), producer.getClass().getSimpleName());
    }
}
