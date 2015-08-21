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

import static org.forgerock.audit.events.AuditEventHelper.*;
import static org.forgerock.audit.util.JsonSchemaUtils.generateJsonPointers;
import static org.forgerock.audit.util.JsonValueUtils.*;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.events.handlers.TopicAndEvent;
import org.forgerock.audit.util.JsonSchemaUtils;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.util.CsvContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles AuditEvents by writing them to a CSV file.
 */
public class CSVAuditEventHandler extends AuditEventHandlerBase<CSVAuditEventHandlerConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(CSVAuditEventHandler.class);

    private Map<String, JsonValue> auditEvents;
    private String auditLogDirectory;
    private CsvPreference csvPreference;
    private final Map<String, ICsvMapWriter> writers = new HashMap<>();

    private boolean secure;
    private String keystoreFilename;
    private String keystorePassword;

    private static final ObjectMapper mapper;

    static {
        JsonFactory jsonFactory = new JsonFactory();
        mapper = new ObjectMapper(jsonFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAuditEventsMetaData(final Map<String, JsonValue> auditEvents) {
        this.auditEvents = auditEvents;
    }

    /**
     * Configure the CSVAuditEventHandler.
     * {@inheritDoc}
     */
    @Override
    public void configure(final CSVAuditEventHandlerConfiguration config) throws ResourceException {
        synchronized (this) {
            cleanup();

            auditLogDirectory = config.getLogDirectory();
            logger.info("Audit logging to: {}", auditLogDirectory);

            File file = new File(auditLogDirectory);
            if (!file.isDirectory()) {
                if (file.exists()) {
                    logger.warn("Specified path is file but should be a directory: " + auditLogDirectory);
                } else {
                    if (!file.mkdirs()) {
                        logger.warn("Unable to create audit directory in the path: " + auditLogDirectory);
                    }
                }
            }
            csvPreference = createCsvPreference(config);
            secure = config.getCsvSecurity().isEnabled();
            if (secure) {
                keystoreFilename = config.getCsvSecurity().getFilename();
                keystorePassword = config.getCsvSecurity().getPassword();
            }
        }
    }

    private CsvPreference createCsvPreference(final CSVAuditEventHandlerConfiguration config) {
        CSVAuditEventHandlerConfiguration.CsvConfiguration csvConfiguration = config.getCsvConfiguration();
        final CsvPreference.Builder builder = new CsvPreference.Builder(csvConfiguration.getQuoteChar(),
                                                                        csvConfiguration.getDelimiterChar(),
                                                                        csvConfiguration.getEndOfLineSymbols());

        builder.useQuoteMode(new AlwaysQuoteMode());
        return builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws ResourceException {
        cleanup();
    }

    /**
     * Create a csv audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(String topic, JsonValue event) {
        try {
            checkTopic(topic);
            publishEventWithRetry(topic, event, getFieldOrder(topic), true);
            return newResourceResponse(
                    event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, event).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public synchronized void publishEvents(List<TopicAndEvent> events) {
        Map<String, Set<String>> topicCache = new HashMap<String, Set<String>>();
        // publish all events
        int nbPublished = 0;
        try {
            for (TopicAndEvent topicAndEvent : events) {
                String topic = topicAndEvent.getTopic();
                Set<String> fieldOrder = topicCache.get(topic);
                if (fieldOrder == null) {
                    checkTopic(topic);
                    fieldOrder = getFieldOrder(topic);
                    topicCache.put(topic, fieldOrder);
                }
                publishEventWithRetry(topic, topicAndEvent.getEvent(), fieldOrder, false);
                nbPublished++;
            }
        } catch (IOException e) {
            String message = "Could not publish all buffered events." + "Size of events buffer: " + events.size()
                    + ", number of events published: " + nbPublished;
            logger.error(message.toString(), e);
        }
        // flushes writers for all topics
        int nbFlushed = 0;
        try {
            for (String topic : topicCache.keySet()) {
                ICsvMapWriter writer = getWriter(topic, null, false);
                writer.flush();
                nbFlushed++;
            }
        } catch (IOException e) {
            StringBuilder message = new StringBuilder("Could not flush all topics for buffered events.")
                .append("Number of topics: ").append(topicCache.size())
                .append(", number topics flushed: ").append(nbFlushed);
            logger.error(message.toString(), e);
        }
    }

    private void checkTopic(String topic) throws ResourceException, InternalServerErrorException {
        final JsonValue auditEventProperties = AuditEventHelper.getAuditEventProperties(auditEvents.get(topic));
        if (auditEventProperties == null || auditEventProperties.isNull()) {
            throw new InternalServerErrorException("No audit event properties defined for audit event: " + topic);
        }
    }

    /**
     * Publishes the provided event, and returns the writer used.
     */
    private void publishEventWithRetry(
            final String topic, final JsonValue event, final Set<String> fieldOrder, boolean mustFlush)
                    throws ResourceException {
        ICsvMapWriter csvWriter = null;
        try {
            csvWriter = writeEvent(topic, event, fieldOrder, mustFlush);
        } catch (IOException ex) {
            // Re-try once in case the writer stream became closed for some reason
            logger.debug("IOException during entry write, reset writer and re-try {}", ex.getMessage());
            synchronized (this) {
                resetWriter(topic, csvWriter);
            }
            try {
                writeEvent(topic, event, fieldOrder, mustFlush);
            } catch (IOException ex2) {
                throw new BadRequestException(ex2);
            }
        }
    }

    private ICsvMapWriter writeEvent(
            final String topic, final JsonValue event, final Set<String> fieldOrder, boolean mustFlush)
                    throws IOException, InternalServerErrorException {
        File auditFile = getOrCreateAuditFile(topic, fieldOrder);
        ICsvMapWriter csvWriter = getWriter(topic, auditFile, true);
        writeEntry(csvWriter, event, fieldOrder);
        if (mustFlush) {
            csvWriter.flush();
        }
        return csvWriter;
    }

    private Set<String> getFieldOrder(final String topic) throws ResourceException {
        final Set<String> fieldOrder = new LinkedHashSet<>();
        fieldOrder.addAll(generateJsonPointers(AuditEventHelper.getAuditEventSchema(auditEvents
                .get(topic))));
        return fieldOrder;
    }

    /** Returns the audit file to use for logging the event. It may imply its creation. */
    private File getOrCreateAuditFile(final String auditEventType, final Set<String> fieldOrder) throws IOException,
            InternalServerErrorException {
        File auditFile = getAuditLogFile(auditEventType);
        // Create header if creating a new file
        if (!auditFile.exists()) {
            synchronized (this) {
                final ICsvMapWriter existingCsvWriter = getWriter(auditEventType, auditFile, false);
                final File auditTmpFile = new File(auditFile.getParent(), auditEventType + ".tmp");
                // This is atomic, so only one caller will succeed with created
                final boolean created = auditTmpFile.createNewFile();
                if (created) {
                    final ICsvMapWriter tmpFileWriter = createCsvMapWriter(auditTmpFile);
                    tmpFileWriter.writeHeader(buildHeaders(fieldOrder));
                    tmpFileWriter.close();
                    if (!auditTmpFile.renameTo(auditFile)) {
                        logger.error("Unable to rename audit temp file");
                        throw new InternalServerErrorException("Unable to rename audit temp file");
                    }
                    resetWriter(auditEventType, existingCsvWriter);
                }
            }
        }
        return auditFile;
    }

    private ICsvMapWriter createCsvMapWriter(final File auditTmpFile) throws IOException {
        CsvMapWriter csvWriter = new CsvMapWriter(new FileWriter(auditTmpFile, true), csvPreference);

        if (secure) {
            HmacCalculator hmacCalculator = setupHmacCalculator();
            return new CsvHmacMapWriter(csvWriter, hmacCalculator);
        } else {
            return csvWriter;
        }
    }

    HmacCalculator setupHmacCalculator() {
        final HmacCalculator hmacCalculator = new HmacCalculator(keystoreFilename, keystorePassword);
        hmacCalculator.init();
        return hmacCalculator;
    }

    private ICsvMapReader createCsvMapReader(final File auditFile) throws IOException {
        CsvMapReader csvReader = new CsvMapReader(new FileReader(auditFile), csvPreference);

        if (secure) {
            return new CsvHmacMapReader(csvReader);
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
            String topic,
            QueryRequest query,
            QueryResourceHandler handler) {
        try {
            for (final JsonValue value : getEntries(topic, query.getQueryFilter())) {
                handler.handleResource(newResourceResponse(value.get(FIELD_CONTENT_ID).asString(), null, value));
            }
            return newQueryResponse().asPromise();
        } catch (Exception e) {
            return new BadRequestException(e).asPromise();
        }
    }

    /**
     * Read from the csv audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(String topic, String resourceId) {
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
            return new BadRequestException(e).asPromise();
        }
    }

    private File getAuditLogFile(final String type) {
        return new File(auditLogDirectory, type + ".csv");
    }

    private ICsvMapWriter getWriter(final String topic, final File auditFile, final boolean createIfMissing)
            throws IOException {
        synchronized (writers) {
            ICsvMapWriter existingWriter = writers.get(topic);
            if (existingWriter == null && createIfMissing) {
                existingWriter = createCsvMapWriter(auditFile);
                writers.put(topic, existingWriter);
            }
            return existingWriter;
        }
    }

    private void writeEntry(
            final ICsvMapWriter csvWriter,
            final JsonValue obj,
            final Collection<String> fieldOrder) throws IOException {

        Map<String, String> cells = new HashMap<>(fieldOrder.size());
        for (String key : fieldOrder) {
            cells.put(jsonPointerToDotNotation(key), extractValue(obj, key));
        }
        csvWriter.write(cells, buildHeaders(fieldOrder));
    }

    private void resetWriter(final String auditEventType, final ICsvMapWriter writerToReset) {
        synchronized (writers) {
            final ICsvMapWriter existingWriter = writers.get(auditEventType);
            if (existingWriter != null && writerToReset != null && existingWriter == writerToReset) {
                writers.remove(auditEventType);
                // attempt clean-up close
                try {
                    existingWriter.close();
                } catch (Exception ex) {
                    // Debug level as the writer is expected to potentially be invalid
                    logger.debug("File writer close in resetWriter reported failure ", ex);
                }
            }
        }
    }

    /**
     * Parser the csv file corresponding the the specified audit entry type and returns a set of matching audit entries.
     *
     * @param auditEntryType the audit log type
     * @param queryFilter the query filter to apply to the entries
     * @return  A audit log entry; null if no entry exists
     * @throws Exception
     */
    private Set<JsonValue> getEntries(final String auditEntryType, QueryFilter<JsonPointer> queryFilter)
            throws IOException, ResourceException {
        final File auditFile = getAuditLogFile(auditEntryType);
        final Set<JsonValue> results = new HashSet<>();
        if (queryFilter == null) {
            queryFilter = QueryFilter.alwaysTrue();
        }
        if (auditFile.exists()) {
            ICsvMapReader reader = null;
            try {
                reader = createCsvMapReader(auditFile);

                // the header elements are used to map the values to the bean (names must match)
                final String[] header = convertDotNotationToSlashes(reader.getHeader(true));
                final CellProcessor[] processors = createCellProcessors(auditEntryType, header);
                Map<String, Object> entry;
                while ((entry = reader.read(header, processors)) != null) {
                    entry = convertDotNotationToSlashes(entry);
                    final JsonValue jsonEntry = expand(entry);
                    if (queryFilter.accept(JSONVALUE_FILTER_VISITOR, jsonEntry)) {
                        results.add(jsonEntry);
                    }
                }

            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        return results;
    }

    private CellProcessor[] createCellProcessors(final String auditEntryType, final String[] headers)
            throws ResourceException {
        final List<CellProcessor> cellProcessors = new ArrayList<>();
        final JsonValue auditEvent = auditEvents.get(auditEntryType);

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
    public class ParseJsonValue implements CellProcessor {

        @Override
        public Object execute(final Object value, final CsvContext context) {
            JsonValue jv = null;
            // Check if value is JSON object
            if (((String) value).startsWith("{") && ((String) value).endsWith("}")) {
                try {
                    jv = new JsonValue(mapper.readValue((String) value, Map.class));
                } catch (Exception e) {
                    logger.debug("Error parsing JSON string: " + e.getMessage());
                }
            } else if (((String) value).startsWith("[") && ((String) value).endsWith("]")) {
                try {
                    jv = new JsonValue(mapper.readValue((String) value, List.class));
                } catch (Exception e) {
                    logger.debug("Error parsing JSON string: " + e.getMessage());
                }
            }
            if (jv == null) {
                return value;
            }
            return jv.getObject();
        }

    }

    private void cleanup() throws ResourceException {
        auditLogDirectory = null;
        try {
            for (ICsvMapWriter csvWriter : writers.values()) {
                if (csvWriter != null) {
                    csvWriter.flush();
                    csvWriter.close();
                }
            }
        } catch (IOException e) {
            logger.error("Unable to close filewriters during {} cleanup", this.getClass().getName(), e);
            throw new InternalServerErrorException(
                    "Unable to close filewriters during " + this.getClass().getName() + " cleanup", e);
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
        List<String> newList = new LinkedList<>();
        for (String entry : entries) {
            newList.add(dotNotationToJsonPointer(entry));
        }
        return newList.toArray(new String[newList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CSVAuditEventHandlerConfiguration> getConfigurationClass() {
        return CSVAuditEventHandlerConfiguration.class;
    }
}
