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
import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class VersionRouterTest {

    private VersionRouter versionRouter;

    private RequestHandler handlerOne;
    private RequestHandler handlerTwo;
    private RequestHandler handlerThree;

    @BeforeClass
    public void setUpClass() {
        handlerOne = mock(RequestHandler.class);
        handlerTwo = mock(RequestHandler.class);
        handlerThree = mock(RequestHandler.class);
    }

    @BeforeMethod
    public void setUp() {
        versionRouter = new VersionRouter();
        Mockito.reset(handlerOne, handlerTwo, handlerThree);
    }

    @DataProvider(name = "data")
    private Object[][] dataProvider() {
        return new Object[][]{
                {"3.0", true, null},
                {"1.0", false, handlerTwo},
                {"1.1", false, handlerTwo},
                {"1.9", true, null},
                {"2.1", false, handlerThree},
        };
    }

    private void addRoutes() {
        versionRouter.addVersion("1.0", handlerOne);
        versionRouter.addVersion("1.5", handlerTwo);
        versionRouter.addVersion("2.1", handlerThree);
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteCreateVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        CreateRequest request = Requests.newCreateRequest("RESOURCE_NAME", json(object()));

        //When
        Promise<Resource, ResourceException> promise = versionRouter.handleCreate(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handleCreate(Matchers.<ServerContext>anyObject(), Matchers.<CreateRequest>anyObject());
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteReadVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        ReadRequest request = Requests.newReadRequest("RESOURCE_NAME");

        //When
        Promise<Resource, ResourceException> promise = versionRouter.handleRead(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handleRead(Matchers.<ServerContext>anyObject(), Matchers.<ReadRequest>anyObject());
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteUpdateVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        UpdateRequest request = Requests.newUpdateRequest("RESOURCE_NAME", json(object()));

        //When
        Promise<Resource, ResourceException> promise = versionRouter.handleUpdate(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handleUpdate(Matchers.<ServerContext>anyObject(), Matchers.<UpdateRequest>anyObject());
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteDeleteVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        DeleteRequest request = Requests.newDeleteRequest("RESOURCE_NAME");

        //When
        Promise<Resource, ResourceException> promise = versionRouter.handleDelete(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handleDelete(Matchers.<ServerContext>anyObject(), Matchers.<DeleteRequest>anyObject());
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRoutePatchVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        PatchRequest request = Requests.newPatchRequest("RESOURCE_NAME");

        //When
        Promise<Resource, ResourceException> promise = versionRouter.handlePatch(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handlePatch(Matchers.<ServerContext>anyObject(), Matchers.<PatchRequest>anyObject());
        }
    }

    @Test (dataProvider = "data")
    @SuppressWarnings("unchecked")
    public void shouldRouteActionVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        ActionRequest request = Requests.newActionRequest("RESOURCE_NAME", "ACTION_ID").setContent(json(object()));

        //When
        Promise<JsonValue, ResourceException> promise = versionRouter.handleAction(context, request);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handleAction(Matchers.<ServerContext>anyObject(), Matchers.<ActionRequest>anyObject());
        }
    }

    @Test (dataProvider = "data")
    public void shouldRouteQueryVersionRequests(String requestedVersion, boolean expectException,
            RequestHandler provider) {

        //Given
        addRoutes();

        AcceptAPIVersionContext apiVersionContext = new AcceptAPIVersionContext(new RootContext(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(requestedVersion).build());
        ServerContext context = new ServerContext(apiVersionContext);
        QueryRequest request = Requests.newQueryRequest("");
        QueryResourceHandler handler = mock(QueryResourceHandler.class);

        //When
        Promise<QueryResult, ResourceException> promise = versionRouter.handleQuery(context, request, handler);

        //Then
        if (expectException) {
            assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(provider).handleQuery(Matchers.<ServerContext>anyObject(), Matchers.<QueryRequest>anyObject(), eq(handler));
        }
    }
}
