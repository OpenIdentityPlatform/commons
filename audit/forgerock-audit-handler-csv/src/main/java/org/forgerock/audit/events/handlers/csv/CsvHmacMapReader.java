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
import java.util.Map;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.ICsvMapReader;

/**
 * This class to read a secure CSV file. It completely hides the last column, which is the HMAC column.
 * It does not do any checking regarding the HMAC validity.
 *
 */
public class CsvHmacMapReader implements ICsvMapReader {

    private static final String HMAC = "HMAC";

    private ICsvMapReader delegate;

    /**
     * Constructs a new CsvHmacWriter.
     *
     * @param delegate the real CsvMapReader to read from.
     */
    public CsvHmacMapReader(ICsvMapReader delegate) {
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
        String[] newNameMapping = addExtraColumn(nameMapping);

        return dropExtraColumn(delegate.read(newNameMapping));
    }

    @Override
    public String[] getHeader(boolean firstLineCheck) throws IOException {
        String[] header = delegate.getHeader(firstLineCheck);
        if (header == null) {
            return null;
        }
        // The last column is the HMAC one.
        String[] result = new String[header.length-1];
        System.arraycopy(header, 0, result, 0, result.length);

        return result;
    }

    @Override
    public Map<String, Object> read(String[] nameMapping, CellProcessor[] processors) throws IOException {
        String[] newNameMapping = addExtraColumn(nameMapping);

        CellProcessor[] newProcessors = new CellProcessor[processors.length + 1];
        System.arraycopy(processors, 0, newProcessors, 0, processors.length);
        newProcessors[processors.length] = null;

        return dropExtraColumn(delegate.read(newNameMapping, newProcessors));
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
        String[] newHeader = new String[header.length + 1];
        System.arraycopy(header, 0, newHeader, 0, header.length);
        newHeader[header.length] = HMAC;
        return newHeader;
    }

    private <T> Map<String, T> dropExtraColumn(Map<String, T> source) {
        if (source != null) {
            source.remove(HMAC);
        }
        return source;
    }

}
