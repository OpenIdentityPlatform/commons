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
 * Portions copyright 2024-2026 3A Systems LLC.
 */
package org.forgerock.audit.handlers.csv;

import static java.lang.String.format;
import static org.forgerock.audit.events.AuditEventHelper.ARRAY_TYPE;
import static org.forgerock.audit.events.AuditEventHelper.OBJECT_TYPE;
import static org.forgerock.audit.events.AuditEventHelper.dotNotationToJsonPointer;
import static org.forgerock.audit.events.AuditEventHelper.getAuditEventProperties;
import static org.forgerock.audit.events.AuditEventHelper.getAuditEventSchema;
import static org.forgerock.audit.events.AuditEventHelper.getPropertyType;
import static org.forgerock.audit.events.AuditEventHelper.jsonPointerToDotNotation;
import static org.forgerock.audit.util.JsonSchemaUtils.generateJsonPointers;
import static org.forgerock.audit.util.JsonValueUtils.JSONVALUE_FILTER_VISITOR;
import static org.forgerock.audit.util.JsonValueUtils.expand;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.inject.Inject;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.CsvSecurity;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.providers.KeyStoreHandlerProvider;
import org.forgerock.audit.retention.TimeStampFileNamingPolicy;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.audit.util.GzipUtil;
import org.forgerock.audit.util.JsonValueUtils;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.util.CsvContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles AuditEvents by writing them to a CSV file.
 */
public class CsvAuditEventHandler extends AuditEventHandlerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvAuditEventHandler.class);

    /** Name of action to force file rotation. */
    public static final String ROTATE_FILE_ACTION_NAME = "rotate";

    static final String SECURE_CSV_FILENAME_PREFIX = "tamper-evident-";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Random RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final CsvAuditEventHandlerConfiguration configuration;
    private final CsvPreference csvPreference;
    private final ConcurrentMap<String, CsvWriter> writers = new ConcurrentHashMap<>();
    /**
     * Guards the writer of each topic in {@link #writers}: the writer is used under the read lock of its topic, and
     * opened, closed and replaced under the write lock, so that it is not closed while an event is written to it or
     * while it rotates its file. The methods that open, close or replace a writer expect the caller to hold that write
     * lock. A lock per topic lets the other topics publish while the writer of one topic is replaced.
     */
    private final Map<String, ReadWriteLock> writerLockByTopic;
    /**
     * Set by {@link #shutdown()} before it closes the writers, and cleared by {@link #startup()} once it has opened
     * them. Meanwhile, a topic left without a writer gets no new one, so that its events fail instead of being written
     * to a writer that nothing closes.
     */
    private volatile boolean shutDown;
    private final Map<String, Set<String>> fieldOrderByTopic;
    /** Caches a JSON pointer for each field. */
    private final Map<String, JsonPointer> jsonPointerByField;
    /** Caches the dot notation for each field. */
    private final Map<String, String> fieldDotNotationByField;
    private KeyStoreHandler keyStoreHandler;

    /**
     * Create a new CsvAuditEventHandler instance.
     *
     * @param configuration
     *          Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData
     *          Meta-data for all audit event topics.
     * @param keyStoreHandlerProvider
     *          The secure storage to use for keys.
     */
    @Inject
    public CsvAuditEventHandler(
            final CsvAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit KeyStoreHandlerProvider keyStoreHandlerProvider) {

        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.configuration = configuration;
        this.csvPreference = createCsvPreference(this.configuration);
        CsvSecurity security = configuration.getSecurity();
        if (security.isEnabled()) {
            Duration duration = security.getSignatureIntervalDuration();
            Reject.ifTrue(duration.isZero() || duration.isUnlimited(),
                    "The signature interval can't be zero or unlimited");

            if (security.getKeyStoreHandlerName() != null) {
                this.keyStoreHandler = keyStoreHandlerProvider.getKeystoreHandler(security.getKeyStoreHandlerName());
            } else {
                try {
                    keyStoreHandler = new JcaKeyStoreHandler(CsvSecureConstants.KEYSTORE_TYPE, security.getFilename(),
                            security.getPassword());
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Unable to create secure storage from file: " + security.getFilename(), e);
                }
            }
        }

        Map<String, ReadWriteLock> lockByTopic = new HashMap<>();
        Map<String, Set<String>> orderByTopic = new HashMap<>();
        Map<String, JsonPointer> pointerByField = new HashMap<>();
        Map<String, String> dotNotationByField = new HashMap<>();
        for (String topic : this.eventTopicsMetaData.getTopics()) {
            lockByTopic.put(topic, new ReentrantReadWriteLock());
            try {
                Set<String> fieldOrder = getFieldOrder(topic, this.eventTopicsMetaData);
                for (String field : fieldOrder) {
                    if (!pointerByField.containsKey(field)) {
                        pointerByField.put(field, new JsonPointer(field));
                        dotNotationByField.put(field, jsonPointerToDotNotation(field));
                    }
                }
                orderByTopic.put(topic, Collections.unmodifiableSet(fieldOrder));
            } catch (ResourceException e) {
                LOGGER.error(topic + " topic schema meta-data misconfigured.");
            }
        }
        this.writerLockByTopic = Collections.unmodifiableMap(lockByTopic);
        this.fieldOrderByTopic = Collections.unmodifiableMap(orderByTopic);
        this.jsonPointerByField = Collections.unmodifiableMap(pointerByField);
        this.fieldDotNotationByField = Collections.unmodifiableMap(dotNotationByField);
    }

    private CsvPreference createCsvPreference(final CsvAuditEventHandlerConfiguration config) {
        return new CsvPreference.Builder(
                config.getFormatting().getQuoteChar(),
                config.getFormatting().getDelimiterChar(),
                config.getFormatting().getEndOfLineSymbols())
                .useQuoteMode(new AlwaysQuoteMode())
                .build();
    }

    /**
     * {@inheritDoc}
     * <p>
     * After a {@link #shutdown()}, the handler stays shut down until this method returns: an event published, or a
     * {@code rotate} action, meanwhile on a topic that it has not opened yet fails with a
     * {@link ServiceUnavailableException}.
     */
    @Override
    public void startup() throws ResourceException {
        LOGGER.trace("Audit logging to: {}", configuration.getLogDirectory());
        File file = new File(configuration.getLogDirectory());
        if (!file.isDirectory()) {
            if (file.exists()) {
                LOGGER.warn("Specified path is file but should be a directory: {}", configuration.getLogDirectory());
            } else {
                if (!file.mkdirs()) {
                    LOGGER.warn("Unable to create audit directory in the path: {}", configuration.getLogDirectory());
                }
            }
        }
        try {
            for (String topic : eventTopicsMetaData.getTopics()) {
                File auditLogFile = getAuditLogFile(topic);
                final Lock writeLock = writerLockByTopic.get(topic).writeLock();
                writeLock.lock();
                try {
                    openWriter(topic, auditLogFile);
                } catch (IOException e) {
                    LOGGER.error("Error when creating audit file: {}", auditLogFile, e);
                } finally {
                    writeLock.unlock();
                }
            }
        } finally {
            // also when a writer throws a RuntimeException: the topics that are not opened open at their next event
            shutDown = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() throws ResourceException {
        cleanup();
    }

    /**
     * Create a csv audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event) {
        try {
            checkTopic(topic);
            publishEventWithRetry(topic, event);
            return newResourceResponse(
                    event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, event).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private void checkTopic(String topic) throws ResourceException {
        final JsonValue auditEventProperties = getAuditEventProperties(eventTopicsMetaData.getSchema(topic));
        if (auditEventProperties == null || auditEventProperties.isNull()) {
            throw new InternalServerErrorException("No audit event properties defined for audit event: " + topic);
        }
    }

    /**
     * Publishes the provided event to the writer of the topic, under the read lock of the topic. If the topic has no
     * writer, or if the write fails, the event is written under the write lock, to a new writer unless another thread
     * has replaced the writer meanwhile. Once the handler is shut down, the event fails instead.
     */
    private void publishEventWithRetry(final String topic, final JsonValue event)
                    throws ResourceException {
        final ReadWriteLock writerLock = writerLockByTopic.get(topic);
        CsvWriter csvWriter = null;
        writerLock.readLock().lock();
        try {
            csvWriter = writers.get(topic);
            if (csvWriter != null) {
                writeEvent(topic, csvWriter, event);
                return;
            }
            LOGGER.debug("CSV file writer for {} topic is null", topic);
        } catch (IOException ex) {
            // Re-try once in case the writer stream became closed for some reason
            LOGGER.debug("IOException while writing ({})", ex.getMessage());
        } finally {
            writerLock.readLock().unlock();
        }
        // The write lock waits for any reset to complete before proceeding - Otherwise, we may lose multiple events
        // or have multiple threads attempting to reset the writer.
        writerLock.writeLock().lock();
        try {
            if (shutDown) {
                // shutdown() removes the writers it closes, so the event ends here instead of in a new writer
                throw new ServiceUnavailableException("Unable to publish event for topic: " + topic
                        + ", the handler is shut down");
            }
            // Lookup the current writer directly from the map so we can check if another thread has reset it.
            CsvWriter newCsvWriter = writers.get(topic);
            if (newCsvWriter == null || newCsvWriter == csvWriter) {
                // No writer, or the one that failed: the writer hasn't been reset.
                newCsvWriter = resetAndReopenWriter(topic);
                LOGGER.debug("Resetting writer");
            } else {
                LOGGER.debug("Writer reset by another thread");
            }
            writeEvent(topic, newCsvWriter, event);
        } catch (ResourceException e) {
            // the handler is shut down, or no new writer could be opened
            throw e;
        } catch (IOException e) {
            // a failure of the server, such as a full disk, not of the request
            throw new InternalServerErrorException(e);
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    private CsvWriter writeEvent(final String topic, CsvWriter csvWriter, final JsonValue event)
                    throws IOException {
        writeEntry(topic, csvWriter, event);
        EventBufferingConfiguration bufferConfig = configuration.getBuffering();
        if (!bufferConfig.isEnabled() || bufferConfig.isAutoFlush()) {
            csvWriter.flush();
        }
        return csvWriter;
    }

    private Set<String> getFieldOrder(final String topic, final EventTopicsMetaData eventTopicsMetaData)
            throws ResourceException {
        final Set<String> fieldOrder = new LinkedHashSet<>();
        fieldOrder.addAll(generateJsonPointers(getAuditEventSchema(eventTopicsMetaData.getSchema(topic))));
        return fieldOrder;
    }

    private CsvWriter openWriter(final String topic, final File auditFile) throws IOException {
        final CsvWriter writer = createCsvWriter(auditFile, topic);
        writers.put(topic, writer);
        return writer;
    }

    private CsvWriter createCsvWriter(final File auditFile, String topic) throws IOException {
        String[] headers = buildHeaders(fieldOrderByTopic.get(topic));
        if (configuration.getSecurity().isEnabled()) {
            return new SecureCsvWriter(auditFile, headers, csvPreference, configuration, keyStoreHandler, RANDOM);
        } else {
            return new StandardCsvWriter(auditFile, headers, csvPreference, configuration);
        }
    }

    private ICsvMapReader createCsvMapReader(final File auditFile) throws IOException {
        CsvMapReader csvReader = new CsvMapReader(new FileReader(auditFile), csvPreference);

        if (configuration.getSecurity().isEnabled()) {
            return new CsvSecureMapReader(csvReader);
        } else {
            return csvReader;
        }
    }

    private String[] buildHeaders(final Collection<String> fieldOrder) {
        final String[] headers = new String[fieldOrder.size()];
        fieldOrder.toArray(headers);
        for (int i = 0; i < headers.length; i++) {
            headers[i] = jsonPointerToDotNotation(headers[i]);
        }
        return headers;
    }

    /**
     * Perform a query on the csv audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(
            Context context,
            String topic,
            QueryRequest query,
            QueryResourceHandler handler) {
        try {
            for (final JsonValue value : getEntries(topic, query.getQueryFilter())) {
                handler.handleResource(newResourceResponse(value.get(FIELD_CONTENT_ID).asString(), null, value));
            }
            return newQueryResponse().asPromise();
        } catch (Exception e) {
            // a failure to read the audit file is a failure of the server
            return ResourceExceptionsUtil.adapt(e).asPromise();
        }
    }

    /**
     * Read from the csv audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        try {
            final Set<JsonValue> entry = getEntries(topic, QueryFilters.parse("/_id eq \"" + resourceId + "\""));
            if (entry.isEmpty()) {
                throw new NotFoundException(topic + " audit log not found");
            }
            final JsonValue resource = entry.iterator().next();
            return newResourceResponse(resource.get(FIELD_CONTENT_ID).asString(), null, resource).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (IOException e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(
            Context context, String topic, ActionRequest request) {
        try {
            String action = request.getAction();
            if (topic == null) {
                return new BadRequestException(format("Topic is required for action %s", action)).asPromise();
            }
            if (action.equals(ROTATE_FILE_ACTION_NAME)) {
                return handleRotateAction(topic).asPromise();
            }
            final String error = format("This action is unknown for the CSV handler: %s", action);
            return new BadRequestException(error).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private ActionResponse handleRotateAction(String topic)
            throws ResourceException {
        final ReadWriteLock writerLock = writerLockByTopic.get(topic);
        if (writerLock == null) {
            LOGGER.debug("Unable to rotate file for unknown topic: {}", topic);
            throw new BadRequestException("Unable to rotate file for topic: " + topic);
        }
        final boolean rotationEnabled = configuration.getFileRotation().isRotationEnabled();
        // With rotation enabled, the writer rotates its own file while publishers keep writing to it, and the read lock
        // keeps a reset from closing it meanwhile. The default rotation closes and replaces the writer, so it waits for
        // the events being written to it.
        final Lock lock = rotationEnabled ? writerLock.readLock() : writerLock.writeLock();
        File rotatedFile = null;
        lock.lock();
        try {
            CsvWriter csvWriter = writers.get(topic);
            if (csvWriter == null) {
                LOGGER.debug("Unable to rotate file for topic: {}", topic);
                if (shutDown) {
                    throw new ServiceUnavailableException("Unable to rotate file for topic: " + topic
                            + ", the handler is shut down");
                }
                // the writer of the topic could not be opened
                throw new InternalServerErrorException("Unable to rotate file for topic: " + topic);
            }
            if (rotationEnabled) {
                final boolean rotated;
                try {
                    rotated = csvWriter.forceRotation();
                } catch (IOException e) {
                    throw new InternalServerErrorException("Error when rotating file for topic: " + topic, e);
                }
                if (!rotated) {
                    // outside the try: a ResourceException is an IOException
                    throw new InternalServerErrorException("Unable to rotate file for topic: " + topic);
                }
            } else {
                // use a default rotation instead
                rotatedFile = rotateAndReopenWriter(topic);
            }
        } finally {
            lock.unlock();
        }
        if (rotatedFile != null) {
            // compress outside the write lock, which publishers and shutdown wait for
            GzipUtil.compressRotatedFile(rotatedFile);
        }
        return Responses.newActionResponse(json(object(field("rotated", "true"))));
    }

    private File getAuditLogFile(final String type) {
        final String prefix = configuration.getSecurity().isEnabled() ? SECURE_CSV_FILENAME_PREFIX : "";
        return new File(configuration.getLogDirectory(), prefix + type + ".csv");
    }

    private void writeEntry(final String topic, final CsvWriter csvWriter, final JsonValue obj) throws IOException {
        Set<String> fieldOrder = fieldOrderByTopic.get(topic);
        Map<String, String> cells = new HashMap<>(fieldOrder.size());
        for (String key : fieldOrder) {
            final String value = JsonValueUtils.extractValueAsString(obj, key);
            if (value != null && !value.isEmpty()) {
                cells.put(fieldDotNotationByField.get(key), value);
            }
        }
        csvWriter.writeEvent(cells);
    }

    private CsvWriter resetAndReopenWriter(final String topic) throws InternalServerErrorException {
        closeWriter(topic);
        try {
            return openWriter(topic, getAuditLogFile(topic));
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * Renames the audit file of a topic to its next timestamped name, and opens a new audit file. With tamper evident
     * logging, the keystore of the audit file is renamed after the archive, and the new audit file gets a new one.
     *
     * @return The archive name of the rotated file. A file to compress is renamed without the .gz suffix, and the
     *         caller compresses it with {@link GzipUtil#compressRotatedFile(File)}.
     */
    private File rotateAndReopenWriter(final String topic) throws InternalServerErrorException {
        closeWriter(topic);
        final File auditLogFile = getAuditLogFile(topic);
        final File rotatedFile = new TimeStampFileNamingPolicy(auditLogFile, null, null).getNextName();
        final File renamedFile = GzipUtil.removeGzipSuffix(rotatedFile);
        if (!auditLogFile.renameTo(renamedFile)) {
            throw new InternalServerErrorException(
                    format("Unable to rename file %s to %s when rotating", auditLogFile, renamedFile));
        }
        if (configuration.getSecurity().isEnabled()) {
            // named after the archive name, as SecureCsvWriter names it when RotatableWriter rotates the file
            final File keyStoreFile = CsvSecureUtils.keyStoreFile(auditLogFile);
            final File archivedKeyStoreFile = CsvSecureUtils.keyStoreFile(rotatedFile);
            if (!keyStoreFile.renameTo(archivedKeyStoreFile)) {
                LOGGER.error("Unable to rename {} to {} when rotating", keyStoreFile, archivedKeyStoreFile);
                // a new audit file would overwrite the keys of the archived file in its keystore, so the writer
                // resumes the audit file instead; if the audit file cannot be put back, the archive loses its keys
                if (!renamedFile.renameTo(auditLogFile)) {
                    LOGGER.error("Unable to rename {} back to {}; the new audit file overwrites the keys of {} in {}",
                            renamedFile, auditLogFile, renamedFile, keyStoreFile);
                }
                resetAndReopenWriter(topic);
                throw new InternalServerErrorException(
                        format("Unable to rename file %s to %s when rotating", keyStoreFile, archivedKeyStoreFile));
            }
        }
        resetAndReopenWriter(topic);
        return rotatedFile;
    }

    private void closeWriter(final String topic) {
        CsvWriter writerToClose = writers.remove(topic);
        if (writerToClose != null) {
            // attempt clean-up close
            try {
                writerToClose.close();
            } catch (Exception ex) {
                // A tamper evident file that fails to close lacks its final signature, and fails verification
                LOGGER.error("Unable to close the CSV file writer of topic {}", topic, ex);
            }
        }
    }

    /**
     * Parser the csv file corresponding the the specified audit entry type and returns a set of matching audit entries.
     *
     * @param auditEntryType the audit log type
     * @param queryFilter the query filter to apply to the entries
     * @return  A audit log entry; null if no entry exists
     * @throws IOException If unable to get an entry from the CSV file.
     */
    private Set<JsonValue> getEntries(final String auditEntryType, QueryFilter<JsonPointer> queryFilter)
            throws IOException {
        final File auditFile = getAuditLogFile(auditEntryType);
        final Set<JsonValue> results = new HashSet<>();
        if (queryFilter == null) {
            queryFilter = QueryFilter.alwaysTrue();
        }
        if (auditFile.exists()) {
            try (ICsvMapReader reader = createCsvMapReader(auditFile)) {
                final String[] fileHeader = reader.getHeader(true);
                if (fileHeader == null) {
                    // a tamper evident writer writes the header with the first event, so the file holds no event yet
                    return results;
                }
                // the header elements are used to map the values to the bean (names must match)
                final String[] header = convertDotNotationToSlashes(fileHeader);
                final CellProcessor[] processors = createCellProcessors(auditEntryType, header);
                Map<String, Object> entry;
                while ((entry = reader.read(header, processors)) != null) {
                    entry = convertDotNotationToSlashes(entry);
                    final JsonValue jsonEntry = expand(entry);
                    if (queryFilter.accept(JSONVALUE_FILTER_VISITOR, jsonEntry)) {
                        results.add(jsonEntry);
                    }
                }

            }
        }
        return results;
    }

    private CellProcessor[] createCellProcessors(final String auditEntryType, final String[] headers)
            throws ResourceException {
        final List<CellProcessor> cellProcessors = new ArrayList<>();
        final JsonValue auditEvent = eventTopicsMetaData.getSchema(auditEntryType);

        for (String header: headers) {
            final String propertyType = getPropertyType(auditEvent, new JsonPointer(header));
            if ((propertyType.equals(OBJECT_TYPE) || propertyType.equals(ARRAY_TYPE))) {
                cellProcessors.add(new Optional(new ParseJsonValue()));
            } else {
                cellProcessors.add(new Optional());
            }
        }

        return cellProcessors.toArray(new CellProcessor[cellProcessors.size()]);
    }

    /**
     * CellProcessor for parsing JsonValue objects from CSV file.
     */
    public static class ParseJsonValue implements CellProcessor {

        @Override
        public Object execute(final Object value, final CsvContext context) {
            JsonValue jv = null;
            // Check if value is JSON object
            if (((String) value).startsWith("{") && ((String) value).endsWith("}")) {
                try {
                    jv = new JsonValue(MAPPER.readValue((String) value, Map.class));
                } catch (Exception e) {
                    LOGGER.debug("Error parsing JSON string: " + e.getMessage());
                }
            } else if (((String) value).startsWith("[") && ((String) value).endsWith("]")) {
                try {
                    jv = new JsonValue(MAPPER.readValue((String) value, List.class));
                } catch (Exception e) {
                    LOGGER.debug("Error parsing JSON string: " + e.getMessage());
                }
            }
            if (jv == null) {
                return value;
            }
            return jv.getObject();
        }

    }

    private void cleanup() throws ResourceException {
        IOException failure = null;
        shutDown = true;
        for (Map.Entry<String, ReadWriteLock> topicLock : writerLockByTopic.entrySet()) {
            final Lock writeLock = topicLock.getValue().writeLock();
            writeLock.lock();
            // every writer is closed, also when the flush or another writer fails: the scheduler of a tamper evident
            // writer would keep the JVM from exiting. The closed writer leaves the map, so that neither an event nor
            // the rotate action reaches it.
            try (CsvWriter writer = writers.remove(topicLock.getKey())) {
                if (writer != null) {
                    writer.flush();
                }
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            } finally {
                writeLock.unlock();
            }
        }
        if (failure != null) {
            LOGGER.error("Unable to close filewriters during {} cleanup", this.getClass().getName(), failure);
            throw new InternalServerErrorException(
                    "Unable to close filewriters during " + this.getClass().getName() + " cleanup", failure);
        }
    }

    private Map<String, Object> convertDotNotationToSlashes(final Map<String, Object> entries) {
        final Map<String, Object> newEntry = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            final String key = dotNotationToJsonPointer(entry.getKey());
            newEntry.put(key, entry.getValue());
        }
        return newEntry;
    }

    private String[] convertDotNotationToSlashes(final String[] entries) {
        String[] result = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            result[i] = dotNotationToJsonPointer(entries[i]);
        }
        return result;
    }

}
