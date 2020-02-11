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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.json.resource.http.HttpUtils.ETAG_ANY;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_NONE_MATCH;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.session.Session;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.http.session.SessionContext;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentApiVersionHeader;
import org.forgerock.http.header.WarningHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.DefaultVersionBehaviour;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.RouteMatchers;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RoutingTest {

    private Handler handler;
    private ResourceApiVersionBehaviourManager apiVersionBehaviourManager;

    @BeforeMethod
    public void setup() {
        apiVersionBehaviourManager = RouteMatchers.newResourceApiVersionBehaviourManager();
        handler = createHandler();
    }

    @Test(dataProvider = "requestData")
    public void shouldRouteToCrestResources(String requestedResourceApiVersion,
            DefaultVersionBehaviour defaultVersionBehaviour, String expectedContentResourceApiVersion,
            boolean isWarningExcepted, Request request) {

        //Given
        Context context = mockContext();
        if (requestedResourceApiVersion != null) {
            request.getHeaders().put(AcceptApiVersionHeader.valueOf("resource=" + requestedResourceApiVersion));
        }

        apiVersionBehaviourManager.setDefaultVersionBehaviour(defaultVersionBehaviour);

        //When
        Response response = handler.handle(context, request).getOrThrowUninterruptibly();

        //Then
        if (expectedContentResourceApiVersion != null) {
            assertThat(response.getHeaders().getFirst(ContentApiVersionHeader.NAME)).isEqualTo("protocol=2.1,resource="
                    + expectedContentResourceApiVersion);
        } else {
            assertThat(response.getHeaders().getFirst(ContentApiVersionHeader.NAME)).isNull();
        }
        if (isWarningExcepted) {
            assertThat(response.getHeaders().getFirst(WarningHeader.NAME)).isNotNull();
        } else {
            assertThat(response.getHeaders().getFirst(WarningHeader.NAME)).isNull();
        }
    }

    @DataProvider
    private Object[][] requestData() {
        List<Object[]> requestData = new ArrayList<>();
        requestData.addAll(createRequestData());
        requestData.addAll(createViaPutRequestData());
        requestData.addAll(readRequestData());
        requestData.addAll(updateRequestData());
        requestData.addAll(deleteRequestData());
        requestData.addAll(patchRequestData());
        requestData.addAll(actionRequestData());
        requestData.addAll(queryRequestData());
        requestData.addAll(queryRequestReturningResourcesData());
        return requestData.toArray(new Object[][]{});
    }

    private List<Object[]> createRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("POST")
                    .setUri(URI.create("json/users?_action=create"))
                    .setEntity(new HashMap<>());
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> createViaPutRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("PUT")
                    .setUri(URI.create("json/users/demo"))
                    .setEntity(new HashMap<>());
            request.getHeaders().put(HEADER_IF_NONE_MATCH, ETAG_ANY);
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> readRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("GET")
                    .setUri(URI.create("json/users/USER_ID"));
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> updateRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("PUT")
                    .setUri(URI.create("json/users/USER_ID"))
                    .setEntity(new HashMap<>());
            request.getHeaders().put("If-Match", "\"*\"");
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> deleteRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        Request request = new Request()
                .setMethod("DELETE")
                .setUri(URI.create("json/users/USER_ID"));
        for (Object[] data : data()) {
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> patchRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("PATCH")
                    .setUri(URI.create("json/users/USER_ID"))
                    .setEntity(new ArrayList<>());
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> actionRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("POST")
                    .setUri(URI.create("json/users/USER_ID?_action=ACTION"))
                    .setEntity(new ArrayList<>());
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> queryRequestData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("GET")
                    .setUri(URI.create("json/users?_queryFilter=true"))
                    .setEntity(new ArrayList<>());
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> queryRequestReturningResourcesData() {
        List<Object[]> requestData = new ArrayList<>();
        for (Object[] data : data()) {
            Request request = new Request()
                    .setMethod("GET")
                    .setUri(URI.create("json/users?_queryFilter=false"))
                    .setEntity(new ArrayList<>());
            Object[] newData = Arrays.copyOf(data, data.length + 1);
            newData[data.length] = request;
            requestData.add(newData);
        }
        return requestData;
    }

    private List<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {null, null, "2.0", true},
            {null, DefaultVersionBehaviour.NONE, null, true},
            {null, DefaultVersionBehaviour.LATEST, "2.0", true},
            {null, DefaultVersionBehaviour.OLDEST, "1.0", true},
            {"1.0", null, "1.1", false},
            {"1.0", DefaultVersionBehaviour.NONE, "1.1", false},
            {"1.0", DefaultVersionBehaviour.LATEST, "1.1", false},
            {"1.0", DefaultVersionBehaviour.OLDEST, "1.1", false},
            {"1.1", null, "1.1", false},
            {"1.1", DefaultVersionBehaviour.NONE, "1.1", false},
            {"1.1", DefaultVersionBehaviour.LATEST, "1.1", false},
            {"1.1", DefaultVersionBehaviour.OLDEST, "1.1", false},
            {"2.0", null, "2.0", false},
            {"2.0", DefaultVersionBehaviour.NONE, "2.0", false},
            {"2.0", DefaultVersionBehaviour.LATEST, "2.0", false},
            {"2.0", DefaultVersionBehaviour.OLDEST, "2.0", false},
        });
    }

    private Handler createHandler() {
        org.forgerock.http.routing.Router rootRouter = new org.forgerock.http.routing.Router();
        rootRouter.addRoute(org.forgerock.http.routing.RouteMatchers.requestUriMatcher(STARTS_WITH, "json"),
                createCrestHandler());
        return rootRouter;
    }

    private Handler createCrestHandler() {
        Router versionRouter = new Router();

        versionRouter.addRoute(version(1), mockCollectionResourceProvider());
        versionRouter.addRoute(version(1, 1), mockCollectionResourceProvider());
        versionRouter.addRoute(version(2), mockCollectionResourceProvider());

        Router router = new Router();
        router.addRoute(STARTS_WITH, uriTemplate("users"), versionRouter);

        return CrestHttp.newHttpHandler(new FilterChain(router,
                org.forgerock.json.resource.RouteMatchers.resourceApiVersionContextFilter(apiVersionBehaviourManager)));
    }

    private CollectionResourceProvider mockCollectionResourceProvider() {
        Promise<ResourceResponse, ResourceException> resourcePromise =
                Promises.newResultPromise(newResource());
        Promise<ActionResponse, ResourceException> jsonValuePromise =
                Promises.newResultPromise(Responses.newActionResponse(mock(JsonValue.class)));
        final Promise<QueryResponse, ResourceException> queryPromise =
                Promises.newResultPromise(Responses.newQueryResponse());

        CollectionResourceProvider provider = mock(CollectionResourceProvider.class);
        given(provider.createInstance(any(Context.class), any(CreateRequest.class))).willReturn(resourcePromise);
        given(provider.readInstance(any(Context.class), anyString(), any(ReadRequest.class)))
                .willReturn(resourcePromise);
        given(provider.updateInstance(any(Context.class), anyString(), any(UpdateRequest.class)))
                .willReturn(resourcePromise);
        given(provider.deleteInstance(any(Context.class), anyString(), any(DeleteRequest.class)))
                .willReturn(resourcePromise);
        given(provider.patchInstance(any(Context.class), anyString(), any(PatchRequest.class)))
                .willReturn(resourcePromise);
        given(provider.actionCollection(any(Context.class), any(ActionRequest.class)))
                .willReturn(jsonValuePromise);
        given(provider.actionInstance(any(Context.class), anyString(), any(ActionRequest.class)))
                .willReturn(jsonValuePromise);
        doAnswer(new Answer<Promise<QueryResponse, ResourceException>>() {
            @Override
            public Promise<QueryResponse, ResourceException> answer(InvocationOnMock invocationOnMock) {
                QueryRequest request = (QueryRequest) invocationOnMock.getArguments()[1];
                if (!"true".equals(request.getQueryFilter().toString())) {
                    QueryResourceHandler resourceHandler = (QueryResourceHandler) invocationOnMock.getArguments()[2];
                    resourceHandler.handleResource(newResource());
                }
                return queryPromise;
            }
        }).when(provider)
                .queryCollection(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class));
        return provider;
    }

    private ResourceResponse newResource() {
        return newResourceResponse("", "", json(object()));
    }

    private Context mockContext() {
        return new AttributesContext(new SessionContext(mock(Context.class), mock(Session.class)));
    }
}
