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
import java.util.Map;

import org.supercsv.io.CsvMapWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * Responsible for formatting audit events and column headers as CSV strings.
 */
class CsvFormatter {

    final StringWriter stringWriter;
    final CsvMapWriter csvWriter;

    public CsvFormatter(CsvPreference csvPreference) {
        stringWriter = new StringWriter();
        csvWriter = new CsvMapWriter(stringWriter, csvPreference, false);
    }

    public String formatHeader(String[] headers) throws IOException {
        csvWriter.writeHeader(headers);
        return takeBufferContents();
    }

    public String formatEvent(Map<String, String> values, String[] headers) throws IOException {
        csvWriter.write(values, headers);
        return takeBufferContents();
    }

    private String takeBufferContents() {
        String bufferContents = getBufferContents();
        clearBuffer();
        return bufferContents;
    }

    private String getBufferContents() {
        return stringWriter.toString();
    }

    private void clearBuffer() {
        stringWriter.getBuffer().setLength(0);
    }

}
