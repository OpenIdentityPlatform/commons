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
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.forgerock.services.context.Context;
import org.forgerock.http.routing.UriRouterContext;
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
        Context context = mock(Context.class);
        ReadRequest request = mockRequest(ReadRequest.class, requestUri);
        newRouter.handleRead(context, request);

        verify(expectedHandler).handleRead(any(Context.class), any(ReadRequest.class));
    }

    @Test
    public void shouldAddRouteToCollectionResourceProvider() {

        //Given
        CollectionResourceProvider provider = mock(CollectionResourceProvider.class);
        Context context = mock(Context.class);
        ReadRequest request = mockRequest(ReadRequest.class, "users/demo");

        //When
        router.addRoute(uriTemplate("users"), provider);

        //Then
        router.handleRead(context, request);
        verify(provider).readInstance(any(Context.class), anyString(), any(ReadRequest.class));
    }

    @Test
    public void shouldAddRouteToSingletonResourceProvider() {

        //Given
        SingletonResourceProvider provider = mock(SingletonResourceProvider.class);
        Context context = mock(Context.class);
        ReadRequest request = mockRequest(ReadRequest.class, "config");

        //When
        router.addRoute(uriTemplate("config"), provider);

        //Then
        router.handleRead(context, request);
        verify(provider).readInstance(any(Context.class), any(ReadRequest.class));
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
        Context context = newRouterContext(mock(Context.class), remainingUri);
        ActionRequest request = mock(ActionRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleAction(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleAction(any(Context.class), any(ActionRequest.class));
        } else {
            verify(defaultRouteHandler).handleAction(any(Context.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleCreateShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        Context context = newRouterContext(mock(Context.class), remainingUri);
        CreateRequest request = mock(CreateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleCreate(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleCreate(any(Context.class), any(CreateRequest.class));
        } else {
            verify(defaultRouteHandler).handleCreate(any(Context.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleDeleteShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        Context context = newRouterContext(mock(Context.class), remainingUri);
        DeleteRequest request = mock(DeleteRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleDelete(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleDelete(any(Context.class), any(DeleteRequest.class));
        } else {
            verify(defaultRouteHandler).handleDelete(any(Context.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handlePatchShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        Context context = newRouterContext(mock(Context.class), remainingUri);
        PatchRequest request = mock(PatchRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handlePatch(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handlePatch(any(Context.class), any(PatchRequest.class));
        } else {
            verify(defaultRouteHandler).handlePatch(any(Context.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleQueryShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        Context context = newRouterContext(mock(Context.class), remainingUri);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler resourceHandler = mock(QueryResourceHandler.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleQuery(context, request, resourceHandler);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleQuery(any(Context.class), any(QueryRequest.class),
                    any(QueryResourceHandler.class));
        } else {
            verify(defaultRouteHandler).handleQuery(any(Context.class), eq(request),
                    any(QueryResourceHandler.class));
        }
    }

    @Test(dataProvider = "testData")
    public void handleReadShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        Context context = newRouterContext(mock(Context.class), remainingUri);
        ReadRequest request = mock(ReadRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleRead(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleRead(any(Context.class), any(ReadRequest.class));
        } else {
            verify(defaultRouteHandler).handleRead(any(Context.class), eq(request));
        }
    }

    @Test(dataProvider = "testData")
    public void handleUpdateShouldCallBestRoute(String remainingUri, boolean expectedRequestToBeCopied) {

        //Given
        RequestHandler defaultRouteHandler = mock(RequestHandler.class);
        Context context = newRouterContext(mock(Context.class), remainingUri);
        UpdateRequest request = mock(UpdateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.handleUpdate(context, request);

        //Then
        if (expectedRequestToBeCopied) {
            verify(defaultRouteHandler).handleUpdate(any(Context.class), any(UpdateRequest.class));
        } else {
            verify(defaultRouteHandler).handleUpdate(any(Context.class), eq(request));
        }
    }

    @Test
    public void handleCreateShouldReturn404ResponseExceptionIfNoRouteFound() {

        //Given
        Context context = mock(Context.class);
        CreateRequest request = mock(CreateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<ResourceResponse, ResourceException> promise = router.handleCreate(context, request);

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
        Context context = mock(Context.class);
        ReadRequest request = mock(ReadRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<ResourceResponse, ResourceException> promise = router.handleRead(context, request);

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
        Context context = mock(Context.class);
        UpdateRequest request = mock(UpdateRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<ResourceResponse, ResourceException> promise = router.handleUpdate(context, request);

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
        Context context = mock(Context.class);
        DeleteRequest request = mock(DeleteRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<ResourceResponse, ResourceException> promise = router.handleDelete(context, request);

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
        Context context = mock(Context.class);
        PatchRequest request = mock(PatchRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<ResourceResponse, ResourceException> promise = router.handlePatch(context, request);

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
        Context context = mock(Context.class);
        ActionRequest request = mock(ActionRequest.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<ActionResponse, ResourceException> promise = router.handleAction(context, request);

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
        Context context = mock(Context.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler resultHandler = mock(QueryResourceHandler.class);

        given(request.getResourcePath()).willReturn("users/demo");

        //When
        Promise<QueryResponse, ResourceException> promise = router.handleQuery(context, request, resultHandler);

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

    private Context newRouterContext(Context parentContext, String remainingUri) {
        return new UriRouterContext(parentContext, "MATCHED_URI", remainingUri, Collections.<String, String>emptyMap());
    }

    private <T extends Request> T mockRequest(Class<T> clazz, String resourcePath) {
        T request = mock(clazz);
        given(request.getResourcePath()).willReturn(resourcePath);
        given(request.getResourcePathObject()).willReturn(ResourcePath.valueOf(resourcePath));
        return request;
    }
}
