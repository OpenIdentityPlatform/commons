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

package org.forgerock.json.resource;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.forgerock.http.RootContext;
import org.forgerock.http.RoutingMode;
import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.VersionSelector.DefaultVersionBehaviour;
import org.forgerock.util.promise.Promise;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @since 2.4.0
 */
@SuppressWarnings("javadoc")
public class VersionedRouterTest {

    private UriRouter router;
    private VersionRouter versionRouter1;
    private VersionRouter versionRouter2;

    private RequestHandler usersHandlerOne;
    private RequestHandler usersHandlerTwo;
    private RequestHandler usersHandlerThree;

    private RequestHandler groupsHandlerOne;
    private RequestHandler groupsHandlerTwo;
    private RequestHandler groupsHandlerThree;

    @BeforeClass
    public void setUp() {
        router = new UriRouter();

        usersHandlerOne = mock(RequestHandler.class);
        usersHandlerTwo = mock(RequestHandler.class);
        usersHandlerThree = mock(RequestHandler.class);

        groupsHandlerOne = mock(RequestHandler.class);
        groupsHandlerTwo = mock(RequestHandler.class);
        groupsHandlerThree = mock(RequestHandler.class);

        versionRouter1 = new VersionRouter();
        versionRouter2 = new VersionRouter();
        router.addRoute(RoutingMode.EQUALS, "/users", versionRouter1
                .addVersion("1.0", usersHandlerOne)
                .addVersion("1.5", usersHandlerTwo)
                .addVersion("2.1", usersHandlerThree));

        router.addRoute(RoutingMode.EQUALS, "/groups", versionRouter2
                .addVersion("1.0", groupsHandlerOne)
                .addVersion("1.5", groupsHandlerTwo)
                .addVersion("2.1", groupsHandlerThree));
    }

    @BeforeMethod
    public void setupMethod() {
        Mockito.reset(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne, groupsHandlerTwo,
                groupsHandlerThree);
    }

    @DataProvider(name = "dataWithVersionHeader")
    private Object[][] dataWithVersionHeader() {
        return new Object[][]{
                {"users", "3.0", true, null},
                {"groups", "1.0", false, groupsHandlerTwo},
                {"users", "1.1", false, usersHandlerTwo},
                {"groups", "1.9", true, null},
                {"users", "2.1", false, usersHandlerThree},
                {"groups", "2.1", false, groupsHandlerThree},
        };
    }

    @Test (dataProvider = "dataWithVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteCreateVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        CreateRequest request = Requests.newCreateRequest(resource, json(object()));

        //When
        Promise<Resource, ResourceException> promise = router.handleCreate(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleCreate(Matchers.<ServerContext>anyObject(), Matchers.<CreateRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteReadVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        ReadRequest request = Requests.newReadRequest(resource);

        //When
        Promise<Resource, ResourceException> promise = router.handleRead(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleRead(Matchers.<ServerContext>anyObject(), Matchers.<ReadRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteUpdateVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        UpdateRequest request = Requests.newUpdateRequest(resource, json(object()));

        //When
        Promise<Resource, ResourceException> promise = router.handleUpdate(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleUpdate(Matchers.<ServerContext>anyObject(), Matchers.<UpdateRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteDeleteVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        DeleteRequest request = Requests.newDeleteRequest(resource);

        //When
        Promise<Resource, ResourceException> promise = router.handleDelete(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleDelete(Matchers.<ServerContext>anyObject(), Matchers.<DeleteRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRoutePatchVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        PatchRequest request = Requests.newPatchRequest(resource);

        //When
        Promise<Resource, ResourceException> promise = router.handlePatch(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handlePatch(Matchers.<ServerContext>anyObject(), Matchers.<PatchRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteActionVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        ActionRequest request = Requests.newActionRequest(resource, "ACTION_ID").setContent(json(object()));

        //When
        Promise<JsonValue, ResourceException> promise = router.handleAction(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleAction(Matchers.<ServerContext>anyObject(), Matchers.<ActionRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithVersionHeader")
    public void shouldRouteQueryVersionRequests(String resource, String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0")
                        .withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        QueryRequest request = Requests.newQueryRequest(resource);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);

        //When
        Promise<QueryResult, ResourceException> promise = router.handleQuery(context, request, handler);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleQuery(Matchers.<ServerContext>anyObject(), Matchers.<QueryRequest>anyObject(), eq(handler));
        }
    }

    @DataProvider(name = "dataWithoutVersionHeader")
    private Object[][] dataWithoutVersionHeader() {
        return new Object[][]{
                {"users", DefaultVersionBehaviour.LATEST, false, usersHandlerThree},
                {"groups", DefaultVersionBehaviour.LATEST, false, groupsHandlerThree},
                {"users", DefaultVersionBehaviour.OLDEST, false, usersHandlerOne},
                {"groups", DefaultVersionBehaviour.OLDEST, false, groupsHandlerOne},
                {"users", DefaultVersionBehaviour.NONE, true, null},
                {"groups", DefaultVersionBehaviour.NONE, true, null},
        };
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteCreateRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        CreateRequest request = Requests.newCreateRequest(resource, json(object()));
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<Resource, ResourceException> promise = router.handleCreate(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleCreate(Matchers.<ServerContext>anyObject(), Matchers.<CreateRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteReadRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        ReadRequest request = Requests.newReadRequest(resource);
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<Resource, ResourceException> promise = router.handleRead(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleRead(Matchers.<ServerContext>anyObject(), Matchers.<ReadRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteUpdateRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        UpdateRequest request = Requests.newUpdateRequest(resource, json(object()));
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<Resource, ResourceException> promise = router.handleUpdate(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleUpdate(Matchers.<ServerContext>anyObject(), Matchers.<UpdateRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteDeleteRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        DeleteRequest request = Requests.newDeleteRequest(resource);
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<Resource, ResourceException> promise = router.handleDelete(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleDelete(Matchers.<ServerContext>anyObject(), Matchers.<DeleteRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRoutePatchRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        PatchRequest request = Requests.newPatchRequest(resource);
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<Resource, ResourceException> promise = router.handlePatch(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handlePatch(Matchers.<ServerContext>anyObject(), Matchers.<PatchRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    @SuppressWarnings("unchecked")
    public void shouldRouteActionRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        ActionRequest request = Requests.newActionRequest(resource, "ACTION_ID").setContent(json(object()));
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<JsonValue, ResourceException> promise = router.handleAction(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleAction(Matchers.<ServerContext>anyObject(), Matchers.<ActionRequest>anyObject());
        }
    }

    @Test (dataProvider = "dataWithoutVersionHeader")
    public void shouldRouteQueryRequestsUsingDefaultVersionBehaviour(String resource,
            DefaultVersionBehaviour versionBehaviour, boolean expectException, RequestHandler provider) {

        //Given
        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").build());
        ServerContext context = new ServerContext(apiVersionContext);
        QueryRequest request = Requests.newQueryRequest(resource);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        setDefaultVersionBehaviour(versionRouter1, versionBehaviour);
        setDefaultVersionBehaviour(versionRouter2, versionBehaviour);

        //When
        Promise<QueryResult, ResourceException> promise = router.handleQuery(context, request, handler);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
            verifyZeroInteractions(usersHandlerOne, usersHandlerTwo, usersHandlerThree, groupsHandlerOne,
                    groupsHandlerTwo, groupsHandlerThree);
        } else {
            verify(provider).handleQuery(Matchers.<ServerContext>anyObject(), Matchers.<QueryRequest>anyObject(),
                    eq(handler));
        }
    }

    private static void setDefaultVersionBehaviour(VersionRouter router, DefaultVersionBehaviour versionBehaviour) {
        switch (versionBehaviour) {
            case LATEST:
                router.defaultToLatest();
                break;
            case OLDEST:
                router.defaultToOldest();
                break;
            case NONE:
                router.noDefault();
                break;
        }
    }
}
