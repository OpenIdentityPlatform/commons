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

package org.forgerock.audit;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.promise.Promise;

/**
 * AuditService proxy that allows products to implement threadsafe hot-swappable configuration updates.
 * <p/>
 * The proxied AuditService can be swapped by calling {@link #setDelegate(AuditService)}.
 * <p/>
 * Thread-safety is achieved by blocking proxied calls until the old AuditService has flushed all buffers
 * and closed any open file or network connections.
 */
public class AuditServiceProxy implements AuditService {

    /** Parameter that may be used when using an action, to provide the name of the handler to use as a target. */
    public static final String ACTION_PARAM_TARGET_HANDLER = "handler";

    private final ReentrantReadWriteLock delegateLock;
    private AuditService delegate;

    /**
     * Create a new {@code AuditServiceProxy}.
     *
     * @param delegate
     *          The {@code AuditService} that this object should proxy.
     */
    public AuditServiceProxy(AuditService delegate) {
        this(delegate, new ReentrantReadWriteLock());
    }

    @VisibleForTesting
    AuditServiceProxy(AuditService delegate, ReentrantReadWriteLock delegateLock) {
        Reject.ifNull(delegate);
        this.delegate = delegate;
        this.delegateLock = delegateLock;
    }

    /**
     * Sets the AuditService this object proxies.
     * <p/>
     * Thread-safety is achieved by blocking proxied calls until the old AuditService has flushed all buffers
     * and closed any open file or network connections.
     *
     * @param newDelegate
     *          A new AuditService instance with updated configuration.
     * @throws ServiceUnavailableException If the new audit service cannot be started.
     */
    public void setDelegate(AuditService newDelegate) throws ServiceUnavailableException {
        Reject.ifNull(newDelegate);
        obtainWriteLock();
        try {
            final AuditService oldDelegate = this.delegate;
            if (oldDelegate == newDelegate) {
                return;
            }
            oldDelegate.shutdown();
            newDelegate.startup();
            this.delegate = newDelegate;
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        obtainReadLock();
        try {
            return delegate.handleRead(context, request);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        obtainReadLock();
        try {
            return delegate.handleCreate(context, request);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        obtainReadLock();
        try {
            return delegate.handleUpdate(context, request);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        obtainReadLock();
        try {
            return delegate.handleDelete(context, request);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        obtainReadLock();
        try {
            return delegate.handlePatch(context, request);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(
            Context context, QueryRequest request, QueryResourceHandler handler) {
        obtainReadLock();
        try {
            return delegate.handleQuery(context, request, handler);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        obtainReadLock();
        try {
            return delegate.handleAction(context, request);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public AuditServiceConfiguration getConfig() throws ServiceUnavailableException {
        obtainReadLock();
        try {
            return delegate.getConfig();
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public AuditEventHandler getRegisteredHandler(String handlerName) throws ServiceUnavailableException {
        obtainReadLock();
        try {
            return delegate.getRegisteredHandler(handlerName);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Collection<AuditEventHandler> getRegisteredHandlers() throws ServiceUnavailableException {
        obtainReadLock();
        try {
            return delegate.getRegisteredHandlers();
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public boolean isAuditing(String topic) throws ServiceUnavailableException {
        obtainReadLock();
        try {
            return delegate.isAuditing(topic);
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public Set<String> getKnownTopics() throws ServiceUnavailableException {
        obtainReadLock();
        try {
            return delegate.getKnownTopics();
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public void shutdown() {
        obtainWriteLock();
        try {
            delegate.shutdown();
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public void startup() throws ServiceUnavailableException {
        obtainWriteLock();
        try {
            delegate.startup();
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public boolean isRunning() {
        obtainReadLock();
        try {
            return delegate.isRunning();
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Obtain the read lock or block until it becomes available.
     *
     * @throws IllegalStateException If the current thread already holds the write lock.
     */
    protected final void obtainReadLock() {
        delegateLock.readLock().lock();
        if (delegateLock.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException(
                    "AuditServiceProxy should not be called from delegate shutdown or startup operations");
        }
    }

    /**
     * Release the read lock.
     */
    protected final void releaseReadLock() {
        delegateLock.readLock().unlock();
    }

    /**
     * Obtain the write lock or block until it becomes available.
     */
    protected final void obtainWriteLock() {
        delegateLock.writeLock().lock();
    }

    /**
     * Release the write lock.
     */
    protected final void releaseWriteLock() {
        delegateLock.writeLock().unlock();
    }
}
