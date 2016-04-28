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
package org.forgerock.audit.handlers.csv;

import static org.forgerock.util.Reject.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.forgerock.audit.events.handlers.writers.AsynchronousTextWriter;
import org.forgerock.audit.events.handlers.writers.RotatableWriter;
import org.forgerock.audit.events.handlers.writers.TextWriter;
import org.forgerock.audit.events.handlers.writers.TextWriterAdapter;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Responsible for writing to a CSV file.
 */
class StandardCsvWriter implements CsvWriter {

    private static final Logger logger = LoggerFactory.getLogger(StandardCsvWriter.class);

    private final CsvFormatter csvFormatter;
    private final String[] headers;
    private final Writer csvWriter;
    private RotatableWriter rotatableWriter;

    StandardCsvWriter(File csvFile, String[] headers, CsvPreference csvPreference,
            CsvAuditEventHandlerConfiguration config) throws IOException {
        Reject.ifTrue(config.getSecurity().isEnabled(), "StandardCsvWriter should not be used if security is enabled");
        boolean fileAlreadyInitialized = csvFile.exists();
        if (fileAlreadyInitialized) {
            try (ICsvMapReader reader = new CsvMapReader(new BufferedReader(new FileReader(csvFile)), csvPreference)) {
                final String[] actualHeaders = reader.getHeader(true);
                // Assert that the 2 headers equals.
                if (actualHeaders == null) {
                    fileAlreadyInitialized = false;
                } else {
                    if (actualHeaders.length != headers.length) {
                        throw new IOException("Resuming an existing CSV file but the headers do not match.");
                    }
                    for (int idx = 0; idx < actualHeaders.length; idx++) {
                        if (!actualHeaders[idx].equals(headers[idx])) {
                            throw new IOException("Resuming an existing CSV file but the headers do not match.");
                        }
                    }
                }
            }
        }
        this.headers = checkNotNull(headers, "The headers can't be null.");
        csvFormatter = new CsvFormatter(csvPreference);
        csvWriter = constructWriter(csvFile, fileAlreadyInitialized, config);

        if (rotatableWriter != null) {
            rotatableWriter.registerRotationHooks(new CsvRotationHooks(csvFormatter, headers));
        }

        if (!fileAlreadyInitialized) {
            writeHeader(headers);
            csvWriter.flush();
        }
    }

    private Writer constructWriter(File csvFile, boolean append, CsvAuditEventHandlerConfiguration config)
            throws IOException {
        TextWriter textWriter;
        if (config.getFileRotation().isRotationEnabled()) {
            rotatableWriter = new RotatableWriter(csvFile, config, append);
            textWriter = rotatableWriter;
        } else {
            textWriter = new TextWriter.Stream(new FileOutputStream(csvFile, append));
        }

        if (config.getBuffering().isEnabled()) {
            EventBufferingConfiguration bufferConfig = config.getBuffering();
            textWriter = new AsynchronousTextWriter("CsvHandler", bufferConfig.isAutoFlush(), textWriter);
        }
        return new TextWriterAdapter(textWriter);
    }

    /**
     * Forces rotation of the writer.
     * <p>
     * Rotation is possible only if file rotation is enabled.
     *
     * @return {@code true} if rotation was done, {@code false} otherwise.
     * @throws IOException
     *          If an error occurs
     */
    @Override
    public boolean forceRotation() throws IOException {
        return rotatableWriter != null ? rotatableWriter.forceRotation() : false;
    }

    public void writeHeader(String... headers) throws IOException {
        csvWriter.write(csvFormatter.formatHeader(headers));
    }

    /**
     * Write a row into the CSV files.
     * @param values The keys of the {@link Map} have to match the column's header.
     * @throws IOException
     */
    @Override
    public void writeEvent(Map<String, String> values) throws IOException {
        csvWriter.write(csvFormatter.formatEvent(values, headers));
    }

    /**
     * Flush the data into the CSV file.
     * @throws IOException
     */
    public void flush() throws IOException {
        csvWriter.flush();
    }

    @Override
    public void close() throws IOException {
        csvWriter.close();
    }

}
