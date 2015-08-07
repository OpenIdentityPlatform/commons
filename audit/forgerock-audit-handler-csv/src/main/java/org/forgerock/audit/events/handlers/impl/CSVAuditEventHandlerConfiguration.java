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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.util.Reject;

/**
 * A configuration for CSV audit event handler.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <pre>
 *  {
 *    "logDirectory" : "/tmp/audit",
 *    "csvConfiguration" : {
 *      "quoteChar" : ";"
 *    }
 *  }
 * </pre>
 */
public class CSVAuditEventHandlerConfiguration {

    @JsonProperty(required=true)
    private String logDirectory;

    private CsvConfiguration csvConfiguration = new CsvConfiguration();

    /**
     * Returns the directory where CSV file is located.
     *
     * @return the location of the CSV file.
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * Sets the directory where CSV file is located.
     *
     * @param directory
     *            the directory.
     */
    public void setLogDirectory(String directory) {
        logDirectory = directory;
    }

    /**
     * Returns the csvConfiguration
     *
     * @return the csvConfiguration
     */
    public CsvConfiguration getCsvConfiguration() {
        return csvConfiguration;
    }

    /**
     *
     * @param csvConfiguration the csvConfiguration to set
     */
    public void setCsvConfiguration(CsvConfiguration csvConfiguration) {
        this.csvConfiguration = Reject.checkNotNull(csvConfiguration);
    }

    public static class CsvConfiguration {

        private char quoteChar = '"';
        private char delimiterChar = ',';
        private String endOfLineSymbols = System.getProperty("line.separator");

        public char getQuoteChar() {
            return quoteChar;
        }

        public void setQuoteChar(char quoteChar) {
            this.quoteChar = quoteChar;
        }

        public int getDelimiterChar() {
            return delimiterChar;
        }

        public void setDelimiterChar(char delimiterChar) {
            this.delimiterChar = delimiterChar;
        }

        public String getEndOfLineSymbols() {
            return endOfLineSymbols;
        }

        public void setEndOfLineSymbols(String endOfLineSymbols) {
            this.endOfLineSymbols = endOfLineSymbols;
        }
    }
}
