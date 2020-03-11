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
package org.forgerock.audit.handlers.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.events.EventTopicsMetaDataBuilder.coreTopicSchemas;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.h2.tools.RunScript;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("javadoc")
public class JdbcAuditEventHandlerTest {

    public static final String H2_DRIVER = "org.h2.Driver";
    public static final String H2_JDBC_URL = "jdbc:h2:mem:audit";

    public static final String EVENTS_JSON = "/events.json";

    public static final String AUDIT_SQL_SCRIPT = "/audit.sql";
    public static final String TEST_AUDIT_EVENT_TOPIC = "test";

    public static final String SHUTDOWN = "SHUTDOWN";

    public static final String EVENT_NAME_FIELD = "eventName";
    public static final String TRANSACTION_ID_FIELD = "transactionId";
    public static final String ID_FIELD = "_id";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String USER_ID_FIELD = "userId";

    public static final String ID_VALUE = "UUID";
    public static final String EVENT_NAME_VALUE = "eventName";
    public static final String USER_ID_VALUE = "test@forgerock.com";
    public static final String TRANSACTION_ID_VALUE = "transactionId";
    public static final String CUSTOM_OBJECT_FIELD = "customObject";
    public static final String CUSTOM_OBJECT_KEY_FIELD = "key";
    public static final String CUSTOM_OBJECT_VALUE = "value";
    public static final String CUSTOM_ARRAY_FIELD = "customArray";
    public static final String CUSTOM_ARRAY_VALUE = "Item1";
    public static final String CUSTOM_INTEGER_FIELD = "customInteger";
    public static final int CUSTOM_INTEGER_VALUE = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;

    @BeforeMethod
    private void setUpDataBase()  throws SQLException, ClassNotFoundException {
        Class.forName(H2_DRIVER);
        connection = DriverManager.getConnection(H2_JDBC_URL);
        InputStream sqlScript = getClass().getResourceAsStream(AUDIT_SQL_SCRIPT);
        RunScript.execute(connection, new InputStreamReader(sqlScript));
    }

    @AfterMethod
    private void tearDownDataBase() {
        try {
            connection.createStatement().execute(SHUTDOWN);
        } catch (SQLException e) {
            // do nothing
        }
        connection = null;
    }

    /**
     * Integration test.
     */
    @Test
    public void canConfigureJdbcHandlerFromJsonAndRegisterWithAuditService() throws Exception {
        // given
        final JsonValue additionalSchema = json(object(field("test", getEventsMetaData())));
        final EventTopicsMetaData eventTopicsMetaData = coreTopicSchemas()
                .withAdditionalTopicSchemas(additionalSchema).build();
        final AuditServiceBuilder auditServiceBuilder = newAuditService().withEventTopicsMetaData(eventTopicsMetaData);
        final JsonValue config = AuditJsonConfig.getJson(getResource("/event-handler.json"));

        // when
        AuditJsonConfig.registerHandlerToService(config, auditServiceBuilder);

        // then
        AuditService auditService = auditServiceBuilder.build();
        auditService.startup();
        AuditEventHandler registeredHandler = auditService.getRegisteredHandler("jdbc");
        assertThat(registeredHandler).isNotNull();
    }

    @Test
    public void canConfigureBufferedJdbcHandlerFromJsonAndRegisterWithAuditService() throws Exception {
        // given
        final JsonValue additionalSchema = json(object(field("test", getEventsMetaData())));
        final EventTopicsMetaData eventTopicsMetaData = coreTopicSchemas()
                .withAdditionalTopicSchemas(additionalSchema).build();
        final AuditServiceBuilder auditServiceBuilder = newAuditService().withEventTopicsMetaData(eventTopicsMetaData);
        final JsonValue config = AuditJsonConfig.getJson(getResource("/event-handler.json"));

        // when
        AuditJsonConfig.registerHandlerToService(config, auditServiceBuilder);

        // then
        AuditService auditService = auditServiceBuilder.build();
        auditService.startup();
        AuditEventHandler registeredHandler = auditService.getRegisteredHandler("jdbc");
        assertThat(registeredHandler).isNotNull();
    }

    private InputStream getResource(String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }

    @Test
    public void testPublish() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        System.out.println(new ObjectMapper().writeValueAsString(configuration));
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
            handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        // then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        AssertJJsonValueAssert.assertThat(promise.get().getContent()).isEqualTo(event);
    }

    @Test
    public void testCreateWithEmptyDB() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        connection.createStatement().execute(SHUTDOWN);

        // when
        final Promise<ResourceResponse, ResourceException> promise =
            handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void testCreateWithNoTableMapping() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        configuration.setTableMappings(new LinkedList<TableMapping>());
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
            handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void testRead() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // create entry
        Promise<ResourceResponse, ResourceException> promise =
            handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        // when
        promise = handler.readEvent(context, TEST_AUDIT_EVENT_TOPIC, promise.get().getId());

        // then
        AssertJPromiseAssert.assertThat(promise).succeeded();

        assertThat(promise.get().getContent().asMap())
                .containsEntry(ID_FIELD, ID_VALUE)
                .containsEntry(EVENT_NAME_FIELD, EVENT_NAME_VALUE)
                .containsKeys(TIMESTAMP_FIELD)
                .containsEntry(TRANSACTION_ID_FIELD, TRANSACTION_ID_VALUE)
                .containsEntry(USER_ID_FIELD, USER_ID_VALUE)
                .containsEntry(CUSTOM_OBJECT_FIELD,
                        Collections.singletonMap(CUSTOM_OBJECT_KEY_FIELD, CUSTOM_OBJECT_VALUE))
                .containsEntry(CUSTOM_ARRAY_FIELD, Collections.singletonList(CUSTOM_ARRAY_VALUE))
                .containsEntry(CUSTOM_INTEGER_FIELD, CUSTOM_INTEGER_VALUE);
    }

    @Test
    public void testReadWithNoEntry() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final Context context = new RootContext();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
            handler.readEvent(context, TEST_AUDIT_EVENT_TOPIC, ID_VALUE);

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }

    @Test
    public void testReadWithEmptyDB() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // create entry
        Promise<ResourceResponse, ResourceException> promise =
                handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);
        connection.createStatement().execute(SHUTDOWN);

        // when
        promise = handler.readEvent(context, TEST_AUDIT_EVENT_TOPIC, promise.get().getId());

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void testReadWithNoTableMapping() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // create entry
        Promise<ResourceResponse, ResourceException> promise =
                handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        configuration.setTableMappings(new LinkedList<TableMapping>());
        handler = createJdbcAuditEventHandler(configuration);

        // when
        promise = handler.readEvent(context, TEST_AUDIT_EVENT_TOPIC, promise.get().getId());

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void testQuery() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // create entry
        Promise<ResourceResponse, ResourceException> promise =
                handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        final QueryRequest queryRequest = Requests.newQueryRequest(TEST_AUDIT_EVENT_TOPIC)
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer(ID_FIELD), promise.get().getId()));

        final List<ResourceResponse> resourceResponses = new LinkedList<>();

        // when
        final Promise<QueryResponse, ResourceException> queryPromise =
                handler.queryEvents(context, TEST_AUDIT_EVENT_TOPIC, queryRequest, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        resourceResponses.add(resourceResponse);
                        return true;
                    }
                });

        assertThat(resourceResponses.size()).isEqualTo(1);
        assertThat(resourceResponses.get(0).getContent().asMap())
                .containsEntry(ID_FIELD, ID_VALUE)
                .containsEntry(EVENT_NAME_FIELD, EVENT_NAME_VALUE)
                .containsKeys(TIMESTAMP_FIELD)
                .containsEntry(TRANSACTION_ID_FIELD, TRANSACTION_ID_VALUE)
                .containsEntry(USER_ID_FIELD, USER_ID_VALUE)
                .containsEntry(CUSTOM_OBJECT_FIELD,
                        Collections.singletonMap(CUSTOM_OBJECT_KEY_FIELD, CUSTOM_OBJECT_VALUE))
                .containsEntry(CUSTOM_ARRAY_FIELD, Collections.singletonList(CUSTOM_ARRAY_VALUE))
                .containsEntry(CUSTOM_INTEGER_FIELD, CUSTOM_INTEGER_VALUE);
    }

    @Test
    public void testQueryWithEmptyDB() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // create entry
        Promise<ResourceResponse, ResourceException> promise =
                handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        final QueryRequest queryRequest = Requests.newQueryRequest(TEST_AUDIT_EVENT_TOPIC)
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer(ID_FIELD), promise.get().getId()));

        final List<ResourceResponse> resourceResponses = new LinkedList<>();

        connection.createStatement().execute(SHUTDOWN);

        // when
        final Promise<QueryResponse, ResourceException> queryPromise =
                handler.queryEvents(context, TEST_AUDIT_EVENT_TOPIC, queryRequest, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        resourceResponses.add(resourceResponse);
                        return true;
                    }
                });

        // then
        AssertJPromiseAssert.assertThat(queryPromise)
                .failedWithException()
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void testQueryWithNoTableMapping() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(false);
        JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // create entry
        Promise<ResourceResponse, ResourceException> promise =
                handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        final QueryRequest queryRequest = Requests.newQueryRequest(TEST_AUDIT_EVENT_TOPIC)
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer(ID_FIELD), promise.get().getId()));

        final List<ResourceResponse> resourceResponses = new LinkedList<>();

        configuration.setTableMappings(new LinkedList<TableMapping>());
        handler = createJdbcAuditEventHandler(configuration);

        // when
        final Promise<QueryResponse, ResourceException> queryPromise =
                handler.queryEvents(context, TEST_AUDIT_EVENT_TOPIC, queryRequest, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        resourceResponses.add(resourceResponse);
                        return true;
                    }
                });

        // then
        AssertJPromiseAssert.assertThat(queryPromise)
                .failedWithException()
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void testPublishWithBuffering() throws Exception {
        // given
        final JdbcAuditEventHandlerConfiguration configuration = createConfiguration(true);
        System.out.println(new ObjectMapper().writeValueAsString(configuration));
        final JdbcAuditEventHandler handler = createJdbcAuditEventHandler(configuration);
        final JsonValue event = makeEvent();
        final Context context = new RootContext();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                handler.publishEvent(context, TEST_AUDIT_EVENT_TOPIC, event);

        // then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        AssertJJsonValueAssert.assertThat(promise.get().getContent()).isEqualTo(event);
    }

    private JdbcAuditEventHandler createJdbcAuditEventHandler(final JdbcAuditEventHandlerConfiguration configuration)
            throws Exception {
        EventTopicsMetaData eventsMetaData = getEventsMetaData();
        configuration.setTopics(eventsMetaData.getTopics());
        JdbcAuditEventHandler handler = new JdbcAuditEventHandler(configuration, eventsMetaData, null);
        handler.startup();
        return handler;
    }

    private JdbcAuditEventHandlerConfiguration createConfiguration(final boolean bufferingEnabled) throws Exception {
        if (bufferingEnabled) {
            return MAPPER.readValue(
                    getResource("/event-handler-config-buffering.json"), JdbcAuditEventHandlerConfiguration.class);

        } else {
            return MAPPER.readValue(
                    getResource("/event-handler-config.json"), JdbcAuditEventHandlerConfiguration.class);
        }
    }


    private JsonValue makeEvent() {
        final AuditEvent testAuditEvent = TestAuditEventBuilder.testAuditEventBuilder()
                .eventName(EVENT_NAME_VALUE)
                .userId(USER_ID_VALUE)
                .timestamp(System.currentTimeMillis())
                .transactionId(TRANSACTION_ID_VALUE)
                .customObject(Collections.<String, Object>singletonMap(CUSTOM_OBJECT_KEY_FIELD, CUSTOM_OBJECT_VALUE))
                .customArray(Collections.singletonList(CUSTOM_ARRAY_VALUE))
                .customInteger(CUSTOM_INTEGER_VALUE)
                .toEvent();
        testAuditEvent.getValue().put(ID_FIELD, ID_VALUE);
        return testAuditEvent.getValue();
    }

    private EventTopicsMetaData getEventsMetaData() throws Exception {
        Map<String, JsonValue> events = new LinkedHashMap<>();
        try (final InputStream configStream = getClass().getResourceAsStream(EVENTS_JSON)) {
            final JsonValue predefinedEventTypes = new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        }
        return new EventTopicsMetaData(events);
    }

    static class TestAuditEventBuilder<T extends TestAuditEventBuilder<T>>
            extends AuditEventBuilder<T> {

        @SuppressWarnings("rawtypes")
        public static TestAuditEventBuilder<?> testAuditEventBuilder() {
            return new TestAuditEventBuilder();
        }

        public T customObject(Map<String, Object> object) {
            jsonValue.put(CUSTOM_OBJECT_FIELD, object);
            return self();
        }

        public T customArray(List<String> object) {
            jsonValue.put(CUSTOM_ARRAY_FIELD, object);
            return self();
        }

        public T customInteger(final int integer) {
            jsonValue.put(CUSTOM_INTEGER_FIELD, integer);
            return self();
        }
    }
}
