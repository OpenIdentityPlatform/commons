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

package org.forgerock.audit.handlers.syslog;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration object for the {@link SyslogAuditEventHandler}.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 *
 * <pre>
 *  {
 *    "protocol" : "TCP",
 *    "host" : "https://forgerock.example.com",
 *    "port" : 6514,
 *    "connectTimeout" : 30000,
 *    "facility" : "local0",
 *    "productName" : "OpenAM"
 *  }
 * </pre>
 */
public class SyslogAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty(required=true)
    private TransportProtocol protocol;

    @JsonProperty(required=true)
    private String host;

    @JsonProperty(required=true)
    private int port;

    private int connectTimeout;

    @JsonProperty(required=true)
    private Facility facility;

    @JsonProperty(required=true)
    private String productName;

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
     * Only applies when {@link TransportProtocol.TCP} is active.
     *
     * @return the connect timeout.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the timeout after which attempts to connect to the Syslog daemon will be abandoned.
     * <p/>
     * Only applies when {@link TransportProtocol.TCP} is active.
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
     * Returns the product name constant that should be applied to all Syslog messages.
     *
     * @return the product name.
     *
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.5">RFC-5424 section 6.2.5</a>
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Sets the product name constant that should be applied to all Syslog messages.
     *
     * @param productName
     *          the product name.
     *
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.5">RFC-5424 section 6.2.5</a>
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SyslogAuditEventHandlerConfiguration that = (SyslogAuditEventHandlerConfiguration) o;

        return this.port == that.port &&
                this.connectTimeout == that.connectTimeout &&
                this.protocol == that.protocol &&
                this.facility == that.facility &&
                !(this.productName != null ? !this.productName.equals(that.productName) : that.productName != null) &&
                !(this.host != null ? !this.host.equals(that.host) : that.host != null);
    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + connectTimeout;
        result = 31 * result + (facility != null ? facility.hashCode() : 0);
        result = 31 * result + (productName != null ? productName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SyslogAuditEventHandlerConfiguration{" +
                "protocol=" + protocol +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", connectTimeout=" + connectTimeout +
                ", facility=" + facility +
                ", productName='" + productName + '\'' +
                '}';
    }
}
