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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the FilterChain class based on the use cases in CREST-7.
 */
@SuppressWarnings({ "javadoc" })
public final class FiltersTest {

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
    public void testMatchResourcePathFalse() {
        final FilterCondition condition = Filters.matchResourcePath("nomatch/name");
        final Request request = Requests.newActionRequest("resource/name", "test");
        assertThat(condition.matches(null, request)).isFalse();
    }

    @Test
    public void testMatchResourcePathTrue() {
        final FilterCondition condition = Filters.matchResourcePath("resource/.*");
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
        final List<FilterCondition> cl = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            cl.add(c(values[i]));
        }
        return cl;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <R> Promise<R, ResourceException> invokeFilter(final ServerContext context, final RequestType type,
            final Filter filter, final QueryResourceHandler handler, final RequestHandler next) {
        switch (type) {
            case ACTION:
                return (Promise<R, ResourceException>) filter.filterAction(context, ACTION_REQUEST, next);
            case CREATE:
                return (Promise<R, ResourceException>) filter.filterCreate(context, CREATE_REQUEST, next);
            case DELETE:
                return (Promise<R, ResourceException>) filter.filterDelete(context, DELETE_REQUEST, next);
            case PATCH:
                return (Promise<R, ResourceException>) filter.filterPatch(context, PATCH_REQUEST, next);
            case QUERY:
                return (Promise<R, ResourceException>) filter.filterQuery(context, QUERY_REQUEST, handler, next);
            case READ:
                return (Promise<R, ResourceException>) filter.filterRead(context, READ_REQUEST, next);
            case UPDATE:
                return (Promise<R, ResourceException>) filter.filterUpdate(context, UPDATE_REQUEST, next);
        }
        throw new IllegalStateException("Unknown operation! " + type.toString());
    }

    @SuppressWarnings({ "unchecked" })
    private <R> Promise<R, ResourceException> invokeRequestHandler(final ServerContext context, final RequestType type,
            final RequestHandler handler) {
        switch (type) {
            case ACTION:
                return (Promise<R, ResourceException>) handler.handleAction(context, ACTION_REQUEST);
            case CREATE:
                return (Promise<R, ResourceException>) handler.handleCreate(context, CREATE_REQUEST);
            case DELETE:
                return (Promise<R, ResourceException>) handler.handleDelete(context, DELETE_REQUEST);
            case PATCH:
                return (Promise<R, ResourceException>) handler.handlePatch(context, PATCH_REQUEST);
            case QUERY:
                return (Promise<R, ResourceException>) handler.handleQuery(context, QUERY_REQUEST, null);
            case READ:
                return (Promise<R, ResourceException>) handler.handleRead(context, READ_REQUEST);
            case UPDATE:
                return (Promise<R, ResourceException>) handler.handleUpdate(context, UPDATE_REQUEST);
        }
        throw new IllegalStateException("Unknown operation! " + type.toString());
    }

    @SuppressWarnings({ "rawtypes" })
    private QueryResourceHandler mockHandler(final RequestType type) {
        return type == RequestType.QUERY ? mock(QueryResourceHandler.class) : null;
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
}
