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
package org.forgerock.util.thread;

import org.forgerock.util.Reject;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for generating ExecutorService instances which are automatically
 * wired up to shutdown when the ShutdownListener event triggers.
 *
 * This factory simplifies the creation of ExecutorServices which could overlook
 * the important step of registering with the ShutdownManager. Failure to do so
 * will prevent the server from shutting down.
 *
 * Note: Executors created using this factory will be triggered with the
 * ExecutorService#shutdownNow method. This will interrupt all blocking calls
 * made by those threads. This may be important for some users.
 *
 * @since 1.3.5
 */
public class ExecutorServiceFactory {
    private final ShutdownManager shutdownManager;

    /**
     * Create an instance of the factory.
     *
     * @param shutdownManager Required to ensure each ExecutorService will be shutdown.
     */
    public ExecutorServiceFactory(ShutdownManager shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    /**
     * Generates a ScheduledExecutorService which has been pre-registered with the
     * ShutdownManager.
     *
     * @see java.util.concurrent.Executors#newScheduledThreadPool(int)
     *
     * @param poolSize The size of the ScheduledExecutorService thread pool.
     *
     * @return A non null ScheduledExecutorService
     */
    public ScheduledExecutorService createScheduledService(int poolSize) {
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(poolSize);
        registerShutdown(service);
        return service;
    }

    /**
     * Creates a fixed size Thread Pool ExecutorService which has been pre-registered with
     * the {@link org.forgerock.util.thread.listener.ShutdownManager}.
     *
     * @param pool The size of the pool to create.
     * @param factory The {@link java.util.concurrent.ThreadFactory} used to generate new threads.
     * @return Non null.
     */
    public ExecutorService createFixedThreadPool(int pool, ThreadFactory factory) {
        ExecutorService service = Executors.newFixedThreadPool(pool, factory);
        registerShutdown(service);
        return service;
    }

    /**
     * Create a fixed size Thread Pool ExecutorService using the provided name as the prefix
     * of the thread names.
     *
     * @see #createFixedThreadPool(int, java.util.concurrent.ThreadFactory)
     *
     * @param pool Size of the fixed pool.
     * @param threadNamePrefix The thread name prefix to use when generating new threads.
     * @return Non null.
     */
    public ExecutorService createFixedThreadPool(int pool, String threadNamePrefix) {
        ExecutorService service = Executors.newFixedThreadPool(pool, new NamedThreadFactory(threadNamePrefix));
        registerShutdown(service);
        return service;
    }

    /**
     * Create a fixed size Thread Pool ExecutorService.
     * @see #createFixedThreadPool(int, java.util.concurrent.ThreadFactory)
     *
     * @param pool Size of the fixed pool.
     * @return Non null.
     */
    public ExecutorService createFixedThreadPool(int pool) {
        ExecutorService service = Executors.newFixedThreadPool(pool);
        registerShutdown(service);
        return service;
    }

    /**
     * Generates a Cached Thread Pool ExecutorService which has been pre-registered with the
     * ShutdownManager. The provided ThreadFactory is used by the service when creating Threads.
     *
     * @see java.util.concurrent.Executors#newCachedThreadPool(java.util.concurrent.ThreadFactory)
     *
     * @param factory The ThreadFactory that will be used when generating threads. May not be null.
     * @return A non null ExecutorService.
     */
    public ExecutorService createCachedThreadPool(ThreadFactory factory) {
        ExecutorService service = Executors.newCachedThreadPool(factory);
        registerShutdown(service);
        return service;
    }

    /**
     * Generates a Cached Thread Pool ExecutorService using the provided name as a prefix
     * of the thread names.
     *
     * @see #createCachedThreadPool(java.util.concurrent.ThreadFactory)
     *
     * @param threadNamePrefix The thread name prefix to use when generating new threads.
     * @return Non null.
     */
    public ExecutorService createCachedThreadPool(String threadNamePrefix) {
        ExecutorService service = Executors.newCachedThreadPool(new NamedThreadFactory(threadNamePrefix));
        registerShutdown(service);
        return service;
    }

    /**
     * Generates a Cached Thread Pool ExecutorService.
     * @see #createCachedThreadPool(java.util.concurrent.ThreadFactory)
     *
     * @return Non null.
     */
    public ExecutorService createCachedThreadPool() {
        ExecutorService service = Executors.newCachedThreadPool();
        registerShutdown(service);
        return service;
    }

    /**
     * Generates a ThreadPoolExecutor with the provided values, and registers that executor as listening for
     * shutdown messages.
     *
     * @param coreSize the number of threads to keep in the pool, even if they are idle
     * @param maxSize Max number of threads in the pool
     * @param idleTimeout When the number of threads is greater than core, maximum time that excess idle
     *                    threads will wait before terminating
     * @param timeoutTimeunit The time unit for the idleTimeout argument
     * @param runnables Queue of threads to be run
     * @return a configured ExecutorService, registered to listen to shutdown messages.
     */
    public ExecutorService createThreadPool(int coreSize, int maxSize, long idleTimeout,
                                            TimeUnit timeoutTimeunit, BlockingQueue<Runnable> runnables) {
        Reject.ifTrue(coreSize < 0);
        Reject.ifTrue(maxSize < coreSize || maxSize <= 0);
        Reject.ifTrue(idleTimeout < 0);

        ExecutorService service = new ThreadPoolExecutor(coreSize, maxSize, idleTimeout, timeoutTimeunit,
                runnables);
        registerShutdown(service);
        return service;
    }

    /**
     * Registers a listener to trigger shutdown of the ExecutorService.
     * @param service Non null ExecutorService to register.
     */
    private void registerShutdown(final ExecutorService service) {
        shutdownManager.addShutdownListener(
                new ShutdownListener() {
                    public void shutdown() {
                        service.shutdownNow();
                    }
                });
    }

    /**
     * Used to generate threads with a provided name. Each new thread will
     * have its generated number appended to the end of it, in the form -X, where
     * X is incremented once for each thread created.
     */
    private class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger count = new AtomicInteger(0);
        private final String name;

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, name + "-" +  count.getAndIncrement());
        }
    }

}
