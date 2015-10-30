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
package org.forgerock.audit.handlers.csv;

import static org.forgerock.util.Reject.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.handlers.csv.CSVAuditEventHandlerConfiguration.CsvSecurity;
import org.forgerock.audit.events.handlers.writers.AsynchronousTextWriter;
import org.forgerock.audit.events.handlers.writers.RotatableWriter;
import org.forgerock.audit.events.handlers.writers.TextWriter;
import org.forgerock.audit.events.handlers.writers.TextWriterAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;


/**
 * This a thin wrap above the ICsvMapWriter from supercsv, as we need to keep consistency when resuming a CSV file :
 * same headers, last HMAC, last signature.
 */
public class CsvWriter implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CsvWriter.class);

    private final String[] headers;
    private ICsvMapWriter csvWriter;
    private RotatableWriter rotatableWriter;

    CsvWriter(File csvFile, String[] headers, CsvPreference csvPreference, CSVAuditEventHandlerConfiguration config)
            throws IOException {
        boolean fileAlreadyInitialized = csvFile.exists();
        final CsvSecurity securityConfiguration = config.getSecurity();
        CsvSecureVerifier verifier = null;
        if (fileAlreadyInitialized) {
            // Run the CsvVerifier to check that the file was not tampered,
            // and get the headers and lastSignature for free
            try (ICsvMapReader reader = new CsvMapReader(new BufferedReader(new FileReader(csvFile)), csvPreference)) {
                final String[] actualHeaders;
                if (securityConfiguration.isEnabled()) {
                    verifier = new CsvSecureVerifier(
                            reader, securityConfiguration.getFilename(), securityConfiguration.getPassword());
                    if (!verifier.verify()) {
                        logger.info("The existing secure CSV file was tampered.");
                        throw new IOException("The CSV file was tampered.");
                    } else {
                        logger.info("The existing secure CSV file was not tampered.");
                    }
                    actualHeaders = verifier.getHeaders();
                } else {
                    actualHeaders = reader.getHeader(true);
                }
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

        csvWriter = new CsvMapWriter(constructWriter(csvFile, fileAlreadyInitialized, config), csvPreference, false);
        if (securityConfiguration.isEnabled()) {
            csvWriter = new CsvSecureMapWriter(csvWriter, securityConfiguration.getFilename(),
                    securityConfiguration.getPassword(), securityConfiguration.getSignatureIntervalDuration(),
                    fileAlreadyInitialized);
            CsvSecureMapWriter csvSecureWriter = (CsvSecureMapWriter) csvWriter;
            if (fileAlreadyInitialized) {
                csvSecureWriter.setHeader(headers);
                csvSecureWriter.setLastHMAC(verifier.getLastHMAC());
                csvSecureWriter.setLastSignature(verifier.getLastSignature());
            }

            if (rotatableWriter != null) {
                rotatableWriter.registerRotationHooks(csvSecureWriter);
            }
        } else if (rotatableWriter != null) {
            rotatableWriter.registerRotationHooks(new CsvRotationHooks(csvWriter, headers));
        }

        if (!fileAlreadyInitialized) {
            csvWriter.writeHeader(headers);
            csvWriter.flush();
        }
    }

    private Writer constructWriter(File csvFile, boolean append, CSVAuditEventHandlerConfiguration config)
                    throws IOException {
        TextWriter textWriter;
        if (config.getFileRotation().isRotationEnabled()) {
            rotatableWriter = new RotatableWriter(csvFile, config, append);
            textWriter = rotatableWriter;
        }
        else {
            textWriter = new TextWriter.Stream(new FileOutputStream(csvFile, append));
        }

        if (config.getBuffering().isEnabled()) {
            EventBufferingConfiguration bufferConfig = config.getBuffering();
            textWriter = new AsynchronousTextWriter("CsvHandler", bufferConfig.getMaxSize(),
                    bufferConfig.isAutoFlush(), textWriter);
        }
        return new TextWriterAdapter(textWriter);
    }

    /**
     * Write a row into the CSV files.
     * @param values The keys of the {@link Map} have to match the column's header.
     * @throws IOException
     */
    public void writeRow(Map<String, String> values) throws IOException {
        csvWriter.write(values, headers);
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
