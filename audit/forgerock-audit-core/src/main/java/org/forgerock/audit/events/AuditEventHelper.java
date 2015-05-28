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

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;

import java.util.List;
import java.util.Map;

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
    private static final String CONFIG = "config";
    private static final String LOG_TO = "logTo";

    private static final ObjectMapper mapper;

    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }

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
     * Gets whether a AuditEvent property is required.
     * @param auditEvent the audit event to get the property of.
     * @param property the property to check if required.
     * @return true if the property is required; false otherwise.
     * @throws org.forgerock.json.resource.InternalServerErrorException if the property is unknown
     */
    public static String getPropertyType(final JsonValue auditEvent, final JsonPointer property)
            throws ResourceException {
        if (auditEvent.get(SCHEMA).get(PROPERTIES).get(property) == null) {
            throw new InternalServerErrorException("Unknown audit event property: " + property.toString());
        }
        return auditEvent.get(SCHEMA).get(PROPERTIES).get(property).get(TYPE).asString();
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
     */
    public static JsonValue getAuditEventProperties(final JsonValue auditEvent) throws ResourceException {
        if (auditEvent == null || auditEvent.isNull()) {
            throw new InternalServerErrorException("Can't get properties for a null audit event: ");
        }
        return auditEvent.get(SCHEMA).get(PROPERTIES);
    }
}
