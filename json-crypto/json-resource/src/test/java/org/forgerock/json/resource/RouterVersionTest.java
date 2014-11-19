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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.forgerock.json.fluent.JsonValue;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @since 2.4.0
 */
@SuppressWarnings("javadoc")
public class RouterVersionTest {

    private Router router;

    private RequestHandler usersHandlerOne;
    private RequestHandler usersHandlerTwo;
    private RequestHandler usersHandlerThree;

    private RequestHandler groupsHandlerOne;
    private RequestHandler groupsHandlerTwo;
    private RequestHandler groupsHandlerThree;

    @BeforeClass
    public void setUp() {
        router = new Router();

        usersHandlerOne = mock(RequestHandler.class);
        usersHandlerTwo = mock(RequestHandler.class);
        usersHandlerThree = mock(RequestHandler.class);

        groupsHandlerOne = mock(RequestHandler.class);
        groupsHandlerTwo = mock(RequestHandler.class);
        groupsHandlerThree = mock(RequestHandler.class);

        router.addRoute(RoutingMode.STARTS_WITH, "/users")
                .addVersion("1.0", usersHandlerOne)
                .addVersion("1.5", usersHandlerTwo)
                .addVersion("2.1", usersHandlerThree);

        router.addRoute(RoutingMode.STARTS_WITH, "/groups")
                .addVersion("1.0", groupsHandlerOne)
                .addVersion("1.5", groupsHandlerTwo)
                .addVersion("2.1", groupsHandlerThree);
    }

    @DataProvider(name = "data")
    private Object[][] dataProvider() {
        return new Object[][]{
                {"users", "3.0", true, null},
                {"groups", "1.0", false, groupsHandlerTwo},
                {"users", "1.1", false, usersHandlerTwo},
                {"groups", "1.9", true, null},
                {"users", "2.1", false, usersHandlerThree},
                {"groups", "2.1", false, groupsHandlerThree},
        };
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteCreateVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        CreateRequest request = Requests.newCreateRequest(resource, json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        router.handleCreate(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleCreate(Matchers.<ServerContext>anyObject(), Matchers.<CreateRequest>anyObject(), eq(handler));
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteReadVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        ReadRequest request = Requests.newReadRequest(resource);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        router.handleRead(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleRead(Matchers.<ServerContext>anyObject(), Matchers.<ReadRequest>anyObject(),
                    eq(handler));
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteUpdateVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        UpdateRequest request = Requests.newUpdateRequest(resource, json(object()));
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        router.handleUpdate(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleUpdate(Matchers.<ServerContext>anyObject(), Matchers.<UpdateRequest>anyObject(),
                    eq(handler));
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteDeleteVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        DeleteRequest request = Requests.newDeleteRequest(resource);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        router.handleDelete(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleDelete(Matchers.<ServerContext>anyObject(), Matchers.<DeleteRequest>anyObject(),
                    eq(handler));
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRoutePatchVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        PatchRequest request = Requests.newPatchRequest(resource);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        router.handlePatch(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handlePatch(Matchers.<ServerContext>anyObject(), Matchers.<PatchRequest>anyObject(),
                    eq(handler));
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteActionVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        ActionRequest request = Requests.newActionRequest(resource, "ACTION_ID").setContent(json(object()));
        ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        //When
        router.handleAction(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleAction(Matchers.<ServerContext>anyObject(), Matchers.<ActionRequest>anyObject(),
                    eq(handler));
        }
    }

    @Test (dataProvider = "data")
    public void shouldRouteQueryVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        QueryRequest request = Requests.newQueryRequest(resource);
        QueryResultHandler handler = mock(QueryResultHandler.class);

        //When
        router.handleQuery(context, request, handler);

        //Then
        if (expectException) {
            ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
            verify(handler).handleError(exceptionCaptor.capture());
            assertThat(exceptionCaptor.getValue()).isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleQuery(Matchers.<ServerContext>anyObject(), Matchers.<QueryRequest>anyObject(),
                    eq(handler));
        }
    }
}
