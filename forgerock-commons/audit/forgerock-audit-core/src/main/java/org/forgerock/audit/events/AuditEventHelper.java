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

package org.forgerock.audit.events;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;

import java.util.List;

/**
 * Helper methods for AuditEvents.
 */
public final class AuditEventHelper {

    private static final String PROPERTIES = "properties";

    /** Json String value type. */
    public static final String STRING_TYPE = "string";
    /** Json Object value type. */
    public static final String OBJECT_TYPE = "object";
    /** Json boolean value type. */
    public static final String BOOLEAN_TYPE = "boolean";
    /** Json number value type. */
    public static final String NUMBER_TYPE = "number";
    /** Json array value type. */
    public static final String ARRAY_TYPE = "array";

    private static final String TYPE = "type";
    private static final String SCHEMA = "schema";
    private static final String REQUIRED = "required";
    private static final String LOG_TO = "logTo";

    private AuditEventHelper() {

    }

    /**
     * Gets whether a AuditEvent property is required.
     * @param auditEvent the audit event to get the property of.
     * @param property the property to check if required.
     * @return true if the property is required; false otherwise.
     */
    public static boolean isPropertyRequired(final JsonValue auditEvent, final JsonPointer property) {
        return auditEvent.get(SCHEMA).get(PROPERTIES).get(property).get(REQUIRED).defaultTo(false).asBoolean();
    }

    /**
     * Gets a AuditEvent property type.
     * @param auditEvent the audit event to get the property of.
     * @param property the property to check if required.
     * @return true if the property is required; false otherwise.
     * @throws org.forgerock.json.resource.ResourceException if the property is unknown
     */
    public static String getPropertyType(final JsonValue auditEvent, final JsonPointer property)
            throws ResourceException {

        final String[] pointers  = property.toArray();
        JsonValue properties = auditEvent.get(SCHEMA);
        for (final String pointer : pointers) {
            properties = properties.get(PROPERTIES).get(pointer);
            if (properties == null || properties.isNull()) {
                throw new InternalServerErrorException("Unknown audit event property: " + property.toString());
            }
        }
        return properties.get(TYPE).asString();
    }

    /**
     * Gets the AuditEventHandlers that the audit event is configure to log to.
     * @param auditEvent the audit event JsonValue definition.
     * @return List of audit event handler names to log to.
     */
    public static List<String> getConfiguredAuditEventHandlers(final JsonValue auditEvent) {
        return auditEvent.get(LOG_TO).asList(String.class);
    }

    /**
     * Gets the Audit Event schema properties.
     * @param auditEvent the audit event JsonValue definition.
     * @return JsonValue containing all the properties for the audit event.
     * @throws ResourceException if no audit event is defined
     */
    public static JsonValue getAuditEventProperties(final JsonValue auditEvent) throws ResourceException {
        if (auditEvent == null || auditEvent.isNull()) {
            throw new InternalServerErrorException("Can't get properties for an undefined audit event");
        }
        return auditEvent.get(SCHEMA).get(PROPERTIES);
    }

    /**
     * Gets the Audit Event schema.
     * @param auditEvent the audit event JsonValue definition.
     * @return JsonValue containing the schema object for the audit event.
     * @throws ResourceException if no audit event is defined
     */
    public static JsonValue getAuditEventSchema(final JsonValue auditEvent) throws ResourceException {
        if (auditEvent == null || auditEvent.isNull()) {
            throw new InternalServerErrorException("Can't get the schema for an undefined audit event");
        }
        return auditEvent.get(SCHEMA);
    }

    /**
     * Converts JsonPointer field identifier to dotted-path form.
     *
     * @param fieldName The JsonPointer reference to a field within a JSON object.
     * @return The field name in dotted-path form.
     */
    public static String jsonPointerToDotNotation(final String fieldName) {
        String newPath = fieldName;
        if (fieldName.startsWith("/")) {
            newPath = fieldName.substring(1);
        }
        return (newPath == null) ? null : newPath.replace('/', '.');
    }

    /**
     * Converts dotted-path field identifier to JsonPointer form.
     *
     * @param fieldName The dotted-path reference to a field within a JSON object.
     * @return The field name in JsonPointer form.
     */
    public static String dotNotationToJsonPointer(final String fieldName) {
        return (fieldName == null) ? null : fieldName.replace('.', '/');
    }

}
