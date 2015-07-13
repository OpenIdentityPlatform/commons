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

package org.forgerock.audit.events.handlers.impl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CSVAuditEventHandlerTest {

    @Test
    public void testCreatingAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final CreateRequest createRequest = makeCreateRequest();
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        //when
        csvHandler.createInstance(new ServerContext(new RootContext()), createRequest, resultHandler);

        //then
        verify(resultHandler, never()).handleError(any(ResourceException.class));
        verify(resultHandler).handleResult(resourceCaptor.capture());

        final Resource resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testReadingAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        Resource event = createAccessEvent(csvHandler);

        final ReadRequest readRequest = Requests.newReadRequest("access", event.getId());

        final ResultHandler<Resource> readResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> readArgument = ArgumentCaptor.forClass(Resource.class);

        //when
        csvHandler.readInstance(
                new ServerContext(new RootContext()),
                readRequest.getResourceNameObject().tail(1).toString(),
                readRequest,
                readResultHandler);

        //then
        verify(readResultHandler, times(1)).handleResult(readArgument.capture());
        verify(readResultHandler, never()).handleError(any(ResourceException.class));

        final Resource resource = readArgument.getValue();
        assertResourceEquals(resource, event);
    }

    private static void assertResourceEquals(Resource left, Resource right) {
        Map<String, Object> leftAsMap = dropNullEntries(left.getContent()).asMap();
        Map<String, Object> rightAsMap = dropNullEntries(right.getContent()).asMap();
        assertThat(leftAsMap).isEqualTo(rightAsMap);
    }

    private static JsonValue dropNullEntries(JsonValue jsonValue) {
        JsonValue result = jsonValue.clone();

        for(String key : jsonValue.keys()) {
            if (jsonValue.get(key).isNull()) {
                result.remove(key);
            }
        }

        return result;
    }

    @Test
    public void testDeleteAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        csvHandler.deleteInstance(
                new ServerContext(new RootContext()),
                "_id",
                Requests.newDeleteRequest("access"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        csvHandler.patchInstance(
                new ServerContext(new RootContext()),
                "_id",
                Requests.newPatchRequest("access"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testUpdateAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        csvHandler.updateInstance(
                new ServerContext(new RootContext()),
                "_id",
                Requests.newUpdateRequest("access", new JsonValue(new HashMap<String, Object>())),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntryCollection() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final ResultHandler<JsonValue> resultHandler = mockResultHandler(JsonValue.class);
        final ArgumentCaptor<JsonValue> resourceCaptor = ArgumentCaptor.forClass(JsonValue.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        csvHandler.actionCollection(
                new ServerContext(new RootContext()),
                Requests.newActionRequest("access", "action"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntryInstance() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final ResultHandler<JsonValue> resultHandler = mockResultHandler(JsonValue.class);
        final ArgumentCaptor<JsonValue> resourceCaptor = ArgumentCaptor.forClass(JsonValue.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        csvHandler.actionInstance(
                new ServerContext(new RootContext()),
                "_id",
                Requests.newActionRequest("access", "action"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryOnAuditLogEntry() throws Exception{
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final QueryResultHandler queryResultHandler = mock(QueryResultHandler.class);
        final ArgumentCaptor<QueryResult> queryResultCaptor =
                ArgumentCaptor.forClass(QueryResult.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        Resource event = createAccessEvent(csvHandler);

        final QueryRequest queryRequest = Requests.newQueryRequest("access")
                .setQueryFilter(QueryFilter.valueOf("/_id eq \"_id\""));
        //when
        csvHandler.queryCollection(
                new ServerContext(new RootContext()),
                queryRequest,
                queryResultHandler
        );

        //then
        verify(queryResultHandler, never()).handleError(resourceExceptionCaptor.capture());
        verify(queryResultHandler).handleResult(queryResultCaptor.capture());
        verify(queryResultHandler).handleResource(resourceCaptor.capture());

        final Resource resource = resourceCaptor.getValue();
        assertResourceEquals(resource, event);
    }

    private CreateRequest makeCreateRequest() {
        final JsonValue content = json(
                object(
                        field("_id", "_id"),
                        field("timestamp", "timestamp"),
                        field("transactionId", "transactionId-X")
                )
        );
        return Requests.newCreateRequest("access", content);
    }

    @SuppressWarnings("unchecked")
    private static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);

    }

    private Resource createAccessEvent(AuditEventHandler auditEventHandler) {
        final CreateRequest createRequest = makeCreateRequest();
        final ResultHandler<Resource> createResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> createArgument = ArgumentCaptor.forClass(Resource.class);

        auditEventHandler.createInstance(new ServerContext(new RootContext()), createRequest, createResultHandler);

        verify(createResultHandler, never()).handleError(any(ResourceException.class));
        verify(createResultHandler).handleResult(createArgument.capture());

        return createArgument.getValue();
    }

    @Test
    public void testCreateCsvLogEntryWritesToFile() throws Exception {
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);
        final JsonValue content = json(
                object(
                    field("_id", "1"),
                    field("timestamp", "123456"),
                    field("transactionId", "A10000")));
        CreateRequest createRequest = Requests.newCreateRequest("access", content);

        csvHandler.createInstance(
                new ServerContext(new RootContext()), createRequest, mockResultHandler(Resource.class));

        String expectedContent = "\"_id\",\"timestamp\",\"transactionId\"\n" + "\"1\",\"123456\",\"A10000\"";
        assertThat(logDirectory.resolve("access.csv").toFile()).hasContent(expectedContent);
    }

    private CSVAuditEventHandler createAndConfigureHandler(Path tempDirectory) throws Exception {
        CSVAuditEventHandler handler = new CSVAuditEventHandler();
        CSVAuditEventHandlerConfiguration config = new CSVAuditEventHandlerConfiguration();
        config.setLogDirectory(tempDirectory.toString());
        config.setRecordDelimiter("");
        handler.configure(config);
        addEventsMetaData(handler);
        return handler;
    }

    private void addEventsMetaData(CSVAuditEventHandler handler) throws Exception {
        Map<String, JsonValue> events = new LinkedHashMap<>();
        try (final InputStream configStream = getClass().getResourceAsStream("/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        }
        handler.setAuditEventsMetaData(events);
    }

}
