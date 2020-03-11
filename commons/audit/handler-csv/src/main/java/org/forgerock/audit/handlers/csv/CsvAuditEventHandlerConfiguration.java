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

import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;
import org.forgerock.util.Reject;
import org.forgerock.util.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * A configuration for CSV audit event handler.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 *
 * <pre>
 *  {
 *    "name" : "csv",
 *    "topics": [ "access", "activity", "config", "authentication" ],
 *    "logDirectory" : "/path/to/audit/files/",
 *    "formatting" : {
 *      "quoteChar" : "\"",
 *      "delimiterChar" : ",",
 *      "endOfLineSymbols" : "\n"
 *    },
 *    "security" : {
 *      "enabled" : "true",
 *      "filename" : "/path/to/keystore.jks",
 *      "password" : "correcthorsebatterystaple",
 *      "signatureInterval" : "3 seconds"
 *    },
 *    "buffering" : {
 *      "enabled" : "true",
 *      "autoFlush" : "true"
 *    }
 *  }
 * </pre>
 */
public class CsvAuditEventHandlerConfiguration extends FileBasedEventHandlerConfiguration {

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.csv.logDirectory")
    private String logDirectory;

    @JsonPropertyDescription("audit.handlers.csv.formatting")
    private CsvFormatting formatting = new CsvFormatting();

    @JsonPropertyDescription("audit.handlers.csv.security")
    private CsvSecurity security = new CsvSecurity();

    /** Event buffering is disabled by default. */
    @JsonPropertyDescription("audit.handlers.csv.buffering")
    protected EventBufferingConfiguration buffering = new EventBufferingConfiguration();

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
     * Returns the CSV formatting options.
     *
     * @return the CSV formatting options.
     */
    public CsvFormatting getFormatting() {
        return formatting;
    }

    /**
     * Sets the CSV formatting options.
     *
     * @param formatting
     *            the CSV formatting options to set.
     */
    public void setFormatting(CsvFormatting formatting) {
        this.formatting = Reject.checkNotNull(formatting);
    }

    /**
     * Returns the CSV tamper evident options.
     *
     * @return the CSV tamper evident options.
     */
    public CsvSecurity getSecurity() {
        return security;
    }

    /**
     * Sets the CSV tamper evident options.
     *
     * @param security
     *            the CSV tamper evident options to set.
     */
    public void setSecurity(CsvSecurity security) {
        this.security = Reject.checkNotNull(security);
    }

    /**
     * Returns the configuration for events buffering.
     *
     * @return the configuration
     */
    public EventBufferingConfiguration getBuffering() {
        return buffering;
    }

    /**
     * Sets the configuration for events buffering.
     *
     * @param bufferingConfiguration
     *            The configuration
     */
    public void setBufferingConfiguration(EventBufferingConfiguration bufferingConfiguration) {
        this.buffering = bufferingConfiguration;
    }

    @Override
    public boolean isUsableForQueries() {
        return true;
    }

    /**
     * Contains the csv writer configuration parameters.
     */
    public static class CsvFormatting {
        @JsonPropertyDescription("audit.handlers.csv.formatting.quoteChar")
        private char quoteChar = '"';

        @JsonPropertyDescription("audit.handlers.csv.formatting.delimiterChar")
        private char delimiterChar = ',';

        @JsonPropertyDescription("audit.handlers.csv.formatting.endOfLineSymbols")
        private String endOfLineSymbols = System.getProperty("line.separator");

        /**
         * Gets the character to use to quote the csv entries.
         * @return The quote character.
         */
        public char getQuoteChar() {
            return quoteChar;
        }

        /**
         * Sets the character to use to quote the csv entries.
         * @param quoteChar The quote character.
         */
        public void setQuoteChar(char quoteChar) {
            this.quoteChar = quoteChar;
        }

        /**
         * Gets the character to use to delimit the csv entries.
         * @return The character used to delimit the entries.
         */
        public char getDelimiterChar() {
            return delimiterChar;
        }

        /**
         * Sets the character to use to delimit the csv entries.
         * @param delimiterChar The character used to delimit the entries.
         */
        public void setDelimiterChar(char delimiterChar) {
            this.delimiterChar = delimiterChar;
        }

        /**
         * Gets the end of line symbol.
         * @return The end of line symbol.
         */
        public String getEndOfLineSymbols() {
            return endOfLineSymbols;
        }

        /**
         * Gets the end of line symbol.
         * @param endOfLineSymbols The end of line symbol.
         */
        public void setEndOfLineSymbols(String endOfLineSymbols) {
            this.endOfLineSymbols = endOfLineSymbols;
        }
    }

    /**
     * Contains the configuration parameters to configure tamper evident logging.
     */
    public static class CsvSecurity {

        @JsonPropertyDescription("audit.handlers.csv.security.enabled")
        private boolean enabled = false;

        @JsonPropertyDescription("audit.handlers.csv.security.filename")
        private String filename;

        @JsonPropertyDescription("audit.handlers.csv.security.password")
        private String password;

        @JsonPropertyDescription("audit.handlers.csv.security.keyStoreHandlerName")
        private String keyStoreHandlerName;

        @JsonPropertyDescription("audit.handlers.csv.security.signatureInterval")
        private String signatureInterval;

        @JsonIgnore
        private Duration signatureIntervalDuration;

        /**
         * Enables tamper evident logging. By default tamper evident logging is disabled.
         * @param enabled True - To enable tamper evident logging.
         *                False - To disable tamper evident logging.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         *
         * Gets tamper evident logging enabled status. By default tamper evident logging is disabled.
         * @return True - If tamper evident logging enabled.
         *         False - If tamper evident logging disabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the location of the keystore to be used.
         * @param filename The location of the keystore.
         */
        public void setFilename(String filename) {
            this.filename = filename;
        }

        /**
         * Gets the location of the keystore to be used.
         * @return The location of the keystore.
         */
        public String getFilename() {
            return filename;
        }

        /**
         * Sets the password of the keystore.
         * @param password The password of the keystore.
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Gets the password of the keystore.
         * @return The password of the keystore.
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the signature's interval.
         * @param signatureInterval The time's interval to insert periodically a signature.
         */
        public void setSignatureInterval(String signatureInterval) {
            this.signatureInterval = signatureInterval;
            this.signatureIntervalDuration = Duration.duration(signatureInterval);
        }

        /**
         * Gets the signature's interval.
         * @return The time's interval to insert periodically a signature.
         */
        public String getSignatureInterval() {
            return signatureInterval;
        }

        /**
         * Get's {@link #getSignatureInterval()} value as a {@link Duration}.
         * @return The signature internval as a Duration object.
         */
        public Duration getSignatureIntervalDuration() {
            return signatureIntervalDuration;
        }

        /**
         * Set the key store handler name.
         * @param keyStoreName The name.
         */
        public void setKeyStoreHandlerName(String keyStoreName) {
            this.keyStoreHandlerName = keyStoreName;
        }

        /**
         * Get the key store handler name.
         * @return The name.
         */
        public String getKeyStoreHandlerName() {
            return keyStoreHandlerName;
        }

    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.csv.buffering.enabled")
        private boolean enabled;

        @JsonPropertyDescription("audit.handlers.csv.buffering.autoFlush")
        private boolean autoFlush = true;

        /**
         * Indicates if event buffering is enabled.
         *
         * @return {@code true} if buffering is enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the buffering status.
         *
         * @param enabled
         *            Indicates if buffering is enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Indicates if events are automatically flushed after being written.
         *
         * @return {@code true} if events must be flushed
         */
        public boolean isAutoFlush() {
            return autoFlush;
        }

        /**
         * Sets the auto flush indicator.
         *
         * @param auto
         *            Indicates if events are automatically flushed after being written.
         */
        public void setAutoFlush(boolean auto) {
            this.autoFlush = auto;
        }

    }
}
