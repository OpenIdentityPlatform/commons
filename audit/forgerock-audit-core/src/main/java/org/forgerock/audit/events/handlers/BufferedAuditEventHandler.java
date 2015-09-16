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
package org.forgerock.audit.events.handlers;

import static java.util.concurrent.Executors.*;

import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.promise.Promise;

/**
 * Decorator of an AuditEventHandler which adds event buffering capabilities to the handler.
 *
 * @param <CFG>
 *            type of the configuration
 */
public class BufferedAuditEventHandler<CFG extends EventHandlerConfiguration>
    extends AuditEventHandlerBase<CFG>
    implements BufferCallback<AuditEventTopicState> {

    /** The underlying audit event handler which is decorated. */
    private final AuditEventHandler<CFG> delegate;

    /** The buffer for events. */
    private Buffer<AuditEventTopicState> eventBuffer;

    /** Indicates if buffer must be flushed before performing a read or query event. */
    private boolean forceFlushBeforeRead;

    /**
     * Creates a buffered event handler on the provided event handler.
     *
     * @param handler
     *            The underlying event handler.
     */
    public BufferedAuditEventHandler(AuditEventHandler<CFG> handler) {
        this.delegate = handler;
    }

    @Override
    public void configure(CFG config) throws ResourceException {
        EventBufferingConfiguration bufferConf = config.getBufferingConfig();
        forceFlushBeforeRead = bufferConf.isForceFlushBeforeRead();
        ScheduledExecutorService pool = newScheduledThreadPool(2);
        this.eventBuffer = new Buffer<>(pool, this, bufferConf.getMaxTime(), bufferConf.getMaxSize());

        delegate.configure(config);
    }

    @Override
    public void startup() throws ResourceException {
        // nothing to do
    }

    @Override
    public void shutdown() throws ResourceException {
        eventBuffer.close();
        delegate.shutdown();
    }

    @Override
    public Class<CFG> getConfigurationClass() {
        return delegate.getConfigurationClass();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event) {
        eventBuffer.add(new AuditEventTopicState(context, topic, event));
        return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                null, event).asPromise();
    }

    @Override
    public void bufferFlush(List<AuditEventTopicState> events) {
        delegate.publishEvents(events);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        forceFlushIfNeeded();
        return delegate.readEvent(context, topic, resourceId);
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(Context context, String topic, QueryRequest query,
            QueryResourceHandler handler) {
        forceFlushIfNeeded();
        return this.delegate.queryEvents(context, topic, query, handler);
    }

    /** Force the flush of buffer to ensure all events are available for a read or a query. */
    private void forceFlushIfNeeded() {
        if (forceFlushBeforeRead) {
            eventBuffer.reset();
        }
    }

    /** Indicates if events buffer is empty. */
    @VisibleForTesting
    boolean isBufferEmpty() {
        return eventBuffer.isEmpty();
    }
}
