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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.ICsvMapWriter;

/**
 * This class wraps an ICsvMapWriter and silently adds a last column that is a HMAC of the row to write.
 *
 */
public class CsvHmacMapWriter implements ICsvMapWriter {

    private static final String HMAC = "HMAC";

    private ICsvMapWriter delegate;
    private HmacCalculator hmacCalculator;

    /**
     * Constructs a new CsvHmacWriter.
     *
     * @param delegate the real CsvMapWriter to write to.
     * @param hmacCalculator the HMAC calculator
     */
    public CsvHmacMapWriter(ICsvMapWriter delegate, HmacCalculator hmacCalculator) {
        this.delegate = delegate;
        this.hmacCalculator = hmacCalculator;
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public int getLineNumber() {
        return delegate.getLineNumber();
    }

    @Override
    public int getRowNumber() {
        return delegate.getRowNumber();
    }

    @Override
    public void write(Map<String, ?> values, String... nameMapping) throws IOException {
        write(values, nameMapping, new CellProcessor[nameMapping.length]);
    }

    @Override
    public void writeComment(String comment) throws IOException {
        delegate.writeComment(comment);
    }

    @Override
    public void writeHeader(String... header) throws IOException {
        String[] newHeader = addHMACColumn(header);
        delegate.writeHeader(newHeader);
    }

    @Override
    public void write(Map<String, ?> values, String[] nameMapping, CellProcessor[] processors) throws IOException {
        String[] newNameMapping = addHMACColumn(nameMapping);

        Map<String, Object> newValues = insertHMACSignature(values, nameMapping);

        CellProcessor[] newProcessors = new CellProcessor[newNameMapping.length];
        System.arraycopy(processors, 0, newProcessors, 0, processors.length);
        newProcessors[processors.length] = null;

        delegate.write(newValues, newNameMapping, newProcessors);
    }

    private Map<String, Object> insertHMACSignature(Map<String, ?> values, String[] nameMapping) throws IOException {
        Map<String, Object> newValues = new HashMap<>(values);
        try {
            newValues.put(HMAC, hmacCalculator.calculate(dataToSign(values, nameMapping)));
        } catch (SignatureException e) {
            throw new IOException(e);
        }
        return newValues;
    }

    private String[] addHMACColumn(String... header) {
        String[] newHeader = new String[header.length + 1];
        System.arraycopy(header, 0, newHeader, 0, header.length);
        newHeader[header.length] = HMAC;
        return newHeader;
    }

    private byte[] dataToSign(Map<String, ?> values, String...nameMapping) {
        try {
            return StringUtils.join(nameMapping).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is mandatory in any JVM implementation");
        }
    }

}
