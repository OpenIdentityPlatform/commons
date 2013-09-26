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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the FilterChain class based on the use cases in CREST-7.
 */
@SuppressWarnings({ "javadoc" })
public final class FiltersTest {

    private static final class MockFilter implements UntypedCrossCutFilter<Object> {
        ServerContext context;
        ResourceException error;
        final List<Resource> queryResources = new LinkedList<Resource>();
        Request request;
        Object result;
        private final boolean removeQueryResources;
        private final Object state = new Object();

        // Filter which continues.
        MockFilter(final boolean removeQueryResources) {
            this.removeQueryResources = removeQueryResources;
        }

        // Filter which stops with result.
        MockFilter(final Object result) {
            this.removeQueryResources = false;
            this.result = result;
        }

        // Filter which stops with error.
        MockFilter(final ResourceException error) {
            this.removeQueryResources = false;
            this.error = error;
        }

        @Override
        public void filterGenericError(final ServerContext context, final Object state,
                final ResourceException error, final ResultHandler<Object> handler) {
            // Check state:
            assertThat(this.context).isSameAs(context);
            assertThat(this.request).isNotNull();
            assertThat(this.error).isNull();
            assertThat(this.result).isNull();

            // Check args:
            assertThat(state).isSameAs(this.state);
            assertThat(error).isNotNull();

            // Update state and notify caller:
            this.error = error;
            handler.handleError(error);
        }

        @Override
        public void filterGenericRequest(final ServerContext context, final Request request,
                final RequestHandler next, final CrossCutFilterResultHandler<Object, Object> handler) {
            // Check state:
            assertThat(this.context).isNull();
            assertThat(this.request).isNull();

            // Check args:
            assertThat(context).isNotNull();
            assertThat(next).isNotNull();
            assertThat(request).isNotNull();

            // Update state and notify caller:
            this.context = context;
            this.request = request;

            if (result != null) {
                // Stop with result.
                handler.handleResult(result);
            } else if (error != null) {
                // Stop with error.
                handler.handleError(error);
            } else {
                // Continue.
                handler.handleContinue(context, state);
            }
        }

        @Override
        public <R> void filterGenericResult(final ServerContext context, final Object state,
                final R result, final ResultHandler<R> handler) {
            // Check state:
            assertThat(this.context).isSameAs(context);
            assertThat(this.request).isNotNull();
            assertThat(this.error).isNull();
            assertThat(this.result).isNull();

            // Check args:
            assertThat(state).isSameAs(this.state);
            assertThat(result).isNotNull();

            // Update state and notify caller:
            this.result = result;
            handler.handleResult(result);
        }

        @Override
        public void filterQueryResource(final ServerContext context, final Object state,
                final Resource resource, final QueryResultHandler handler) {
            // Check state:
            assertThat(this.context).isSameAs(context);
            assertThat(this.request).isNotNull();
            assertThat(this.error).isNull();
            assertThat(this.result).isNull();

            // Check args:
            assertThat(state).isSameAs(this.state);
            assertThat(resource).isNotNull();

            // Update state and notify caller:
            queryResources.add(resource);
            if (!removeQueryResources) {
                handler.handleResource(resource);
            }
        }

    }

    private static final class MockRequestHandler implements RequestHandler {
        Request request;
        private final boolean returnError;

        MockRequestHandler(final boolean returnError) {
            this.returnError = returnError;
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            checkState(context, request);
            if (returnError) {
                handler.handleError(ERROR);
            } else {
                handler.handleResult(JSON);
            }
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            handle0(context, request, handler);
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            handle0(context, request, handler);
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            handle0(context, request, handler);
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            checkState(context, request);
            if (returnError) {
                handler.handleError(ERROR);
            } else {
                handler.handleResource(RESOURCE1);
                handler.handleResource(RESOURCE2);
                handler.handleResult(QUERY_RESULT);
            }
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            handle0(context, request, handler);
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            handle0(context, request, handler);
        }

        private void checkState(final ServerContext context, final Request request) {
            assertThat(this.request).isNull();
            assertThat(context).isNotNull();
            assertThat(request).isNotNull();
            this.request = request;
        }

        private void handle0(final ServerContext context, final Request request,
                final ResultHandler<Resource> handler) {
            checkState(context, request);
            if (returnError) {
                handler.handleError(ERROR);
            } else {
                handler.handleResult(RESOURCE1);
            }
        }
    }

    private static final ResourceException ERROR = new InternalServerErrorException();
    private static final JsonValue JSON = new JsonValue(Collections.singletonMap("test", "value"));
    private static final QueryResult QUERY_RESULT = new QueryResult();
    private static final Resource RESOURCE1 = new Resource("id1", "rev", JSON);
    private static final Resource RESOURCE2 = new Resource("id2", "rev", JSON);

    private static final ActionRequest ACTION_REQUEST = Requests
            .newActionRequest("test", "action");
    private static final CreateRequest CREATE_REQUEST = Requests.newCreateRequest("test", JSON);
    private static final DeleteRequest DELETE_REQUEST = Requests.newDeleteRequest("test", "id");
    private static final PatchRequest PATCH_REQUEST = Requests.newPatchRequest("test");
    private static final QueryRequest QUERY_REQUEST = Requests.newQueryRequest("test");
    private static final ReadRequest READ_REQUEST = Requests.newReadRequest("test", "id");
    private static final UpdateRequest UPDATE_REQUEST = Requests.newUpdateRequest("test", "id",
            JSON);

    @DataProvider
    public Object[][] booleanData() {
        // @formatter:off
        return new Object[][] {
            { cl(),             true,  false },
            { cl(false),        false, false },
            { cl(true),         true,  true },
            { cl(false, false), false, false },
            { cl(false, true),  false, true },
            { cl(true,  false), false, true },
            { cl(true,  true),  true,  true },
        };
        // @formatter:on
    }

    @DataProvider
    public Object[][] requestTypes() {
        final int sz = RequestType.values().length;
        final Object[][] args = new Object[sz][1];
        for (int i = 0; i < sz; i++) {
            args[i] = new Object[1];
            args[i][0] = RequestType.values()[i];
        }
        return args;
    }

    @Test(dataProvider = "booleanData")
    public void testAnd(final List<FilterCondition> conditions, final boolean andExpected,
            final boolean orExpected) {
        final FilterCondition condition = Filters.and(conditions.toArray(new FilterCondition[0]));
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isEqualTo(andExpected);
    }

    @Test(dataProvider = "requestTypes")
    @SuppressWarnings({ "rawtypes" })
    public void testAsFilterContinueWithError(final RequestType type) {
        final MockFilter filter = new MockFilter(false);
        final Filter wrapped = Filters.asFilter(filter);
        final MockRequestHandler next = new MockRequestHandler(true);
        final ServerContext context = mock(ServerContext.class);
        final ResultHandler handler = mockHandler(type);
        invokeFilter(context, type, wrapped, handler, next);

        // Check post-conditions: the request handler should have been invoked.
        assertThat(next.request).isSameAs(request(type));
        assertThat(filter.context).isSameAs(context);
        assertThat(filter.request).isSameAs(request(type));
        assertThat(filter.error).isSameAs(ERROR);
        assertThat(filter.result).isNull();
        assertThat(filter.queryResources).isEmpty();
        verify(handler).handleError(ERROR);
        verifyNoMoreInteractions(handler);
    }

    @Test(dataProvider = "requestTypes")
    public void testAsFilterContinueWithResultAndKeepResources(final RequestType type) {
        testAsFilterContinueWithResult0(type, false);
    }

    @Test(dataProvider = "requestTypes")
    public void testAsFilterContinueWithResultAndRemoveResources(final RequestType type) {
        testAsFilterContinueWithResult0(type, true);
    }

    @Test(dataProvider = "requestTypes")
    public void testAsFilterStopWithError(final RequestType type) {
        final MockFilter filter = new MockFilter(ERROR);
        final Filter wrapped = Filters.asFilter(filter);
        final RequestHandler next = mock(RequestHandler.class);
        final ServerContext context = mock(ServerContext.class);
        final ResultHandler<Object> handler = mockHandler(type);
        invokeFilter(context, type, wrapped, handler, next);

        // Check post-conditions: the request handler should not have been invoked.
        verifyZeroInteractions(next);
        assertThat(filter.context).isSameAs(context);
        assertThat(filter.request).isSameAs(request(type));
        assertThat(filter.result).isNull();
        assertThat(filter.queryResources).isEmpty();
        verify(handler, never()).handleResult(any(Object.class));
        verify(handler).handleError(ERROR);
    }

    @Test(dataProvider = "requestTypes")
    public void testAsFilterStopWithResult(final RequestType type) {
        final Object result = result(type);
        final MockFilter filter = new MockFilter(result);
        final Filter wrapped = Filters.asFilter(filter);
        final RequestHandler next = mock(RequestHandler.class);
        final ServerContext context = mock(ServerContext.class);
        final ResultHandler<Object> handler = mockHandler(type);
        invokeFilter(context, type, wrapped, handler, next);

        // Check post-conditions: the request handler should not have been invoked.
        verifyZeroInteractions(next);
        assertThat(filter.context).isSameAs(context);
        assertThat(filter.request).isSameAs(request(type));
        assertThat(filter.error).isNull();
        assertThat(filter.queryResources).isEmpty();
        verify(handler).handleResult(result);
        verify(handler, never()).handleError(any(ResourceException.class));
    }

    @Test(dataProvider = "requestTypes")
    public void testConditionalFilterFalse(final RequestType type) {
        final Filter filter = mock(Filter.class);
        final FilterCondition condition = c(false);
        final Filter conditionalFilter = Filters.conditionalFilter(condition, filter);
        final RequestHandler next = mock(RequestHandler.class);
        final ServerContext context = mock(ServerContext.class);
        invokeFilter(context, type, conditionalFilter, null, next);
        // Filter should not have been invoked and next should have.
        verifyZeroInteractions(filter);
        invokeRequestHandler(context, type, verify(next));
    }

    @Test(dataProvider = "requestTypes")
    public void testConditionalFilterTrue(final RequestType type) {
        final Filter filter = mock(Filter.class);
        final FilterCondition condition = c(true);
        final Filter conditionalFilter = Filters.conditionalFilter(condition, filter);
        final RequestHandler next = mock(RequestHandler.class);
        final ServerContext context = mock(ServerContext.class);
        invokeFilter(context, type, conditionalFilter, null, next);
        // Filter should have been invoked and next should not.
        invokeFilter(context, type, verify(filter), null, next);
        verifyZeroInteractions(next);
    }

    @Test
    public void testMatchRequestTypeManyFalse() {
        final FilterCondition condition =
                Filters.matchRequestType(RequestType.READ, RequestType.DELETE);
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isFalse();
    }

    @Test
    public void testMatchRequestTypeManyTrue() {
        final FilterCondition condition =
                Filters.matchRequestType(RequestType.ACTION, RequestType.DELETE);
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isTrue();
    }

    @Test
    public void testMatchRequestTypeSingleFalse() {
        final FilterCondition condition = Filters.matchRequestType(RequestType.READ);
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isFalse();
    }

    @Test
    public void testMatchRequestTypeSingleTrue() {
        final FilterCondition condition = Filters.matchRequestType(RequestType.ACTION);
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isTrue();
    }

    @Test
    public void testMatchResourceNameFalse() {
        final FilterCondition condition = Filters.matchResourceName("nomatch/name");
        final Request request = Requests.newActionRequest("resource/name", "test");
        assertThat(condition.matches(null, request)).isFalse();
    }

    @Test
    public void testMatchResourceNameTrue() {
        final FilterCondition condition = Filters.matchResourceName("resource/.*");
        final Request request = Requests.newActionRequest("resource/name", "test");
        assertThat(condition.matches(null, request)).isTrue();
    }

    @Test
    public void testNotFalse() {
        final FilterCondition condition = Filters.not(c(true));
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isFalse();
    }

    @Test
    public void testNotTrue() {
        final FilterCondition condition = Filters.not(c(false));
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isTrue();
    }

    @Test(dataProvider = "booleanData")
    public void testOr(final List<FilterCondition> conditions, final boolean andExpected,
            final boolean orExpected) {
        final FilterCondition condition = Filters.or(conditions.toArray(new FilterCondition[0]));
        final Request request = Requests.newActionRequest("", "test");
        assertThat(condition.matches(null, request)).isEqualTo(orExpected);
    }

    private FilterCondition c(final boolean value) {
        return new FilterCondition() {

            @Override
            public boolean matches(final ServerContext context, final Request request) {
                return value;
            }

            @Override
            public String toString() {
                return String.valueOf(value);
            }
        };
    }

    private List<FilterCondition> cl(final boolean... values) {
        final List<FilterCondition> cl = new ArrayList<FilterCondition>(values.length);
        for (int i = 0; i < values.length; i++) {
            cl.add(c(values[i]));
        }
        return cl;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void invokeFilter(final ServerContext context, final RequestType type,
            final Filter filter, final ResultHandler handler, final RequestHandler next) {
        switch (type) {
        case ACTION:
            filter.filterAction(context, ACTION_REQUEST, handler, next);
            break;
        case CREATE:
            filter.filterCreate(context, CREATE_REQUEST, handler, next);
            break;
        case DELETE:
            filter.filterDelete(context, DELETE_REQUEST, handler, next);
            break;
        case PATCH:
            filter.filterPatch(context, PATCH_REQUEST, handler, next);
            break;
        case QUERY:
            filter.filterQuery(context, QUERY_REQUEST, (QueryResultHandler) handler, next);
            break;
        case READ:
            filter.filterRead(context, READ_REQUEST, handler, next);
            break;
        case UPDATE:
            filter.filterUpdate(context, UPDATE_REQUEST, handler, next);
            break;
        }
    }

    private void invokeRequestHandler(final ServerContext context, final RequestType type,
            final RequestHandler handler) {
        switch (type) {
        case ACTION:
            handler.handleAction(context, ACTION_REQUEST, null);
            break;
        case CREATE:
            handler.handleCreate(context, CREATE_REQUEST, null);
            break;
        case DELETE:
            handler.handleDelete(context, DELETE_REQUEST, null);
            break;
        case PATCH:
            handler.handlePatch(context, PATCH_REQUEST, null);
            break;
        case QUERY:
            handler.handleQuery(context, QUERY_REQUEST, null);
            break;
        case READ:
            handler.handleRead(context, READ_REQUEST, null);
            break;
        case UPDATE:
            handler.handleUpdate(context, UPDATE_REQUEST, null);
            break;
        }
    }

    @SuppressWarnings("unchecked")
    private ResultHandler<Object> mockHandler(final RequestType type) {
        return type == RequestType.QUERY ? mock(QueryResultHandler.class)
                : mock(ResultHandler.class);
    }

    private Request request(final RequestType type) {
        switch (type) {
        case ACTION:
            return ACTION_REQUEST;
        case CREATE:
            return CREATE_REQUEST;
        case DELETE:
            return DELETE_REQUEST;
        case PATCH:
            return PATCH_REQUEST;
        case QUERY:
            return QUERY_REQUEST;
        case READ:
            return READ_REQUEST;
        default: // UPDATE
            return UPDATE_REQUEST;
        }
    }

    private Object result(final RequestType type) {
        switch (type) {
        case ACTION:
            return JSON;
        case QUERY:
            return QUERY_RESULT;
        default:
            return RESOURCE1;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void testAsFilterContinueWithResult0(final RequestType type,
            final boolean removeResources) {
        final Object result = result(type);
        final MockFilter filter = new MockFilter(removeResources);
        final Filter wrapped = Filters.asFilter(filter);
        final MockRequestHandler next = new MockRequestHandler(false);
        final ServerContext context = mock(ServerContext.class);
        final ResultHandler handler = mockHandler(type);
        invokeFilter(context, type, wrapped, handler, next);

        // Check post-conditions: the request handler should have been invoked.
        assertThat(next.request).isSameAs(request(type));
        assertThat(filter.context).isSameAs(context);
        assertThat(filter.request).isSameAs(request(type));
        assertThat(filter.error).isNull();
        assertThat(filter.result).isSameAs(result);
        if (type == RequestType.QUERY) {
            assertThat(filter.queryResources).containsExactly(RESOURCE1, RESOURCE2);
            if (!removeResources) {
                verify((QueryResultHandler) handler).handleResource(RESOURCE1);
                verify((QueryResultHandler) handler).handleResource(RESOURCE2);
            }
            verify((QueryResultHandler) handler).handleResult(QUERY_RESULT);
        } else {
            assertThat(filter.queryResources).isEmpty();
            verify(handler).handleResult(result);
        }
        verifyNoMoreInteractions(handler);
    }
}
