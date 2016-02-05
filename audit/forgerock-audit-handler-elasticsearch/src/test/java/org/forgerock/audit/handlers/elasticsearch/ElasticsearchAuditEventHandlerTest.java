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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.elasticsearch;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticsearchAuditEventHandlerTest {

    public static final int TOTAL_RESULTS = 1;
    public static final String ID = "id";

    @Test
    public void testSuccessfulQuery() throws Exception {

        // given
        final Promise<Response, NeverThrowsException> responsePromise = mock(Promise.class);
        final Client client = createClient(responsePromise);
        final AuditEventHandler handler = createElasticSearchAuditEventHandler(client);
        final QueryRequest queryRequest = Requests.newQueryRequest("access");
        final QueryResourceHandler queryResourceHandler = mock(QueryResourceHandler.class);
        final List<ResourceResponse> responses = new LinkedList<>();
        final JsonValue clientResponsePayload = json(object(
                field("hits", object(
                        field("total", TOTAL_RESULTS),
                        field("hits", array(object(
                                field("_index", "audit"),
                                field("_type", "access"),
                                field("_id", ID),
                                field("_source", object(
                                        field("transactionId", "transactionId"),
                                        field("timestamp", "timestamp")
                                ))
                        )))
                ))
        ));
        final Response clientResponse = createClientResponse(Status.OK, clientResponsePayload);

        queryRequest.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());

        when(queryResourceHandler.handleResource(any(ResourceResponse.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0] instanceof ResourceResponse) {
                    responses.add((ResourceResponse) invocation.getArguments()[0]);
                    return true;
                } else {
                    return false;
                }
            }
        });
        when(responsePromise.get()).thenReturn(clientResponse);

        // when
        Promise<QueryResponse, ResourceException> result =
                handler.queryEvents(mock(Context.class), "access", queryRequest, queryResourceHandler);

        // then
        final QueryResponse queryResponse = result.get();
        org.assertj.core.api.Assertions.assertThat(queryResponse.getPagedResultsCookie()).isEqualTo(null);
        org.assertj.core.api.Assertions.assertThat(queryResponse.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.EXACT);
        org.assertj.core.api.Assertions.assertThat(queryResponse.getTotalPagedResults()).isEqualTo(TOTAL_RESULTS);
        org.assertj.core.api.Assertions.assertThat(responses.size()).isEqualTo(TOTAL_RESULTS);
        final ResourceResponse resourceResponse = responses.get(0);
        org.assertj.core.api.Assertions.assertThat(resourceResponse.getId()).isEqualTo(ID);
        org.assertj.core.api.Assertions.assertThat(resourceResponse.getContent().asMap()).isEqualTo(
                json(object(
                    field("transactionId", "transactionId"),
                    field("timestamp", "timestamp")
                )).asMap()
        );
    }

    @Test
    public void testFailedQuery() throws Exception {

        // given
        final Promise<Response, NeverThrowsException> responsePromise = mock(Promise.class);
        final Client client = createClient(responsePromise);
        final AuditEventHandler handler = createElasticSearchAuditEventHandler(client);
        final QueryRequest queryRequest = Requests.newQueryRequest("access");
        final QueryResourceHandler queryResourceHandler = mock(QueryResourceHandler.class);
        final List<ResourceResponse> responses = new LinkedList<>();
        final Response clientResponse = createClientResponse(Status.INTERNAL_SERVER_ERROR, json(object()));

        queryRequest.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());

        when(responsePromise.get()).thenReturn(clientResponse);

        // when
        Promise<QueryResponse, ResourceException> result =
                handler.queryEvents(mock(Context.class), "access", queryRequest, queryResourceHandler);

        // then
        assertThat(result).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    private AuditEventHandler createElasticSearchAuditEventHandler(final Client client) throws Exception {
        final ElasticsearchAuditEventHandlerConfiguration configuration =
                new ElasticsearchAuditEventHandlerConfiguration();
        final Set<String> topics = new HashSet<>();
        topics.add("authentication");
        topics.add("access");
        topics.add("activity");
        topics.add("config");
        configuration.setTopics(topics);
        // setup config
        final ElasticsearchAuditEventHandler handler =
                new ElasticsearchAuditEventHandler(
                        configuration,
                        getEventTopicsMetaData(),
                        client);
        return handler;
    }

    private Client createClient(final Promise<Response, NeverThrowsException> promise) {
        final Handler handler = mock(Handler.class);
        final Client client = new Client(handler);
        when(handler.handle(any(Context.class), any(Request.class))).thenReturn(promise);
        return client;
    }

    private EventTopicsMetaData getEventTopicsMetaData() throws Exception {
        Map<String, JsonValue> events = new LinkedHashMap<>();
        try (final InputStream configStream = getClass().getResourceAsStream("/org/forgerock/audit/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        }
        return new EventTopicsMetaData(events);
    }

    private Response createClientResponse(final Status status, final JsonValue payload) {
        final Response response = new Response();
        response.setStatus(status);
        response.setEntity(payload);
        return response;
    }
}
