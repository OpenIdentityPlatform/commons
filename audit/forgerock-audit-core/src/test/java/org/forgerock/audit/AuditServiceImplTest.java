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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.events.EventTopicsMetaDataBuilder.coreTopicSchemas;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandler;
import org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandlerConfiguration;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings({"javadoc", "rawtypes", "unchecked" })
public class AuditServiceImplTest {

    private static final String QUERY_HANDLER_NAME = "pass-through";

    private EventTopicsMetaData eventTopicsMetaData;

    @BeforeMethod
    protected void setUp() throws Exception {
        eventTopicsMetaData = coreTopicSchemas().build();
    }

    @Test
    public void shouldDelegateCreateRequestToRegisteredHandler() throws Exception {
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final PassThroughAuditEventHandlerConfiguration configuration = new PassThroughAuditEventHandlerConfiguration();
        configuration.setName("mock");
        configuration.setTopics(Collections.singleton("access"));
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
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
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        final CreateRequest createRequest = makeCreateRequest("activity");

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing("activity")).isFalse();
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
    public void shouldFailCreateRequestIfAuditEventTopicIsNotKnown() throws ServiceUnavailableException {
        //given
        final AuditService auditService = newAuditService().build();
        auditService.startup();
        final CreateRequest createRequest = makeCreateRequest("unknownTopic");

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
    public void shouldDelegateReadRequestToConfiguredHandlerForQueries() throws Exception {
        //given
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME);
        final PassThroughAuditEventHandler queryAuditEventHandler = spyPassThroughAuditEventHandler(QUERY_HANDLER_NAME);
        final PassThroughAuditEventHandler otherAuditEventHandler = spyPassThroughAuditEventHandler("otherHandler");
        final Set<AuditEventHandler> handlers = asSet(queryAuditEventHandler, otherAuditEventHandler);
        final AuditService auditService = new AuditServiceImpl(configuration, eventTopicsMetaData, handlers);
        auditService.startup();

        final Promise<ResourceResponse, ResourceException> dummyResponse =
                newResourceResponse(null, null, json(object())).asPromise();
        reset(otherAuditEventHandler); // So verifyZeroInteractions will work
        given(queryAuditEventHandler.readEvent(any(Context.class), eq("access"), eq("1234"))).willReturn(dummyResponse);

        final ReadRequest readRequest = Requests.newReadRequest("access", "1234");

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
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
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
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
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
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
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
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
                .build();

        //when
        final Promise<ActionResponse, ResourceException> promise =
                auditService.handleAction(
                        new RootContext(),
                        Requests.newActionRequest("_id", "unknownAction"));

        //then
        assertThat(promise)
                .failedWithException()
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldDelegateQueryRequestToConfiguredHandlerForQueries() throws Exception {
        final AuditServiceConfiguration configuration = getAuditServiceConfiguration(QUERY_HANDLER_NAME);
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
                newQueryRequest("access"),
                mock(QueryResourceHandler.class));

        //then
        verify(eventHandler).queryEvents(
                any(Context.class), any(String.class), any(QueryRequest.class), any(QueryResourceHandler.class));
        assertThat(promise).isSameAs(queryResponsePromise);
    }

    @Test
    public void shouldFailCreateRequestIfAuditEventIsMissingTransactionId() throws ResourceException {
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
                .build();
        auditService.startup();

        // No transactionId in the JSON content
        final JsonValue content = json(object(field("_id", "_id"),
                field("timestamp", "timestamp")));

        final CreateRequest createRequest = newCreateRequest("access", content);

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
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
                .build();
        auditService.startup();

        // No timestamp in the JSON content
        final JsonValue content = json(object(field("_id", "_id"),
                field("transactionId", "transactionId")));

        final CreateRequest createRequest = newCreateRequest("access", content);

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
        final Class<PassThroughAuditEventHandler> clazz = PassThroughAuditEventHandler.class;
        final PassThroughAuditEventHandlerConfiguration configuration = new PassThroughAuditEventHandlerConfiguration();
        configuration.setName("mock");
        configuration.setTopics(Collections.singleton("access"));
        configuration.setEnabled(false);
        final AuditService auditService = newAuditService()
                .withConfiguration(getAuditServiceConfiguration(QUERY_HANDLER_NAME))
                .withAuditEventHandler(clazz, configuration)
                .build();
        auditService.startup();

        final CreateRequest createRequest = makeCreateRequest();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                auditService.handleCreate(new RootContext(), createRequest);

        //then
        assertThat(auditService.isAuditing("access")).isFalse();
        assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    private AuditServiceConfiguration getAuditServiceConfiguration(String queryHandlerName) {
        final AuditServiceConfiguration config = new AuditServiceConfiguration();
        config.setHandlerForQueries(queryHandlerName);
        config.setAvailableAuditEventHandlers(
                singletonList("org.forgerock.audit.events.handlers.impl.PassThroughAuditEventHandler"));
        return config;
    }

    public static CreateRequest makeCreateRequest() {
        return makeCreateRequest("access");
    }

    public static CreateRequest makeCreateRequest(String event) {
        final JsonValue content = json(
                object(
                        field("_id", "_id"),
                        field("timestamp", "timestamp"),
                        field("transactionId", "transactionId"))
        );
        return newCreateRequest(event, content);
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
