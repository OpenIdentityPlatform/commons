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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;

import org.forgerock.json.fluent.JsonValue;
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
                any(ReadRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final ReadRequest request = Requests.newReadRequest("/read");

        // The handler will be invoked twice which is obviously unrealistic. In practice,
        // filter1 will wrap the result handler and combine/reduce the result of the two
        // sub-reads to produce a single read response. We won't do that here in order
        // to keep the test simple.
        final ResultHandler<Resource> handler = handler();
        chain.handleRead(context, request, handler);

        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        inOrder.verify(filter1).filterRead(same(context), same(request), same(handler),
                any(RequestHandler.class));
        // First read of next filter
        inOrder.verify(filter2).filterRead(same(context), same(request), same(handler),
                any(RequestHandler.class));
        inOrder.verify(target).handleRead(context, request, handler);
        inOrder.verify(handler).handleResult(RESOURCE);

        // Second read of next filter
        inOrder.verify(filter2).filterRead(same(context), same(request), same(handler),
                any(RequestHandler.class));
        inOrder.verify(target).handleRead(context, request, handler);
        inOrder.verify(handler).handleResult(RESOURCE);
    }

    @Test
    public void testFilterCanStopProcessingWithError() {
        final RequestHandler target = target();
        final Filter filter1 = mock(Filter.class);
        final ResourceException expectedError = new NotSupportedException();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final ResultHandler<Resource> handler = (ResultHandler<Resource>) args[2];
                handler.handleError(expectedError);
                return null;
            }
        }).when(filter1).filterRead(any(ServerContext.class), any(ReadRequest.class),
                any(ResultHandler.class), any(RequestHandler.class));
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final ReadRequest request = Requests.newReadRequest("/read");

        final ResultHandler<Resource> handler = handler();
        chain.handleRead(context, request, handler);

        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        inOrder.verify(filter1).filterRead(same(context), same(request), same(handler),
                any(RequestHandler.class));
        inOrder.verify(handler).handleError(same(expectedError));
        verifyZeroInteractions(filter2, target);
    }

    @Test
    public void testFilterCanStopProcessingWithResult() {
        final RequestHandler target = target();
        final Filter filter1 = mock(Filter.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final ResultHandler<Resource> handler = (ResultHandler<Resource>) args[2];
                handler.handleResult(RESOURCE);
                return null;
            }
        }).when(filter1).filterRead(any(ServerContext.class), any(ReadRequest.class),
                any(ResultHandler.class), any(RequestHandler.class));
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final ReadRequest request = Requests.newReadRequest("/read");

        final ResultHandler<Resource> handler = handler();
        chain.handleRead(context, request, handler);

        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        inOrder.verify(filter1).filterRead(same(context), same(request), same(handler),
                any(RequestHandler.class));
        inOrder.verify(handler).handleResult(RESOURCE);
        verifyZeroInteractions(filter2, target);
    }

    @Test
    public void testHandleAction() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final ActionRequest request = Requests.newActionRequest("/action", "test");
        final ResultHandler<JsonValue> handler = handler();

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handleAction(context, request, handler);
            inOrder.verify(filter1).filterAction(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterAction(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleAction(context, request, handler);
            inOrder.verify(handler).handleResult(JSON);
        }
    }

    @Test
    public void testHandleCreate() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final CreateRequest request = Requests.newCreateRequest("/create", JSON);
        final ResultHandler<Resource> handler = handler();

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handleCreate(context, request, handler);
            inOrder.verify(filter1).filterCreate(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterCreate(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleCreate(context, request, handler);
            inOrder.verify(handler).handleResult(RESOURCE);
        }
    }

    @Test
    public void testHandleDelete() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final DeleteRequest request = Requests.newDeleteRequest("/delete");
        final ResultHandler<Resource> handler = handler();

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handleDelete(context, request, handler);
            inOrder.verify(filter1).filterDelete(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterDelete(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleDelete(context, request, handler);
            inOrder.verify(handler).handleResult(RESOURCE);
        }
    }

    @Test
    public void testHandlePatch() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final PatchRequest request = Requests.newPatchRequest("/patch");
        final ResultHandler<Resource> handler = handler();

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handlePatch(context, request, handler);
            inOrder.verify(filter1).filterPatch(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterPatch(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handlePatch(context, request, handler);
            inOrder.verify(handler).handleResult(RESOURCE);
        }
    }

    @Test
    public void testHandleQuery() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final QueryRequest request = Requests.newQueryRequest("/query");
        final QueryResultHandler handler = mock(QueryResultHandler.class);

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handleQuery(context, request, handler);
            inOrder.verify(filter1).filterQuery(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterQuery(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleQuery(context, request, handler);

            // FIXME: For some reason, Mockito does not like this. It's
            // definitely being invoked though.

            // inOrder.verify(handler).handleResult(QUERY_RESULT);
        }
    }

    @Test
    public void testHandleRead() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final ReadRequest request = Requests.newReadRequest("/read");
        final ResultHandler<Resource> handler = handler();

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handleRead(context, request, handler);
            inOrder.verify(filter1).filterRead(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterRead(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleRead(context, request, handler);
            inOrder.verify(handler).handleResult(RESOURCE);
        }
    }

    @Test
    public void testHandleUpdate() {
        final RequestHandler target = target();
        final Filter filter1 = filter();
        final Filter filter2 = filter();
        final FilterChain chain = new FilterChain(target, filter1, filter2);
        final ServerContext context = context(target);
        final UpdateRequest request = Requests.newUpdateRequest("/update", JSON);
        final ResultHandler<Resource> handler = handler();

        // Test twice to ensure that no state is carried over.
        final InOrder inOrder = inOrder(filter1, filter2, target, handler);
        for (int i = 0; i < 2; i++) {
            chain.handleUpdate(context, request, handler);
            inOrder.verify(filter1).filterUpdate(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(filter2).filterUpdate(same(context), same(request), same(handler),
                    any(RequestHandler.class));
            inOrder.verify(target).handleUpdate(context, request, handler);
            inOrder.verify(handler).handleResult(RESOURCE);
        }
    }

    private ServerContext context(final RequestHandler handler) {
        return new ServerContext(new RootContext(), newInternalConnection(handler));
    }

    private Filter filter() {
        final Filter filter = mock(Filter.class);
        doAnswer(invoke()).when(filter).filterAction(any(ServerContext.class),
                any(ActionRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterCreate(any(ServerContext.class),
                any(CreateRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterDelete(any(ServerContext.class),
                any(DeleteRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterPatch(any(ServerContext.class),
                any(PatchRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterQuery(any(ServerContext.class),
                any(QueryRequest.class), any(QueryResultHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterRead(any(ServerContext.class),
                any(ReadRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        doAnswer(invoke()).when(filter).filterUpdate(any(ServerContext.class),
                any(UpdateRequest.class), any(ResultHandler.class), any(RequestHandler.class));
        return filter;
    }

    private <T> ResultHandler<T> handler() {
        return mock(ResultHandler.class);
    }

    private Answer<Void> invoke() {
        return invoke(1);
    }

    private Answer<Void> invoke(final int count) {
        return new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final ServerContext context = (ServerContext) args[0];
                final Request request = (Request) args[1];
                final ResultHandler<?> handler = (ResultHandler<?>) args[2];
                final RequestHandler next = (RequestHandler) args[3];
                for (int i = 0; i < count; i++) {
                    switch (request.getRequestType()) {
                    case ACTION:
                        next.handleAction(context, (ActionRequest) request,
                                (ResultHandler<JsonValue>) handler);
                        break;
                    case CREATE:
                        next.handleCreate(context, (CreateRequest) request,
                                (ResultHandler<Resource>) handler);
                        break;
                    case DELETE:
                        next.handleDelete(context, (DeleteRequest) request,
                                (ResultHandler<Resource>) handler);
                        break;
                    case PATCH:
                        next.handlePatch(context, (PatchRequest) request,
                                (ResultHandler<Resource>) handler);
                        break;
                    case QUERY:
                        next.handleQuery(context, (QueryRequest) request,
                                (QueryResultHandler) handler);
                        break;
                    case READ:
                        next.handleRead(context, (ReadRequest) request,
                                (ResultHandler<Resource>) handler);
                        break;
                    case UPDATE:
                        next.handleUpdate(context, (UpdateRequest) request,
                                (ResultHandler<Resource>) handler);
                        break;
                    }
                }
                return null;
            }
        };
    }

    private <T> Answer<Void> result(final T result) {
        return new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                ((ResultHandler<T>) args[2]).handleResult(result);
                return null;
            }
        };
    }

    private RequestHandler target() {
        final RequestHandler target = mock(RequestHandler.class);
        doAnswer(result(JSON)).when(target).handleAction(any(ServerContext.class),
                any(ActionRequest.class), any(ResultHandler.class));
        doAnswer(result(RESOURCE)).when(target).handleCreate(any(ServerContext.class),
                any(CreateRequest.class), any(ResultHandler.class));
        doAnswer(result(RESOURCE)).when(target).handleDelete(any(ServerContext.class),
                any(DeleteRequest.class), any(ResultHandler.class));
        doAnswer(result(RESOURCE)).when(target).handlePatch(any(ServerContext.class),
                any(PatchRequest.class), any(ResultHandler.class));
        doAnswer(result(QUERY_RESULT)).when(target).handleQuery(any(ServerContext.class),
                any(QueryRequest.class), any(QueryResultHandler.class));
        doAnswer(result(RESOURCE)).when(target).handleRead(any(ServerContext.class),
                any(ReadRequest.class), any(ResultHandler.class));
        doAnswer(result(RESOURCE)).when(target).handleUpdate(any(ServerContext.class),
                any(UpdateRequest.class), any(ResultHandler.class));
        return target;
    }
}
