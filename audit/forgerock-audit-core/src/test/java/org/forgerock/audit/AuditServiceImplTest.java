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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.events.EventTopicsMetaDataBuilder.coreTopicSchemas;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandler;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandlerConfiguration;
import org.forgerock.audit.filter.FilterPolicy;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings({"javadoc", "rawtypes", "unchecked" })
public class AuditServiceImplTest {

    private static final String QUERY_HANDLER_NAME = "pass-through";
    public static final String FILTERED_FIELD = "filteredField";
    public static final String FILTERED_VALUE = "filteredValue";

    private EventTopicsMetaData eventTopicsMetaData;

    @BeforeMethod
    protected void setUp() throws Exception {
        eventTopicsMetaData = coreTopicSchemas().build();
    }

    @Test
    public void shouldDelegateCreateRequestToRegisteredHandler() throws Exception {
        final String topic = "access";
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final PassThroughAuditEventHandlerConfiguration configuration = new PassThroughAuditEventHandlerConfiguration();
        configuration.setName(QUERY_HANDLER_NAME);
        configuration.setTopics(Collections.singleton(topic));
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic))
                .withAuditEventHandler(clazz, configuration)
                .build();
        auditService.startup();

        final CreateRequest createRequest = makeCreateRequest();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing("access")).isTrue();
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
    public void shouldIgnoreCreateRequestIfAuditEventTopicNotMappedToHandler() throws Exception {
        //given
        final String topic = "activity";
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        final CreateRequest createRequest = makeCreateRequest(topic);

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing(topic)).isFalse();
        assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(Collections.emptyMap());
    }

    @Test
    public void shouldFailCreateRequestIfAuditEventTopicIsNotKnown() throws ServiceUnavailableException {
        //given
        final String topic = "unknownTopic";
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        final CreateRequest createRequest = makeCreateRequest(topic);

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing("unknownTopic")).isFalse();
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(ResourceException.class);
    }

    @Test
    public void shouldFailCreateRequestIfHandlerConfiguredForQueriesThrowsException() throws Exception {
        //given
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME, "access");
        final AuditEventHandler queryHandler = mock(AuditEventHandler.class);
        given(queryHandler.isEnabled()).willReturn(true);
        given(queryHandler.getName()).willReturn(QUERY_HANDLER_NAME);
        given(queryHandler.getHandledTopics()).willReturn(new HashSet<>(Arrays.asList("access")));
        final PassThroughAuditEventHandler otherHandler = spyPassThroughAuditEventHandler("otherHandler");
        final Set<AuditEventHandler> handlers = asSet(queryHandler, otherHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        final Promise<ResourceResponse, ResourceException> exception = new InternalServerErrorException().asPromise();
        given(queryHandler.publishEvent(any(Context.class), eq("access"), any(JsonValue.class))).willReturn(exception);
        final CreateRequest createRequest = makeCreateRequest();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        verify(queryHandler, times(1)).publishEvent(any(Context.class), eq("access"), any(JsonValue.class));
        verify(otherHandler, times(1)).publishEvent(any(Context.class), eq("access"), any(JsonValue.class));
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void shouldIgnoreCreateRequestExceptionsNotComingFromHandlerConfiguredForQueries() throws Exception {
        //given
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME, "access");
        final PassThroughAuditEventHandler queryHandler = spyPassThroughAuditEventHandler(QUERY_HANDLER_NAME);
        final AuditEventHandler otherHandler = mock(AuditEventHandler.class);
        given(otherHandler.isEnabled()).willReturn(true);
        given(otherHandler.getName()).willReturn("otherHandler");
        given(otherHandler.getHandledTopics()).willReturn(new HashSet<>(Arrays.asList("access")));
        final Set<AuditEventHandler> handlers = asSet(queryHandler, otherHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        final Promise<ResourceResponse, ResourceException> exception = new InternalServerErrorException().asPromise();
        given(otherHandler.publishEvent(any(Context.class), eq("access"), any(JsonValue.class))).willReturn(exception);
        final CreateRequest createRequest = makeCreateRequest();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        verify(queryHandler, times(1)).publishEvent(any(Context.class), eq("access"), any(JsonValue.class));
        verify(otherHandler, times(1)).publishEvent(any(Context.class), eq("access"), any(JsonValue.class));
        final ResourceResponse resource = promise.get();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void shouldDelegateReadRequestToConfiguredHandlerForQueries() throws Exception {
        //given
        final String topic = "access";
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic);
        final PassThroughAuditEventHandler queryAuditEventHandler = spyPassThroughAuditEventHandler(QUERY_HANDLER_NAME);
        final PassThroughAuditEventHandler otherAuditEventHandler = spyPassThroughAuditEventHandler("otherHandler");
        final Set<AuditEventHandler> handlers = asSet(queryAuditEventHandler, otherAuditEventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        final Promise<ResourceResponse, ResourceException> dummyResponse =
                newResourceResponse(null, null, json(object())).asPromise();
        reset(otherAuditEventHandler); // So verifyZeroInteractions will work
        given(queryAuditEventHandler.readEvent(any(Context.class), eq(topic), eq("1234"))).willReturn(dummyResponse);

        final ReadRequest readRequest = Requests.newReadRequest(topic, "1234");

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleRead(new RootContext(), readRequest);

        //then
        assertThat(promise).isSameAs(dummyResponse);
        verifyZeroInteractions(otherAuditEventHandler);
    }

    @Test
    public void shouldNotSupportDeleteOfAuditEvents() throws ResourceException {
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, null))
                .build();
        //when
        Promise<ResourceResponse, ResourceException> promise =
                auditService.handleDelete(
                        new RootContext(),
                        Requests.newDeleteRequest("_id"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void shouldNotSupportPatchOfAuditEvents() throws ResourceException {
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, null))
                .build();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handlePatch(
                        new RootContext(),
                        Requests.newPatchRequest("_id"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void shouldNotSupportUpdateOfAuditEvents() throws ResourceException {
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, null))
                .build();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleUpdate(
                        new RootContext(),
                        Requests.newUpdateRequest("_id", new JsonValue(new HashMap<String, Object>())));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void shouldNotSupportActions() throws ResourceException {
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, null))
                .build();

        //when
        final Promise<ActionResponse, ResourceException> promise =
                auditService.handleAction(
                        new RootContext(),
                        Requests.newActionRequest("access", "unknownAction"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldDelegateQueryRequestToConfiguredHandlerForQueries() throws Exception {
        final String topic = "access";
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic);
        final PassThroughAuditEventHandler eventHandler = spyPassThroughAuditEventHandler(QUERY_HANDLER_NAME);
        final Set<AuditEventHandler> handlers = asSet(eventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        final Promise<QueryResponse, ResourceException> queryResponsePromise = newQueryResponse().asPromise();
        when(eventHandler.queryEvents(
                any(Context.class), any(String.class), any(QueryRequest.class), any(QueryResourceHandler.class)))
            .thenReturn(queryResponsePromise);

        //when
        final Promise<QueryResponse, ResourceException> promise = auditService.handleQuery(
                new RootContext(),
                newQueryRequest(topic),
                mock(QueryResourceHandler.class));

        //then
        verify(eventHandler).queryEvents(
                any(Context.class), any(String.class), any(QueryRequest.class), any(QueryResourceHandler.class));
        assertThat(promise).isSameAs(queryResponsePromise);
    }

    @Test
    public void shouldFailCreateRequestIfAuditEventIsMissingTransactionId() throws ResourceException {
        final String topic = "access";
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic))
                .build();
        auditService.startup();

        // No transactionId in the JSON content
        final JsonValue content = json(object(field("_id", "_id"),
                field("timestamp", "timestamp")));

        final CreateRequest createRequest = newCreateRequest(topic, content);

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldFailCreateRequestIfAuditEventIsMissingTimestamp() throws ResourceException {
        final String topic = "access";
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic))
                .build();
        auditService.startup();

        // No timestamp in the JSON content
        final JsonValue content = json(object(field("_id", "_id"),
                field("transactionId", "transactionId")));

        final CreateRequest createRequest = newCreateRequest(topic, content);

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void canQueryForRegisteredHandlerByName() throws Exception {
        final PassThroughAuditEventHandlerConfiguration firstConfiguration = new PassThroughAuditEventHandlerConfiguration();
        firstConfiguration.setName("firstHandler");
        firstConfiguration.setTopics(Collections.singleton("access"));
        final PassThroughAuditEventHandlerConfiguration secondConfiguration = new PassThroughAuditEventHandlerConfiguration();
        secondConfiguration.setName("secondHandler");
        secondConfiguration.setTopics(Collections.singleton("access"));
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final AuditService auditService = newAuditService()
                .withAuditEventHandler(clazz, firstConfiguration)
                .withAuditEventHandler(clazz, secondConfiguration)
                .build();
        auditService.startup();

        assertThat(auditService.getRegisteredHandler("firstHandler").getName()).isEqualTo("firstHandler");
        assertThat(auditService.getRegisteredHandler("secondHandler").getName()).isEqualTo("secondHandler");
    }

    @Test
    public void canQueryForTopicHandlingBasedOnRegisteredHandlers() throws Exception {
        final PassThroughAuditEventHandlerConfiguration firstConfiguration = new PassThroughAuditEventHandlerConfiguration();
        firstConfiguration.setName("firstHandler");
        firstConfiguration.setTopics(Collections.singleton("access"));
        final PassThroughAuditEventHandlerConfiguration secondConfiguration = new PassThroughAuditEventHandlerConfiguration();
        secondConfiguration.setName("secondHandler");
        secondConfiguration.setTopics(Collections.singleton("activity"));
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final AuditService auditService = newAuditService()
                .withAuditEventHandler(clazz, firstConfiguration)
                .withAuditEventHandler(clazz, secondConfiguration)
                .build();
        auditService.startup();

        assertThat(auditService.isAuditing("access")).isTrue();
        assertThat(auditService.isAuditing("activity")).isTrue();
        assertThat(auditService.isAuditing("config")).isFalse();
        assertThat(auditService.isAuditing("authentication")).isFalse();
        assertThat(auditService.isAuditing("unknown-topic")).isFalse();
    }

    @Test
    public void canQueryForKnownTopics() throws Exception {
        // given
        final AuditService auditService = newAuditService().build();
        auditService.startup();

        // when
        Set<String> knownTopics = auditService.getKnownTopics();

        // then
        assertThat(knownTopics).containsOnly("access", "activity", "authentication", "config");
    }

    @Test
    public void shouldNotBeRunningIfStartupNotYetCalled() throws Exception {
        final AuditService auditService = newAuditService().build();
        assertThat(auditService.isRunning()).isFalse();
    }

    @Test
    public void shouldBeRunningIfStartupCalledButShutdownNotYetCalled() throws Exception {
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        assertThat(auditService.isRunning()).isTrue();
    }

    @Test
    public void shouldBeNotRunningIfShutdownCalled() throws Exception {
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        auditService.shutdown();
        assertThat(auditService.isRunning()).isFalse();
    }

    @Test
    public void shouldStartupAllHandlersWhenStartupIsCalled() throws Exception {
        //given
        final AuditServiceConfiguration configuration = new AuditServiceConfiguration();
        final PassThroughAuditEventHandler eventHandler = spyPassThroughAuditEventHandler("pass");
        final Set<AuditEventHandler> handlers = asSet(eventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);

        //when
        auditService.startup();

        //then
        verify(eventHandler).startup();
    }

    @Test
    public void shouldSkipHandlersStartupIfAlreadyRunning() throws Exception {
        //given
        final AuditServiceConfiguration configuration = new AuditServiceConfiguration();
        final PassThroughAuditEventHandler eventHandler = spyPassThroughAuditEventHandler("pass");
        final Set<AuditEventHandler> handlers = asSet(eventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);

        //when
        auditService.startup();
        auditService.startup();

        //then
        verify(eventHandler, times(1)).startup();
    }

    @Test
    public void shouldShutdownAllHandlersWhenShutdownIsCalledWhenRunning() throws Exception {
        //given
        final AuditServiceConfiguration configuration = new AuditServiceConfiguration();
        final PassThroughAuditEventHandler eventHandler = spyPassThroughAuditEventHandler("pass");
        final Set<AuditEventHandler> handlers = asSet(eventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        //when
        auditService.shutdown();

        //then
        verify(eventHandler).shutdown();
    }

    @Test
    public void shouldSkipHandlersShutdownIfNotStarted() throws Exception {
        //given
        final PassThroughAuditEventHandlerConfiguration firstConfiguration = new PassThroughAuditEventHandlerConfiguration();
        firstConfiguration.setName("firstHandler");
        firstConfiguration.setTopics(Collections.singleton("access"));
        final PassThroughAuditEventHandlerConfiguration secondConfiguration = new PassThroughAuditEventHandlerConfiguration();
        secondConfiguration.setName("secondHandler");
        secondConfiguration.setTopics(Collections.singleton("access"));
        final PassThroughAuditEventHandler firstHandler = mock(PassThroughAuditEventHandler.class, "firstHandler");
        final PassThroughAuditEventHandler secondHandler = mock(PassThroughAuditEventHandler.class, "secondHandler");
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final AuditService auditService = newAuditService()
                .withAuditEventHandler(clazz, firstConfiguration)
                .withAuditEventHandler(clazz, secondConfiguration)
                .build();
        // note, no call to startup()

        //when
        auditService.shutdown();

        //then
        verify(firstHandler, times(0)).shutdown();
        verify(secondHandler, times(0)).shutdown();
    }

    @Test
    public void shouldFailCreateRequestIfAuditServiceIsShutdown() throws Exception {
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        final CreateRequest createRequest = makeCreateRequest();
        auditService.shutdown();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("AuditService not running");
    }

    @Test
    public void shouldFailReadRequestIfAuditServiceIsShutdown() {
        final AuditService auditService = newAuditService().build();
        final ReadRequest readRequest = Requests.newReadRequest("access", "1234");
        auditService.shutdown();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleRead(new RootContext(), readRequest);

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("AuditService not running");
    }

    @Test
    public void shouldFailQueryRequestIfAuditServiceIsShutdown() {
        final AuditService auditService = newAuditService().build();
        auditService.shutdown();

        //when
        final Promise<QueryResponse, ResourceException> promise = auditService.handleQuery(
                new RootContext(),
                newQueryRequest("access"),
                mock(QueryResourceHandler.class));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("AuditService not running");
    }

    @Test(expectedExceptions = ServiceUnavailableException.class,
            expectedExceptionsMessageRegExp = "AuditService not running")
    public void shouldFailIfShutdownAuditServiceIsAskedForItsConfiguration() throws ServiceUnavailableException {
        final AuditService auditService = newAuditService().build();
        auditService.shutdown();
        auditService.getConfig();
    }

    @Test(expectedExceptions = ServiceUnavailableException.class,
            expectedExceptionsMessageRegExp = "AuditService not running")
    public void shouldFailIfShutdownAuditServiceIsAskedForRegisteredHandler() throws ServiceUnavailableException {
        final AuditService auditService = newAuditService().build();
        auditService.shutdown();
        auditService.getRegisteredHandler("ignored-text");
    }

    @Test(expectedExceptions = ServiceUnavailableException.class,
            expectedExceptionsMessageRegExp = "AuditService not running")
    public void shouldFailIfShutdownAuditServiceIsAskedIfAuditingTopic() throws ServiceUnavailableException {
        final AuditService auditService = newAuditService().build();
        auditService.shutdown();
        auditService.isAuditing("access");
    }

    @Test(expectedExceptions = ServiceUnavailableException.class,
            expectedExceptionsMessageRegExp = "AuditService not running")
    public void shouldFailIfShutdownAuditServiceIsAskedForKnownTopics() throws ServiceUnavailableException {
        final AuditService auditService = newAuditService().build();
        auditService.shutdown();
        auditService.getKnownTopics();
    }

    @Test
    public void shouldNotDelegateCreateRequestToADisabledHandler() throws Exception {
        final String topic = "access";
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final PassThroughAuditEventHandlerConfiguration configuration = new PassThroughAuditEventHandlerConfiguration();
        configuration.setName("mock");
        configuration.setTopics(Collections.singleton(topic));
        configuration.setEnabled(false);
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic))
                .withAuditEventHandler(clazz, configuration)
                .build();
        auditService.startup();

        final CreateRequest createRequest = makeCreateRequest();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing(topic)).isFalse();
        assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(Collections.emptyMap());
    }

    @Test
    public void shouldDelegateFilteredCreateRequestToRegisteredHandler() throws Exception {
        final String topic = "access";
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final PassThroughAuditEventHandlerConfiguration configuration = new PassThroughAuditEventHandlerConfiguration();
        configuration.setName(QUERY_HANDLER_NAME);
        configuration.setTopics(Collections.singleton(topic));
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic))
                .withAuditEventHandler(clazz, configuration)
                .build();
        auditService.startup();

        final CreateRequest createRequest = makeCreateRequest();
        final JsonValue expectedResponseContent = makeCreateContent();
        expectedResponseContent.remove(FILTERED_FIELD);
        expectedResponseContent.remove(FILTERED_VALUE);

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing(topic)).isTrue();
        assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        expectedResponseContent.put("_id", resource.getId());
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(expectedResponseContent.asMap());
    }

    @Test
    public void shouldNotDelegateReadRequestToConfiguredHandlerForQueriesWhenNoResourceIdIsGiven() throws Exception {
        //given
        final String topic = "access";
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic);
        final PassThroughAuditEventHandler queryAuditEventHandler = spyPassThroughAuditEventHandler(QUERY_HANDLER_NAME);
        final PassThroughAuditEventHandler otherAuditEventHandler = spyPassThroughAuditEventHandler("otherHandler");
        final Set<AuditEventHandler> handlers = asSet(queryAuditEventHandler, otherAuditEventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        reset(otherAuditEventHandler); // So verifyZeroInteractions will work

        final ReadRequest readRequest = Requests.newReadRequest(topic);

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleRead(new RootContext(), readRequest);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        verifyZeroInteractions(otherAuditEventHandler);
    }

    @Test
    public void shouldNotDelegateReadRequestToConfiguredHandlerForQueriesWhenInvalidResourcePathGiven() throws Exception {
        //given
        final String topic = "access";
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME, topic);
        final PassThroughAuditEventHandler queryAuditEventHandler = spyPassThroughAuditEventHandler(QUERY_HANDLER_NAME);
        final PassThroughAuditEventHandler otherAuditEventHandler = spyPassThroughAuditEventHandler("otherHandler");
        final Set<AuditEventHandler> handlers = asSet(queryAuditEventHandler, otherAuditEventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        reset(otherAuditEventHandler); // So verifyZeroInteractions will work

        final ReadRequest readRequest = Requests.newReadRequest(topic, "id");
        readRequest.setResourcePath(readRequest.getResourcePathObject().child("ThirdPathField"));

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleRead(new RootContext(), readRequest);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
        verifyZeroInteractions(otherAuditEventHandler);
    }

    private AuditServiceConfiguration getAuditServiceConfiguration(String queryHandlerName, String topic) {
        final AuditServiceConfiguration config = new AuditServiceConfiguration();
        config.setHandlerForQueries(queryHandlerName);
        config.setAvailableAuditEventHandlers(
                singletonList("org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandler"));
        if (topic != null) {
            final Map<String, FilterPolicy> filterPolicies = new LinkedHashMap<>();
            filterPolicies.put("value", createValueFilter(topic));
            filterPolicies.put("field", createFieldFilter(topic));
            config.setFilterPolicies(filterPolicies);
        }
        return config;
    }

    public static CreateRequest makeCreateRequest() {
        return makeCreateRequest("access");
    }

    public static CreateRequest makeCreateRequest(String event) {
        return newCreateRequest(event, makeCreateContent());
    }

    public static JsonValue makeCreateContent() {
        return json(
                object(
                        field("_id", "_id"),
                        field("timestamp", "timestamp"),
                        field("transactionId", "transactionId"),
                        field(FILTERED_FIELD, "value"),
                        field(FILTERED_VALUE, "value")
                )
        );
    }

    private static FilterPolicy createFieldFilter(final String event) {
        final FilterPolicy filterPolicy = new FilterPolicy();
        filterPolicy.setIncludeIf(Collections.EMPTY_LIST);
        filterPolicy.setExcludeIf(asList("/" + event + "/" + FILTERED_FIELD));
        return filterPolicy;
    }

    private static FilterPolicy createValueFilter(final String event) {
        final FilterPolicy filterPolicy = new FilterPolicy();
        filterPolicy.setIncludeIf(Collections.EMPTY_LIST);
        filterPolicy.setExcludeIf(asList("/" + event + "/" + FILTERED_VALUE));
        return filterPolicy;
    }

    private PassThroughAuditEventHandler spyPassThroughAuditEventHandler(String name) {
        PassThroughAuditEventHandlerConfiguration configuration = new PassThroughAuditEventHandlerConfiguration();
        configuration.setName(name);
        configuration.setTopics(eventTopicsMetaData.getTopics());
        return spy(new PassThroughAuditEventHandler(configuration, eventTopicsMetaData));
    }

    private Set<AuditEventHandler> asSet(AuditEventHandler... entries) {
        return new HashSet<>(Arrays.asList(entries));

    }
}
