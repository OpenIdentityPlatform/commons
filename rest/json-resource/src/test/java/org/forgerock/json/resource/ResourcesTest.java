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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.TestUtils.*;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJJsonValueResponseAssert.assertThat;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.annotations.Action;
import org.forgerock.json.resource.annotations.Create;
import org.forgerock.json.resource.annotations.Delete;
import org.forgerock.json.resource.annotations.Patch;
import org.forgerock.json.resource.annotations.Query;
import org.forgerock.json.resource.annotations.Read;
import org.forgerock.json.resource.annotations.Update;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link org.forgerock.json.resource.Resources}.
 */
@SuppressWarnings("javadoc")
public final class ResourcesTest {

    @DataProvider
    public Object[][] testFilterData() {
        // @formatter:off
        return new Object[][] {

                // Null content
                {
                        filter(),
                        content(null),
                        expected(null)
                },

                {
                        filter("/"),
                        content(null),
                        expected(null)
                },

                {
                        filter("/a/b"),
                        content(null),
                        expected(null)
                },

                {
                        filter("/1"),
                        content(null),
                        expected(null)
                },

                // Empty object
                {
                        filter(),
                        content(object()),
                        expected(object())
                },

                {
                        filter("/"),
                        content(object()),
                        expected(object())
                },

                {
                        filter("/a/b"),
                        content(object()),
                        expected(object())
                },

                {
                        filter("/1"),
                        content(object()),
                        expected(object())
                },

                // Miscellaneous
                {
                        filter(),
                        content(object(field("a", "1"), field("b", "2"))),
                        expected(object(field("a", "1"), field("b", "2")))
                },

                {
                        filter("/"),
                        content(object(field("a", "1"), field("b", "2"))),
                        expected(object(field("a", "1"), field("b", "2")))
                },

                {
                        filter("/a"),
                        content(object(field("a", "1"), field("b", "2"))),
                        expected(object(field("a", "1")))
                },

                {
                        filter("/a/b"),
                        content(object(field("a", "1"), field("b", "2"))),
                        expected(object())
                },

                {
                        filter("/a"),
                        content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                        expected(object(field("a", object(field("b", "1"), field("c", "2")))))
                },

                {
                        filter("/a/b"),
                        content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                        expected(object(field("b", "1")))
                },

                {
                        filter("/a/b", "/d"),
                        content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                        expected(object(field("b", "1"), field("d", "3")))
                },

                {
                        filter("/a/b", "/a"),
                        content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                        expected(object(field("b", "1"), field("a", object(field("b", "1"), field("c", "2")))))
                },

                {
                        filter("/a", "/a/b"),
                        content(object(field("a", object(field("b", "1"), field("c", "2"))), field("d", "3"))),
                        expected(object(field("a", object(field("b", "1"), field("c", "2"))), field("b", "1")))
                },

        };
        // @formatter:on
    }

    @Test(dataProvider = "testFilterData")
    public void testFilter(List<JsonPointer> filter, JsonValue content, JsonValue expected) {
        Assertions.assertThat(Resources.filterResource(content, filter).getObject()).isEqualTo(
                expected.getObject());
    }

    @DataProvider
    public Object[][] testCollectionResourceProviderData() {
        // @formatter:off
        return new Object[][] {
                { "test", "test" },
                { "test%2fuser", "test/user" },
                { "test user", "test user" },
                { "test%20user", "test user" },
                { "test+%2buser", "test++user" }
        };
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    @Test(dataProvider = "testCollectionResourceProviderData")
    public void testCollectionResourceProvider(String resourcePath, String expectedId)
            throws Exception {
        CollectionResourceProvider collection = mock(CollectionResourceProvider.class);
        RequestHandler handler = Resources.newCollection(collection);
        Connection connection = Resources.newInternalConnection(handler);
        ReadRequest read = Requests.newReadRequest(resourcePath);
        connection.readAsync(new RootContext(), read);
        ArgumentCaptor<ReadRequest> captor = ArgumentCaptor.forClass(ReadRequest.class);
        verify(collection).readInstance(any(ServerContext.class), eq(expectedId), captor.capture());
        Assertions.assertThat(captor.getValue().getResourcePath()).isEqualTo("");
    }

    @DataProvider
    public Object[][] annotatedRequestHandlerData() {
        // @formatter:off
        return new Object[][]{
                // Class                    | Collection | Create | Read  | Update | Delete | Patch | RAction | CAction | Query |
                { NoMethods.class,                 false ,  false , false ,  false ,  false , false ,   false ,   false , false },
                { NoMethods.class,                  true ,  false , false ,  false ,  false , false ,   false ,   false , false },
                { AnnotationCollection.class,       true ,   true ,  true ,   true ,   true ,  true ,    true ,    true ,  true },
                { AnnotationSingleton.class,       false ,  false ,  true ,   true ,  false ,  true ,    true ,   false , false },
                { ConventionCollection.class,       true ,   true ,  true ,   true ,   true ,  true ,   false ,   false ,  true },
                { ConventionSingleton.class,       false ,  false ,  true ,   true ,  false ,  true ,   false ,   false , false },
        };
        // @formatter:on
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testCreateAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        CreateRequest req = Requests.newCreateRequest("/test", json(object(field("dummy", "test"))));

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.createAsync(new RootContext(), req);

        // Then
        if (create && collection) {
            assertThat(promise).succeeded().withId().isEqualTo("create");
        } else if (collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testReadAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        ReadRequest req = Requests.newReadRequest("/test");

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.readAsync(new RootContext(), req);

        // Then
        if (read && !collection) {
            assertThat(promise).succeeded().withId().isEqualTo("read");
        } else if (!collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testReadCollectionItemAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        ReadRequest req = Requests.newReadRequest("/test/fred");

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.readAsync(new RootContext(), req);

        // Then
        if (read && collection) {
            assertThat(promise).succeeded().withId().isEqualTo("read-fred");
        } else if (collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testUpdateAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        UpdateRequest req = Requests.newUpdateRequest("/test", json(object(field("dummy", "test"))));

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.updateAsync(new RootContext(), req);

        // Then
        if (update && !collection) {
            assertThat(promise).succeeded().withId().isEqualTo("update");
        } else if (!collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testUpdateCollectionItemAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        UpdateRequest req = Requests.newUpdateRequest("/test/fred", json(object(field("dummy", "test"))));

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.updateAsync(new RootContext(), req);

        // Then
        if (update && collection) {
            assertThat(promise).succeeded().withId().isEqualTo("update-fred");
        } else if (collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testDeleteCollectionItemAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        DeleteRequest req = Requests.newDeleteRequest("/test/fred");

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.deleteAsync(new RootContext(), req);

        // Then
        if (delete && collection) {
            assertThat(promise).succeeded().withId().isEqualTo("delete-fred");
        } else if (collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testPatchAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        PatchRequest req = Requests.newPatchRequest("/test");

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.patchAsync(new RootContext(), req);

        // Then
        if (patch && !collection) {
            assertThat(promise).succeeded().withId().isEqualTo("patch");
        } else if (!collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testPatchCollectionItemAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        PatchRequest req = Requests.newPatchRequest("/test/fred");

        // When
        Promise<ResourceResponse, ResourceException> promise = connection.patchAsync(new RootContext(), req);

        // Then
        if (patch && collection) {
            assertThat(promise).succeeded().withId().isEqualTo("patch-fred");
        } else if (collection) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testActionAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        String actionId1 = collection ? "collectionAction1" : "instanceAction1";
        ActionRequest req1 = Requests.newActionRequest("/test", actionId1);
        String actionId2 = collection ? "collectionAction2" : "instanceAction2";
        ActionRequest req2 = Requests.newActionRequest("/test", actionId2);

        // When
        Promise<ActionResponse, ResourceException> promise1 = connection.actionAsync(new RootContext(), req1);
        Promise<ActionResponse, ResourceException> promise2 = connection.actionAsync(new RootContext(), req2);

        // Then
        if ((collection && collectionAction) || (!collection && resourceAction)) {
            assertThat(promise1).succeeded().withContent().stringAt("result").isEqualTo(actionId1);
            assertThat(promise2).succeeded().withContent().stringAt("result").isEqualTo(actionId2);
        } else {
            assertThat(promise1).failedWithException().isInstanceOf(NotSupportedException.class);
            assertThat(promise2).failedWithException().isInstanceOf(NotSupportedException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testActionCollectionItemAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        ActionRequest req1 = Requests.newActionRequest("/test/fred", "instanceAction1");
        ActionRequest req2 = Requests.newActionRequest("/test/fred", "instanceAction2");

        // When
        Promise<ActionResponse, ResourceException> promise1 = connection.actionAsync(new RootContext(), req1);
        Promise<ActionResponse, ResourceException> promise2 = connection.actionAsync(new RootContext(), req2);

        // Then
        if (collectionAction && collection) {
            assertThat(promise1).succeeded().withContent().stringAt("result").isEqualTo("instanceAction1-fred");
            assertThat(promise2).succeeded().withContent().stringAt("result").isEqualTo("instanceAction2-fred");
        } else if (collection) {
            assertThat(promise1).failedWithException().isInstanceOf(NotSupportedException.class);
            assertThat(promise2).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise1).failedWithException().isInstanceOf(NotFoundException.class);
            assertThat(promise2).failedWithException().isInstanceOf(NotFoundException.class);
        }
    }

    @Test(dataProvider = "annotatedRequestHandlerData")
    public void testQueryCollectionAnnotatedRequestHandler(Class<?> requestHandler, boolean collection, boolean create, boolean read,
            boolean update, boolean delete, boolean patch, boolean resourceAction, boolean collectionAction,
            boolean query)
            throws Exception {

        // Given
        Object provider = requestHandler.newInstance();
        Connection connection = Resources.newInternalConnection(createHandler(collection, provider));
        QueryRequest req = Requests.newQueryRequest("/test");

        // When
        Promise<QueryResponse, ResourceException> promise = connection.queryAsync(new RootContext(), req, mock(QueryResourceHandler.class));

        // Then
        if (query && collection) {
            AssertJPromiseAssert.assertThat(promise).succeeded();
            QueryResponse result = promise.get();
            Assertions.assertThat(result.getPagedResultsCookie()).isEqualTo("query");
        } else if (collection) {
            AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        }
    }

    private RequestHandler createHandler(boolean collection, Object provider) {
        RequestHandler handler = collection ? Resources.newCollection(provider) : Resources.newSingleton(provider);
        Router router = new Router();
        router.addRoute(requestUriMatcher(collection ? STARTS_WITH : EQUALS, "test"), handler);
        return router;
    }

    @org.forgerock.json.resource.annotations.RequestHandler
    public static final class NoMethods {
    }

    @org.forgerock.json.resource.annotations.RequestHandler
    public static final class AnnotationCollection {
        @Create
        public Promise<ResourceResponse, ResourceException> myCreate(CreateRequest request) {
            return newResultPromise(newResourceResponse("create", "1", json(object(field("result", "read")))));
        }
        @Read
        public Promise<ResourceResponse, ResourceException> myRead(String id) {
            return newResultPromise(newResourceResponse("read-" + id, "1", json(object(field("result", null)))));
        }
        @Update
        public Promise<ResourceResponse, ResourceException> myUpdate(UpdateRequest request, String id) {
            return newResultPromise(newResourceResponse("update-" + id, "1", json(object(field("result", null)))));
        }
        @Delete
        public Promise<ResourceResponse, ResourceException> myDelete(String id) {
            return newResultPromise(newResourceResponse("delete-" + id, "1", json(object(field("result", null)))));
        }
        @Patch
        public Promise<ResourceResponse, ResourceException> myPatch(PatchRequest request, String id) {
            return newResultPromise(newResourceResponse("patch-" + id, "1", json(object(field("result", null)))));
        }
        @Action("instanceAction1")
        public Promise<ActionResponse, ResourceException> instAction1(String id) {
            return newResultPromise(Responses.newActionResponse(json(object(field("result", "instanceAction1-" + id)))));
        }
        @Action
        public Promise<ActionResponse, ResourceException> instanceAction2(String id) {
            return newResultPromise(Responses.newActionResponse(json(object(field("result", "instanceAction2-" + id)))));
        }
        @Action("collectionAction1")
        public Promise<ActionResponse, ResourceException> action1() {
            return newResultPromise(Responses.newActionResponse(json(object(field("result", "collectionAction1")))));
        }
        @Action
        public Promise<ActionResponse, ResourceException> collectionAction2() {
            return newResultPromise(Responses.newActionResponse(json(object(field("result", "collectionAction2")))));
        }
        @Query
        public Promise<QueryResponse, ResourceException> query(QueryRequest request, QueryResourceHandler handler) {
            return newResultPromise(newQueryResponse("query", CountPolicy.NONE, QueryResponse.NO_COUNT));
        }
    }

    @org.forgerock.json.resource.annotations.RequestHandler
    public static final class AnnotationSingleton {
        @Read
        public Promise<ResourceResponse, ResourceException> myRead() {
            return newResultPromise(newResourceResponse("read", "1", json(object(field("result", "read")))));
        }
        @Update
        public Promise<ResourceResponse, ResourceException> myUpdate(UpdateRequest request) {
            return newResultPromise(newResourceResponse("update", "1", json(object(field("result", null)))));
        }
        @Patch
        public Promise<ResourceResponse, ResourceException> myPatch(PatchRequest request) {
            return newResultPromise(newResourceResponse("patch", "1", json(object(field("result", null)))));
        }
        @Action("instanceAction1")
        public Promise<ActionResponse, ResourceException> action1() {
            return newResultPromise(Responses.newActionResponse(json(object(field("result", "instanceAction1")))));
        }
        @Action
        public Promise<ActionResponse, ResourceException> instanceAction2() {
            return newResultPromise(Responses.newActionResponse(json(object(field("result", "instanceAction2")))));
        }
    }

    @org.forgerock.json.resource.annotations.RequestHandler
    public static final class ConventionCollection {
        public Promise<ResourceResponse, ResourceException> create(CreateRequest request) {
            return newResultPromise(newResourceResponse("create", "1", json(object(field("result", "read")))));
        }
        public Promise<ResourceResponse, ResourceException> read(String id) {
            return newResultPromise(newResourceResponse("read-" + id, "1", json(object(field("result", null)))));
        }
        public Promise<ResourceResponse, ResourceException> update(UpdateRequest request, String id) {
            return newResultPromise(newResourceResponse("update-" + id, "1", json(object(field("result", null)))));
        }
        public Promise<ResourceResponse, ResourceException> delete(String id) {
            return newResultPromise(newResourceResponse("delete-" + id, "1", json(object(field("result", null)))));
        }
        public Promise<ResourceResponse, ResourceException> patch(PatchRequest request, String id) {
            return newResultPromise(newResourceResponse("patch-" + id, "1", json(object(field("result", null)))));
        }
        public Promise<QueryResponse, ResourceException> query(QueryRequest request, QueryResourceHandler handler) {
            return newResultPromise(newQueryResponse("query", CountPolicy.NONE, QueryResponse.NO_COUNT));
        }
    }

    @org.forgerock.json.resource.annotations.RequestHandler
    public static final class ConventionSingleton {
        public Promise<ResourceResponse, ResourceException> read() {
            return newResultPromise(newResourceResponse("read", "1", json(object(field("result", "read")))));
        }
        public Promise<ResourceResponse, ResourceException> update(UpdateRequest request) {
            return newResultPromise(newResourceResponse("update", "1", json(object(field("result", null)))));
        }
        public Promise<ResourceResponse, ResourceException> patch(PatchRequest request) {
            return newResultPromise(newResourceResponse("patch", "1", json(object(field("result", null)))));
        }
    }

}
