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
 * Copyright 2013 Cybernetica AS
 * Portions copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.audit.handlers.syslog;

import static org.forgerock.audit.events.AuditEventBuilder.EVENT_NAME;
import static org.forgerock.audit.events.AuditEventBuilder.TIMESTAMP;
import static org.forgerock.audit.events.AuditEventHelper.getAuditEventSchema;
import static org.forgerock.audit.events.AuditEventHelper.jsonPointerToDotNotation;
import static org.forgerock.audit.util.JsonSchemaUtils.generateJsonPointers;
import static org.forgerock.audit.util.JsonValueUtils.extractValue;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for formatting an {@link AuditEvent}'s JSON representation as an RFC-5424 compliant Syslog message.
 *
 * Objects are immutable and can therefore be freely shared across threads without synchronization.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5424">RFC-5424</a>
 */
public class SyslogFormatter {

    private static final Logger logger = LoggerFactory.getLogger(SyslogFormatter.class);

    private static final String SYSLOG_SPEC_VERSION = "1";
    private static final String NIL_VALUE = "-";

    private final Map<String, StructuredDataFormatter> structuredDataFormatters;
    private final String hostname;
    private final String appName;
    private final String procId;
    private final Facility facility;

    /**
     * Construct a new SyslogFormatter.
     *
     * @param auditEventsMetaData Schemas and additional meta-data for known audit event topics.
     * @param config Configuration options.
     * @param localHostNameProvider Strategy for obtaining hostname of current server.
     */
    public SyslogFormatter(Map<String, JsonValue> auditEventsMetaData, SyslogAuditEventHandlerConfiguration config,
            LocalHostNameProvider localHostNameProvider) {

        Reject.ifNull(localHostNameProvider, "LocalHostNameProvider must not be null");

        this.hostname = getLocalHostName(localHostNameProvider);
        this.procId = String.valueOf(SyslogFormatter.class.hashCode());
        this.appName = config.getProductName();
        this.facility = config.getFacility();
        this.structuredDataFormatters = Collections.unmodifiableMap(
                createStructuredDataFormatters(appName, auditEventsMetaData));
    }

    /**
     * Translate the provided <code>auditEvent</code> to an RFC-5424 compliant Syslog message.
     *
     * @param topic The topic of the provided <code>auditEvent</code>.
     * @param auditEvent The audit event to be formatted.
     *
     * @return an RFC-5424 compliant Syslog message.
     *
     * @throws IllegalArgumentException If this formatter has no meta-data for the specified <code>topic</code>.
     */
    public String format(String topic, JsonValue auditEvent) {

        Reject.ifFalse(canFormat(topic), "Unknown event topic");

        final Severity severity = Severity.INFORMATIONAL; // TODO: Establish from auditEvent, (schema-dependent)
        final String priority = String.valueOf(calculatePriorityValue(facility, severity));
        final String timestamp = auditEvent.get(TIMESTAMP).asString();
        final String msgId = auditEvent.get(EVENT_NAME).asString();
        final String structuredData = structuredDataFormatters.get(topic).format(auditEvent);
        final String msg = "";

        return "<" + priority + ">"         // https://tools.ietf.org/html/rfc5424#section-6.2.1    PRI
                + SYSLOG_SPEC_VERSION + " " // https://tools.ietf.org/html/rfc5424#section-6.2.2    VERSION
                + timestamp + " "           // https://tools.ietf.org/html/rfc5424#section-6.2.3    TIMESTAMP
                + hostname + " "            // https://tools.ietf.org/html/rfc5424#section-6.2.4    HOSTNAME
                + appName + " "             // https://tools.ietf.org/html/rfc5424#section-6.2.5    APP-NAME
                + procId + " "              // https://tools.ietf.org/html/rfc5424#section-6.2.6    PROCID
                + msgId + " "               // https://tools.ietf.org/html/rfc5424#section-6.2.7    MSGID
                + structuredData + " "      // https://tools.ietf.org/html/rfc5424#section-6.3      STRUCTURED-DATA
                + msg;                      // https://tools.ietf.org/html/rfc5424#section-6.4      MSG
    }

    /**
     * Returns <code>true</code> if this formatter has been configured to handle events of the specified topic.
     *
     * @param topic The topic of the <code>auditEvent</code> to be formatted.
     *
     * @return <code>true</code> if this formatter has been configured to handle events of the specified topic;
     *         <code>false</code> otherwise.
     */
    public boolean canFormat(String topic) {
        return structuredDataFormatters.containsKey(topic);
    }

    private Map<String, StructuredDataFormatter> createStructuredDataFormatters(
            String productName,
            Map<String, JsonValue> auditEventsMetaData) {

        final Map<String, StructuredDataFormatter> results = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : auditEventsMetaData.entrySet()) {
            results.put(entry.getKey(), new StructuredDataFormatter(productName, entry.getKey(), entry.getValue()));
        }
        return results;
    }

    /**
     * Calculates the Syslog message PRI value.
     *
     * @see <a href=https://tools.ietf.org/html/rfc5424#section-6.2.1>RFC-5424 section 6.2.1</a>
     */
    private int calculatePriorityValue(Facility facility, Severity severityLevel) {
        return (facility.getCode() * 8) + severityLevel.getCode();
    }

    /**
     * Calculates the Syslog message HOSTNAME value.
     *
     * @see <a href=https://tools.ietf.org/html/rfc5424#section-6.2.4>RFC-5424 section 6.2.4</a>
     */
    private String getLocalHostName(LocalHostNameProvider localHostNameProvider) {
        String localHostName = localHostNameProvider.getLocalHostName();
        return localHostName != null ? localHostName : NIL_VALUE;
    }

    /**
     * Responsible for formatting an {@link AuditEvent}'s JSON representation as an RFC-5424 compliant SD-ELEMENT.
     *
     * Objects are immutable and can therefore be freely shared across threads without synchronization.
     *
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.3">RFC-5424 section 6.3</a>
     */
    private static class StructuredDataFormatter {

        private static final String FORGEROCK_IANA_ENTERPRISE_ID = "36733";

        private final String id;
        private final Set<String> fieldNames;

        /**
         * Construct a new StructuredDataFormatter.
         *
         * @param productName Name of the ForgeRock product in which the {@link org.forgerock.audit.AuditService}
         *                    is executing; the SD-ID of each STRUCTURED-DATA element is derived from the
         *                    <code>productName</code> and <code>topic</code>.
         * @param topic Coarse-grained categorisation of the types of audit events that this formatter handles;
         *              the SD-ID of each STRUCTURED-DATA element is derived from the <code>productName</code>
         *              and <code>topic</code>.
         * @param auditEventMetaData Schema and additional meta-data for the audit event topic.
         */
        public StructuredDataFormatter(String productName, String topic, JsonValue auditEventMetaData) {

            Reject.ifNull(productName, "Product name required.");
            Reject.ifNull(topic, "Audit event topic name required.");

            JsonValue auditEventSchema;
            try {
                auditEventSchema = getAuditEventSchema(auditEventMetaData);
            } catch (ResourceException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            id = topic + "." + productName + "@" + FORGEROCK_IANA_ENTERPRISE_ID;
            fieldNames = Collections.unmodifiableSet(generateJsonPointers(auditEventSchema));
        }

        /**
         * Translate the provided <code>auditEvent</code> to an RFC-5424 compliant SD-ELEMENT.
         *
         * @param auditEvent The audit event to be formatted.
         *
         * @return an RFC-5424 compliant SD-ELEMENT.
         */
        public String format(JsonValue auditEvent) {

            StringBuilder sd = new StringBuilder();

            sd.append("[");
            sd.append(id);
            for (String fieldName : fieldNames) {
                sd.append(" ");
                sd.append(formatParamName(fieldName));
                sd.append("=\"");
                sd.append(formatParamValue(extractValue(auditEvent, fieldName)));
                sd.append("\"");
            }
            sd.append("]");

            return sd.toString();
        }

        private String formatParamName(String name) {
            return jsonPointerToDotNotation(name);
        }

        private String formatParamValue(String value) {
            if (value == null) {
                return "";
            } else {
                return value.replaceAll("[\\\\\"\\]]", "\\\\$0");
            }
        }
    }
}
