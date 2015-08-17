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

import org.forgerock.json.JsonPointer;

import java.util.LinkedHashMap;
import java.util.Map;

public class TableMappingAndParameters {

    private TableMapping tableMapping;
    private Map<String, FieldValuePair> parameters;

    public TableMappingAndParameters(final TableMapping tableMapping) {
        this.tableMapping = tableMapping;
        this.parameters = new LinkedHashMap<>();
    }

    public TableMappingAndParameters(final TableMapping tableMapping, final Map<String, FieldValuePair> parameters) {
        this.tableMapping = tableMapping;
        this.parameters = parameters;
    }

    public TableMapping getTableMapping() {
        return tableMapping;
    }

    public Map<String, FieldValuePair> getParameters() {
        return parameters;
    }

    public String getColumnName(final JsonPointer field) {
        final String fieldString = field.toString();
        if (tableMapping.getFieldToColumn().get(fieldString) != null) {
            return tableMapping.getFieldToColumn().get(fieldString);
        } else {
            //remove forward slash and return the result
            return tableMapping.getFieldToColumn().get(fieldString.substring(1));
        }
    }

    static class FieldValuePair {

        private JsonPointer field;
        private Object value;

        public FieldValuePair(final JsonPointer field, final Object value) {
            this.field = field;
            this.value = value;
        }

        public JsonPointer getField() {
            return field;
        }

        public Object getValue() {
            return value;
        }
    }
}
