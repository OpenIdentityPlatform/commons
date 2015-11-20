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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.supercsv.io.CsvMapWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * Responsible for formatting audit events and column headers as CSV strings.
 * <p/>
 * Objects are assumed to be used from a single thread and are therefore not thread-safe.
 */
class CsvFormatter {

    final StringBuilderWriter writer;
    final CsvMapWriter csvWriter;

    public CsvFormatter(CsvPreference csvPreference) {
        writer = new StringBuilderWriter();
        csvWriter = new CsvMapWriter(writer, csvPreference, false);
    }

    public String formatHeader(String[] headers) throws IOException {
        csvWriter.writeHeader(headers);
        return writer.takeBufferContents();
    }

    public String formatEvent(Map<String, String> values, String[] headers) throws IOException {
        csvWriter.write(values, headers);
        return writer.takeBufferContents();
    }

    /**
     * Adapter that exposes {@link Writer} interface to allow supercsv output to be collected to a {@link StringBuffer}.
     * <p/>
     * This is an alternative to using {@link StringWriter} that avoids unnecessary synchronization.
     * <p/>
     * Due to the lack of synchronization, objects are not thread-safe.
     */
    private static final class StringBuilderWriter extends Writer {

        private final StringBuilder buffer;

        private StringBuilderWriter() {
            buffer = new StringBuilder();
        }

        @Override
        public void write(int c) throws IOException {
            buffer.append(c);
        }

        @Override
        public void write(char cbuf[]) throws IOException {
            buffer.append(cbuf);
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            buffer.append(cbuf, off, len);
        }

        @Override
        public void write(String str) throws IOException {
            buffer.append(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            buffer.append(str, off, len);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        public String takeBufferContents() {
            String s = buffer.toString();
            buffer.setLength(0);
            return s;
        }
    }

}
