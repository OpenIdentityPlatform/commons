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

import org.forgerock.audit.events.handlers.AuditEventHandlerConfiguration;

/**
 * A configuration for CSV audit event handler.
 */
public class CSVHandlerConfiguration implements AuditEventHandlerConfiguration {

    private String logDirectory;

    private String recordDelimiter;

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
     * Returns the record delimiter.
     *
     * @return the log delimiter.
     */
    public String getRecordDelimiter() {
        return recordDelimiter;
    }

    /**
     * Sets the record delimiter .
     *
     * @param delimiter
     *            the delimiter.
     */
    public void setRecordDelimiter(String delimiter) {
        recordDelimiter = delimiter;
    }

}
