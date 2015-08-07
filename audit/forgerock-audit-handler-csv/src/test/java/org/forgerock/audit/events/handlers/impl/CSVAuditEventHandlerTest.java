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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.context.ServerContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CSVAuditEventHandlerTest {

    @Test
    public void testCreatingAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final CreateRequest createRequest = makeCreateRequest();

        //when
        Promise<Resource, ResourceException> promise =
                csvHandler.createInstance(new ServerContext(new RootContext()), createRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded()
                .withObject()
                .isInstanceOf(Resource.class);

        // TODO-brmiller should use AssertJResourceAssert
        final Resource resource = promise.get();
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

        //when
        Promise<Resource, ResourceException> promise =
                csvHandler.readInstance(
                        new ServerContext(new RootContext()),
                        readRequest.getResourcePathObject().tail(1).toString(),
                        readRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded()
                .withObject()
                .isInstanceOf(Resource.class);

        // TODO-brmiller should use AssertJResourceAssert
        final Resource resource = promise.get();
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

        //when
        Promise<Resource, ResourceException> promise =
                csvHandler.deleteInstance(
                        new ServerContext(new RootContext()),
                        "_id",
                        Requests.newDeleteRequest("access"));

        //then
        AssertJPromiseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<Resource, ResourceException> promise =
                csvHandler.patchInstance(
                        new ServerContext(new RootContext()),
                        "_id",
                        Requests.newPatchRequest("access"));

        //then
        AssertJPromiseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testUpdateAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<Resource, ResourceException> promise =
                csvHandler.updateInstance(
                        new ServerContext(new RootContext()),
                        "_id",
                        Requests.newUpdateRequest("access", new JsonValue(new HashMap<String, Object>())));

        //then
        AssertJPromiseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntryCollection() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<JsonValue, ResourceException> promise =
                csvHandler.actionCollection(
                        new ServerContext(new RootContext()),
                        Requests.newActionRequest("access", "action"));

        //then
        AssertJPromiseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntryInstance() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<JsonValue, ResourceException> promise =
                csvHandler.actionInstance(
                        new ServerContext(new RootContext()),
                        "_id",
                        Requests.newActionRequest("access", "action"));

        //then
        AssertJPromiseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryOnAuditLogEntry() throws Exception{
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final QueryResourceHandler queryResourceHandler = mock(QueryResourceHandler.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        Resource event = createAccessEvent(csvHandler);

        final QueryRequest queryRequest = Requests.newQueryRequest("access")
                .setQueryFilter(QueryFilters.parse("/_id eq \"_id\""));
        //when
        Promise<QueryResult, ResourceException> promise =
                csvHandler.queryCollection(
                        new ServerContext(new RootContext()),
                        queryRequest,
                        queryResourceHandler);

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        verify(queryResourceHandler).handleResource(resourceCaptor.capture());

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

    private Resource createAccessEvent(AuditEventHandler<?> auditEventHandler) throws Exception {
        final CreateRequest createRequest = makeCreateRequest();
        final ResultHandler<Resource> createResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> createArgument = ArgumentCaptor.forClass(Resource.class);

        Promise<Resource, ResourceException> promise =
                auditEventHandler.createInstance(new ServerContext(new RootContext()), createRequest);

        AssertJPromiseAssert.assertThat(promise).succeeded()
                .isInstanceOf(Resource.class);

        // TODO-brmiller should use AssertJResourceAssert
        return promise.get();
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

        csvHandler.createInstance(new ServerContext(new RootContext()), createRequest);

        String expectedContent = "\"_id\",\"timestamp\",\"transactionId\"\n" + "\"1\",\"123456\",\"A10000\"";
        assertThat(logDirectory.resolve("access.csv").toFile()).hasContent(expectedContent);
    }

    private CSVAuditEventHandler createAndConfigureHandler(Path tempDirectory) throws Exception {
        CSVAuditEventHandler handler = new CSVAuditEventHandler();
        CSVAuditEventHandlerConfiguration config = new CSVAuditEventHandlerConfiguration();
        config.setLogDirectory(tempDirectory.toString());
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
