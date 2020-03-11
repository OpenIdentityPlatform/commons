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
package org.forgerock.audit.handlers.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.JsonPointer;

/**
 * Stores a pair of {@link TableMapping} and a map of parameters.
 */
class TableMappingParametersPair {

    private TableMapping tableMapping;
    private Map<String, Object> parameters;

    /**
     * Creates a TableMappingParametersPair given a {@link TableMapping}. A empty parameter map is created.
     * @param tableMapping The {@link TableMapping} to create the pair with.
     */
    public TableMappingParametersPair(final TableMapping tableMapping) {
        this(tableMapping, new LinkedHashMap<String, Object>());
    }

    /**
     * Creates a TableMappingParametersPair given a {@link TableMapping} and parameter map.
     * @param tableMapping A {@link TableMapping}.
     * @param parameters A Map of replacement parameters.
     */
    public TableMappingParametersPair(final TableMapping tableMapping, final Map<String, Object> parameters) {
        this.tableMapping = tableMapping;
        this.parameters = parameters;
    }

    /**
     * Gets the {@link TableMapping} in the pair.
     * @return A {@link TableMapping}.
     */
    public TableMapping getTableMapping() {
        return tableMapping;
    }

    /**
     * Gets the replacement parameters in the pair.
     * @return A map of replacement parameters.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Utility method to get the column name out of a {@link TableMapping}.
     * @param field The {@link JsonPointer} field to get the column of.
     * @return The column name mapped to the given field.
     */
    public String getColumnName(final JsonPointer field) {
        final String fieldString = field.toString();
        if (tableMapping.getFieldToColumn().get(fieldString) != null) {
            return tableMapping.getFieldToColumn().get(fieldString);
        } else {
            //remove forward slash and return the result
            return tableMapping.getFieldToColumn().get(fieldString.substring(1));
        }
    }
}
