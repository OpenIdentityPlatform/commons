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

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.context.ServerContext;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RouterTest {

    private Router router;

    private RequestHandler routeOneHandler;
    private RequestHandler routeTwoHandler;
    private RequestHandler defaultRouteHandler;

    @BeforeClass
    public void setupClass() {
        routeOneHandler = mock(RequestHandler.class);
        routeTwoHandler = mock(RequestHandler.class);
        defaultRouteHandler = mock(RequestHandler.class);
    }

    @BeforeMethod
    public void setup() {
        router = new Router();
    }

    @DataProvider
    private Object[][] data() {
        return new Object[][]{
            {"users/demo", routeOneHandler},
            {"config", routeTwoHandler},
            {"groups/admin", defaultRouteHandler},
        };
    }

    @SuppressWarnings("unchecked")
    @Test(dataProvider = "data")
    public void creatingRouterFromExistingRouterShouldCopyAllRoutes(String requestUri, RequestHandler expectedHandler) {

        //Given
        router.addRoute(requestUriMatcher(STARTS_WITH, "users/{id}"), routeOneHandler);
        router.addRoute(requestUriMatcher(EQUALS, "config"), routeTwoHandler);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        Router newRouter = new Router(router);

        //Then
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = mockRequest(ReadRequest.class, requestUri);
        newRouter.handleRead(context, request);

        verify(expectedHandler).handleRead(any(ServerContext.class), any(ReadRequest.class));
    }

    @Test
    public void shouldAddRouteToCollectionResourceProvider() {

        //Given
        CollectionResourceProvider provider = mock(CollectionResourceProvider.class);
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = mockRequest(ReadRequest.class, "users/demo");

        //When
        router.addRoute("users", provider);

        //Then
        router.handleRead(context, request);
        verify(provider).readInstance(any(ServerContext.class), anyString(), any(ReadRequest.class));
    }

    @Test
    public void shouldAddRouteToSingletonResourceProvider() {

        //Given
        SingletonResourceProvider provider = mock(SingletonResourceProvider.class);
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = mockRequest(ReadRequest.class, "config");

        //When
        router.addRoute("config", provider);

        //Then
        router.handleRead(context, request);
        verify(provider).readInstance(any(ServerContext.class), any(ReadRequest.class));
    }

    @DataProvider
    private Object[][] testData() {
        return new Object[][]{
            {"", true}, // wasRouted = true
            {"users/demo", false}, // wasRouted = false
        };
    }

    @Test(dataProvider = "testData")
    public void handleActionShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        ActionRequest request = mock(ActionRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleAction(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleAction(any(ServerContext.class), any(ActionRequest.class));
        } else {
            verify(defaultRouteHandler).handleAction(any(ServerContext.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleCreateShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        CreateRequest request = mock(CreateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleCreate(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleCreate(any(ServerContext.class), any(CreateRequest.class));
        } else {
            verify(defaultRouteHandler).handleCreate(any(ServerContext.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleDeleteShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        DeleteRequest request = mock(DeleteRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleDelete(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleDelete(any(ServerContext.class), any(DeleteRequest.class));
        } else {
            verify(defaultRouteHandler).handleDelete(any(ServerContext.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handlePatchShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        PatchRequest request = mock(PatchRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handlePatch(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handlePatch(any(ServerContext.class), any(PatchRequest.class));
        } else {
            verify(defaultRouteHandler).handlePatch(any(ServerContext.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleQueryShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler resourceHandler = mock(QueryResourceHandler.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleQuery(context, request, resourceHandler);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleQuery(any(ServerContext.class), any(QueryRequest.class),
                    any(QueryResourceHandler.class));
        } else {
            verify(defaultRouteHandler).handleQuery(any(ServerContext.class), eq(request),
                    any(QueryResourceHandler.class));
        }
    }

    @Test(dataProvider = "testData")
    public void handleReadShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        ReadRequest request = mock(ReadRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleRead(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleRead(any(ServerContext.class), any(ReadRequest.class));
        } else {
            verify(defaultRouteHandler).handleRead(any(ServerContext.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleUpdateShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        ServerContext context = newRouterContext(mock(ServerContext.class), remainingUri);
        UpdateRequest request = mock(UpdateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleUpdate(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleUpdate(any(ServerContext.class), any(UpdateRequest.class));
        } else {
            verify(defaultRouteHandler).handleUpdate(any(ServerContext.class), eq(request));
        }
    }

    @Test
    public void handleCreateShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        CreateRequest request = mock(CreateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<Resource, ResourceException> promise = router.handleCreate(context, request);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    @Test
    public void handleReadShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ReadRequest request = mock(ReadRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<Resource, ResourceException> promise = router.handleRead(context, request);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    @Test
    public void handleUpdateShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = mock(UpdateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<Resource, ResourceException> promise = router.handleUpdate(context, request);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    @Test
    public void handleDeleteShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = mock(DeleteRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<Resource, ResourceException> promise = router.handleDelete(context, request);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    @Test
    public void handlePatchShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        PatchRequest request = mock(PatchRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<Resource, ResourceException> promise = router.handlePatch(context, request);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    @Test
    public void handleActionShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        ActionRequest request = mock(ActionRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<JsonValue, ResourceException> promise = router.handleAction(context, request);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    @Test
    public void handleQueryShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        ServerContext context = mock(ServerContext.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler resultHandler = mock(QueryResourceHandler.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<QueryResult, ResourceException> promise = router.handleQuery(context, request, resultHandler);

        //Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(ResourceException.class);
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(404);
            assertThat(e.getMessage()).isEqualTo("Resource 'users/demo' not found");
        }
    }

    private ServerContext newRouterContext(Context parentContext, String remainingUri) {
        return new RouterContext(parentContext, "MATCHED_URI", remainingUri, Collections.<String, String>emptyMap());
    }

    private <T extends Request> T mockRequest(Class<T> clazz, String resourcePath) {
        T request = mock(clazz);
        given(request.getResourcePath()).willReturn(resourcePath);
        given(request.getResourcePathObject()).willReturn(ResourcePath.valueOf(resourcePath));
        return request;
    }
}
