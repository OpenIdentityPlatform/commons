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

package org.forgerock.audit.events.handlers.impl;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Handles AuditEvents by writing them to a CSV file.
 */
public class CSVAuditEventHandler extends AuditEventHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(CSVAuditEventHandler.class);

    private final Map<String, JsonValue> auditEvents;
    private String auditLogDirectory;
    private String recordDelim;

    private static final String CONFIG_LOG_LOCATION = "location";
    private static final String CONFIG_LOG_RECORD_DELIM = "recordDelimiter";

    private final Map<String, FileWriter> fileWriters = new HashMap<String, FileWriter>();
    private static final ObjectMapper MAPPER;

    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        MAPPER = new ObjectMapper(jsonFactory);
    }


    /**
     * Constructs a CSVAuditEventHandler with a list of auditEvents to handle.
     */
    public CSVAuditEventHandler() {
        this(new HashMap<String, JsonValue>());
    }

    /**
     * Constructs a CSVAuditEventHandler with a list of auditEvents to handle.
     * @param auditEvents List of AuditEvents to handle.
     */
    public CSVAuditEventHandler(final Map<String, JsonValue> auditEvents) {
        this.auditEvents = auditEvents;
    }

    /**
     * Configure the CSVAuditEventHandler.
     * @{inheritDoc}
     */
    @Override
    public void configure(final JsonValue config) throws ResourceException {
        synchronized (this) {
            cleanup();

            auditLogDirectory = config.get(CONFIG_LOG_LOCATION).asString();
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

            recordDelim = config.get(CONFIG_LOG_RECORD_DELIM).asString();
            if (StringUtils.isBlank(recordDelim)) {
                recordDelim = System.getProperty("line.separator");
            }
        }
    }

    @Override
    public void close() throws ResourceException {
        cleanup();
    }

    /**
     * Perform an action on the csv audit log.
     * @{inheritDoc}
     */
    @Override
    public void actionCollection(
            final ServerContext context,
            final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Perform an action on the csv audit log entry.
     * @{inheritDoc}
     */
    @Override
    public void actionInstance(
            final ServerContext context,
            final String resourceId,
            final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Create a csv audit log entry.
     * @{inheritDoc}
     */
    @Override
    public void createInstance(
            final ServerContext context,
            final CreateRequest request,
            final ResultHandler<Resource> handler) {

        try {
            // Re-try once in case the writer stream became closed for some reason
            boolean retry = false;
            int retryCount = 0;
            final String auditEventType = request.getResourceName();
            do {
                retry = false;
                FileWriter fileWriter = null;
                try {
                    final Set<String> auditEventProperties =
                            AuditEventHelper.getAuditEventProperties(auditEvents.get(auditEventType)).keys();
                    if (auditEventProperties == null || auditEventProperties.isEmpty()) {
                        throw new InternalServerErrorException("No audit event properties defined for audit event: "
                                + auditEventType);
                    }
                    final Collection<String> fieldOrder =
                            new TreeSet<String>(Collator.getInstance());
                    fieldOrder.addAll(auditEventProperties);

                    File auditFile = getAuditLogFile(auditEventType);
                    // Create header if creating a new file
                    if (!auditFile.exists()) {
                        synchronized (this) {
                            final FileWriter existingFileWriter = getWriter(auditEventType, auditFile, false);
                            final File auditTmpFile = new File(auditFile.getParent(), auditEventType + ".tmp");
                            // This is atomic, so only one caller will succeed with created
                            final boolean created = auditTmpFile.createNewFile();
                            if (created) {
                                final FileWriter tmpFileWriter = new FileWriter(auditTmpFile, true);
                                writeHeaders(fieldOrder, tmpFileWriter);
                                tmpFileWriter.close();
                                if (!auditTmpFile.renameTo(auditFile)) {
                                    logger.error("Unable to rename audit temp file");
                                    throw new InternalServerErrorException("Unable to rename audit temp file");
                                }
                                resetWriter(auditEventType, existingFileWriter);
                            }
                        }
                    }
                    fileWriter = getWriter(auditEventType, auditFile, true);
                    writeEntry(fileWriter, request.getContent().asMap(), fieldOrder);
                } catch (IOException ex) {
                    if (retryCount == 0) {
                        retry = true;
                        logger.debug("IOException during entry write, reset writer and re-try {}", ex.getMessage());
                        synchronized (this) {
                            resetWriter(auditEventType, fileWriter);
                        }
                    } else {
                        throw new BadRequestException(ex);
                    }
                }
                ++retryCount;
            } while (retry);
            handler.handleResult(
                    new Resource(
                            request.getContent().get(Resource.FIELD_CONTENT_ID).asString(),
                            null,
                            new JsonValue(request.getContent())
                    )
            );
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Perform a query on the csv audit log.
     * @{inheritDoc}
     */
    @Override
    public void queryCollection(
            final ServerContext context,
            final QueryRequest request,
            final QueryResultHandler handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Read from the csv audit log.
     * @{inheritDoc}
     */
    @Override
    public void readInstance(
            final ServerContext context,
            final String resourceId,
            final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final String auditEventType = (new JsonPointer(request.getResourceName())).get(0);
            final Map<String, Object> entry = getEntry(auditEventType, resourceId);
            if (entry == null) {
                throw new NotFoundException(auditEventType + " audit log not found");
            }
            final JsonValue auditEvent = new JsonValue(entry);
            handler.handleResult(new Resource(auditEvent.get(Resource.FIELD_CONTENT_ID).asString(), null, auditEvent));
        } catch (Exception e) {
            handler.handleError(new BadRequestException(e));
        }
    }

    private File getAuditLogFile(final String type) {
        return new File(auditLogDirectory, type + ".csv");
    }

    private FileWriter getWriter(final String auditEventType, final File auditFile, final boolean createIfMissing)
            throws IOException {
        synchronized (fileWriters) {
            FileWriter existingWriter = fileWriters.get(auditEventType);
            if (existingWriter == null && createIfMissing) {
                existingWriter = new FileWriter(auditFile, true);
                fileWriters.put(auditEventType, existingWriter);
            }
            return existingWriter;
        }
    }

    private void writeEntry(
            final FileWriter fileWriter,
            final Map<String, Object> obj,
            final Collection<String> fieldOrder) throws IOException {

        final Iterator<String> iter = fieldOrder.iterator();
        final StringBuilder entry = new StringBuilder();
        while (iter.hasNext()) {
            final String key = iter.next();
            Object value = obj.get(key);
            entry.append("\"");
            if (value != null) {
                if (value instanceof Map) {
                    value = new JsonValue(value).toString();
                } else if (value instanceof List) {
                    value = new JsonValue(value).toString();
                }
                final String rawStr = value.toString();
                // Escape quotes with double quotes
                final String escapedStr = rawStr.replaceAll("\"", "\"\"");
                entry.append(escapedStr);
            }
            entry.append("\"");
            if (iter.hasNext()) {
                entry.append(",");
            }
        }
        entry.append(recordDelim);
        fileWriter.append(entry.toString());
        fileWriter.flush();
    }

    private void writeHeaders(final Collection<String> fieldOrder, final FileWriter fileWriter)
            throws IOException {
        final Iterator<String> iter = fieldOrder.iterator();
        final StringBuilder header = new StringBuilder();
        while (iter.hasNext()) {
            final String key = iter.next();
            header.append("\"");
            final String escapedStr = key.replaceAll("\"", "\"\"");
            header.append(escapedStr);
            header.append("\"");
            if (iter.hasNext()) {
                header.append(",");
            }
        }
        header.append(recordDelim);
        fileWriter.append(header.toString());
    }

    private void resetWriter(final String auditEventType, final FileWriter writerToReset) {
        synchronized (fileWriters) {
            final FileWriter existingWriter = fileWriters.get(auditEventType);
            if (existingWriter != null && writerToReset != null && existingWriter == writerToReset) {
                fileWriters.remove(auditEventType);
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
     * Parser the csv file corresponding the the specified audit entry type and returns a audit entry.
     *
     * @param auditEntryType the audit log type
     * @param id the identifier of the entry to return.
     * @return  A audit log entry; null if no entry exists
     * @throws Exception
     */
    private Map<String, Object> getEntry(final String auditEntryType, final String id) throws Exception {
        final File auditFile = getAuditLogFile(auditEntryType);
        if (auditFile.exists()) {
            ICsvMapReader reader = null;
            try {
                reader =
                        new CsvMapReader(
                                new FileReader(auditFile),
                                new CsvPreference.Builder('"', ',', recordDelim).build());

                // the header elements are used to map the values to the bean (names must match)
                final String[] header = reader.getHeader(true);
                final CellProcessor[] processors = createCellProcessors(auditEntryType, header);
                Map<String, Object> entry;
                while ((entry = reader.read(header, processors)) != null) {
                    if (entry.get("_id").equals(id)) {
                        return entry;
                    }
                }

            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        return null;
    }

    private CellProcessor[] createCellProcessors(final String auditEntryType, final String[] headers)
            throws ResourceException {
        final List<CellProcessor> cellProcessors = new ArrayList<CellProcessor>();
        final JsonValue auditEvent = auditEvents.get(auditEntryType);

        for (String header: headers) {
            final String propertyType = AuditEventHelper.getPropertyType(auditEvent, new JsonPointer(header));
            final boolean propertyRequired = AuditEventHelper.isPropertyRequired(auditEvent, new JsonPointer(header));
            if ((propertyType.equals(AuditEventHelper.OBJECT_TYPE) || propertyType.equals(AuditEventHelper.ARRAY_TYPE))
                    && propertyRequired) {
                cellProcessors.add(new NotNull(new ParseJsonValue()));
            } else if ((propertyType.equals(AuditEventHelper.OBJECT_TYPE)
                    || propertyType.equals(AuditEventHelper.ARRAY_TYPE)) && !propertyRequired) {
                cellProcessors.add(new Optional(new ParseJsonValue()));
            } else if (propertyRequired) {
                cellProcessors.add(new NotNull());
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
                    jv = new JsonValue(MAPPER.readValue((String) value, Map.class));
                } catch (Exception e) {
                    logger.debug("Error parsing JSON string: " + e.getMessage());
                }
            } else if (((String) value).startsWith("[") && ((String) value).endsWith("]")) {
                try {
                    jv = new JsonValue(MAPPER.readValue((String) value, List.class));
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
        recordDelim = null;
        try {
            for (FileWriter fileWriter : fileWriters.values()) {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            }
        } catch (IOException e) {
            logger.error("Unable to close filewriters during {} cleanup", this.getClass().getName(), e);
            throw new InternalServerErrorException(
                    "Unable to close filewriters during " + this.getClass().getName() + " cleanup", e);
        }
    }
}
