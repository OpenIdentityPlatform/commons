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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandler;
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

@SuppressWarnings({"javadoc", "rawtypes", "unchecked" })
public class AuditServiceTest {

    private static final ObjectMapper mapper;
    private AuditServiceConfiguration config;

    static {
        final JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }

    @BeforeMethod
    public void setUp() {
        try {
            final InputStream configStream = getClass().getResourceAsStream("/audit.json");
            JsonValue jsonConfig = new JsonValue(mapper.readValue(configStream, Map.class));
            config = getConfigFromJson(jsonConfig);
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse audit.json config", e);
        }
    }

    private AuditServiceConfiguration getConfigFromJson(JsonValue jsonConfig) {
        AuditServiceConfiguration config = new AuditServiceConfiguration();
        String key = "useForQueries";
        if (jsonConfig.isDefined(key)) {
            config.setQueryHandlerName(jsonConfig.get(key).asString());
            return config;
        }
        throw new RuntimeException("Json config does not contain expected property: " + key);
    }

    @AfterMethod
    public void tearDown() {
        config = null;
    }

    @Test
    public void testRegisterInjectEventMetaData() throws Exception {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        AuditEventHandler<?> auditEventHandler = mock(AuditEventHandler.class);
        final ArgumentCaptor<Map> auditEventMetaDataCaptor = ArgumentCaptor.forClass(Map.class);

        auditService.register(auditEventHandler, "mock", Collections.singleton("access"));
        verify(auditEventHandler).setAuditEventsMetaData(auditEventMetaDataCaptor.capture());

        Map<String, JsonValue> auditEventMetaData = auditEventMetaDataCaptor.getValue();
        assertThat(auditEventMetaData).containsKey("access");
        JsonValue accessMetaData = auditEventMetaData.get("access");
        assertThat(accessMetaData.isDefined("schema")).isTrue();
    }

    @Test
    public void testRegisterForSomeEvents() throws Exception {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        // No events specified, we want to get all events !
        PassThroughAuditEventHandler auditEventHandler = new PassThroughAuditEventHandler();
        // Only interested about the access events.
        auditService.register(auditEventHandler, "pass-through", Collections.singleton("access"));

        final CreateRequest createRequestAccess = makeCreateRequest("access");
        final ResultHandler<Resource> resultHandlerAccess = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptorAccess = ArgumentCaptor.forClass(Resource.class);

        final CreateRequest createRequestActivity = makeCreateRequest("activity");
        final ResultHandler<Resource> resultHandlerActivity = mockResultHandler(Resource.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequestAccess, resultHandlerAccess);
        auditService.handleCreate(new ServerContext(new RootContext()), createRequestActivity, resultHandlerActivity);

        //then
        verify(resultHandlerAccess, never()).handleError(any(ResourceException.class));
        verify(resultHandlerAccess).handleResult(resourceCaptorAccess.capture());

        Resource resource = resourceCaptorAccess.getValue();
        assertThat(resource != null);
        assertThat(resource.getContent().asMap().equals(createRequestAccess.getContent().asMap()));

        verifyZeroInteractions(resultHandlerActivity);
    }

    @Test
    public void testCreatingAuditLogEntry() throws Exception {
        //given
        final AuditService auditService = new AuditService();
        auditService.configure(config);
        PassThroughAuditEventHandler auditEventHandler = new PassThroughAuditEventHandler();
        auditService.register(auditEventHandler, "pass-through", Collections.singleton("access"));

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
    public void testReadingAuditLogEntry() throws Exception {
        //given
        final AuditService auditService = new AuditService();
        AuditEventHandler<?> queryAuditEventHandler = mock(AuditEventHandler.class, "queryAuditEventHandler");
        auditService.register(queryAuditEventHandler, "query-handler", Collections.singleton("access"));

        AuditEventHandler<?> auditEventHandler = mock(AuditEventHandler.class, "auditEventHandler");
        auditService.register(auditEventHandler, "another-handler", Collections.singleton("access"));
        reset(auditEventHandler, queryAuditEventHandler); // So the verify assertions below can work.

        auditService.configure(getConfigFromJson(json(object(field("useForQueries", "query-handler")))));

        final ReadRequest readRequest = Requests.newReadRequest("access", "1234");
        ResultHandler<Resource> readResultHandler = mockResultHandler(Resource.class, "readResultHandler");
        ServerContext context = new ServerContext(new RootContext());

        //when
        auditService.handleRead(context, readRequest, readResultHandler);

        //then
        verify(queryAuditEventHandler).readInstance(same(context),
                                                    eq("1234"),
                                                    same(readRequest),
                                                    same(readResultHandler));
        verifyZeroInteractions(auditEventHandler);
    }

    @Test
    public void testDeleteAuditLogEntry() throws ResourceException {
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
    public void testPatchAuditLogEntry() throws ResourceException {
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
    public void testUpdateAuditLogEntry() throws ResourceException {
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
    public void testActionOnAuditLogEntry() throws ResourceException {
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
    public void testQueryOnAuditLogEntry() throws ResourceException {
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

    @Test
    public void testAdditionalEventTypes() throws Exception {
        JsonValue additionalEventTypes = json(object(field("foo", "bar")));
        //given
        final AuditService auditService = new AuditService(new JsonValue(null), additionalEventTypes);
        auditService.configure(config);

        PassThroughAuditEventHandler auditEventHandler = new PassThroughAuditEventHandler();
        // Only interested about the foo events.
        auditService.register(auditEventHandler, "pass-through", Collections.singleton("foo"));

        final CreateRequest createRequestAccess = makeCreateRequest("foo");
        final ResultHandler<Resource> resultHandlerAccess = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptorAccess = ArgumentCaptor.forClass(Resource.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequestAccess, resultHandlerAccess);

        //then
        verify(resultHandlerAccess, never()).handleError(any(ResourceException.class));
        verify(resultHandlerAccess).handleResult(resourceCaptorAccess.capture());

        Resource resource = resourceCaptorAccess.getValue();
        assertThat(resource != null);
        assertThat(resource.getContent().asMap().equals(createRequestAccess.getContent().asMap()));
    }

    private CreateRequest makeCreateRequest() {
        return makeCreateRequest("access");
    }

    private CreateRequest makeCreateRequest(String event) {
        final JsonValue content = json(
                object(
                        field("_id", "_id"),
                        field("timestamp", "timestamp"),
                        field("transactionId", "transactionId")
                )
        );
        return Requests.newCreateRequest(event, content);
    }

    private static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);
    }

    private static <T> ResultHandler<T> mockResultHandler(Class<T> type, String name) {
        return mock(ResultHandler.class, name);
    }
}
