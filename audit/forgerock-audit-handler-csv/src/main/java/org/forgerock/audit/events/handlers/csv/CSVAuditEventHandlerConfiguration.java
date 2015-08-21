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

import com.fasterxml.jackson.annotation.JsonProperty;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.util.Reject;

/**
 * A configuration for CSV audit event handler.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 *
 * <pre>
 *  {
 *    "logDirectory" : "/tmp/audit",
 *    "csvConfiguration" : {
 *      "quoteChar" : ";"
 *    },
 *    "csvSecurity" : {
 *      "enabled" : true,
 *      "filename" : "/var/secure/secure-audit.jks"
 *    }
 *  }
 * </pre>
 */
public class CSVAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty(required = true)
    private String logDirectory;

    private CsvConfiguration csvConfiguration = new CsvConfiguration();

    private CsvSecurity csvSecurity = new CsvSecurity();

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
     * @param csvConfiguration
     *            the csvConfiguration to set
     */
    public void setCsvConfiguration(CsvConfiguration csvConfiguration) {
        this.csvConfiguration = Reject.checkNotNull(csvConfiguration);
    }



    /**
     * Returns the csvSecurity
     *
     * @return the csvSecurity
     */
    public CsvSecurity getCsvSecurity() {
        return csvSecurity;
    }

    /**
     *
     * @param csvSecurity the csvSecurity to set
     */
    public void setCsvSecurity(CsvSecurity csvSecurity) {
        this.csvSecurity = Reject.checkNotNull(csvSecurity);
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

    public static class CsvSecurity {

        private boolean enabled = false;
        private String filename;
        private String password;

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

    }
}
