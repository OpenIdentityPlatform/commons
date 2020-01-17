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
package org.forgerock.audit.handlers.jdbc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contains the necessary information to map an event to a database table, and the event fields to the columns
 * in that database table.
 */
public class TableMapping {
    @JsonProperty
    @JsonPropertyDescription("audit.handlers.jdbc.mapping.event")
    private String event;

    @JsonProperty
    @JsonPropertyDescription("audit.handlers.jdbc.mapping.table")
    private String table;

    @JsonProperty
    @JsonPropertyDescription("audit.handlers.jdbc.mapping.fieldToColumn")
    private Map<String, String> fieldToColumn = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Gets the audit event the table mapping is for.
     * @return The audit event the mapping is for.
     */
    public String getEvent() {
        return event;
    }

    /**
     * Sets the audit event the table mapping is for.
     * @param event The audit event the mapping is for.
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * Gets the table name for the mapping.
     * @return The table name for the mapping.
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the table name for the mapping.
     * @param table The table name for the mapping.
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Sets the field to column mapping.
     * @return The field to column mapping.
     */
    public Map<String, String> getFieldToColumn() {
        return Collections.unmodifiableMap(fieldToColumn);
    }

    /**
     * Sets the field to column mapping. The map should be case insensitive.
     * @param fieldToColumn The field to column mapping.
     */
    public void setFieldToColumn(Map<String, String> fieldToColumn) {
        this.fieldToColumn = fieldToColumn;
    }
}
