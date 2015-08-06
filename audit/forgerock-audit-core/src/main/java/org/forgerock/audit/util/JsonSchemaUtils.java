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

package org.forgerock.audit.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.schema.JsonSchema;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains Utility methods for dealing with JsonSchema data.
 */
public final class JsonSchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaUtils.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String PROPERTIES = "properties";
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String OBJECT = "object";
    private static final String FORWARD_SLASH = "/";

    private JsonSchemaUtils() {
        //prevent instantiation
    }

    /**
     * Generates the Set of {@link org.forgerock.json.fluent.JsonPointer JsonPointer}s in a given JsonSchema.
     * @param schema the JsonSchema to generate the {@link org.forgerock.json.fluent.JsonPointer JsonPointer}s for
     * @return a set of JsonPointers as strings
     */
    public static Set<String> generateJsonPointers(final JsonValue schema) {
        return concatPrefix(getPointers(schema.get(PROPERTIES)), schema.get(ID).asString());
    }

    private static Set<String> getPointers(final JsonValue properties) {
        final Set<String> pointers = new LinkedHashSet<>();
        final Set<String> keys = properties.keys();
        for (String key : keys) {
            final JsonValue property = properties.get(key);
            if (OBJECT.equals(property.get(TYPE).asString())) {
                //get pointers in sub object
                final Set<String> subPointers = getPointers(property.get(PROPERTIES));
                pointers.addAll(concatPrefix(subPointers, key));
            } else {
                //add the primitive type id
                pointers.add(key);
            }
        }
        return pointers;
    }

    private static Set<String> concatPrefix(final Set<String> pointers, final String id) {
        final Set<String> newPointers = new LinkedHashSet<>();
        if (pointers.isEmpty()) {
            newPointers.add(id);
            return newPointers;
        }
        for (String pointer : pointers) {
            final StringBuilder stringBuilder = new StringBuilder();
            if (FORWARD_SLASH.equals(id)) {
                stringBuilder.append(id).append(pointer);
            } else {
                stringBuilder.append(id).append(FORWARD_SLASH).append(pointer);
            }
            newPointers.add(stringBuilder.toString());
        }
        return newPointers;
    }

    /**
     * Gets the configuration schema for an audit event handler as json schema.
     * @param auditEventHandler The audit event handler to get the config schema for.
     * @return The config schema as json schema.
     * @throws AuditException If any error occurs parsing the config class for schema.
     */
    public static JsonValue getAuditEventHandlerConfigurationSchema(final AuditEventHandler auditEventHandler)
            throws AuditException {
        try {
            JsonSchema schema = mapper.generateJsonSchema(auditEventHandler.getConfigurationClass());
            return new JsonValue(mapper.readValue(schema.toString(), Map.class));
        } catch (IOException e) {
            logger.error("Unable to parse configuration class schema for class {}",
                    auditEventHandler.getClass().getName(), e);
            throw new AuditException(
                    String.format("Unable to parse configuration class schema for class %s",
                            auditEventHandler.getClass().getName()), e);
        }
    }
}
