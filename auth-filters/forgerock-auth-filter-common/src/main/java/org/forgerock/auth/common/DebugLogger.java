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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.auth.common;

/**
 * Base interface for all debug logging implementations.
 *
 * @since 1.0.0
 */
public interface DebugLogger {

    /**
     * Logs the given message at the trace level.
     *
     * @param message The log message.
     */
    void trace(final String message);

    /**
     * Logs the given message at the trace level.
     *
     * @param message The log message.
     * @param t The throwable to log.
     */
    void trace(final String message, final Throwable t);

    /**
     * Logs the given message at the debug level.
     *
     * @param message The log message.
     */
    void debug(final String message);

    /**
     * Logs the given message at the debug level.
     *
     * @param message The log message.
     * @param t The throwable to log.
     */
    void debug(final String message, final Throwable t);

    /**
     * Logs the given message at the error level.
     *
     * @param message The log message.
     */
    void error(final String message);

    /**
     * Logs the given message at the error level.
     *
     * @param message The log message.
     * @param t The throwable to log.
     */
    void error(final String message, final Throwable t);

    /**
     * Logs the given message at the warning level.
     *
     * @param message The log message.
     */
    void warn(final String message);

    /**
     * Logs the given message at the warning level.
     *
     * @param message The log message.
     * @param t The throwable to log.
     */
    void warn(final String message, final Throwable t);
}
