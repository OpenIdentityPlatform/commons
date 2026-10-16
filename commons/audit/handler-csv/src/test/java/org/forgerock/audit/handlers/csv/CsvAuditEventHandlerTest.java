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
import static org.forgerock.audit.AuditServiceBuilder.*;
import static org.forgerock.audit.AuditServiceProxy.*;
import static org.forgerock.audit.handlers.csv.CsvAuditEventHandler.*;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.KEYSTORE_PASSWORD;
import static org.forgerock.audit.handlers.csv.SecureCsvWriterTest.assertThreadsTerminate;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration.FileRotation;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.audit.providers.DefaultKeyStoreHandlerProvider;
import org.forgerock.audit.providers.KeyStoreHandlerProvider;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandlerDecorator;
import org.forgerock.audit.util.GzipUtil;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.mockito.ArgumentCaptor;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Test
    public void testActionToRotateFileCompressesOutsideHandlerMonitor() throws Exception {
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
            // the second rotation takes the handler monitor, so it would wait for a compression running under it
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
