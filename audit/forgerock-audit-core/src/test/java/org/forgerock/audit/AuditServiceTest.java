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

package org.forgerock.audit;

import static org.fest.assertions.api.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotSupportedException;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuditServiceTest {

    private static final ObjectMapper MAPPER;
    private JsonValue config;
    File file = null;

    static {
        final JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        MAPPER = new ObjectMapper(jsonFactory);
    }

    @BeforeMethod
    public void setUp() {
        try {
            final InputStream configStream =
                    AuditServiceTest.class.getResourceAsStream("/audit.json");
            config = new JsonValue(MAPPER.readValue(configStream, Map.class));
            file = File.createTempFile("access", "csv");
            config.get("eventHandlers").get("csv").get("config").put("location", file.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse audit.json config", e);
        }
    }

    @AfterMethod
    public void tearDown() {
        config = null;
        file.delete();

    }

    @Test
    public void testCreatingAuditLogEntry() throws ResourceException {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);

        final CreateRequest createRequest = makeCreateRequest();
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequest, resultHandler);

        //then
        verify(resultHandler, never()).handleError(any(ResourceException.class));
        verify(resultHandler).handleResult(resourceCaptor.capture());

        final Resource resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testReadingAuditLogEntry() throws ResourceException {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);

        final CreateRequest createRequest = makeCreateRequest();
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        auditService.handleCreate(new ServerContext(new RootContext()), createRequest, resultHandler);

        verify(resultHandler, never()).handleError(any(ResourceException.class));
        verify(resultHandler).handleResult(resourceCaptor.capture());

        final ReadRequest readRequest = Requests.newReadRequest("access", resourceCaptor.getValue().getId());

        //when
        auditService.handleRead(new ServerContext(new RootContext()), readRequest, resultHandler);

        //then
        verify(resultHandler, times(1)).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(any(ResourceException.class));

        final Resource resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testDeleteAuditLogEntry() throws ResourceException{
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleDelete(
                new ServerContext(new RootContext()),
                Requests.newDeleteRequest("_id"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchAuditLogEntry() throws ResourceException{
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handlePatch(
                new ServerContext(new RootContext()),
                Requests.newPatchRequest("_id"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testUpdateAuditLogEntry() throws ResourceException{
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleUpdate(
                new ServerContext(new RootContext()),
                Requests.newUpdateRequest("_id", new JsonValue(new HashMap<String, Object>())),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionOnAuditLogEntry() throws ResourceException{
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        final ResultHandler<JsonValue> resultHandler = mockResultHandler(JsonValue.class);
        final ArgumentCaptor<JsonValue> resourceCaptor = ArgumentCaptor.forClass(JsonValue.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleAction(
                new ServerContext(new RootContext()),
                Requests.newActionRequest("_id", "action"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryOnAuditLogEntry() throws ResourceException{
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        final QueryResultHandler resultHandler = mock(QueryResultHandler.class);
        final ArgumentCaptor<QueryResult> resourceCaptor = ArgumentCaptor.forClass(QueryResult.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleQuery(
                new ServerContext(new RootContext()),
                Requests.newQueryRequest("_id"),
                resultHandler
        );

        //then
        verify(resultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceConfigure() throws ResourceException{
        //given
        final AuditService auditService = new AuditService();

        //when
        auditService.configure(config);

        //then
        assertThat(auditService.getConfig().asMap()).isEqualTo(config.asMap());
    }

    @Test
    public void testMandatoryFieldTransactionId() throws ResourceException {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);

        // No transactionId in the JSON content
        final JsonValue content = json(object(field("_id", "_id"),
                                              field("timestamp", "timestamp")));

        final CreateRequest createRequest = Requests.newCreateRequest("access", content);
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);

        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequest, resultHandler);

        //then
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());
        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(BadRequestException.class);
    }

    @Test
    public void testMandatoryFieldTimestamp() throws ResourceException {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);

        // No timestamp in the JSON content
        final JsonValue content = json(object(field("_id", "_id"),
                                              field("transactionId", "transactionId")));

        final CreateRequest createRequest = Requests.newCreateRequest("access", content);
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);

        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequest, resultHandler);

        //then
        verify(resultHandler).handleError(resourceExceptionCaptor.capture());
        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(BadRequestException.class);
    }

    private CreateRequest makeCreateRequest() {
        final JsonValue content = json(
                object(
                        field("_id", "_id"),
                        field("timestamp", "timestamp"),
                        field("transactionId", "transactionId")
                )
        );
        return Requests.newCreateRequest("access", content);
    }

    @SuppressWarnings("unchecked")
    private static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);

    }
 }
