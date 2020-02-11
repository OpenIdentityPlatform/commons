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
package org.forgerock.audit.handlers.syslog;

import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Configuration object for the {@link SyslogAuditEventHandler}.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 *
 * <pre>
    {
      "protocol" : "TCP",
      "host" : "https://forgerock.example.com",
      "port" : 6514,
      "connectTimeout" : 30000,
      "facility" : "local0",
      "severityFieldMappings": [{
        "topic" : "system-status",
        "field"  : "level",
        "valueMappings" : {
          "SEVERE" : "EMERGENCY",
          "WARNING" : "WARNING",
          "INFO" : "INFORMATIONAL"
        },
        "buffering" : {
          "enabled" : "true"
        }
      }]
    }
   </pre>
 */
public class SyslogAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.syslog.transportProtocol")
    private TransportProtocol protocol;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.syslog.host")
    private String host;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.syslog.port")
    private int port;

    @JsonPropertyDescription("audit.handlers.syslog.connectTimeout")
    private int connectTimeout;

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.syslog.facility")
    private Facility facility;

    @JsonProperty
    @JsonPropertyDescription("audit.handlers.syslog.severityFieldMappings")
    private List<SeverityFieldMapping> severityFieldMappings = new ArrayList<>();

    /** Event buffering is disabled by default. */
    @JsonPropertyDescription("audit.handlers.syslog.buffering")
    protected EventBufferingConfiguration buffering = new EventBufferingConfiguration();

    /**
     * Returns the protocol over which messages transmitted to the Syslog daemon.
     *
     * @return the transport protocol.
     */
    public TransportProtocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol over which messages transmitted to the Syslog daemon.
     *
     * @param protocol
     *          the transport protocol.
     */
    public void setProtocol(TransportProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns the hostname of the Syslog daemon to which messages should be published.
     *
     * @return the hostname.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the hostname of the Syslog daemon to which messages should be published.
     *
     * @param host
     *          the hostname.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the port of the Syslog daemon to which messages should be published.
     *
     * @return the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port of the Syslog daemon to which messages should be published.
     *
     * @param port
     *          the port.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the timeout after which attempts to connect to the Syslog daemon will be abandoned.
     * <p/>
     * Only applies when {@link TransportProtocol#TCP} is active.
     *
     * @return the connect timeout.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the timeout after which attempts to connect to the Syslog daemon will be abandoned.
     * <p/>
     * Only applies when {@link TransportProtocol#TCP} is active.
     *
     * @param connectTimeout
     *          the connect timeout.
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Returns the facility constant that should be applied to all Syslog messages.
     *
     * @return the facility.
     *
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.1">RFC-5424 section 6.2.1</a>
     */
    public Facility getFacility() {
        return facility;
    }

    /**
     * Sets the facility constant that should be applied to all Syslog messages.
     *
     * @param facility
     *          the facility.
     *
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.1">RFC-5424 section 6.2.1</a>
     */
    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    /**
     * Returns the configurations for mapping audit event field values to Syslog severity values.
     *
     * @return the severity field mappings.
     */
    public List<SeverityFieldMapping> getSeverityFieldMappings() {
        return severityFieldMappings;
    }

    /**
     * Sets the configurations for mapping audit event field values to Syslog severity values.
     *
     * @param severityFieldMappings
     *          the severity field mappings.
     */
    public void setSeverityFieldMappings(List<SeverityFieldMapping> severityFieldMappings) {
        this.severityFieldMappings = severityFieldMappings;
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
        return false;
    }

    /**
     * Encapsulates configuration for mapping audit event field values to Syslog severity values.
     */
    public static final class SeverityFieldMapping {

        @JsonProperty(required = true)
        @JsonPropertyDescription("audit.handlers.syslog.severityFieldMapping.topic")
        private String topic;

        @JsonProperty(required = true)
        @JsonPropertyDescription("audit.handlers.syslog.severityFieldMapping.field")
        private String field;

        @JsonProperty(required = true)
        @JsonPropertyDescription("audit.handlers.syslog.severityFieldMapping.valueMappings")
        private Map<String, Severity> valueMappings = new HashMap<>();

        /**
         * Returns the name of the event topic to which this mapping applies.
         *
         * @return the event topic name.
         */
        public String getTopic() {
            return topic;
        }

        /**
         * Sets the name of the event topic to which this mapping applies.
         *
         * @param topic
         *          the event topic name.
         */
        public void setTopic(String topic) {
            this.topic = topic;
        }

        /**
         * Returns the name of the event topic field to which this mapping applies.
         * <p/>
         * If the chosen field is nested, JsonPointer notation should be used.
         *
         * @return the event topic field name.
         */
        public String getField() {
            return field;
        }

        /**
         * Sets the name of the event topic field to which this mapping applies.
         *
         * @param field
         *          the event topic field name.
         */
        public void setField(String field) {
            this.field = field;
        }

        /**
         * Returns the mapping of audit event values to Syslog severity values.
         *
         * @return the value mappings.
         */
        public Map<String, Severity> getValueMappings() {
            return unmodifiableMap(valueMappings);
        }

        /**
         * Sets the mapping of audit event values to Syslog severity values.
         *
         * @param valueMappings
         *          the value mappings.
         */
        public void setValueMappings(Map<String, Severity> valueMappings) {
            this.valueMappings = new HashMap<>(valueMappings);
        }
    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.syslog.buffering.enabled")
        private boolean enabled;

        @JsonPropertyDescription("audit.handlers.syslog.buffering.maxSize")
        private int maxSize = 5000;

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

    }
}
