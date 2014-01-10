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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.utils;

import org.forgerock.auth.common.DebugLogger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This Debug Logger implementation provides buffering ability. This enables log calls to be made without actually
 * having a "real" Debug Logger instance available, then when one does become available it can be set and all the
 * buffered log calls can be made onto the "real" Debug Logger instance.
 *
 * @since 1.3.0
 */
public class DebugLoggerBuffer implements DebugLogger {

    private final ConcurrentLinkedQueue<LogEntry> logBuffer = new ConcurrentLinkedQueue<LogEntry>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private DebugLogger logger;

    /**
     * Sets the underlying Debug Logger instance which log calls will be delegated to and empties its buffers by making
     * the appropriate calls to the given Debug Logger.
     *
     * @param logger The Debug Logger instance.
     */
    public final void setDebugLogger(final DebugLogger logger) {
        try {
            writeLock.lock();
            this.logger = logger;
            processLogBuffer(logger);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Processes the Log Buffer queue and reads from the queue and logs the entry to the "real" debug logger.
     *
     * @param debugLogger The "real" debug logger.
     */
    private void processLogBuffer(final DebugLogger debugLogger) {
        while (!logBuffer.isEmpty()) {
            LogEntry entry = logBuffer.poll();
            log(debugLogger, entry.level, entry.message, entry.t);
        }
    }

    /**
     * Determines which log call to make on the "real" debug logger.
     *
     * @param debugLogger The "real" debug logger.
     * @param level The Log Level.
     * @param message The message of the entry.
     * @param t The throwable of the entry.
     */
    private void log(final DebugLogger debugLogger, final LogLevel level, final String message, final Throwable t) {
        switch (level) {
            case TRACE: {
                if (t == null) {
                    debugLogger.trace(message);
                } else {
                    debugLogger.trace(message, t);
                }
                break;
            }
            case DEBUG: {
                if (t == null) {
                    debugLogger.debug(message);
                } else {
                    debugLogger.debug(message, t);
                }
                break;
            }
            case ERROR: {
                if (t == null) {
                    debugLogger.error(message);
                } else {
                    debugLogger.error(message, t);
                }
                break;
            }
            case WARN: {
                if (t == null) {
                    debugLogger.warn(message);
                } else {
                    debugLogger.warn(message, t);
                }
                break;
            }
        }
    }

    /**
     * Logs the message by either adding it to the log buffer, if no "real" logger is set, or by making the log
     * call to the "real" logger.
     *
     * @param level The Log Level.
     * @param message The message of the entry.
     * @param t The throwable of the entry.
     */
    private void log(final LogLevel level, final String message, final Throwable t) {
        try {
            readLock.lock();
            if (logger == null) {
                logBuffer.add(new LogEntry(level, message, t));
                return;
            }
        } finally {
            readLock.unlock();
        }
        log(logger, level, message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void trace(final String message) {
        log(LogLevel.TRACE, message, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void trace(final String message, final Throwable t) {
        log(LogLevel.TRACE, message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void debug(final String message) {
        log(LogLevel.DEBUG, message, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void debug(final String message, final Throwable t) {
        log(LogLevel.DEBUG, message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void error(final String message) {
        log(LogLevel.ERROR, message, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void error(final String message, final Throwable t) {
        log(LogLevel.ERROR, message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void warn(final String message) {
        log(LogLevel.WARN, message, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void warn(final String message, final Throwable t) {
        log(LogLevel.WARN, message, t);
    }

    /**
     * Log Levels.
     */
    private static enum LogLevel {
        TRACE,
        DEBUG,
        ERROR,
        WARN
    }

    /**
     * Models a buffered log entry.
     */
    private final static class LogEntry {

        private final LogLevel level;
        private final String message;
        private final Throwable t;

        /**
         * Constructs a new Log Entry.
         *
         * @param level The Log Level.
         * @param message The message of the entry.
         * @param t The throwable of the entry.
         */
        private LogEntry(final LogLevel level, final String message, final Throwable t) {
            this.level = level;
            this.message = message;
            this.t = t;
        }
    }
}
