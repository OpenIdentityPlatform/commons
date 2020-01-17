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
 * Portions copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.syslog;

import static org.forgerock.audit.util.ResourceExceptionsUtil.adapt;
import static org.forgerock.audit.util.ResourceExceptionsUtil.notSupported;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.net.InetSocketAddress;
import javax.inject.Inject;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.providers.DefaultLocalHostNameProvider;
import org.forgerock.audit.providers.LocalHostNameProvider;
import org.forgerock.audit.providers.ProductInfoProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The handler publishes audit events formatted using {@link SyslogFormatter} to a syslog daemon using
 * the configured {@link SyslogPublisher}. The publisher is flushed after each write.
 */
public class SyslogAuditEventHandler extends AuditEventHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(SyslogAuditEventHandler.class);

    private final SyslogPublisher publisher;
    private final SyslogFormatter formatter;

    /**
     * Create a new SyslogAuditEventHandler instance.
     *
     * @param configuration
     *          Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData
     *          Meta-data for all audit event topics.
     * @param productInfoProvider
     *          Provides info such as product name.
     * @param localHostNameProvider
     *          Provides local host name.
     */
    @Inject
    public SyslogAuditEventHandler(
            final SyslogAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final ProductInfoProvider productInfoProvider,
            @Audit final LocalHostNameProvider localHostNameProvider) {

        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        Reject.ifNull(configuration.getProtocol(),
                "Syslog transport 'protocol' of TCP or UDP is required");
        Reject.ifNull(configuration.getHost(),
                "Syslog destination server 'host' is required");
        Reject.ifTrue(configuration.getPort() < 0 || configuration.getPort() > 65535,
                "Syslog destination server 'port' between 0 and 65535 is required");
        Reject.ifNull(configuration.getFacility(),
                "Syslog 'facility' is required");
        Reject.ifTrue(configuration.getProtocol() == TransportProtocol.TCP && configuration.getConnectTimeout() == 0,
                "Syslog 'connectTimeout' is required for TCP connections");

        InetSocketAddress socketAddress = new InetSocketAddress(configuration.getHost(), configuration.getPort());
        this.publisher = configuration.getProtocol().getPublisher(socketAddress, configuration);
        this.formatter = new SyslogFormatter(
                eventTopicsMetaData,
                configuration,
                getLocalHostNameProvider(localHostNameProvider),
                getProductNameProvider(productInfoProvider));

        logger.debug("Successfully configured Syslog audit event handler.");
    }

    private ProductInfoProvider getProductNameProvider(ProductInfoProvider productInfoProvider) {
        if (productInfoProvider != null) {
            return productInfoProvider;
        } else {
            logger.debug("No {} provided; using default.", ProductInfoProvider.class.getSimpleName());
            return new DefaultProductInfoProvider();
        }
    }

    private LocalHostNameProvider getLocalHostNameProvider(LocalHostNameProvider localHostNameProvider) {
        if (localHostNameProvider != null) {
            return localHostNameProvider;
        } else {
            logger.debug("No {} provided; using default.", LocalHostNameProvider.class.getSimpleName());
            return new DefaultLocalHostNameProvider();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startup() {
        // nothing to do
    }

    /**
     * Closes the connections established by {@link SyslogPublisher}.
     */
    @Override
    public void shutdown() {
        synchronized (publisher) {
            publisher.close();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event) {

        try {
            final String syslogMessage = formatAsSyslogMessage(topic, event);
            synchronized (publisher) {
                publisher.publishMessage(syslogMessage);
            }

            return newResourceResponse(
                    event.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                    null,
                    event.clone()).asPromise();

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
    public Promise<QueryResponse, ResourceException> queryEvents(
            Context context,
            String topic,
            QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler) {
        return notSupported(queryRequest).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        return new NotSupportedException("query operations are not supported").asPromise();
    }

    /**
     * Default implementation of ProductNameProvider.
     */
    private static class DefaultProductInfoProvider implements ProductInfoProvider {

        @Override
        public String getProductName() {
            return null;
        }
    }
}
