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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.ICsvMapReader;

/**
 * This class to read a secure CSV file. It completely hides the last column, which is the HMAC column.
 * It does not do any checking regarding the HMAC validity.
 *
 */
class CsvSecureMapReader implements ICsvMapReader {

    private static final Logger logger = LoggerFactory.getLogger(CsvSecureMapReader.class);

    private static final String HMAC = "HMAC";
    private static final String SIGNATURE = "SIGNATURE";

    private ICsvMapReader delegate;

    /**
     * Constructs a new CsvHmacWriter.
     *
     * @param delegate the real CsvMapReader to read from.
     */
    public CsvSecureMapReader(ICsvMapReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public String get(int n) {
        return delegate.get(n);
    }

    @Override
    public Map<String, String> read(String... nameMapping) throws IOException {
        Map<String, Object> values = read(nameMapping, new CellProcessor[nameMapping.length]);

        Map<String, String> result = new HashMap<>(values.size());
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            result.put(key, value == null ? null : value.toString());
        }

        return result;
    }

    @Override
    public String[] getHeader(boolean firstLineCheck) throws IOException {
        String[] header = delegate.getHeader(firstLineCheck);
        if (header == null) {
            return null;
        }
        // The 2 last columns are the HMAC and SIGNATURE one.
        String[] result = new String[header.length - 2];
        System.arraycopy(header, 0, result, 0, result.length);

        return result;
    }

    @Override
    public Map<String, Object> read(String[] nameMapping, CellProcessor[] processors) throws IOException {
        String[] newNameMapping = addExtraColumn(nameMapping);

        CellProcessor[] newProcessors = new CellProcessor[newNameMapping.length];
        System.arraycopy(processors, 0, newProcessors, 0, processors.length);
        newProcessors[processors.length] = null;
        newProcessors[processors.length + 1] = null;

        return dropExtraColumns(delegate.read(newNameMapping, newProcessors));
    }

    @Override
    public int getLineNumber() {
        return delegate.getLineNumber();
    }

    @Override
    public String getUntokenizedRow() {
        return delegate.getUntokenizedRow();
    }

    @Override
    public int getRowNumber() {
        return delegate.getRowNumber();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    private String[] addExtraColumn(String... header) {
        String[] newHeader = new String[header.length + 2];
        System.arraycopy(header, 0, newHeader, 0, header.length);
        newHeader[header.length] = HMAC;
        newHeader[header.length + 1] = SIGNATURE;
        return newHeader;
    }

    private <T> Map<String, T> dropExtraColumns(Map<String, T> source) {
        if (source != null) {
            source.remove(HMAC);
            source.remove(SIGNATURE);
        }
        return source;
    }
}
