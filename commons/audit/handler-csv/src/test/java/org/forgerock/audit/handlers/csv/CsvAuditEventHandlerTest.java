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
 * Portions copyright 2020-2026 3A Systems LLC.
 */

package org.forgerock.audit.handlers.csv;

import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.audit.AuditServiceBuilder.*;
import static org.forgerock.audit.AuditServiceProxy.*;
import static org.forgerock.audit.handlers.csv.CsvAuditEventHandler.*;
import static org.forgerock.audit.handlers.csv.CsvWriterTest.NON_ASCII_VALUE;
import static org.forgerock.audit.handlers.csv.CsvWriterTest.reencodeInWindows1252;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_PASSWORD;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.assertThreadsTerminate;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;

import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration.FileRotation;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.handlers.csv.CsvSecureVerifier.VerificationResult;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.audit.providers.DefaultKeyStoreHandlerProvider;
import org.forgerock.audit.providers.KeyStoreHandlerProvider;
import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.retention.TimeStampFileNamingPolicy;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.secure.KeyStoreSecureStorage;
import org.forgerock.audit.util.GzipUtil;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryFilters;
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
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mockito.ArgumentCaptor;
import org.supercsv.prefs.CsvPreference;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

@SuppressWarnings("javadoc")
public class CsvAuditEventHandlerTest {

    static final String KEYSTORE_FILENAME = "target/test-classes/keystore-signature.jks";

    /**
     * Integration test.
     */
    @Test
    public void canConfigureCsvHandlerFromJsonAndRegisterWithAuditService() throws Exception {
        // given
        final AuditServiceBuilder auditServiceBuilder = newAuditService();

        final DefaultKeyStoreHandlerProvider provider = new DefaultKeyStoreHandlerProvider();
        KeyStoreHandler keyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME,
                "password");
        KeyStoreHandlerDecorator deco = new KeyStoreHandlerDecorator(keyStoreHandler);
        provider.registerKeyStoreHandler("csvSecure", keyStoreHandler);
        DependencyProviderBase dependencyProvider = new DependencyProviderBase();
        dependencyProvider.register(KeyStoreHandlerProvider.class, provider);

        auditServiceBuilder.withDependencyProvider(dependencyProvider);
        final JsonValue config = AuditJsonConfig.getJson(getResource("/event-handler-config.json"));
        config.get("/config/logDirectory").setObject(System.getProperty("java.io.tmpdir"));

        // when
        AuditJsonConfig.registerHandlerToService(config, auditServiceBuilder);

        // then
        AuditService auditService = auditServiceBuilder.build();
        auditService.startup();
        try {
            AuditEventHandler registeredHandler = auditService.getRegisteredHandler("csv");
            assertThat(registeredHandler).isNotNull();
        } finally {
            auditService.shutdown();
        }
    }

    private InputStream getResource(String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }

    @Test
    public void testCreatingAuditLogEntryWithBuffering() throws Exception {
        //given
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler()
                .loggingTo(logDirectory).withBufferingEnabled().build();
        final Context context = new RootContext();
        try {

            // when
            csvHandler.publishEvent(context, "access", buildEvent(1));
            csvHandler.publishEvent(context, "access", buildEvent(2));

            // then
            final String expectedContent = "\"_id\",\"timestamp\",\"transactionId\"\n"
                    + "\"_id1\",\"timestamp\",\"transactionId-X\"\n" + "\"_id2\",\"timestamp\",\"transactionId-X\"";
            final File file = logDirectory.resolve("access.csv").toFile();
            int tries = 0;
            while ((!file.exists() || file.length() < expectedContent.length()) && tries < 10) {
                Thread.sleep(10);
                tries++;
            }
            assertThat(file).hasContent(expectedContent);
        } finally {
            csvHandler.shutdown();
        }
    }

    @Test
    public void testCreatingAuditLogEntry() throws Exception {
        //given
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();

        final CreateRequest createRequest = makeCreateRequest();

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                csvHandler.publishEvent(context, "access", createRequest.getContent());

        //then
        AssertJPromiseAssert.assertThat(promise)
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
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();

        final ResourceResponse event = createAccessEvent(csvHandler);

        final ReadRequest readRequest = Requests.newReadRequest("access", event.getId());

        //when
        final Promise<ResourceResponse, ResourceException> promise =
                csvHandler.readEvent(context, "access", readRequest.getResourcePathObject().tail(1).toString());

        //then
        AssertJPromiseAssert.assertThat(promise)
                .succeeded()
                .withObject()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        final ResourceResponse resource = promise.get();
        assertResourceEquals(resource, event);
    }

    @Test
    public void testReadingAuditLogEntryWithNonAsciiValue() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        try {
            final JsonValue content = json(object(
                    field("_id", "1"),
                    field("timestamp", "123456"),
                    field("transactionId", NON_ASCII_VALUE)));
            csvHandler.publishEvent(context, "access", content).getOrThrow();

            final ResourceResponse resource = csvHandler.readEvent(context, "access", "1").getOrThrow();
            assertThat(resource.getContent().get("transactionId").asString()).isEqualTo(NON_ASCII_VALUE);
        } finally {
            csvHandler.shutdown();
        }
    }

    @Test
    public void testReadingAuditLogEntryNotInUtf8() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final Context context = new RootContext();
        CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        try {
            // "Grüße": letters that windows-1252 holds
            csvHandler.publishEvent(context, "access", json(object(
                    field("_id", "1"),
                    field("timestamp", "123456"),
                    field("transactionId", "Grüße")))).getOrThrow();
        } finally {
            csvHandler.shutdown();
        }
        reencodeInWindows1252(logDirectory.resolve("access.csv").toFile());

        csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        try {
            csvHandler.publishEvent(context, "access", json(object(
                    field("_id", "2"),
                    field("timestamp", "123456"),
                    field("transactionId", NON_ASCII_VALUE)))).getOrThrow();

            // the file is resumed as it is: the row written before reads back with U+FFFD in place of each byte that
            // is not UTF-8, the row appended after it is in UTF-8
            assertThat(csvHandler.readEvent(context, "access", "1").getOrThrow()
                    .getContent().get("transactionId").asString()).isEqualTo("Gr\uFFFD\uFFFDe");
            assertThat(csvHandler.readEvent(context, "access", "2").getOrThrow()
                    .getContent().get("transactionId").asString()).isEqualTo(NON_ASCII_VALUE);
        } finally {
            csvHandler.shutdown();
        }
    }

    private static void assertResourceEquals(ResourceResponse left, ResourceResponse right) {
        Map<String, Object> leftAsMap = dropNullEntries(left.getContent()).asMap();
        Map<String, Object> rightAsMap = dropNullEntries(right.getContent()).asMap();
        assertThat(leftAsMap).isEqualTo(rightAsMap);
    }

    private static JsonValue dropNullEntries(JsonValue jsonValue) {
        JsonValue result = jsonValue.clone();

        for (String key : jsonValue.keys()) {
            if (jsonValue.get(key).isNull()) {
                result.remove(key);
            }
        }

        return result;
    }

    @Test
    public void testQueryOnAuditLogEntry() throws Exception {
        //given
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();

        final QueryResourceHandler queryResourceHandler = mock(QueryResourceHandler.class);
        final ArgumentCaptor<ResourceResponse> resourceCaptor = ArgumentCaptor.forClass(ResourceResponse.class);

        final ResourceResponse event = createAccessEvent(csvHandler);

        final QueryRequest queryRequest = Requests.newQueryRequest("access")
                .setQueryFilter(QueryFilters.parse("/_id eq \"_id0\""));
        //when
        final Promise<QueryResponse, ResourceException> promise =
                csvHandler.queryEvents(context, "access", queryRequest, queryResourceHandler);

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        verify(queryResourceHandler).handleResource(resourceCaptor.capture());

        final ResourceResponse resource = resourceCaptor.getValue();
        assertResourceEquals(resource, event);
    }

    @DataProvider
    private Object[][] noEventYetData() {
        return new Object[][] {
                // label, rotate action before the read ?
                { "after startup", false },
                { "after the rotate action", true }
        };
    }

    @Test(dataProvider = "noEventYetData")
    public void testReadingTamperEvidentAuditLogWithNoEventYet(String label, boolean rotate) throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled().build();
        final Context context = new RootContext();
        try {
            leaveTamperEvidentFileWithNoEvent(csvHandler, logDirectory, label, rotate);

            final Promise<ResourceResponse, ResourceException> promise =
                    csvHandler.readEvent(context, "access", "_id1");

            AssertJPromiseAssert.assertThat(promise)
                    .as("when " + label)
                    .failedWithException()
                    .isInstanceOf(NotFoundException.class);
        } finally {
            csvHandler.shutdown();
        }
    }

    @Test(dataProvider = "noEventYetData")
    public void testQueryOnTamperEvidentAuditLogWithNoEventYet(String label, boolean rotate) throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled().build();
        final Context context = new RootContext();
        final QueryResourceHandler queryResourceHandler = mock(QueryResourceHandler.class);
        try {
            leaveTamperEvidentFileWithNoEvent(csvHandler, logDirectory, label, rotate);

            final Promise<QueryResponse, ResourceException> promise = csvHandler.queryEvents(context, "access",
                    Requests.newQueryRequest("access").setQueryFilter(QueryFilters.parse("true")),
                    queryResourceHandler);

            AssertJPromiseAssert.assertThat(promise).as("when " + label).succeeded();
            verify(queryResourceHandler, never()).handleResource(any(ResourceResponse.class));
        } finally {
            csvHandler.shutdown();
        }
    }

    /**
     * The tamper evident writer writes the header with the first event, so the audit file is empty after startup, and
     * after the rotate action of a topic without file rotation until the next event.
     */
    private void leaveTamperEvidentFileWithNoEvent(CsvAuditEventHandler csvHandler, Path logDirectory, String label,
            boolean rotate) throws Exception {
        if (rotate) {
            final Context context = new RootContext();
            csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
            final ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                    .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
            csvHandler.handleAction(context, "access", actionRequest).getOrThrow();
        }
        assertThat(logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + "access.csv").toFile())
                .as("when " + label)
                .isEmpty();
    }

    private CreateRequest makeCreateRequest() {
        return Requests.newCreateRequest("access", buildEvent());
    }

    private JsonValue buildEvent() {
        return buildEvent(0);
    }

    private JsonValue buildEvent(int index) {
        final JsonValue content = json(
                object(
                        field("_id", "_id" + index),
                        field("timestamp", "timestamp"),
                        field("transactionId", "transactionId-X")
                        )
                );
        return content;
    }

    @SuppressWarnings("unchecked")
    private static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);
    }

    private ResourceResponse createAccessEvent(AuditEventHandler auditEventHandler) throws Exception {
        final CreateRequest createRequest = makeCreateRequest();
        final Context context = new RootContext();

        final Promise<ResourceResponse, ResourceException> promise =
                auditEventHandler.publishEvent(context, "access", createRequest.getContent());

        AssertJPromiseAssert.assertThat(promise)
                .succeeded()
                .isInstanceOf(ResourceResponse.class);

        // TODO should use AssertJResourceResponseAssert
        return promise.get();
    }

    @Test
    public void testCreateCsvLogEntryWritesToFile() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();

        final JsonValue content = json(
                object(
                        field("_id", "1"),
                        field("timestamp", "123456"),
                        field("transactionId", "A10000")));
        final CreateRequest createRequest = Requests.newCreateRequest("access", content);

        csvHandler.publishEvent(context, "access", createRequest.getContent());

        String expectedContent = "\"_id\",\"timestamp\",\"transactionId\"\n"
                + "\"1\",\"123456\",\"A10000\"";
        assertThat(logDirectory.resolve("access.csv").toFile()).hasContent(expectedContent);
    }

    @Test
    public void testStartupWithAuditFileOfMoreColumnsThanTopicSchema() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final Path auditFile = logDirectory.resolve("access.csv");
        // written before the last field was removed from the topic schema
        final String fileHeaders = "\"_id\",\"timestamp\",\"transactionId\",\"userId\"";
        Files.write(auditFile, List.of(fileHeaders));

        // the writer of the topic fails to open, and the handler starts without it
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        try {
            AssertJPromiseAssert.assertThat(csvHandler.publishEvent(new RootContext(), "access", buildEvent()))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
        } finally {
            csvHandler.shutdown();
        }

        assertThat(Files.readAllLines(auditFile, UTF_8)).containsExactly(fileHeaders);
    }

    @Test
    public void testShutdownClosesEveryTamperEvidentWriterWhenOneFailsToClose() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder builder =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled();
        // A second topic, so that a writer is left to close after the first one fails.
        final Map<String, JsonValue> topics = new LinkedHashMap<>();
        topics.put("access", builder.eventTopicsMetaData.getSchema("access"));
        topics.put("activity", builder.eventTopicsMetaData.getSchema("access"));
        final EventTopicsMetaData eventTopicsMetaData = new EventTopicsMetaData(topics);
        builder.config.setTopics(eventTopicsMetaData.getTopics());
        // The writers open at startup, and their schedulers create their threads in the group of that thread.
        final ThreadGroup writerThreads = new ThreadGroup("CsvAuditEventHandlerTest");
        final FutureTask<CsvAuditEventHandler> creation = new FutureTask<>(() -> {
            final CsvAuditEventHandler handler =
                    new CsvAuditEventHandler(builder.config, eventTopicsMetaData, builder.keystoreHandlerProvider);
            handler.startup();
            return handler;
        });
        final Thread creator = new Thread(writerThreads, creation);
        creator.start();
        final CsvAuditEventHandler csvHandler = creation.get();
        creator.join();

        final Context context = new RootContext();
        for (String topic : topics.keySet()) {
            csvHandler.publishEvent(context, topic, buildEvent());
            // A keystore cannot be stored over a directory, so the final signature of every writer fails.
            final File keyStoreFile =
                    logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + topic + ".csv.keystore").toFile();
            assertThat(keyStoreFile.delete()).isTrue();
            assertThat(keyStoreFile.mkdir()).isTrue();
        }
        assertThat(writerThreads.activeCount()).as("scheduler threads").isEqualTo(topics.size());

        assertThatThrownBy(csvHandler::shutdown).isInstanceOf(InternalServerErrorException.class);
        // The scheduler threads are not daemon threads, they would keep the JVM from exiting.
        assertThreadsTerminate(writerThreads);
    }

    @Test
    public void testStartupOpensOtherTopicsWhenTamperEvidentFileCannotBeResumed() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder builder =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled();
        // A second topic, so that a topic is left to open after the first one fails.
        final Map<String, JsonValue> topics = new LinkedHashMap<>();
        topics.put("access", builder.eventTopicsMetaData.getSchema("access"));
        topics.put("activity", builder.eventTopicsMetaData.getSchema("access"));
        final EventTopicsMetaData eventTopicsMetaData = new EventTopicsMetaData(topics);
        builder.config.setTopics(eventTopicsMetaData.getTopics());
        final Context context = new RootContext();

        // A file and a keystore for every topic.
        CsvAuditEventHandler csvHandler =
                new CsvAuditEventHandler(builder.config, eventTopicsMetaData, builder.keystoreHandlerProvider);
        csvHandler.startup();
        for (String topic : topics.keySet()) {
            AssertJPromiseAssert.assertThat(csvHandler.publishEvent(context, topic, buildEvent(1))).succeeded();
        }
        csvHandler.shutdown();

        csvHandler = new CsvAuditEventHandler(builder.config, eventTopicsMetaData, builder.keystoreHandlerProvider);
        // The topic that startup opens first, so that the other topic is opened only if startup goes on.
        final List<String> handledTopics = new ArrayList<>(csvHandler.getHandledTopics());
        final String failingTopic = handledTopics.get(0);
        final String otherTopic = handledTopics.get(1);
        // The HMAC of the row no longer matches its value, so the file fails verification when it is resumed.
        final Path failingFile = logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + failingTopic + ".csv");
        final String content = new String(Files.readAllBytes(failingFile), UTF_8);
        Files.write(failingFile, content.replace("transactionId-X", "transactionId-Y").getBytes(UTF_8));

        final AuditService auditService = newAuditService()
                .withEventTopicsMetaData(eventTopicsMetaData)
                .withAuditEventHandler(csvHandler)
                .build();
        try {
            auditService.startup();
            assertThat(auditService.isRunning()).isTrue();

            AssertJPromiseAssert.assertThat(csvHandler.publishEvent(context, otherTopic, buildEvent(2))).succeeded();
            assertThat(logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + otherTopic + ".csv").toFile())
                    .content().contains("\"_id2\"");
            AssertJPromiseAssert.assertThat(csvHandler.publishEvent(context, failingTopic, buildEvent(2)))
                    .failedWithException().isInstanceOf(InternalServerErrorException.class);
        } finally {
            auditService.shutdown();
        }
    }

    @DataProvider
    private Object[][] publishAfterShutdownData() {
        return new Object[][] {
                // label, is tamper evident logging enabled ?, is rotation enabled ?
                { "standard", false, false },
                { "tamper evident", true, false },
                { "standard with rotation", false, true },
                { "tamper evident with rotation", true, true }
        };
    }

    @Test(dataProvider = "publishAfterShutdownData")
    public void testPublishEventAfterShutdownFails(String label, boolean isTamperEvident, boolean isRotationEnabled)
            throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder = csvAuditEventHandler().loggingTo(logDirectory);
        if (isTamperEvident) {
            csvAuditEventHandlerBuilder.withSecureLoggingEnabled();
        }
        if (isRotationEnabled) {
            csvAuditEventHandlerBuilder.withRotationEnabled();
        }
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();
        final Context context = new RootContext();
        csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
        csvHandler.shutdown();

        // the event is reported as lost, and is written neither to the closed writer nor to a new one
        AssertJPromiseAssert.assertThat(csvHandler.publishEvent(context, "access", buildEvent(2)))
                .failedWithException()
                .isInstanceOf(ServiceUnavailableException.class);

        final File liveFile =
                logDirectory.resolve((isTamperEvident ? SECURE_CSV_FILENAME_PREFIX : "") + "access.csv").toFile();
        assertThat(liveFile).as("when " + label).content(UTF_8)
                .containsOnlyOnce("\"_id1\"")
                .doesNotContain("\"_id2\"");
        if (isTamperEvident) {
            final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                    new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
            final KeyStoreHandler liveKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                    CsvSecureUtils.keyStoreFile(liveFile).getPath(), Base64.encode(
                            keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded()));
            final VerificationResult verificationResult = new CsvSecureVerifier(liveFile,
                    CsvPreference.EXCEL_PREFERENCE, new KeyStoreSecureStorage(liveKeyStoreHandler,
                            keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE))).verify();
            assertThat(verificationResult.hasPassedVerification())
                    .as("when " + label + ": " + verificationResult.getFailureReason())
                    .isTrue();
        }
    }

    @Test(dataProvider = "rotateActionData")
    public void testActionToRotateFileAfterShutdownFails(String label, boolean isRotationEnabled) throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder = csvAuditEventHandler().loggingTo(logDirectory);
        if (isRotationEnabled) {
            csvAuditEventHandlerBuilder.withRotationEnabled();
        }
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();
        final Context context = new RootContext();
        csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
        csvHandler.shutdown();

        final ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "access", actionRequest))
                .failedWithException()
                .isInstanceOf(ServiceUnavailableException.class);

        // the file is not rotated, and no new writer opens a new file
        final File liveFile = logDirectory.resolve("access.csv").toFile();
        assertThat(logDirectory.toFile().listFiles()).as("when " + label).containsExactly(liveFile);
        assertThat(liveFile).as("when " + label).content(UTF_8).containsOnlyOnce("\"_id1\"");
    }

    @Test
    public void testFailuresOfAuditFileAreServerErrors() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        // A directory in place of the audit file: it can be neither written nor read.
        Files.createDirectory(logDirectory.resolve("access.csv")).toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        try {
            // The request is right: these are failures of the server, which a client may retry.
            AssertJPromiseAssert.assertThat(csvHandler.publishEvent(context, "access", buildEvent(1)))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
            AssertJPromiseAssert.assertThat(csvHandler.readEvent(context, "access", "_id1"))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
            final QueryRequest queryRequest = Requests.newQueryRequest("access")
                    .setQueryFilter(QueryFilters.parse("/_id eq \"_id1\""));
            AssertJPromiseAssert.assertThat(
                    csvHandler.queryEvents(context, "access", queryRequest, mock(QueryResourceHandler.class)))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
            final ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                    .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "access", actionRequest))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
        } finally {
            csvHandler.shutdown();
        }
    }

    @Test
    public void testPublishEventAfterShutdownAndStartup() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        final File liveFile = logDirectory.resolve("access.csv").toFile();
        final File movedFile = logDirectory.resolve("access.csv.moved").toFile();
        csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
        csvHandler.shutdown();
        // A file cannot be opened over a directory, so the topic gets no writer at startup, and its next event opens
        // one: after the shutdown, startup() lets an event open the writer again.
        assertThat(liveFile.renameTo(movedFile)).isTrue();
        assertThat(liveFile.mkdir()).isTrue();
        csvHandler.startup();
        try {
            assertThat(liveFile.delete()).isTrue();
            assertThat(movedFile.renameTo(liveFile)).isTrue();
            csvHandler.publishEvent(context, "access", buildEvent(2)).getOrThrow();
            csvHandler.publishEvent(context, "access", buildEvent(3)).getOrThrow();
        } finally {
            csvHandler.shutdown();
        }

        assertThat(liveFile).content(UTF_8)
                .containsOnlyOnce("\"_id1\"")
                .containsOnlyOnce("\"_id2\"")
                .containsOnlyOnce("\"_id3\"");
    }

    @Test
    public void testRestartLetsOtherTopicsPublishWhenTamperEvidentKeyStoreCannotBeStored() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder builder =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled();
        // A second topic, so that a topic is left to open after the first one fails.
        final Map<String, JsonValue> topics = new LinkedHashMap<>();
        topics.put("access", builder.eventTopicsMetaData.getSchema("access"));
        topics.put("activity", builder.eventTopicsMetaData.getSchema("access"));
        final EventTopicsMetaData eventTopicsMetaData = new EventTopicsMetaData(topics);
        builder.config.setTopics(eventTopicsMetaData.getTopics());
        final CsvAuditEventHandler csvHandler =
                new CsvAuditEventHandler(builder.config, eventTopicsMetaData, builder.keystoreHandlerProvider);
        final Context context = new RootContext();
        csvHandler.startup();
        for (String topic : topics.keySet()) {
            csvHandler.publishEvent(context, topic, buildEvent(1)).getOrThrow();
        }
        csvHandler.shutdown();
        // The topic that startup opens first, so that the other topic is opened only if startup goes on.
        final List<String> handledTopics = new ArrayList<>(csvHandler.getHandledTopics());
        final String failingTopic = handledTopics.get(0);
        final String otherTopic = handledTopics.get(1);
        // Without its file, the writer starts a new one, and stores its first keys into a keystore that it loads but
        // cannot write. A file that cannot be resumed, or a keystore that cannot be loaded, fails with an IOException
        // (#341), which startup() reports for the topic; a keystore that cannot be stored is still a RuntimeException.
        Files.delete(logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + failingTopic + ".csv"));
        final File failingKeyStore =
                logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + failingTopic + ".csv.keystore").toFile();
        if (!failingKeyStore.setWritable(false) || failingKeyStore.canWrite()) {
            // As root, a file without the write permission can still be written.
            throw new SkipException("The keystore of the CSV file cannot be made read-only");
        }

        try {
            try {
                csvHandler.startup();
                fail("startup() does not throw for a keystore that the writer cannot store");
            } catch (RuntimeException e) {
                // The writer throws it for the keys that it cannot store (#340), and startup() opens no other topic.
            }
            // The handler is no longer shut down: the topics that startup() did not open open at their next event.
            csvHandler.publishEvent(context, otherTopic, buildEvent(2)).getOrThrow();
        } finally {
            csvHandler.shutdown();
            failingKeyStore.setWritable(true);
        }

        assertThat(logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + otherTopic + ".csv").toFile())
                .content(UTF_8).containsOnlyOnce("\"_id2\"");
    }

    @Test
    public void testPublishEventOpensWriterWhenTopicIsLeftWithoutOne() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        final Map<String, CsvWriter> writers = writersOf(csvHandler);
        final CsvWriter failingWriter = mock(CsvWriter.class);
        // As when another publisher resets this writer meanwhile, and fails to open a new one: the topic is left
        // without a writer.
        doAnswer(invocation -> {
            writers.remove("access", failingWriter);
            throw new IOException("Stream closed");
        }).when(failingWriter).writeEvent(anyMap());
        writers.put("access", failingWriter).close();
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
        } finally {
            csvHandler.shutdown();
        }

        assertThat(logDirectory.resolve("access.csv").toFile()).content(UTF_8).containsOnlyOnce("\"_id1\"");
    }

    @Test
    public void testPublishEventDuringStartupResetsWriterOfReopenedTopic() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        csvHandler.shutdown();
        final CsvWriter failingWriter = mock(CsvWriter.class);
        doThrow(new IOException("Stream closed")).when(failingWriter).writeEvent(anyMap());
        // As when startup() has reopened the topic, and has not returned yet: the handler is still shut down.
        writersOf(csvHandler).put("access", failingWriter);
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
        } finally {
            csvHandler.shutdown();
        }

        assertThat(logDirectory.resolve("access.csv").toFile()).content(UTF_8).containsOnlyOnce("\"_id1\"");
    }

    @Test
    public void testStartupClosesTheWriterThatItReplaces() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final CsvWriter openedWriter = mock(CsvWriter.class);
        // As when an event opened it after a startup() that threw, and startup() is called again.
        writersOf(csvHandler).put("access", openedWriter).close();
        try {
            csvHandler.startup();
        } finally {
            csvHandler.shutdown();
        }

        verify(openedWriter).close();
    }

    @Test
    public void testActionsThatTheRequestGetsWrongAreBadRequests() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        final ActionRequest rotateRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        final ActionRequest unknownRequest = Requests.newActionRequest("access", "unknown")
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        try {
            // The same request would fail again: no topic, an unknown action, an unknown topic.
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, null, rotateRequest))
                    .failedWithException()
                    .isInstanceOf(BadRequestException.class);
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "access", unknownRequest))
                    .failedWithException()
                    .isInstanceOf(BadRequestException.class);
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "unknown", rotateRequest))
                    .failedWithException()
                    .isInstanceOf(BadRequestException.class);
        } finally {
            csvHandler.shutdown();
        }
    }

    @Test
    public void testActionToRotateFileThatFailsIsServerError() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandler csvHandler =
                csvAuditEventHandler().loggingTo(logDirectory).withRotationEnabled().build();
        final Context context = new RootContext();
        final ActionRequest rotateRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        final Map<String, CsvWriter> writers = writersOf(csvHandler);
        // A writer whose file cannot be written, and one whose file cannot be renamed.
        final CsvWriter failingWriter = mock(CsvWriter.class);
        when(failingWriter.forceRotation()).thenThrow(new IOException("No space left on device"));
        final CsvWriter notRotatingWriter = mock(CsvWriter.class);
        when(notRotatingWriter.forceRotation()).thenReturn(false);
        writers.put("access", failingWriter).close();
        try {
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "access", rotateRequest))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
            writers.put("access", notRotatingWriter);
            // Not wrapped into the failure of forceRotation(): a ResourceException is an IOException.
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "access", rotateRequest))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessage("Unable to rotate file for topic: access");
        } finally {
            csvHandler.shutdown();
        }
    }

    /** The writers of the handler, so that a test can put a writer that fails as it needs. */
    @SuppressWarnings("unchecked")
    private static Map<String, CsvWriter> writersOf(final CsvAuditEventHandler handler) throws Exception {
        final Field field = CsvAuditEventHandler.class.getDeclaredField("writers");
        field.setAccessible(true);
        return (Map<String, CsvWriter>) field.get(handler);
    }

    @DataProvider
    private Object[][] rotateActionData() {
        return new Object[][] {
                // label, is rotation enabled ?
                { "rotation enabled", true },
                { "rotation not enabled", false }
        };
    }

    @Test(dataProvider = "rotateActionData")
    public void testActionToRotateFile(String label, boolean isRotationEnabled) throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder = csvAuditEventHandler().loggingTo(logDirectory);
        if (isRotationEnabled) {
            csvAuditEventHandlerBuilder.withRotationEnabled();
        }
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();

        final Context context = new RootContext();
        try {
            final JsonValue content = json(object(
                    field("_id", "1"),
                    field("timestamp", "123456"),
                    field("transactionId", "A10000")));
            csvHandler.publishEvent(context, "access", Requests.newCreateRequest("access", content).getContent());

            // delete file action should remove the file and create a fresh one
            ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                    .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
            csvHandler.handleAction(context, "access", actionRequest);
        } finally {
            csvHandler.shutdown();
        }

        assertThat(logDirectory.resolve("access.csv").toFile()).as("when " + label + " in " + logDirectory.toString())
            .hasContent("\"_id\",\"timestamp\",\"transactionId\"\n");
        // the default rotation file suffix ends with .gz
        final File[] rotatedFiles = logDirectory.toFile().listFiles((dir, name) -> !name.equals("access.csv"));
        assertThat(rotatedFiles).as("when " + label).hasSize(1);
        rotatedFiles[0].deleteOnExit();
        assertThat(rotatedFiles[0].getName()).endsWith(".gz");
        try (InputStream in = new GZIPInputStream(new FileInputStream(rotatedFiles[0]))) {
            assertThat(new String(in.readAllBytes(), UTF_8)).as("when " + label).contains("\"A10000\"");
        }
    }

    @Test(dataProvider = "rotateActionData")
    public void testActionToRotateTamperEvidentFile(String label, boolean isRotationEnabled) throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled();
        if (isRotationEnabled) {
            csvAuditEventHandlerBuilder.withRotationEnabled();
        }
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();

        final Context context = new RootContext();
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1));
            ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                    .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
            csvHandler.handleAction(context, "access", actionRequest);
        } finally {
            csvHandler.shutdown();
        }

        // the archive is verified with its own keystore, as the CLI verifies it
        final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
        final String archiveKeyStorePassword = Base64.encode(
                keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        final File liveFile = logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + "access.csv").toFile();
        // with the prefix that withRotationEnabled() configures
        final FileNamingPolicy fileNamingPolicy =
                new TimeStampFileNamingPolicy(liveFile, null, isRotationEnabled ? "prefix" : null);
        final List<VerificationResult> verificationResults = new CsvSecureArchiveVerifier(fileNamingPolicy,
                archiveKeyStorePassword, keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE),
                CsvPreference.EXCEL_PREFERENCE).verify();
        assertThat(verificationResults).as("when " + label).hasSize(1);
        final VerificationResult verificationResult = verificationResults.get(0);
        // the default rotation file suffix ends with .gz
        assertThat(verificationResult.getArchiveFile().getName()).as("when " + label).endsWith(".gz");
        assertThat(verificationResult.hasPassedVerification())
                .as("when " + label + ": " + verificationResult.getFailureReason())
                .isTrue();
    }

    @Test
    public void testActionToRotateTamperEvidentFileKeepsFileWhenKeyStoreCannotBeRenamed() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final File liveFile = logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + "access.csv").toFile();
        final CsvAuditEventHandler csvHandler =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled().build();

        final Context context = new RootContext();
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1));
            // A file cannot be renamed onto a directory. The archive name has the current second in the default
            // rotation suffix, so the keystore name of every archive name of the next 30 s is taken by one.
            final DateTimeFormatter rotationSuffix = DateTimeFormat.forPattern("-yyyy.MM.dd-HH.mm.ss");
            final LocalDateTime now = LocalDateTime.now();
            for (int second = -1; second <= 30; second++) {
                Files.createDirectory(logDirectory.resolve(
                        liveFile.getName() + now.plusSeconds(second).toString(rotationSuffix) + ".gz.keystore"));
            }
            ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                    .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
            AssertJPromiseAssert.assertThat(csvHandler.handleAction(context, "access", actionRequest))
                    .failedWithException()
                    .isInstanceOf(InternalServerErrorException.class);
            csvHandler.publishEvent(context, "access", buildEvent(2));
        } finally {
            csvHandler.shutdown();
        }

        // the audit file is not archived, and the writer resumed it with its keys
        assertThat(logDirectory.toFile().listFiles((dir, name) -> !name.endsWith(".keystore")))
                .containsExactly(liveFile);
        assertThat(liveFile).content(UTF_8).contains("\"_id1\"", "\"_id2\"");
        final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
        final KeyStoreHandler liveKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                CsvSecureUtils.keyStoreFile(liveFile).getPath(), Base64.encode(
                        keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded()));
        final VerificationResult verificationResult = new CsvSecureVerifier(liveFile, CsvPreference.EXCEL_PREFERENCE,
                new KeyStoreSecureStorage(liveKeyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE))).verify();
        assertThat(verificationResult.hasPassedVerification())
                .as(verificationResult.getFailureReason())
                .isTrue();
    }

    @Test(dataProvider = "rotateActionData")
    public void testPublishEventKeepsTamperEvidentWriterWhenKeyStoreCannotBeStored(String label,
            boolean isRotationEnabled) throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder =
                csvAuditEventHandler().loggingTo(logDirectory).withSecureLoggingEnabled();
        if (isRotationEnabled) {
            csvAuditEventHandlerBuilder.withRotationEnabled();
        }
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();
        final File liveFile = logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + "access.csv").toFile();
        final File keyStoreFile = CsvSecureUtils.keyStoreFile(liveFile);
        final File movedKeyStoreFile = new File(keyStoreFile.getPath() + ".moved");
        final Context context = new RootContext();
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1)).getOrThrow();
            // A keystore cannot be stored over a directory: the row of the event is written, and its key is not stored.
            assertThat(keyStoreFile.renameTo(movedKeyStoreFile)).isTrue();
            assertThat(keyStoreFile.mkdir()).isTrue();
            csvHandler.publishEvent(context, "access", buildEvent(2)).getOrThrow();
            assertThat(keyStoreFile.delete()).isTrue();
            assertThat(movedKeyStoreFile.renameTo(keyStoreFile)).isTrue();
            // the writer stores the key with the next row
            csvHandler.publishEvent(context, "access", buildEvent(3)).getOrThrow();
        } finally {
            csvHandler.shutdown();
        }

        assertThat(liveFile).as("when " + label).content(UTF_8)
                .containsOnlyOnce("\"_id1\"")
                .containsOnlyOnce("\"_id2\"")
                .containsOnlyOnce("\"_id3\"");
        final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
        final KeyStoreHandler liveKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                keyStoreFile.getPath(), Base64.encode(
                        keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded()));
        final VerificationResult verificationResult = new CsvSecureVerifier(liveFile, CsvPreference.EXCEL_PREFERENCE,
                new KeyStoreSecureStorage(liveKeyStoreHandler,
                        keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE))).verify();
        assertThat(verificationResult.hasPassedVerification())
                .as("when " + label + ": " + verificationResult.getFailureReason())
                .isTrue();
    }

    @Test
    public void testPublishEventKeepsTamperEvidentWriterWhenRotationAfterItFails() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        // A file that holds an event is rotated at the write of the next event, and a new file that holds only the
        // header and the last signature is not.
        final FileRotation fileRotation = new FileRotation();
        fileRotation.setRotationEnabled(true);
        fileRotation.setRotationFilePrefix("prefix");
        fileRotation.setRotationInterval("disabled");
        fileRotation.setMaxFileSize(1000);
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder = csvAuditEventHandler()
                .loggingTo(logDirectory).withSecureLoggingEnabled().withRotationConfig(fileRotation);
        // the file is rotated by the writes only
        csvAuditEventHandlerBuilder.config.setRotationRetentionCheckInterval("1 hour");
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();
        final File liveFile = logDirectory.resolve(SECURE_CSV_FILENAME_PREFIX + "access.csv").toFile();
        final File keyStoreFile = CsvSecureUtils.keyStoreFile(liveFile);
        final File movedKeyStoreFile = new File(keyStoreFile.getPath() + ".moved");
        final String transactionId = new String(new char[1000]).replace('\0', 'x');
        final Context context = new RootContext();
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1).put("transactionId", transactionId)).getOrThrow();
            // The rotation signs the file before it renames it, and the signature fails when the keystore cannot be
            // stored over a directory: the rotation fails after the row of the event is written.
            assertThat(keyStoreFile.renameTo(movedKeyStoreFile)).isTrue();
            assertThat(keyStoreFile.mkdir()).isTrue();
            csvHandler.publishEvent(context, "access", buildEvent(2).put("transactionId", transactionId)).getOrThrow();
            assertThat(keyStoreFile.delete()).isTrue();
            assertThat(movedKeyStoreFile.renameTo(keyStoreFile)).isTrue();
            // the write of the next event rotates the file
            csvHandler.publishEvent(context, "access", buildEvent(3).put("transactionId", transactionId)).getOrThrow();
        } finally {
            csvHandler.shutdown();
        }

        // the archive is verified with its own keystore, as the CLI verifies it
        final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
        final String archiveKeyStorePassword = Base64.encode(
                keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
        final List<VerificationResult> verificationResults = new CsvSecureArchiveVerifier(
                new TimeStampFileNamingPolicy(liveFile, null, "prefix"), archiveKeyStorePassword,
                keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE),
                CsvPreference.EXCEL_PREFERENCE).verify();
        assertThat(verificationResults).hasSize(1);
        final VerificationResult verificationResult = verificationResults.get(0);
        assertThat(verificationResult.hasPassedVerification())
                .as(verificationResult.getFailureReason())
                .isTrue();
        // every event is written once, to the archive
        try (InputStream in = new GZIPInputStream(new FileInputStream(verificationResult.getArchiveFile()))) {
            assertThat(new String(in.readAllBytes(), UTF_8))
                    .containsOnlyOnce("\"_id1\"")
                    .containsOnlyOnce("\"_id2\"")
                    .containsOnlyOnce("\"_id3\"");
        }
        assertThat(liveFile).content(UTF_8).doesNotContain("\"_id1\"", "\"_id2\"", "\"_id3\"");
    }

    @Test
    public void testActionToRotateFileCompressesOutsideWriteLock() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        final CsvAuditEventHandler csvHandler = csvAuditEventHandler().loggingTo(logDirectory).build();
        final Context context = new RootContext();
        final ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            csvHandler.publishEvent(context, "access", buildEvent());
            // random bytes compress slowly, so compressing this file takes far longer than a rotation
            final byte[] chunk = new byte[1024 * 1024];
            final Random random = new Random(0);
            try (OutputStream out = Files.newOutputStream(logDirectory.resolve("access.csv"), APPEND)) {
                for (int i = 0; i < 16; i++) {
                    random.nextBytes(chunk);
                    out.write(chunk);
                }
            }

            final Future<?> firstRotation =
                    executor.submit(() -> csvHandler.handleAction(context, "access", actionRequest).getOrThrow());
            final File renamedFile = GzipUtil.removeGzipSuffix(awaitArchive(logDirectory));
            if (!renamedFile.exists()) {
                throw new SkipException("The rotated file was compressed before the test could observe it");
            }
            // the second rotation takes the write lock, so it would wait for a compression running under it
            csvHandler.handleAction(context, "access", actionRequest).getOrThrow();
            assertThat(renamedFile).as("rotated file still being compressed").exists();
            firstRotation.get();

            // the second rotation does not take the name of the file still being compressed
            final File[] rotatedFiles = logDirectory.toFile().listFiles((dir, name) -> !name.equals("access.csv"));
            assertThat(rotatedFiles).hasSize(2);
            assertThat(rotatedFiles).allMatch(file -> file.getName().endsWith(".gz"));
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            csvHandler.shutdown();
            // the files are large, so do not leave them until the JVM exits
            for (final File file : logDirectory.toFile().listFiles()) {
                file.delete();
            }
            logDirectory.toFile().delete();
        }
    }

    /** Waits until the only rotated file appears under its archive name, and returns it. */
    private static File awaitArchive(final Path logDirectory) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (System.currentTimeMillis() < deadline) {
            final File[] archives = logDirectory.toFile().listFiles((dir, name) -> name.endsWith(".gz"));
            if (archives.length > 0) {
                return archives[0];
            }
            Thread.sleep(1);
        }
        throw new AssertionError("No archive in " + logDirectory);
    }

    @DataProvider
    private Object[][] tamperEvidentData() {
        return new Object[][] {
                // label, is tamper evident logging enabled ?
                { "standard", false },
                { "tamper evident", true }
        };
    }

    @Test(dataProvider = "tamperEvidentData", timeOut = 60_000)
    public void testActionToRotateFileKeepsEventBeingPublished(String label, boolean isTamperEvident)
            throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder csvAuditEventHandlerBuilder = csvAuditEventHandler().loggingTo(logDirectory);
        if (isTamperEvident) {
            csvAuditEventHandlerBuilder.withSecureLoggingEnabled();
        }
        final CsvAuditEventHandler csvHandler = csvAuditEventHandlerBuilder.build();
        final Context context = new RootContext();
        final ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        final BlockingValue transactionId = new BlockingValue("transactionId-X");
        try {
            csvHandler.publishEvent(context, "access", buildEvent(1));
            // the handler formats the event once it has the writer of the topic, and before it writes to it
            final FutureTask<ResourceResponse> publication = new FutureTask<>(() -> csvHandler.publishEvent(context,
                    "access", json(object(field("_id", "_id2"), field("timestamp", "timestamp"),
                            field("transactionId", transactionId)))).getOrThrow());
            final Thread publisher = new Thread(publication, "publisher");
            publisher.setDaemon(true);
            publisher.start();
            transactionId.awaitSerialization();

            final FutureTask<ActionResponse> rotation =
                    new FutureTask<>(() -> csvHandler.handleAction(context, "access", actionRequest).getOrThrow());
            final Thread rotator = new Thread(rotation, "rotator");
            rotator.setDaemon(true);
            rotator.start();
            // the rotation either closes the writer, or waits for the event being written to it
            awaitTerminationOrReadWriteLock(rotator);
            transactionId.release();
            publication.get(10, TimeUnit.SECONDS);
            rotation.get(10, TimeUnit.SECONDS);
            csvHandler.publishEvent(context, "access", buildEvent(3));
        } finally {
            transactionId.release();
            csvHandler.shutdown();
        }

        // every event is written once, to the archive or to the new file
        final File liveFile =
                logDirectory.resolve((isTamperEvident ? SECURE_CSV_FILENAME_PREFIX : "") + "access.csv").toFile();
        final File[] archives = logDirectory.toFile().listFiles((dir, name) -> name.endsWith(".gz"));
        assertThat(archives).as("when " + label).hasSize(1);
        final String content;
        try (InputStream in = new GZIPInputStream(new FileInputStream(archives[0]))) {
            content = new String(in.readAllBytes(), UTF_8) + new String(Files.readAllBytes(liveFile.toPath()), UTF_8);
        }
        assertThat(content).as("when " + label)
                .containsOnlyOnce("\"_id1\"")
                .containsOnlyOnce("\"_id2\"")
                .containsOnlyOnce("\"_id3\"");
        if (isTamperEvident) {
            // the archive and the new file are verified with their own keystores
            final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                    new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
            final String keyStorePassword = Base64.encode(
                    keyStoreHandler.readSecretKeyFromKeyStore(CsvSecureConstants.ENTRY_PASSWORD).getEncoded());
            final PublicKey publicKey = keyStoreHandler.readPublicKeyFromKeyStore(CsvSecureConstants.ENTRY_SIGNATURE);
            final List<VerificationResult> archiveResults = new CsvSecureArchiveVerifier(
                    new TimeStampFileNamingPolicy(liveFile, null, null), keyStorePassword, publicKey,
                    CsvPreference.EXCEL_PREFERENCE).verify();
            assertThat(archiveResults).hasSize(1);
            assertThat(archiveResults.get(0).hasPassedVerification())
                    .as("archive: " + archiveResults.get(0).getFailureReason())
                    .isTrue();
            final KeyStoreHandler liveKeyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE,
                    CsvSecureUtils.keyStoreFile(liveFile).getPath(), keyStorePassword);
            final VerificationResult liveResult = new CsvSecureVerifier(liveFile, CsvPreference.EXCEL_PREFERENCE,
                    new KeyStoreSecureStorage(liveKeyStoreHandler, publicKey)).verify();
            assertThat(liveResult.hasPassedVerification())
                    .as("new file: " + liveResult.getFailureReason())
                    .isTrue();
        }
    }

    @Test(timeOut = 60_000)
    public void testActionToRotateFileLetsOtherTopicsPublish() throws Exception {
        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        final CsvAuditEventHandlerBuilder builder = csvAuditEventHandler().loggingTo(logDirectory);
        // A second topic, to publish to while the file of the first one rotates.
        final Map<String, JsonValue> topics = new LinkedHashMap<>();
        topics.put("access", builder.eventTopicsMetaData.getSchema("access"));
        topics.put("activity", builder.eventTopicsMetaData.getSchema("access"));
        final EventTopicsMetaData eventTopicsMetaData = new EventTopicsMetaData(topics);
        builder.config.setTopics(eventTopicsMetaData.getTopics());
        final CsvAuditEventHandler csvHandler =
                new CsvAuditEventHandler(builder.config, eventTopicsMetaData, builder.keystoreHandlerProvider);
        csvHandler.startup();
        final Context context = new RootContext();
        final ActionRequest actionRequest = Requests.newActionRequest("access", ROTATE_FILE_ACTION_NAME)
                .setAdditionalParameter(ACTION_PARAM_TARGET_HANDLER, "csv");
        final BlockingValue transactionId = new BlockingValue("transactionId-X");
        try {
            final FutureTask<ResourceResponse> publication = new FutureTask<>(() -> csvHandler.publishEvent(context,
                    "access", json(object(field("_id", "_id1"), field("timestamp", "timestamp"),
                            field("transactionId", transactionId)))).getOrThrow());
            final Thread publisher = new Thread(publication, "publisher");
            publisher.setDaemon(true);
            publisher.start();
            transactionId.awaitSerialization();

            final FutureTask<ActionResponse> rotation =
                    new FutureTask<>(() -> csvHandler.handleAction(context, "access", actionRequest).getOrThrow());
            final Thread rotator = new Thread(rotation, "rotator");
            rotator.setDaemon(true);
            rotator.start();
            awaitTerminationOrReadWriteLock(rotator);
            assertThat(rotator.isAlive()).as("rotation waits for the event being written").isTrue();

            // an event of another topic is written while the rotation waits
            final FutureTask<ResourceResponse> otherPublication = new FutureTask<>(
                    () -> csvHandler.publishEvent(context, "activity", buildEvent(2)).getOrThrow());
            final Thread otherPublisher = new Thread(otherPublication, "other publisher");
            otherPublisher.setDaemon(true);
            otherPublisher.start();
            otherPublication.get(10, TimeUnit.SECONDS);
            assertThat(rotator.isAlive()).as("rotation waits for the event being written").isTrue();

            transactionId.release();
            publication.get(10, TimeUnit.SECONDS);
            rotation.get(10, TimeUnit.SECONDS);
        } finally {
            transactionId.release();
            csvHandler.shutdown();
        }
        assertThat(new String(Files.readAllBytes(logDirectory.resolve("activity.csv")), UTF_8))
                .containsOnlyOnce("\"_id2\"");
    }

    /** Waits until the thread terminates, or waits for a read-write lock. */
    private static void awaitTerminationOrReadWriteLock(final Thread thread) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (thread.isAlive() && !isWaitingForReadWriteLock(thread) && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertThat(!thread.isAlive() || isWaitingForReadWriteLock(thread))
                .as(thread.getName() + " terminates or waits for a read-write lock")
                .isTrue();
    }

    private static boolean isWaitingForReadWriteLock(final Thread thread) {
        final Object blocker = LockSupport.getBlocker(thread);
        return blocker != null && blocker.getClass().getEnclosingClass() == ReentrantReadWriteLock.class;
    }

    /** A value whose serialization, when the handler formats an event, waits until the value is released. */
    private static final class BlockingValue implements JsonSerializable {
        private final String value;
        private final CountDownLatch serialization = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private BlockingValue(final String value) {
            this.value = value;
        }

        void awaitSerialization() throws InterruptedException {
            assertThat(serialization.await(10, TimeUnit.SECONDS)).as("serialization started").isTrue();
        }

        void release() {
            release.countDown();
        }

        @Override
        public void serialize(final JsonGenerator generator, final SerializerProvider serializers)
                throws IOException {
            serialization.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
            generator.writeString(value);
        }

        @Override
        public void serializeWithType(final JsonGenerator generator, final SerializerProvider serializers,
                final TypeSerializer typeSerializer) throws IOException {
            serialize(generator, serializers);
        }
    }

    @Test
    public void testNewCsvFileContainsHeadersAfterRotation() throws Exception {
        // configure CSV handler to perform rotation whenever the file-size exceeds 50 bytes, then write
        // several events to the file. Really, a single event should be sufficient to require rotation but
        // MeteredStream is wrapped with BufferedWriter so it may not give an accurate size.

        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        FileRotation rotationConfig = new FileRotation();
        rotationConfig.setRotationEnabled(true);
        rotationConfig.setRotationFilePrefix("prefix");
        rotationConfig.setRotationInterval("disabled");
        rotationConfig.setMaxFileSize(50);

        final CsvAuditEventHandler csvHandler = csvAuditEventHandler()
                .loggingTo(logDirectory).withRotationConfig(rotationConfig).build();

        assertThatFileRotationAddsCsvHeadersToStartOfNewFiles(logDirectory, csvHandler);
    }

    @Test
    public void testNewCsvFileContainsHeadersAfterRotationWithBufferingEnabled() throws Exception {
        // configure CSV handler to perform rotation whenever the file-size exceeds 50 bytes, then write
        // several events to the file. Really, a single event should be sufficient to require rotation but
        // MeteredStream is wrapped with BufferedWriter so it may not give an accurate size.

        final Path logDirectory = Files.createTempDirectory("CsvAuditEventHandlerTest");
        logDirectory.toFile().deleteOnExit();
        FileRotation rotationConfig = new FileRotation();
        rotationConfig.setRotationEnabled(true);
        rotationConfig.setRotationFilePrefix("prefix");
        rotationConfig.setRotationInterval("disabled");
        rotationConfig.setMaxFileSize(50);

        final CsvAuditEventHandler csvHandler = csvAuditEventHandler()
                .loggingTo(logDirectory).withRotationConfig(rotationConfig).withBufferingEnabled().build();

        assertThatFileRotationAddsCsvHeadersToStartOfNewFiles(logDirectory, csvHandler);
    }

    private void assertThatFileRotationAddsCsvHeadersToStartOfNewFiles(
            Path logDirectory, CsvAuditEventHandler csvHandler) throws Exception {
        final Context context = new RootContext();
        try {
            csvHandler.publishEvent(context, "access", Requests.newCreateRequest("access", buildEvent(1)).getContent());
            csvHandler.publishEvent(context, "access", Requests.newCreateRequest("access", buildEvent(2)).getContent());
            csvHandler.publishEvent(context, "access", Requests.newCreateRequest("access", buildEvent(3)).getContent());
            csvHandler.publishEvent(context, "access", Requests.newCreateRequest("access", buildEvent(4)).getContent());
            csvHandler.publishEvent(context, "access", Requests.newCreateRequest("access", buildEvent(5)).getContent());
        } finally {
            csvHandler.shutdown();
        }

        List<String> linesInCurrentFile = Files.readAllLines(logDirectory.resolve("access.csv"), UTF_8);
        if (linesInCurrentFile.size() > 1) {
            assertThat(linesInCurrentFile.get(1))
                    .describedAs("First file has been rotated")
                    .doesNotStartWith("_id1");
        }
        assertThat(linesInCurrentFile.get(0))
                .describedAs("First line in rotated file should contain CSV column headers")
                .isEqualTo("\"_id\",\"timestamp\",\"transactionId\"");
    }

    private static class CsvAuditEventHandlerBuilder {
        final CsvAuditEventHandlerConfiguration config;
        final EventTopicsMetaData eventTopicsMetaData;
        KeyStoreHandlerProvider keystoreHandlerProvider;

        private CsvAuditEventHandlerBuilder() throws Exception {
            this.eventTopicsMetaData = getEventTopicsMetaData();
            this.config = new CsvAuditEventHandlerConfiguration();
            config.setName("csv");
            config.setTopics(eventTopicsMetaData.getTopics());
            keystoreHandlerProvider = new DefaultKeyStoreHandlerProvider();
        }

        private CsvAuditEventHandlerBuilder loggingTo(Path directory) {
            config.setLogDirectory(directory.toString());
            return this;
        }

        private CsvAuditEventHandlerBuilder withSecureLoggingEnabled() throws Exception {
            CsvAuditEventHandlerConfiguration.CsvSecurity csvSecurity =
                    new CsvAuditEventHandlerConfiguration.CsvSecurity();
            csvSecurity.setEnabled(true);
            csvSecurity.setKeyStoreHandlerName("csvSecure");
            // the close of the writer signs its last rows
            csvSecurity.setSignatureInterval("5 minutes");
            config.setSecurity(csvSecurity);
            keystoreHandlerProvider = setupKeyStoreHandlerProvider();
            return this;
        }

        private EventTopicsMetaData getEventTopicsMetaData() throws Exception {
            Map<String, JsonValue> events = new LinkedHashMap<>();
            try (final InputStream configStream = getClass().getResourceAsStream("/events.json")) {
                final JsonValue predefinedEventTypes = json(new ObjectMapper().readValue(configStream, Map.class));
                for (String eventTypeName : predefinedEventTypes.keys()) {
                    events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
                }
            }
            return new EventTopicsMetaData(events);
        }

        private CsvAuditEventHandlerBuilder withBufferingEnabled() {
            EventBufferingConfiguration conf = new EventBufferingConfiguration();
            conf.setEnabled(true);
            config.setBufferingConfiguration(conf);
            return this;
        }

        private CsvAuditEventHandlerBuilder withRotationEnabled() {
            FileRotation fileRotation = new FileRotation();
            fileRotation.setRotationEnabled(true);
            fileRotation.setRotationFilePrefix("prefix");
            fileRotation.setRotationInterval("disabled");
            fileRotation.setMaxFileSize(100000);
            config.setFileRotation(fileRotation);
            return this;
        }

        private CsvAuditEventHandlerBuilder withRotationConfig(FileRotation fileRotation) {
            config.setFileRotation(fileRotation);
            return this;
        }

        private CsvAuditEventHandler build() throws ResourceException {
            CsvAuditEventHandler handler = new CsvAuditEventHandler(config, eventTopicsMetaData,
                    keystoreHandlerProvider);
            handler.startup();
            return handler;
        }

        private KeyStoreHandlerProvider setupKeyStoreHandlerProvider() throws Exception {
            DefaultKeyStoreHandlerProvider provider = new DefaultKeyStoreHandlerProvider();
            final KeyStoreHandlerDecorator keyStoreHandler = new KeyStoreHandlerDecorator(
                    new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, KEYSTORE_FILENAME, KEYSTORE_PASSWORD));
            provider.registerKeyStoreHandler("csvSecure", keyStoreHandler);

            return provider;
        }

    }
    private static CsvAuditEventHandlerBuilder csvAuditEventHandler() throws Exception {
        return new CsvAuditEventHandlerBuilder();
    }

}
