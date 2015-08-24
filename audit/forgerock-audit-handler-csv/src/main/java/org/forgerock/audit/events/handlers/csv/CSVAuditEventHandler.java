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

import static org.forgerock.util.promise.Promises.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.JsonSchemaUtils;
import org.forgerock.audit.util.JsonValueUtils;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
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
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Responses;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
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

/**
 * Handles AuditEvents by writing them to a CSV file.
 */
public class CSVAuditEventHandler extends AuditEventHandlerBase<CSVAuditEventHandlerConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(CSVAuditEventHandler.class);

    private Map<String, JsonValue> auditEvents;
    private String auditLogDirectory;
    private CsvPreference csvPreference;

    private final Map<String, ICsvMapWriter> writers = new HashMap<>();
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
     * Perform an action on the csv audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(
            final Context context,
            final ActionRequest request) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Perform an action on the csv audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            final Context context,
            final String resourceId,
            final ActionRequest request) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Create a csv audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(
            final Context context,
            final CreateRequest request) {

        try {
            // Re-try once in case the writer stream became closed for some reason
            boolean retry;
            int retryCount = 0;
            final String auditEventType = request.getResourcePath();
            do {
                retry = false;
                ICsvMapWriter csvWriter = null;
                try {
                    final JsonValue auditEventProperties =
                            AuditEventHelper.getAuditEventProperties(auditEvents.get(auditEventType));
                    if (auditEventProperties == null || auditEventProperties.isNull()) {
                        throw new InternalServerErrorException("No audit event properties defined for audit event: "
                                + auditEventType);
                    }
                    final Set<String> fieldOrder = new LinkedHashSet<>();
                    fieldOrder.addAll(JsonSchemaUtils.generateJsonPointers(
                            AuditEventHelper.getAuditEventSchema(auditEvents.get(auditEventType))));

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
                    csvWriter = getWriter(auditEventType, auditFile, true);
                    writeEntry(csvWriter, request.getContent(), fieldOrder);
                } catch (IOException ex) {
                    if (retryCount == 0) {
                        retry = true;
                        logger.debug("IOException during entry write, reset writer and re-try {}", ex.getMessage());
                        synchronized (this) {
                            resetWriter(auditEventType, csvWriter);
                        }
                    } else {
                        throw new BadRequestException(ex);
                    }
                }
                ++retryCount;
            } while (retry);
            return newResultPromise(
                    Responses.newResourceResponse(
                            request.getContent().get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                            null,
                            new JsonValue(request.getContent())
                    )
            );
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    private ICsvMapWriter createCsvMapWriter(final File auditTmpFile) throws IOException {
        return new CsvMapWriter(new FileWriter(auditTmpFile, true), csvPreference);
    }

    private String[] buildHeaders(final Collection<String> fieldOrder) {
        final String[] headers = new String[fieldOrder.size()];
        fieldOrder.toArray(headers);
        for (int i = 0; i < headers.length; i++) {
            headers[i] = convertToDotNotation(headers[i]);
        }
        return headers;
    }

    /**
     * Perform a query on the csv audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(
            final Context context,
            final QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            final String auditEventType = request.getResourcePathObject().head(1).toString();
            for (final JsonValue value : getEntries(auditEventType, request.getQueryFilter())) {
                handler.handleResource(
                        Responses.newResourceResponse(
                                value.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, value));
            }
            return newResultPromise(Responses.newQueryResponse());
        } catch (Exception e) {
            return newExceptionPromise((ResourceException) new BadRequestException(e));
        }
    }

    /**
     * Read from the csv audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(
            final Context context,
            final String resourceId,
            final ReadRequest request) {
        try {
            final String auditEventType = request.getResourcePathObject().head(1).toString();
            final Set<JsonValue> entry =
                    getEntries(auditEventType, QueryFilters.parse("/_id eq \"" + resourceId + "\""));
            if (entry.isEmpty()) {
                throw new NotFoundException(auditEventType + " audit log not found");
            }
            final JsonValue resource = entry.iterator().next();
            return newResultPromise(
                    Responses.newResourceResponse(
                            resource.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, resource));
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        } catch (IOException e) {
            return newExceptionPromise((ResourceException) new BadRequestException(e));
        }
    }

    private File getAuditLogFile(final String type) {
        return new File(auditLogDirectory, type + ".csv");
    }

    private ICsvMapWriter getWriter(final String auditEventType, final File auditFile, final boolean createIfMissing)
            throws IOException {
        synchronized (writers) {
            ICsvMapWriter existingWriter = writers.get(auditEventType);
            if (existingWriter == null && createIfMissing) {
                existingWriter = createCsvMapWriter(auditFile);
                writers.put(auditEventType, existingWriter);
            }
            return existingWriter;
        }
    }

    private void writeEntry(
            final ICsvMapWriter csvWriter,
            final JsonValue obj,
            final Collection<String> fieldOrder) throws IOException {

        Map<String, String> cells = new HashMap<>(fieldOrder.size());
        final Iterator<String> iter = fieldOrder.iterator();
        while (iter.hasNext()) {
            final String key = iter.next();
            cells.put(convertToDotNotation(key),
                      extractValue(obj, key));
        }
        csvWriter.write(cells, buildHeaders(fieldOrder));
        csvWriter.flush();
    }

    private String extractValue(final JsonValue obj, final String key) {
        JsonValue value = obj.get(new JsonPointer(key));
        final String rawStr;
        if (value == null) {
            rawStr = "";
        } else if (value.isString())
            rawStr = value.asString();
        else {
            rawStr = value.toString();
        }
        return rawStr;
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
                reader = new CsvMapReader(new FileReader(auditFile), csvPreference);

                // the header elements are used to map the values to the bean (names must match)
                final String[] header = convertDotNotationToSlashes(reader.getHeader(true));
                final CellProcessor[] processors = createCellProcessors(auditEntryType, header);
                Map<String, Object> entry;
                while ((entry = reader.read(header, processors)) != null) {
                    entry = convertDotNotationToSlashes(entry);
                    final JsonValue jsonEntry = JsonValueUtils.expand(entry);
                    if (queryFilter.accept(JsonValueUtils.JSONVALUE_FILTER_VISITOR, jsonEntry)) {
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
            final String propertyType = AuditEventHelper.getPropertyType(auditEvent, new JsonPointer(header));
            if ((propertyType.equals(AuditEventHelper.OBJECT_TYPE)
                    || propertyType.equals(AuditEventHelper.ARRAY_TYPE))) {
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
            final String key = StringUtils.replace(entry.getKey(), ".", "/");
            newEntry.put(key, entry.getValue());
        }
        return newEntry;
    }

    private String[] convertDotNotationToSlashes(final String[] entries) {
        List<String> newList = new LinkedList<>();
        for (String entry : entries) {
            newList.add(StringUtils.replace(entry, ".", "/"));
        }
        return newList.toArray(new String[0]);
    }

    private String convertToDotNotation(final String path) {
        String newPath = path;
        if (path.startsWith("/")) {
            newPath = path.substring(1);
        }
        return StringUtils.replace(newPath, "/", ".");
    }

    /**
     * {@inheritDoc}
     */
    public Class<CSVAuditEventHandlerConfiguration> getConfigurationClass() {
        return CSVAuditEventHandlerConfiguration.class;
    }
}
