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

import java.util.ArrayList;
import java.util.List;

/**
 * This Debug Logger implementation provides buffering ability. This enables log calls to be made without actually
 * having a "real" Debug Logger instance available, then when one does become available it can be set and all the
 * buffered log calls can be made onto the "real" Debug Logger instance.
 *
 * @since 1.3.0
 */
public class DebugLoggerBuffer implements DebugLogger {

    private final List<LogEntry> traceLogBuffer = new ArrayList<LogEntry>();
    private final List<LogEntry> debugLogBuffer = new ArrayList<LogEntry>();
    private final List<LogEntry> errorLogBuffer = new ArrayList<LogEntry>();
    private final List<LogEntry> warnLogBuffer = new ArrayList<LogEntry>();

    private DebugLogger logger;

    /**
     * Sets the underlying Debug Logger instance which log calls will be delegated to and empties its buffers by making
     * the appropriate calls to the given Debug Logger.
     *
     * @param logger The Debug Logger instance.
     */
    public final synchronized void setDebugLogger(final DebugLogger logger) {
        this.logger = logger;
        for (LogEntry entry : traceLogBuffer) {
            if (entry.t == null) {
                trace(entry.message);
            } else {
                trace(entry.message, entry.t);
            }
        }
        traceLogBuffer.clear();
        for (LogEntry entry : debugLogBuffer) {
            if (entry.t == null) {
                debug(entry.message);
            } else {
                debug(entry.message, entry.t);
            }
        }
        debugLogBuffer.clear();
        for (LogEntry entry : errorLogBuffer) {
            if (entry.t == null) {
                error(entry.message);
            } else {
                error(entry.message, entry.t);
            }
        }
        errorLogBuffer.clear();
        for (LogEntry entry : warnLogBuffer) {
            if (entry.t == null) {
                warn(entry.message);
            } else {
                warn(entry.message, entry.t);
            }
        }
        warnLogBuffer.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void trace(final String message) {
        if (logger == null) {
            traceLogBuffer.add(new LogEntry(message));
            return;
        }

        logger.trace(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void trace(final String message, final Throwable t) {
        if (logger == null) {
            traceLogBuffer.add(new LogEntry(message, t));
            return;
        }

        logger.trace(message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void debug(final String message) {
        if (logger == null) {
            debugLogBuffer.add(new LogEntry(message));
            return;
        }

        logger.debug(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void debug(final String message, final Throwable t) {
        if (logger == null) {
            debugLogBuffer.add(new LogEntry(message, t));
            return;
        }

        logger.debug(message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void error(final String message) {
        if (logger == null) {
            errorLogBuffer.add(new LogEntry(message));
            return;
        }

        logger.error(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void error(final String message, final Throwable t) {
        if (logger == null) {
            errorLogBuffer.add(new LogEntry(message, t));
            return;
        }

        logger.error(message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void warn(final String message) {
        if (logger == null) {
            warnLogBuffer.add(new LogEntry(message));
            return;
        }

        logger.warn(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void warn(final String message, final Throwable t) {
        if (logger == null) {
            warnLogBuffer.add(new LogEntry(message, t));
            return;
        }

        logger.warn(message, t);
    }

    /**
     * Models a buffered log entry.
     */
    private final static class LogEntry {

        private final String message;
        private final Throwable t;

        /**
         * Constructs a new Log Entry.
         *
         * @param message The message of the entry.
         */
        private LogEntry(final String message) {
            this.message = message;
            this.t = null;
        }

        /**
         * Constructs a new Log Entry.
         *
         * @param message The message of the entry.
         * @param t The throwable of the entry.
         */
        private LogEntry(final String message, final Throwable t) {
            this.message = message;
            this.t = t;
        }
    }
}
