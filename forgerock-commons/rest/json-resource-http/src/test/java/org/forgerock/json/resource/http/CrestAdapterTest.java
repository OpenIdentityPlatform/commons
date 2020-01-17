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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.PatchOperation.copy;
import static org.forgerock.json.resource.PatchOperation.move;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newDeleteRequest;
import static org.forgerock.json.resource.Requests.newPatchRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Requests.newUpdateRequest;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.FORBIDDEN;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.NOT_SUPPORTED;
import static org.forgerock.json.resource.ResourceException.UNAVAILABLE;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.Assertions.assertThat;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;
import static org.forgerock.json.resource.http.CrestHttp.newRequestHandler;
import static org.forgerock.util.query.QueryFilter.present;

import java.net.URI;
import java.net.URISyntaxException;

import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.http.servlet.HttpFrameworkServlet;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.Promise;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class CrestAdapterTest {

    private static final Version V3_0 = version("3.0");

    private static <T extends org.forgerock.json.resource.Request> T withVersion(T request, Version version) {
        request.setResourceVersion(version);
        return request;
    }

    private static <T extends org.forgerock.json.resource.Response> T withVersion(T response, Version version) {
        // FIXME Why is the method name different in request and response ?
        response.setResourceApiVersion(version);
        return response;
    }

    private static RequestHandler createRequestHandler(final RequestHandler delegate) throws URISyntaxException {
        return newRequestHandler(newHttpHandler(delegate), new URI("http://localhost/"));
    }

    private static Context newContext() throws URISyntaxException {
        // All this chain is needed because of create request that returns an absolute location
        return new AttributesContext(new HttpContext(new RootContext(), new Request().setUri("http://localhost")));
    }

    @DataProvider
    public static Object[][] resourceResponses() {
        // @Checkstyle:off
        return new Object[][] {
                //{ newResourceResponse(null, null, null) }, // must be a JsonValue
                //{ newResourceResponse(null, null, json(null)) }, // must not be null
                { newResourceResponse("bjensen", null, json(object())) },
                { newResourceResponse(null, "ae32f", json(object())) },
                { newResourceResponse("bjensen", "ae32f", json(singletonMap("hello", "world"))) },
                { withVersion(newResourceResponse("bjensen", "ae32f", json(singletonMap("hello", "world"))), V3_0) },
        };
        // @Checkstyle:on
    }

    @DataProvider
    public static Object[][] resourceExceptions() {
        // @Checkstyle:off
        return new Object[][] {
                { newResourceException(NOT_FOUND) },
                { newResourceException(FORBIDDEN).setReason("a-reason") },
                { newResourceException(UNAVAILABLE).setDetail(json(object(field("hello", "world")))) },
                { newResourceException(NOT_SUPPORTED, "Boom") },
                { newResourceException(INTERNAL_ERROR, "Boom", new Exception("Origin")) },
                { withVersion(newResourceException(BAD_REQUEST, "Boom"), V3_0) },
        };
        // @Checkstyle:on
    }

    @DataProvider
    public static Object[][] deleteRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                { newDeleteRequest("users/bjensen") },
                { newDeleteRequest("users/bjensen").setAdditionalParameter("transmitted", "true") },
                { newDeleteRequest("users/bjensen").addField("name") },
                { withVersion(newDeleteRequest("users/bjensen"), V3_0) },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "deleteRequests")
    public void shouldHandleDeleteRequests(final DeleteRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(new AbstractRequestHandler() {
            @Override
            public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                                                                             final DeleteRequest request) {
                // Assert that re-created request is equal to the initial one
                assertThat(request).isEqualTo(source);
                return newResourceResponse(null, null, json(object())).asPromise();
            }
        });

        ResourceResponse response = handler.handleDelete(newContext(),
                                                         source).getOrThrow();
        assertThat(response).isNotNull();
    }

    @Test(dataProvider = "resourceResponses")
    public void shouldHandleDeleteResponses(final ResourceResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                                                                                     final DeleteRequest request) {
                        return source.asPromise();
                    }
                });

        ResourceResponse response = handler.handleDelete(newContext(),
                                                         newDeleteRequest("users/bjensen")).getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandleDeleteExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                                                                                     final DeleteRequest request) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handleDelete(newContext(),
                                 newDeleteRequest("users/bjensen")).getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }
    }

    @DataProvider
    public static Object[][] readRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                { newReadRequest("users/bjensen") },
                { newReadRequest("users/bjensen", "bjensen") },
                { withVersion(newReadRequest("users/bjensen"), V3_0) },
                { newReadRequest("users/bjensen").setAdditionalParameter("transmitted", "true") },
                { newReadRequest("users/bjensen").addField("name") },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "readRequests")
    public void shouldHandleReadRequests(final ReadRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                                                                                   final ReadRequest request) {
                        // Assert that re-created request is equal to the initial one
                        assertThat(request).isEqualTo(source);
                        return newResourceResponse(null, null, json(object())).asPromise();
                    }
                });

        ResourceResponse response = handler.handleRead(newContext(), source)
                                           .getOrThrow();
        assertThat(response).isNotNull();
    }

    @Test(dataProvider = "resourceResponses")
    public void shouldHandleReadResponses(final ResourceResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                                                                                   final ReadRequest request) {
                        return source.asPromise();
                    }
                });

        ResourceResponse response = handler.handleRead(newContext(), newReadRequest("users/bjensen"))
                                           .getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandleReadExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                                                                                   final ReadRequest request) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handleRead(newContext(), newReadRequest("users/bjensen"))
                   .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }
    }

    @DataProvider
    public static Object[][] updateRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                //{ newUpdateRequest("users/bjensen", json(null)) }, // null is transformed to an empty map => assert error
                { newUpdateRequest("users/bjensen", json(object(field("hello", "world")))) },
                { newUpdateRequest("users/bjensen", "bjensen", json(object(field("hello", "world")))) },
                { newUpdateRequest("users/bjensen", json(object(field("hello", "world")))).setRevision("ae32f") },
                { newUpdateRequest("users/bjensen", "bjensen", json(object(field("hello", "world")))).setRevision("ae32f") },
                { withVersion(newUpdateRequest("users/bjensen", json(object(field("hello", "world")))), V3_0) },
                { newUpdateRequest("users/bjensen", json(object())).setAdditionalParameter("transmitted", "true") },
                { newUpdateRequest("users/bjensen", json(object())).addField("name") },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "updateRequests")
    public void shouldHandleUpdateRequests(final UpdateRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                                     final UpdateRequest request) {
                        // Assert that re-created request is equal to the initial one
                        assertThat(request).isEqualTo(source);
                        return newResourceResponse(null, null, json(object())).asPromise();
                    }
                });

        ResourceResponse response = handler.handleUpdate(newContext(), source)
                                           .getOrThrow();
        assertThat(response).isNotNull();
    }

    @Test(dataProvider = "resourceResponses")
    public void shouldHandleUpdateResponses(final ResourceResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                                     final UpdateRequest request) {
                        return source.asPromise();
                    }
                });

        ResourceResponse response = handler.handleUpdate(newContext(), newUpdateRequest("users/bjensen", json(null)))
                                           .getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandleUpdateExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                                     final UpdateRequest request) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handleUpdate(newContext(), newUpdateRequest("users/bjensen", json(null)))
                   .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }
    }

    @DataProvider
    public static Object[][] patchRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                //{ newPatchRequest("users/bjensen") }, // no operation is invalid
                { newPatchRequest("users/bjensen", copy("uuid", "user_id")) }, // single operation
                { newPatchRequest("users/bjensen", copy("uuid", "user_id"), move("name", "display")) }, // multi operations
                // with resource ID
                // { newPatchRequest("users/bjensen", "bjensen") }, // no operation is invalid
                { newPatchRequest("users/bjensen", "bjensen", copy("uuid", "user_id")) }, // single operation
                { newPatchRequest("users/bjensen", "bjensen", copy("uuid", "user_id"), move("name", "display")) }, // multi operations
                // with version
                { withVersion(newPatchRequest("users/bjensen", copy("uuid", "user_id"), move("name", "display")),
                              V3_0) }, // multi operations
                { newPatchRequest("users/bjensen", copy("uuid", "user_id")).setAdditionalParameter("transmitted", "true") },
                { newPatchRequest("users/bjensen", copy("uuid", "user_id")).addField("name") },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "patchRequests")
    public void shouldHandlePatchRequests(final PatchRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                                                                                    final PatchRequest request) {
                        // Assert that re-created request is equal to the initial one
                        assertThat(request).isEqualTo(source);
                        return newResourceResponse(null, null, json(object())).asPromise();
                    }
                });

        ResourceResponse response = handler.handlePatch(newContext(), source)
                                           .getOrThrow();
        assertThat(response).isNotNull();
    }

    @Test(dataProvider = "resourceResponses")
    public void shouldHandlePatchResponses(final ResourceResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                                                                                    final PatchRequest request) {
                        return source.asPromise();
                    }
                });

        ResourceResponse response = handler.handlePatch(newContext(), newPatchRequest("users/bjensen", copy("a", "b")))
                                           .getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandlePatchExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                                                                                    final PatchRequest request) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handlePatch(newContext(), newPatchRequest("users/bjensen", copy("a", "b")))
                   .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }

    }

    @DataProvider
    public static Object[][] createRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                // without specified ID
                //{ newCreateRequest("users/bjensen", json(null)) }, // null is transformed to an empty map => assert error
                { newCreateRequest("users/bjensen", json(object(field("hello", "world")))) },

                // with ID
                { newCreateRequest("users", "bjensen", json(object(field("hello", "world")))) }, // when providing an ID, the path should not have it

                // with version
                { withVersion(newCreateRequest("users/bjensen", json(object(field("hello", "world")))), V3_0) }, // multi operations

                // with additional things
                { newCreateRequest("users/bjensen", json(object())).setAdditionalParameter("transmitted", "true") },
                { newCreateRequest("users/bjensen", json(object())).addField("name") },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "createRequests")
    public void shouldHandleCreateRequests(final CreateRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                                     final CreateRequest request) {
                        // Assert that re-created request is equal to the initial one
                        assertThat(request).isEqualTo(source);
                        return newResourceResponse(null, null, json(object())).asPromise();
                    }
                });

        ResourceResponse response = handler.handleCreate(newContext(), source)
                                           .getOrThrow();
        assertThat(response).isNotNull();
    }

    @Test(dataProvider = "resourceResponses")
    public void shouldHandleCreateResponses(final ResourceResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                                     final CreateRequest request) {
                        return source.asPromise();
                    }
                });

        UriRouterContext context = new UriRouterContext(newContext(),
                                                        "/users/bjensen",
                                                        null,
                                                        singletonMap("name", "bjensen"));
        ResourceResponse response = handler.handleCreate(context, newCreateRequest("users/bjensen", json(object())))
                                           .getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandleCreateExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                                     final CreateRequest request) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handleCreate(newContext(), newCreateRequest("users/bjensen", json(object())))
                   .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }

    }

    @DataProvider
    public static Object[][] actionRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                { newActionRequest("users/bjensen", "promote") },
                { newActionRequest("users/bjensen", "bjensen", "promote") },

                // with version
                { withVersion(newActionRequest("users/bjensen", "promote"), V3_0) },

                // with additional things
                { newActionRequest("users/bjensen", "promote").setAdditionalParameter("transmitted", "true") },
                { newActionRequest("users/bjensen", "promote").addField("name") },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "actionRequests")
    public void shouldHandleActionRequests(final ActionRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
                                                                                   final ActionRequest request) {
                        // Assert that re-created request is equal to the initial one
                        assertThat(request).isEqualTo(source);
                        return newActionResponse(json(object())).asPromise();
                    }
                });

        ActionResponse response = handler.handleAction(newContext(), source)
                                         .getOrThrow();
        assertThat(response).isNotNull();
    }

    @DataProvider
    public static Object[][] actionResponses() {
        // @Checkstyle:off
        return new Object[][] {
                // { null }, null should be accepted as a valid action response, but that doesn't fit well in my test system ...
                // { newActionResponse(null) }, // ActionResponse expect a non-null wrapped object
                // { newActionResponse(json(null)) }, // ActionResponse expect a non-null wrapped object
                { newActionResponse(json("string")) },
                { newActionResponse(json(true)) },
                { newActionResponse(json(42)) },
                { newActionResponse(json(array("hello"))) },
                { newActionResponse(json(singletonMap("hello", "world"))) },
                { withVersion(newActionResponse(json(singletonMap("hello", "world"))), V3_0) },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "actionResponses")
    public void shouldHandleActionResponses(final ActionResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
                                                                                   final ActionRequest request) {
                        return source.asPromise();
                    }
                });

        ActionResponse response = handler.handleAction(newContext(), newActionRequest("users/bjensen", "promote"))
                                         .getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandleActionExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
                                                                                   final ActionRequest request) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handleAction(newContext(), newActionRequest("users/bjensen", "promote"))
                   .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }

    }

    @DataProvider
    public static Object[][] queryRequests() throws Exception {
        // @Checkstyle:off
        return new Object[][] {
                { newQueryRequest("users").setQueryId("all") },
                { newQueryRequest("users").setQueryId("all").setPagedResultsCookie("mine") }, // unsupported: 'Unrecognized request parameter '_pagedResultsCookie''
                { newQueryRequest("users").setQueryId("all").setPagedResultsOffset(2) }, // unsupported
                { newQueryRequest("users").setQueryId("all").setPageSize(10) },
                { newQueryRequest("users").setQueryId("all").setTotalPagedResultsPolicy(CountPolicy.EXACT) },
                { newQueryRequest("users").setQueryExpression("SELECT * FROM USERS") },
                { newQueryRequest("users").setQueryFilter(present(new JsonPointer("name"))) },
                { withVersion( newQueryRequest("users").setQueryExpression("true"), V3_0) },
                { newQueryRequest("users").setQueryId("all").addField("name") },
                { newQueryRequest("users").setQueryId("all").setAdditionalParameter("transmitted", "true") },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "queryRequests")
    public void shouldHandleQueryRequests(final QueryRequest source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                                                                                 final QueryRequest request,
                                                                                 final QueryResourceHandler qrh) {
                        // Assert that re-created request is equal to the initial one
                        assertThat(request).isEqualTo(source);
                        return newQueryResponse().asPromise();
                    }
                });

        QueryResponse response = handler.handleQuery(newContext(), source, null)
                                        .getOrThrow();
        assertThat(response).isNotNull();
    }

    @DataProvider
    public static Object[][] queryResponses() {
        // @Checkstyle:off
        return new Object[][] {
                { newQueryResponse() },
                { newQueryResponse("paging-cookie") },
                { newQueryResponse("paging-cookie", CountPolicy.NONE, 10) },
                { withVersion(newQueryResponse("paging-cookie", CountPolicy.EXACT, 10), V3_0) },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "queryResponses")
    public void shouldHandleQueryResponses(final QueryResponse source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                                                                                 final QueryRequest request,
                                                                                 final QueryResourceHandler qrh) {
                        return source.asPromise();
                    }
                });

        QueryResponse response = handler.handleQuery(newContext(), newQueryRequest("users").setQueryId("all"), null)
                                        .getOrThrow();

        // Assert that re-created response is equal to the initial one
        assertThat(response).isEqualTo(source);
    }

    @DataProvider
    public static Object[][] queryResourceResponses() {
        // @Checkstyle:off
        return new Object[][] {
                { new ResourceResponse[] { newResourceResponse("bjensen", "0", json(object())) } },
                { new ResourceResponse[] { newResourceResponse("bjensen", "0", json(object())),
                                           newResourceResponse("stephen", "1", json(object(field("id", "steph")))) } },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "queryResourceResponses", enabled = false)
    public void shouldHandleResponseResourcesInQueries(final ResourceResponse[] responses) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                                                                                 final QueryRequest request,
                                                                                 final QueryResourceHandler qrh) {
                        for (ResourceResponse response : responses) {
                            qrh.handleResource(response);
                        }
                        return newQueryResponse().asPromise();
                    }
                });

        final int[] i = {0}; // because I can't change a non-final variable in anonymous inner class
        QueryResourceHandler resourceHandler = new QueryResourceHandler() {
            @Override
            public boolean handleResource(final ResourceResponse resource) {
                assertThat(resource).isEqualTo(responses[i[0]++]);
                return true;
            }
        };
        handler.handleQuery(newContext(),
                            newQueryRequest("users").setQueryId("all"),
                            resourceHandler)
               .getOrThrow();
    }

    @Test
    public void shouldHandleResponseResourcesInQueriesWhenErrorOccurs() throws Exception {

        final ResourceResponse bjensen = newResourceResponse("bjensen", "0", json(object()));
        final ResourceException exception = newResourceException(INTERNAL_ERROR);

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                                                                                 final QueryRequest request,
                                                                                 final QueryResourceHandler qrh) {
                        qrh.handleResource(bjensen);
                        return exception.asPromise();
                    }
                });

        try {
            handler.handleQuery(newContext(), newQueryRequest("users").setQueryId("all"),
                    new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(final ResourceResponse resource) {
                            assertThat(resource).isEqualTo(bjensen);
                            return true;
                        }
                    })
                    .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test(dataProvider = "resourceExceptions")
    public void shouldHandleQueryExceptions(final ResourceException source) throws Exception {

        RequestHandler handler = createRequestHandler(
                new AbstractRequestHandler() {
                    @Override
                    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                                                                                 final QueryRequest request,
                                                                                 final QueryResourceHandler qrh) {
                        return source.asPromise();
                    }
                });

        try {
            handler.handleQuery(newContext(), newQueryRequest("users").setQueryId("all"), null)
                   .getOrThrow();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            // Assert that re-created exceptions is equal to the initial one
            assertThat(e).isEqualTo(source);
        }
    }

    @Test
    public void shouldWorkWithRealHttpServer() throws Exception {
        WebappContext webappContext = new WebappContext("Crest", "/rest");
        ServletRegistration reg = webappContext.addServlet("CHF",
                                                           new HttpFrameworkServlet(new TestCrestHttpApplication()));
        reg.addMapping("/app/*");
        reg.setAsyncSupported(true);
        reg.setLoadOnStartup(1);

        HttpServer server = HttpServer.createSimpleServer(".", "localhost", new PortRange(6000, 7000));
        try (HttpClientHandler httpClientHandler = new HttpClientHandler()) {
            webappContext.deploy(server);
            server.start();

            NetworkListener listener = server.getListener("grizzly");
            String uri = format("http://%s:%d/rest/app/", listener.getHost(), listener.getPort());
            RequestHandler handler = newRequestHandler(httpClientHandler, new URI(uri));

            // Read -------------
            ReadRequest readRequest = newReadRequest("users/bjensen");
            Promise<ResourceResponse, ResourceException> promise = handler.handleRead(new RootContext(), readRequest);
            ResourceResponse readResponse = promise.getOrThrow();
            assertThat(readResponse.getId()).isEqualTo("ae32f");
            assertThat(readResponse.getRevision()).isEqualTo("1");
            assertThat(readResponse.getContent()).isEqualTo(readRequest.toJsonValue());

            // Create -------------
            CreateRequest createRequest = newCreateRequest("users", json(object(field("name", "bjensen"))));
            ResourceResponse createResponse = handler.handleCreate(new RootContext(), createRequest).getOrThrow();
            assertThat(createResponse.getId()).isEqualTo("ae32f");
            assertThat(createResponse.getRevision()).isEqualTo("1");
            assertThat(createResponse.getContent()).isEqualTo(createRequest.toJsonValue());

            // Update -------------
            UpdateRequest updateRequest = newUpdateRequest("users", "bjensen", json(object(field("name", "bjensen"))));
            ResourceResponse updateResponse = handler.handleUpdate(new RootContext(), updateRequest).getOrThrow();
            assertThat(updateResponse.getId()).isEqualTo("ae32f");
            assertThat(updateResponse.getRevision()).isEqualTo("1");
            assertThat(updateResponse.getContent()).isEqualTo(updateRequest.toJsonValue());

            // Delete -------------
            DeleteRequest deleteRequest = newDeleteRequest("users", "bjensen");
            ResourceResponse deleteResponse = handler.handleDelete(new RootContext(), deleteRequest).getOrThrow();
            assertThat(deleteResponse.getId()).isEqualTo("ae32f");
            assertThat(deleteResponse.getRevision()).isEqualTo("1");
            assertThat(deleteResponse.getContent()).isEqualTo(deleteRequest.toJsonValue());

            // Patch -------------
            PatchRequest patchRequest = newPatchRequest("users", "bjensen", copy("name", "login"));
            ResourceResponse patchResponse = handler.handlePatch(new RootContext(), patchRequest).getOrThrow();
            assertThat(patchResponse.getId()).isEqualTo("ae32f");
            assertThat(patchResponse.getRevision()).isEqualTo("1");
            assertThat(patchResponse.getContent()).isEqualTo(patchRequest.toJsonValue());

            // Action -------------
            ActionRequest actionRequest = newActionRequest("users", "bjensen", "promote");
            ActionResponse actionResponse = handler.handleAction(new RootContext(), actionRequest).getOrThrow();
            assertThat(actionResponse.getJsonContent()).isEqualTo(actionRequest.toJsonValue());

            // Query -------------
            final QueryRequest queryRequest = newQueryRequest("users").setQueryId("all");
            QueryResourceHandler queryResourceHandler = new QueryResourceHandler() {
                @Override
                public boolean handleResource(final ResourceResponse resource) {
                    assertThat(resource.getId()).isEqualTo("ae32f");
                    assertThat(resource.getRevision()).isEqualTo("1");
                    assertThat(resource.getContent()).isEqualTo(queryRequest.toJsonValue());
                    return true;
                }
            };
            QueryResponse queryResponse = handler.handleQuery(new RootContext(),
                                                              queryRequest,
                                                              queryResourceHandler).getOrThrow();
            assertThat(queryResponse.getPagedResultsCookie()).isEqualTo("cookie");
        } finally {
            server.shutdownNow();
        }
    }

    private static class TestCrestHttpApplication implements HttpApplication {

        @Override
        public Handler start() throws HttpApplicationException {
            return CrestHttp.newHttpHandler(new RequestHandler() {
                @Override
                public Promise<ActionResponse, ResourceException> handleAction(final Context context,
                                                                               final ActionRequest request) {
                    return newActionResponse(request.toJsonValue()).asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                                 final CreateRequest request) {
                    return newResourceResponse("ae32f", "1", request.toJsonValue()).asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                                                                                 final DeleteRequest request) {
                    return newResourceResponse("ae32f", "1", request.toJsonValue()).asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                                                                                final PatchRequest request) {
                    return newResourceResponse("ae32f", "1", request.toJsonValue()).asPromise();
                }

                @Override
                public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
                                                                             final QueryRequest request,
                                                                             final QueryResourceHandler handler) {
                    handler.handleResource(newResourceResponse("ae32f", "1", request.toJsonValue()));
                    return newQueryResponse("cookie").asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                                                                               final ReadRequest request) {
                	
                    return newResourceResponse("ae32f", "1", request.toJsonValue()).asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                                 final UpdateRequest request) {
                    return newResourceResponse("ae32f", "1", request.toJsonValue()).asPromise();
                }
            });
        }

        @Override
        public Factory<Buffer> getBufferFactory() {
            return null;
        }

        @Override
        public void stop() {

        }
    }
}
