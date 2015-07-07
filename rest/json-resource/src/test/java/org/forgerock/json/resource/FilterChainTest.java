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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.when;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.http.context.RootContext;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

/**
 * Tests the FilterChain class based on the use cases in CREST-7.
 */
@SuppressWarnings({ "javadoc", "unchecked" })
public final class FilterChainTest {
    private static final JsonValue JSON = new JsonValue(Collections.singletonMap("test", "value"));
    private static final QueryResult QUERY_RESULT = new QueryResult();
    private static final Resource RESOURCE = new Resource("id", "rev", JSON);

    /**
     * Tests that a filter can call next filter multiple times. If the cursoring
     * mechanism increments the index for each call then the first invocation
     * will iterate to the target causing the second invocation to go direct to
     * the target rather than the next filter.
     */
    @Test
    public void testFilterCanInvokeMultipleSubRequests() {
        final RequestHandler target = target();
        final Filter filter1 = mock(Filter.class);
        doAnswer(invoke(2)).when(filter1).filterRead(any(ServerContext.class),
                any(ReadRequest.class), any(RequestHandler.class));
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final ReadRequest request = Requests.newReadRequest("read");

        // The handler will be invoked twice which is obviously unrealistic. In practice,
        // filter1 will chain the downstream handler's returned promise and combine/reduce
        // the result of the two sub-reads to produce a single read response. We won't do
        // that here in order to keep the test simple.
        Promise<Resource, ResourceException> promise = chain.handleRead(context, request);

        final InOrder inOrder = inOrder(filter1, filter2, target);
        inOrder.verify(filter1).filterRead(same(context), same(request),
                any(RequestHandler.class));
        // First read of next filter
        inOrder.verify(filter2).filterRead(same(context), same(request),
                any(RequestHandler.class));
        inOrder.verify(target).handleRead(context, request);
        assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);

        // Second read of next filter
        inOrder.verify(filter2).filterRead(same(context), same(request),
                any(RequestHandler.class));
        inOrder.verify(target).handleRead(context, request);
        assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
    }

    @Test
    public void testFilterCanStopProcessingWithError() {
        final RequestHandler target = target();
        final Filter filter1 = mock(Filter.class);
        final ResourceException expectedError = new NotSupportedException();
        given(filter1.filterRead(any(ServerContext.class), any(ReadRequest.class), any(RequestHandler.class)))
                .willReturn(Promises.<Resource, ResourceException>newExceptionPromise(expectedError));
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final ReadRequest request = Requests.newReadRequest("read");

        Promise<Resource, ResourceException> promise = chain.handleRead(context, request);

        final InOrder inOrder = inOrder(filter1, filter2, target);
        inOrder.verify(filter1).filterRead(same(context), same(request), any(RequestHandler.class));
        assertThat(promise).failedWithException().isEqualTo(expectedError);
        verifyZeroInteractions(filter2, target);
    }

    @Test
    public void testFilterCanStopProcessingWithResult() {
        final RequestHandler target = target();
        final Filter filter1 = mock(Filter.class);
        given(filter1.filterRead(any(ServerContext.class), any(ReadRequest.class), any(RequestHandler.class)))
                .willReturn(Promises.<Resource, ResourceException>newResultPromise(RESOURCE));
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final ReadRequest request = Requests.newReadRequest("read");

        Promise<Resource, ResourceException> promise = chain.handleRead(context, request);

        final InOrder inOrder = inOrder(filter1, filter2, target);
        inOrder.verify(filter1).filterRead(same(context), same(request), any(RequestHandler.class));
        assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
        verifyZeroInteractions(filter2, target);
    }

    @Test
    public void testHandleAction() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final ActionRequest request = Requests.newActionRequest("action", "test");

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target);
        for (int i = 0; i < 2; i++) {
            Promise<JsonValue, ResourceException> promise = chain.handleAction(context, request);
            inOrder.verify(filter1).filterAction(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(filter2).filterAction(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(target).handleAction(context, request);
            assertThat(promise).succeeded().withObject().isEqualTo(JSON);
        }
    }

    @Test
    public void testHandleCreate() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final CreateRequest request = Requests.newCreateRequest("create", JSON);

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target);
        for (int i = 0; i < 2; i++) {
            Promise<Resource, ResourceException> promise = chain.handleCreate(context, request);
            inOrder.verify(filter1).filterCreate(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(filter2).filterCreate(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(target).handleCreate(context, request);
            assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
        }
    }

    @Test
    public void testHandleDelete() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final DeleteRequest request = Requests.newDeleteRequest("delete");

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target);
        for (int i = 0; i < 2; i++) {
            Promise<Resource, ResourceException> promise = chain.handleDelete(context, request);
            inOrder.verify(filter1).filterDelete(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(filter2).filterDelete(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(target).handleDelete(context, request);
            assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
        }
    }

    @Test
    public void testHandlePatch() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final PatchRequest request = Requests.newPatchRequest("patch");

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target);
        for (int i = 0; i < 2; i++) {
            Promise<Resource, ResourceException> promise = chain.handlePatch(context, request);
            inOrder.verify(filter1).filterPatch(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(filter2).filterPatch(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(target).handlePatch(context, request);
            assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
        }
    }

    @Test
    public void testHandleQuery() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final QueryRequest request = Requests.newQueryRequest("query");
        final QueryResourceHandler handler = mock(QueryResourceHandler.class);

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            Promise<QueryResult, ResourceException> promise = chain.handleQuery(context, request, handler);
            inOrder.verify(filter1).filterQuery(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterQuery(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleQuery(context, request, handler);
            assertThat(promise).succeeded().withObject().isEqualTo(QUERY_RESULT);
        }
    }

    @Test
    public void testHandleRead() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final ReadRequest request = Requests.newReadRequest("read");

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target);
        for (int i = 0; i < 2; i++) {
            Promise<Resource, ResourceException> promise = chain.handleRead(context, request);
            inOrder.verify(filter1).filterRead(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(filter2).filterRead(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(target).handleRead(context, request);
            assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
        }
    }

    @Test
    public void testHandleUpdate() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context();
        final UpdateRequest request = Requests.newUpdateRequest("update", JSON);

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target);
        for (int i = 0; i < 2; i++) {
            Promise<Resource, ResourceException> promise = chain.handleUpdate(context, request);
            inOrder.verify(filter1).filterUpdate(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(filter2).filterUpdate(same(context), same(request), any(RequestHandler.class));
            inOrder.verify(target).handleUpdate(context, request);
            assertThat(promise).succeeded().withObject().isEqualTo(RESOURCE);
        }
    }

    private ServerContext context() {
        return new ServerContext(new RootContext());
    }

    private Filter filter() {
        final Filter filter = mock(Filter.class);
        doAnswer(invoke()).when(filter).filterAction(any(ServerContext.class),
                any(ActionRequest.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterCreate(any(ServerContext.class),
                any(CreateRequest.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterDelete(any(ServerContext.class),
                any(DeleteRequest.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterPatch(any(ServerContext.class),
                any(PatchRequest.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterQuery(any(ServerContext.class),
                any(QueryRequest.class), any(QueryResourceHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterRead(any(ServerContext.class),
                any(ReadRequest.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterUpdate(any(ServerContext.class),
                any(UpdateRequest.class), any(RequestHandler.class));
        return filter;
    }

    private <R> Answer<Promise<R, ResourceException>> invoke() {
        return invoke(1);
    }

    private <R> Answer<Promise<R, ResourceException>> invoke(final int count) {
        return new Answer<Promise<R, ResourceException>>() {
            @Override
            public Promise<R, ResourceException> answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final ServerContext context = (ServerContext) args[0];
                final Request request = (Request) args[1];
                QueryResourceHandler handler = null;
                final RequestHandler next;
                if (args.length > 3) {
                    handler = (QueryResourceHandler) args[2];
                    next = (RequestHandler) args[3];
                } else {
                    next = (RequestHandler) args[2];
                }
                List<Promise<R, ResourceException>> promises = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    switch (request.getRequestType()) {
                        case ACTION:
                            promises.add((Promise<R, ResourceException>) next.handleAction(context, (ActionRequest) request));
                            break;
                        case CREATE:
                            promises.add((Promise<R, ResourceException>) next.handleCreate(context, (CreateRequest) request));
                            break;
                        case DELETE:
                            promises.add((Promise<R, ResourceException>) next.handleDelete(context, (DeleteRequest) request));
                            break;
                        case PATCH:
                            promises.add((Promise<R, ResourceException>) next.handlePatch(context, (PatchRequest) request));
                            break;
                        case QUERY:
                            promises.add((Promise<R, ResourceException>) next.handleQuery(context, (QueryRequest) request, handler));
                            break;
                        case READ:
                            promises.add((Promise<R, ResourceException>) next.handleRead(context, (ReadRequest) request));
                            break;
                        case UPDATE:
                            promises.add((Promise<R, ResourceException>) next.handleUpdate(context, (UpdateRequest) request));
                            break;
                    }
                }
                return when(promises)
                        .thenAsync(new AsyncFunction<List<R>, R, ResourceException>() {
                            @Override
                            public Promise<R, ResourceException> apply(List<R> rs) throws ResourceException {
                                return newResultPromise(rs.get(rs.size() - 1));
                            }
                        });
            }
        };
    }

    private RequestHandler target() {
        final RequestHandler target = mock(RequestHandler.class);
        given(target.handleAction(any(ServerContext.class), any(ActionRequest.class)))
                .willReturn(Promises.<JsonValue, ResourceException>newResultPromise(JSON));
        given(target.handleCreate(any(ServerContext.class), any(CreateRequest.class)))
                .willReturn(Promises.<Resource, ResourceException>newResultPromise(RESOURCE));
        given(target.handleDelete(any(ServerContext.class), any(DeleteRequest.class)))
                .willReturn(Promises.<Resource, ResourceException>newResultPromise(RESOURCE));
        given(target.handlePatch(any(ServerContext.class), any(PatchRequest.class)))
                .willReturn(Promises.<Resource, ResourceException>newResultPromise(RESOURCE));
        given(target.handleQuery(any(ServerContext.class), any(QueryRequest.class), any(QueryResourceHandler.class)))
                .willReturn(Promises.<QueryResult, ResourceException>newResultPromise(QUERY_RESULT));
        given(target.handleRead(any(ServerContext.class), any(ReadRequest.class)))
                .willReturn(Promises.<Resource, ResourceException>newResultPromise(RESOURCE));
        given(target.handleUpdate(any(ServerContext.class), any(UpdateRequest.class)))
                .willReturn(Promises.<Resource, ResourceException>newResultPromise(RESOURCE));
        return target;
    }
}
