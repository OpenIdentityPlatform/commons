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

import static org.forgerock.audit.util.ResourceExceptionsUtil.adapt;
import static org.forgerock.audit.util.ResourceExceptionsUtil.notSupported;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

/**
 * The handler publishes audit events formatted using {@link SyslogFormatter} to a syslog daemon using
 * the configured {@link SyslogPublisher}. The publisher is initialized by {@link #configure} and flushed after each
 * write.
 */
public class SyslogAuditEventHandler extends AuditEventHandlerBase<SyslogAuditEventHandlerConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(SyslogAuditEventHandler.class);

    private volatile SyslogAuditEventHandlerConfiguration config;
    private volatile Map<String, JsonValue> auditEventsMetaData;
    private volatile SyslogPublisher publisher;
    private volatile SyslogFormatter formatter;

    @Override
    public synchronized void setAuditEventsMetaData(Map<String, JsonValue> auditEventsMetaData) {
        this.auditEventsMetaData = auditEventsMetaData;
        updateFormatter();
    }

    @Override
    public void configure(SyslogAuditEventHandlerConfiguration config) throws ResourceException {
        Reject.ifNull(config.getProtocol(),
                "Syslog transport 'protocol' of TCP or UDP is required");
        Reject.ifNull(config.getHost(),
                "Syslog destination server 'host' is required");
        Reject.ifTrue(config.getPort() < 0 || config.getPort() > 65535,
                "Syslog destination server 'port' between 0 and 65535 is required");
        Reject.ifNull(config.getFacility(),
                "Syslog 'facility' is required");
        Reject.ifTrue(config.getProtocol() == TransportProtocol.TCP && config.getConnectTimeout() == 0,
                "Syslog 'connectTimeout' is required for TCP connections");

        synchronized (this) {
            InetSocketAddress socketAddress = new InetSocketAddress(config.getHost(), config.getPort());
            this.publisher = config.getProtocol().getPublisher(socketAddress, config);
            this.config = config;
            updateFormatter();
        }
        logger.debug("Successfully configured Syslog audit event handler.");
    }

    private void updateFormatter() {
        if (auditEventsMetaData != null && config != null) {
            formatter = new SyslogFormatter(auditEventsMetaData, config);
        }
    }

    @Override
    public Class<SyslogAuditEventHandlerConfiguration> getConfigurationClass() {
        return SyslogAuditEventHandlerConfiguration.class;
    }

    /**
     * Closes the connections established by {@link SyslogPublisher}.
     */
    @Override
    public void close() {
        synchronized (publisher) {
            publisher.closeConnection();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(
            Context context,
            ActionRequest actionRequest) {
        return notSupported(actionRequest).asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            Context context,
            String resourceId,
            ActionRequest actionRequest) {
        return notSupported(actionRequest).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(
            Context context,
            CreateRequest createRequest) {

        try {

            final String topic = createRequest.getResourcePath();
            final JsonValue auditEvent = createRequest.getContent();
            final String syslogMessage = formatAsSyslogMessage(topic, auditEvent);
            synchronized (publisher) {
                publisher.publishSyslogMessages(Collections.singletonList(syslogMessage));
            }

            return newResourceResponse(
                    auditEvent.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                    null,
                    auditEvent.clone()).asPromise();

        } catch (Exception ex) {
            return adapt(ex).asPromise();
        }
    }

    private String formatAsSyslogMessage(String topic, JsonValue auditEvent) throws ResourceException {
        if (!formatter.canFormat(topic)) {
            throw new InternalServerErrorException("Unable to format " + topic + " audit event");
        }
        try {
            return formatter.format(topic, auditEvent);
        } catch (Exception ex) {
            throw new BadRequestException(ex);
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(
            Context context,
            QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler) {
        return notSupported(queryRequest).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(
            Context context,
            String resourceId,
            ReadRequest readRequest) {
        return notSupported(readRequest).asPromise();
    }

}
