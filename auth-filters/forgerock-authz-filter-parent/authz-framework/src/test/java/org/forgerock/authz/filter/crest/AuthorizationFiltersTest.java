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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.filter.crest;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.Test;

public class AuthorizationFiltersTest {

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotCreateFilterForCollectionResourceProviderWhenTargetIsNull() {

        //Given
        CollectionResourceProvider target = null;
        CrestAuthorizationModule[] modules = null;

        //When
        AuthorizationFilters.createFilter(target, modules);

        //Then
        // Expect NullPointerException
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotCreateFilterForCollectionResourceProviderWhenModulesAreNull() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule[] modules = null;

        //When
        AuthorizationFilters.createFilter(target, modules);

        //Then
        // Expect NullPointerException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotCreateFilterForCollectionResourceProviderWhenModulesAreEmpty() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[0];

        //When
        AuthorizationFilters.createFilter(target, modules);

        //Then
        // Expect IllegalArgumentException
    }

    @Test
    public void shouldCreateFilterForCollectionResourceProvider() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        //When
        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        //Then
        assertNotNull(chain);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotCreateFilterForSingletonResourceProviderWhenTargetIsNull() {

        //Given
        SingletonResourceProvider target = null;
        CrestAuthorizationModule[] modules = null;

        //When
        AuthorizationFilters.createFilter(target, modules);

        //Then
        // Expect NullPointerException
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotCreateFilterForSingletonResourceProviderWhenModulesAreNull() {

        //Given
        SingletonResourceProvider target = mock(SingletonResourceProvider.class);
        CrestAuthorizationModule[] modules = null;

        //When
        AuthorizationFilters.createFilter(target, modules);

        //Then
        // Expect NullPointerException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotCreateFilterForSingletonResourceProviderWhenModulesAreEmpty() {

        //Given
        SingletonResourceProvider target = mock(SingletonResourceProvider.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[0];

        //When
        AuthorizationFilters.createFilter(target, modules);

        //Then
        // Expect IllegalArgumentException
    }

    @Test
    public void shouldCreateFilterForSingletonResourceProvider() {

        //Given
        SingletonResourceProvider target = mock(SingletonResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        //When
        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        //Then
        assertNotNull(chain);
    }

    @Test
    public void shouldCreateFilterForRequestHandler() {

        //Given
        RequestHandler target = mock(RequestHandler.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        //When
        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        //Then
        assertNotNull(chain);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterActionWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("RESOURCE_NAME", "ACTION_ID");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizeAction(context, request)).willReturn(authorizePromise);

        //When
        chain.handleAction(context, request, handler);

        //Then
        verify(target).actionInstance(eq(context), eq("RESOURCE_NAME"), Matchers.<ActionRequest>anyObject(),
                eq(handler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterActionWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("RESOURCE_NAME", "ACTION_ID");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizeAction(context, request)).willReturn(authorizePromise);

        //When
        chain.handleAction(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterActionWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        ActionRequest request = Requests.newActionRequest("RESOURCE_NAME", "ACTION_ID");
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizeAction(context, request)).willReturn(authorizePromise);

        //When
        chain.handleAction(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterCreateWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        CreateRequest request = Requests.newCreateRequest("", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizeCreate(context, request)).willReturn(authorizePromise);

        //When
        chain.handleCreate(context, request, handler);

        //Then
        verify(target).createInstance(eq(context), Matchers.<CreateRequest>anyObject(), eq(handler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterCreateWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        CreateRequest request = Requests.newCreateRequest("RESOURCE_NAME", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizeCreate(context, request)).willReturn(authorizePromise);

        //When
        chain.handleCreate(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterCreateWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        CreateRequest request = Requests.newCreateRequest("RESOURCE_NAME", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizeCreate(context, request)).willReturn(authorizePromise);

        //When
        chain.handleCreate(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterDeleteWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = Requests.newDeleteRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizeDelete(context, request)).willReturn(authorizePromise);

        //When
        chain.handleDelete(context, request, handler);

        //Then
        verify(target).deleteInstance(eq(context), eq("RESOURCE_NAME"), Matchers.<DeleteRequest>anyObject(),
                eq(handler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterDeleteWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = Requests.newDeleteRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizeDelete(context, request)).willReturn(authorizePromise);

        //When
        chain.handleDelete(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterDeleteWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        DeleteRequest request = Requests.newDeleteRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizeDelete(context, request)).willReturn(authorizePromise);

        //When
        chain.handleDelete(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterPatchWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        PatchRequest request = Requests.newPatchRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizePatch(context, request)).willReturn(authorizePromise);

        //When
        chain.handlePatch(context, request, handler);

        //Then
        verify(target).patchInstance(eq(context), eq("RESOURCE_NAME"), Matchers.<PatchRequest>anyObject(),
                eq(handler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterPatchWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        PatchRequest request = Requests.newPatchRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizePatch(context, request)).willReturn(authorizePromise);

        //When
        chain.handlePatch(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterPatchWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        PatchRequest request = Requests.newPatchRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizePatch(context, request)).willReturn(authorizePromise);

        //When
        chain.handlePatch(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }

    @Test
    public void shouldFilterQueryWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        QueryRequest request = Requests.newQueryRequest("");
        QueryResultHandler handler = mock(QueryResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizeQuery(context, request)).willReturn(authorizePromise);

        //When
        chain.handleQuery(context, request, handler);

        //Then
        verify(target).queryCollection(eq(context), Matchers.<QueryRequest>anyObject(), eq(handler));
    }

    @Test
    public void shouldFilterQueryWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        QueryRequest request = Requests.newQueryRequest("RESOURCE_CONTAINER");
        QueryResultHandler handler = mock(QueryResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizeQuery(context, request)).willReturn(authorizePromise);

        //When
        chain.handleQuery(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @Test
    public void shouldFilterQueryWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        QueryRequest request = Requests.newQueryRequest("RESOURCE_CONTAINER");
        QueryResultHandler handler = mock(QueryResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizeQuery(context, request)).willReturn(authorizePromise);

        //When
        chain.handleQuery(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterReadWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        ReadRequest request = Requests.newReadRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizeRead(context, request)).willReturn(authorizePromise);

        //When
        chain.handleRead(context, request, handler);

        //Then
        verify(target).readInstance(eq(context), eq("RESOURCE_NAME"), Matchers.<ReadRequest>anyObject(),
                eq(handler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterReadWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        ReadRequest request = Requests.newReadRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizeRead(context, request)).willReturn(authorizePromise);

        //When
        chain.handleRead(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterReadWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        ReadRequest request = Requests.newReadRequest("RESOURCE_NAME");
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizeRead(context, request)).willReturn(authorizePromise);

        //When
        chain.handleRead(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterUpdateWhenAuthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = Requests.newUpdateRequest("RESOURCE_NAME", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());

        given(module.authorizeUpdate(context, request)).willReturn(authorizePromise);

        //When
        chain.handleUpdate(context, request, handler);

        //Then
        verify(target).updateInstance(eq(context), eq("RESOURCE_NAME"), Matchers.<UpdateRequest>anyObject(),
                eq(handler));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterUpdateWhenUnauthorized() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[] {module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = Requests.newUpdateRequest("RESOURCE_NAME", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("REASON",
                        json(object(field("DETAIL", "VALUE")))));

        given(module.authorizeUpdate(context, request)).willReturn(authorizePromise);

        //When
        chain.handleUpdate(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception.getCode(), 403);
        assertEquals(exception.getMessage(), "REASON");
        assertTrue(exception.getDetail().isDefined("DETAIL"));
        assertTrue(exception.getDetail().contains("VALUE"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterUpdateWhenAuthorizationFails() {

        //Given
        CollectionResourceProvider target = mock(CollectionResourceProvider.class);
        CrestAuthorizationModule module = mock(CrestAuthorizationModule.class);
        CrestAuthorizationModule[] modules = new CrestAuthorizationModule[]{module};

        FilterChain chain = AuthorizationFilters.createFilter(target, modules);

        ServerContext context = mock(ServerContext.class);
        UpdateRequest request = Requests.newUpdateRequest("RESOURCE_NAME", json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        ResourceException resourceException = ResourceException.getException(ResourceException.INTERNAL_ERROR);
        Promise<AuthorizationResult, ResourceException> authorizePromise =
                Promises.newFailedPromise(resourceException);

        given(module.authorizeUpdate(context, request)).willReturn(authorizePromise);

        //When
        chain.handleUpdate(context, request, handler);

        //Then
        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCaptor.capture());
        ResourceException exception = exceptionCaptor.getValue();
        assertEquals(exception, resourceException);
    }
}
