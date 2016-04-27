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
import java.util.Map;

/**
 * Responsible for writing to a CSV file.
 */
interface CsvWriter extends AutoCloseable {

    /**
     * Forces rotation of the writer.
     * <p>
     * Rotation is possible only if file rotation is enabled.
     *
     * @return {@code true} if rotation was done, {@code false} otherwise.
     * @throws IOException
     *          If an error occurs
     */
    boolean forceRotation() throws IOException;

    /**
     * Write a row into the CSV files.
     * @param values The keys of the {@link Map} have to match the column's header.
     * @throws IOException
     */
    void writeEvent(Map<String, String> values) throws IOException;

    /**
     * Flush the data into the CSV file.
     * @throws IOException
     */
    void flush() throws IOException;

    void close() throws IOException;
}
