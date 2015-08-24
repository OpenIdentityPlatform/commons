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

package org.forgerock.audit.events.handlers.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
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

        //when
        Promise<ResourceResponse, ResourceException> promise =
                csvHandler.createInstance(new RootContext(), createRequest);

        //then
        assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testReadingAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        ResourceResponse event = createAccessEvent(csvHandler);

        final ReadRequest readRequest = Requests.newReadRequest("access", event.getId());

        //when
        Promise<ResourceResponse, ResourceException> promise =
                csvHandler.readInstance(
                        new RootContext(),
                        readRequest.getResourcePathObject().tail(1).toString(),
                        readRequest);

        //then
        assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        assertResourceEquals(resource, event);
    }

    private static void assertResourceEquals(ResourceResponse left, ResourceResponse right) {
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
        Promise<ResourceResponse, ResourceException> promise =
                csvHandler.deleteInstance(
                        new RootContext(),
                        "_id",
                        Requests.newDeleteRequest("access"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<ResourceResponse, ResourceException> promise =
                csvHandler.patchInstance(
                        new RootContext(),
                        "_id",
                        Requests.newPatchRequest("access"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testUpdateAuditLogEntry() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<ResourceResponse, ResourceException> promise =
                csvHandler.updateInstance(
                        new RootContext(),
                        "_id",
                        Requests.newUpdateRequest("access", new JsonValue(new HashMap<String, Object>())));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntryCollection() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<ActionResponse, ResourceException> promise =
                csvHandler.actionCollection(
                        new RootContext(),
                        Requests.newActionRequest("access", "action"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntryInstance() throws Exception {
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        //when
        Promise<ActionResponse, ResourceException> promise =
                csvHandler.actionInstance(
                        new RootContext(),
                        "_id",
                        Requests.newActionRequest("access", "action"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryOnAuditLogEntry() throws Exception{
        //given
        Path logDirectory = Files.createTempDirectory("CSVAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        CSVAuditEventHandler csvHandler = createAndConfigureHandler(logDirectory);

        final QueryResourceHandler queryResourceHandler = mock(QueryResourceHandler.class);
        final ArgumentCaptor<ResourceResponse> resourceCaptor = ArgumentCaptor.forClass(ResourceResponse.class);

        ResourceResponse event = createAccessEvent(csvHandler);

        final QueryRequest queryRequest = Requests.newQueryRequest("access")
                .setQueryFilter(QueryFilters.parse("/_id eq \"_id\""));
        //when
        Promise<QueryResponse, ResourceException> promise =
                csvHandler.queryCollection(
                        new RootContext(),
                        queryRequest,
                        queryResourceHandler);

        //then
        assertThat(promise).succeeded();
        verify(queryResourceHandler).handleResource(resourceCaptor.capture());

        final ResourceResponse resource = resourceCaptor.getValue();
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

    private ResourceResponse createAccessEvent(AuditEventHandler<?> auditEventHandler) throws Exception {
        final CreateRequest createRequest = makeCreateRequest();
        final ResultHandler<ResourceResponse> createResultHandler = mockResultHandler(ResourceResponse.class);
        final ArgumentCaptor<ResourceResponse> createArgument = ArgumentCaptor.forClass(ResourceResponse.class);

        Promise<ResourceResponse, ResourceException> promise =
                auditEventHandler.createInstance(new RootContext(), createRequest);

        assertThat(promise)
                .succeeded()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
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

        csvHandler.createInstance(new RootContext(), createRequest);

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
