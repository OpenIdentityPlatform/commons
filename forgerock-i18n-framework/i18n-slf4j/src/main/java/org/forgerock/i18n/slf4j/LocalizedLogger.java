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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 *      Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.i18n.slf4j;

import java.util.Locale;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg0;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg1;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg2;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg3;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg4;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg5;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg6;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg7;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg8;
import org.forgerock.i18n.LocalizableMessageDescriptor.Arg9;
import org.forgerock.i18n.LocalizableMessageDescriptor.ArgN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * A logger implementation which formats and localizes messages before
 * forwarding them to an underlying SLF4J {@link Logger}. For performance
 * reasons this implementation will only localize and format messages if logging
 * has been enabled for the associated log level and marker (if present).
 * <p>
 * If no marker is provided, a {@code LocalizedMarker} is automatically constructed
 * with the corresponding {@code LocalizedMessage} to be logged and passed to the
 * underlying SLF4J {@link Logger}. This allow a custom implementation of SLF4J
 * logger adapter to retrieve the complete localizable message when logging.
 */
public final class LocalizedLogger {

    private static final String LOCALIZED_LOGGER_CLASSNAME = LocalizedLogger.class.getName();

    private static final String THREAD_CLASS_NAME = Thread.class.getName();

    /**
     * Returns a localized logger which will forward log messages to an SLF4J
     * {@code Logger} obtained by calling {@link LoggerFactory#getLogger(Class)}
     * . The messages will be localized using the default locale.
     *
     * @param clazz
     *            The name of the wrapped SLF4J {@code Logger}.
     * @return The localized logger.
     * @see LoggerFactory#getLogger(Class)
     */
    public static LocalizedLogger getLocalizedLogger(final Class<?> clazz) {
        final Logger logger = LoggerFactory.getLogger(clazz);
        return new LocalizedLogger(logger, Locale.getDefault());
    }

    /**
     * Returns a localized logger which will forward log messages to the
     * provided SLF4J {@code Logger}. The messages will be localized using the
     * default locale.
     *
     * @param logger
     *            The wrapped SLF4J {@code Logger}.
     * @return The localized logger.
     * @see LoggerFactory#getLogger(String)
     */
    public static LocalizedLogger getLocalizedLogger(final Logger logger) {
        return new LocalizedLogger(logger, Locale.getDefault());
    }

    /**
     * Returns a localized logger which will forward log messages to an SLF4J
     * {@code Logger} obtained by calling
     * {@link LoggerFactory#getLogger(String)}. The messages will be localized
     * using the default locale.
     *
     * @param name
     *            The name of the wrapped SLF4J {@code Logger}.
     * @return The localized logger.
     * @see LoggerFactory#getLogger(String)
     */
    public static LocalizedLogger getLocalizedLogger(final String name) {
        final Logger logger = LoggerFactory.getLogger(name);
        return new LocalizedLogger(logger, Locale.getDefault());
    }

    /**
     * Returns a localized logger with a name corresponding to calling class
     * name. The logger will forward log messages to an SLF4J {@code Logger}
     * obtained by calling {@link LoggerFactory#getLogger(String)}. The messages
     * will be localized using the default locale.
     *
     * @return The localized logger using calling class name as its name
     * @see LoggerFactory#getLogger(String)
     */
    public static LocalizedLogger getLoggerForThisClass() {
        String name = getClassNameOfCaller();
        if (name == null) {
            name = Logger.ROOT_LOGGER_NAME;
        }
        return getLocalizedLogger(name);
    }

    /**
     * Return the name of class that asked for a Logger.
     *
     * @return the class name, or {@code null} if it can't be found
     */
    static String getClassNameOfCaller() {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            // Skip leading frames debug logging classes
            // and getStackTrace method call frame if any.
            for (StackTraceElement aStackTrace : stackTrace) {
                final String name = aStackTrace.getClassName();
                if (!name.equals(THREAD_CLASS_NAME)
                    && !name.equals(LOCALIZED_LOGGER_CLASSNAME)) {
                    return aStackTrace.getClassName();
                }
            }
        }
        return null;
    }

    private final Locale locale;

    private final Logger logger;

    /**
     * Creates a new localized logger which will log localizable messages to the
     * provided SLF4J {@code Logger} in the specified locale.
     *
     * @param logger
     *            The underlying SLF4J {@code Logger} wrapped by this logger.
     * @param locale
     *            The locale to which this logger will localize all log
     *            messages.
     */
    LocalizedLogger(final Logger logger, final Locale locale) {
        this.locale = locale;
        this.logger = logger;
    }

    /**
     * Logs a debug message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#debug(String)
     */
    public void debug(final Arg0 d) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get();
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public void debug(final Arg0 d, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get();
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1> void debug(final Arg1<T1> d, final T1 a1) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1> void debug(final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2> void debug(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2> void debug(final Arg2<T1, T2> d, final T1 a1, final T2 a2, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3> void debug(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3> void debug(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3,
            final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4> void debug(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4> void debug(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5> void debug(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void debug(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6> void debug(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void debug(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void debug(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void debug(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void debug(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void debug(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void debug(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void debug(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#debug(String)
     */
    public void debug(final ArgN d, final Object... args) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.debug(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public void debug(final ArgN d, final Throwable t, final Object... args) {
        if (logger.isDebugEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.debug(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param m
     *            The pre-formatted message.
     * @see org.slf4j.Logger#debug(String)
     */
    public void debug(final LocalizableMessage m) {
        if (logger.isDebugEnabled()) {
            logger.debug(new LocalizedMarker(m), m.toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param m
     *            The pre-formatted message.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public void debug(final LocalizableMessage m, final Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(new LocalizedMarker(m), m.toString(locale), t);
        }
    }

    /**
     * Logs a debug message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, String)
     */
    public void debug(final Marker m, final Arg0 d) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get().toString(locale));
        }
    }

    /**
     * Logs a debug message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, String)
     */
    public void debug(final Marker m, final Arg0 d, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get().toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1> void debug(final Marker m, final Arg1<T1> d, final T1 a1) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1> void debug(final Marker m, final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2> void debug(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2> void debug(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2,
            final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3> void debug(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3> void debug(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4> void debug(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4> void debug(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5> void debug(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void debug(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6> void debug(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void debug(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void debug(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void debug(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void debug(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void debug(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#debug(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void debug(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void debug(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale), t);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#debug(String)
     */
    public void debug(final Marker m, final ArgN d, final Object... args) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(args).toString(locale));
        }
    }

    /**
     * Logs a debug message with an accompanying exception.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#debug(String, Throwable)
     */
    public void debug(final Marker m, final ArgN d, final Throwable t, final Object... args) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(args).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#error(String)
     */
    public void error(final Arg0 d) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get();
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public void error(final Arg0 d, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get();
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1> void error(final Arg1<T1> d, final T1 a1) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1> void error(final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2> void error(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2> void error(final Arg2<T1, T2> d, final T1 a1, final T2 a2, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3> void error(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3> void error(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3,
            final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4> void error(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4> void error(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5> void error(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void error(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6> void error(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void error(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void error(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void error(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void error(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void error(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void error(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void error(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#error(String)
     */
    public void error(final ArgN d, final Object... args) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.error(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public void error(final ArgN d, final Throwable t, final Object... args) {
        if (logger.isErrorEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.error(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param m
     *            The pre-formatted message.
     * @see org.slf4j.Logger#error(String)
     */
    public void error(final LocalizableMessage m) {
        if (logger.isErrorEnabled()) {
            logger.error(new LocalizedMarker(m), m.toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param m
     *            The pre-formatted message.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public void error(final LocalizableMessage m, final Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(new LocalizedMarker(m), m.toString(locale), t);
        }
    }

    /**
     * Logs an error message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, String)
     */
    public void error(final Marker m, final Arg0 d) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get().toString(locale));
        }
    }

    /**
     * Logs an error message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, String)
     */
    public void error(final Marker m, final Arg0 d, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get().toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1> void error(final Marker m, final Arg1<T1> d, final T1 a1) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1> void error(final Marker m, final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2> void error(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2> void error(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2,
            final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3> void error(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3> void error(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4> void error(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4> void error(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5> void error(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void error(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6> void error(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void error(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void error(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void error(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void error(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void error(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#error(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void error(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void error(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale), t);
        }
    }

    /**
     * Logs an error message.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#error(String)
     */
    public void error(final Marker m, final ArgN d, final Object... args) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(args).toString(locale));
        }
    }

    /**
     * Logs an error message with an accompanying exception.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#error(String, Throwable)
     */
    public void error(final Marker m, final ArgN d, final Throwable t, final Object... args) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(args).toString(locale), t);
        }
    }

    /**
     * Returns the locale to which this logger will localize all log messages.
     *
     * @return The locale to which this logger will localize all log messages.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the underlying SLF4J {@code Logger} wrapped by this logger.
     *
     * @return The underlying SLF4J {@code Logger} wrapped by this logger.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the name of this logger.
     *
     * @return The name of this logger.
     * @see org.slf4j.Logger#getName()
     */
    public String getName() {
        return logger.getName();
    }

    /**
     * Logs an info message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#info(String)
     */
    public void info(final Arg0 d) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get();
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public void info(final Arg0 d, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get();
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1> void info(final Arg1<T1> d, final T1 a1) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1> void info(final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2> void info(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2> void info(final Arg2<T1, T2> d, final T1 a1, final T2 a2, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3> void info(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3> void info(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3,
            final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4> void info(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4> void info(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5> void info(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void info(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6> void info(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void info(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void info(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void info(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void info(final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void info(final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#info(String)
     */
    public void info(final ArgN d, final Object... args) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.info(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public void info(final ArgN d, final Throwable t, final Object... args) {
        if (logger.isInfoEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.info(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param m
     *            The pre-formatted message.
     * @see org.slf4j.Logger#info(String)
     */
    public void info(final LocalizableMessage m) {
        if (logger.isInfoEnabled()) {
            logger.info(new LocalizedMarker(m), m.toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param m
     *            The pre-formatted message.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public void info(final LocalizableMessage m, final Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(new LocalizedMarker(m), m.toString(locale), t);
        }
    }

    /**
     * Logs an info message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, String)
     */
    public void info(final Marker m, final Arg0 d) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get().toString(locale));
        }
    }

    /**
     * Logs an info message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, String)
     */
    public void info(final Marker m, final Arg0 d, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get().toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1> void info(final Marker m, final Arg1<T1> d, final T1 a1) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1> void info(final Marker m, final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2> void info(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2> void info(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2,
            final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3> void info(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3> void info(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4> void info(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4> void info(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5> void info(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void info(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6> void info(final Marker m, final Arg6<T1, T2, T3, T4, T5, T6> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void info(final Marker m, final Arg6<T1, T2, T3, T4, T5, T6> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void info(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void info(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void info(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void info(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale), t);
        }
    }

    /**
     * Logs an info message.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#info(String)
     */
    public void info(final Marker m, final ArgN d, final Object... args) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(args).toString(locale));
        }
    }

    /**
     * Logs an info message with an accompanying exception.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#info(String, Throwable)
     */
    public void info(final Marker m, final ArgN d, final Throwable t, final Object... args) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(args).toString(locale), t);
        }
    }

    /**
     * Returns {@code true} if this logger will log debug messages.
     *
     * @return {@code true} if this logger will log debug messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isDebugEnabled()
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Returns {@code true} if this logger will log debug messages associated
     * with the provided marker.
     *
     * @param m
     *            The marker information.
     * @return {@code true} if this logger will log debug messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isDebugEnabled(org.slf4j.Marker)
     */
    public boolean isDebugEnabled(final Marker m) {
        return logger.isDebugEnabled(m);
    }

    /**
     * Returns {@code true} if this logger will log error messages.
     *
     * @return {@code true} if this logger will log error messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isErrorEnabled()
     */
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /**
     * Returns {@code true} if this logger will log error messages associated
     * with the provided marker.
     *
     * @param m
     *            The marker information.
     * @return {@code true} if this logger will log error messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isErrorEnabled(org.slf4j.Marker)
     */
    public boolean isErrorEnabled(final Marker m) {
        return logger.isErrorEnabled(m);
    }

    /**
     * Returns {@code true} if this logger will log info messages.
     *
     * @return {@code true} if this logger will log info messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isInfoEnabled()
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Returns {@code true} if this logger will log info messages associated
     * with the provided marker.
     *
     * @param m
     *            The marker information.
     * @return {@code true} if this logger will log info messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isInfoEnabled(org.slf4j.Marker)
     */
    public boolean isInfoEnabled(final Marker m) {
        return logger.isInfoEnabled(m);
    }

    /**
     * Returns {@code true} if this logger will log trace messages.
     *
     * @return {@code true} if this logger will log trace messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isTraceEnabled()
     */
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * Returns {@code true} if this logger will log trace messages associated
     * with the provided marker.
     *
     * @param m
     *            The marker information.
     * @return {@code true} if this logger will log trace messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isTraceEnabled(org.slf4j.Marker)
     */
    public boolean isTraceEnabled(final Marker m) {
        return logger.isTraceEnabled(m);
    }

    /**
     * Returns {@code true} if this logger will log warning messages.
     *
     * @return {@code true} if this logger will log warning messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isWarnEnabled()
     */
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Returns {@code true} if this logger will log warning messages associated
     * with the provided marker.
     *
     * @param m
     *            The marker information.
     * @return {@code true} if this logger will log warning messages, otherwise
     *         {@code false}.
     * @see org.slf4j.Logger#isWarnEnabled(org.slf4j.Marker)
     */
    public boolean isWarnEnabled(final Marker m) {
        return logger.isWarnEnabled(m);
    }

    /**
     * Logs a trace message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final Arg0 d) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get();
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message.
     *
     * @param msg
     *            The message.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(new LocalizedMarker(LocalizableMessage.raw(msg)), msg);
        }
    }

    /**
     * Logs a trace message with provided exception.
     *
     * @param t
     *            The exception.
     * @see org.slf4j.Logger#trace(String)
     */
    public void traceException(final Throwable t) {
        traceException(t, (t.getMessage() == null ? "" : t.getMessage()));
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void trace(final Arg0 d, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get();
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param t
     *            The throwable to log.
     * @param msg
     *            The message.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void traceException(final Throwable t, final String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(new LocalizedMarker(LocalizableMessage.raw(msg)), msg, t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1> void trace(final Arg1<T1> d, final T1 a1) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message.
     *
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final String format, final Object a1) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, a1);
            logger.trace(new LocalizedMarker(message), message.toString());
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1> void trace(final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param t
     *            The throwable to log.
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void traceException(final Throwable t, final String format, final Object a1) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, a1);
            logger.trace(new LocalizedMarker(message), message.toString(), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2> void trace(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message.
     *
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final String format, final Object a1, final Object a2) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, a1, a2);
            logger.trace(new LocalizedMarker(message), message.toString());
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2> void trace(final Arg2<T1, T2> d, final T1 a1, final T2 a2, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param t
     *            The throwable to log.
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void traceException(final Throwable t, final String format, final Object a1, final Object a2) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, a1, a2);
            logger.trace(new LocalizedMarker(message), message.toString(), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3> void trace(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message.
     *
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final String format, final Object a1, final Object a2, final Object a3) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, a1, a2, a3);
            logger.trace(new LocalizedMarker(message), message.toString());
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3> void trace(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3,
            final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param t
     *            The throwable to log.
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void traceException(final Throwable t, final String format, final Object a1, final Object a2,
        final Object a3) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, a1, a2, a3);
            logger.trace(new LocalizedMarker(message), message.toString(), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4> void trace(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4> void trace(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5> void trace(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void trace(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6> void trace(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void trace(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void trace(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void trace(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void trace(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void trace(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void trace(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void trace(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final ArgN d, final Object... args) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.trace(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void trace(final ArgN d, final Throwable t, final Object... args) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.trace(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param args
     *            The message arguments.
     *
     * @see org.slf4j.Logger#trace(Marker, String)
     */
    public void trace(final String format, final Object...args) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, args);
            logger.trace(new LocalizedMarker(message), message.toString());
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param t
     *            The throwable to log.
     * @param format
     *            The message format, compatible with
     *            {@code java.util.Formatter} rules
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void traceException(final Throwable t, final String format, final Object... args) {
        if (logger.isTraceEnabled()) {
            final LocalizableMessage message = LocalizableMessage.raw(format, args);
            logger.trace(new LocalizedMarker(message), message.toString(), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param m
     *            The pre-formatted message.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final LocalizableMessage m) {
        if (logger.isTraceEnabled()) {
            logger.trace(new LocalizedMarker(m), m.toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param m
     *            The pre-formatted message.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void trace(final LocalizableMessage m, final Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(new LocalizedMarker(m), m.toString(locale), t);
        }
    }

    /**
     * Logs a trace message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, String)
     */
    public void trace(final Marker m, final Arg0 d) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get().toString(locale));
        }
    }

    /**
     * Logs a trace message using the provided {@code Marker}.
     *
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, String)
     */
    public void trace(final Marker m, final Arg0 d, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get().toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1> void trace(final Marker m, final Arg1<T1> d, final T1 a1) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1> void trace(final Marker m, final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2> void trace(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2> void trace(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2,
            final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3> void trace(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3> void trace(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4> void trace(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4> void trace(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5> void trace(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void trace(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6> void trace(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void trace(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void trace(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void trace(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void trace(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void trace(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#trace(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void trace(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void trace(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale), t);
        }
    }

    /**
     * Logs a trace message.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#trace(String)
     */
    public void trace(final Marker m, final ArgN d, final Object... args) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(args).toString(locale));
        }
    }

    /**
     * Logs a trace message with an accompanying exception.
     *
     * @param m
     *            The marker tracermation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#trace(String, Throwable)
     */
    public void trace(final Marker m, final ArgN d, final Throwable t, final Object... args) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(args).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#warn(String)
     */
    public void warn(final Arg0 d) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get();
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public void warn(final Arg0 d, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get();
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1> void warn(final Arg1<T1> d, final T1 a1) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1> void warn(final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2> void warn(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2> void warn(final Arg2<T1, T2> d, final T1 a1, final T2 a2, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3> void warn(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3> void warn(final Arg3<T1, T2, T3> d, final T1 a1, final T2 a2, final T3 a3,
            final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4> void warn(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4> void warn(final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5> void warn(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void warn(final Arg5<T1, T2, T3, T4, T5> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6> void warn(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void warn(final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void warn(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void warn(final Arg7<T1, T2, T3, T4, T5, T6, T7> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warn(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warn(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#warn(String)
     */
    public void warn(final ArgN d, final Object... args) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.warn(new LocalizedMarker(message), message.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public void warn(final ArgN d, final Throwable t, final Object... args) {
        if (logger.isWarnEnabled()) {
            final LocalizableMessage message = d.get(args);
            logger.warn(new LocalizedMarker(message), message.toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param m
     *            The pre-formatted message.
     * @see org.slf4j.Logger#warn(String)
     */
    public void warn(final LocalizableMessage m) {
        if (logger.isWarnEnabled()) {
            logger.warn(new LocalizedMarker(m), m.toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param m
     *            The pre-formatted message.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public void warn(final LocalizableMessage m, final Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(new LocalizedMarker(m), m.toString(locale), t);
        }
    }

    /**
     * Logs a warning message using the provided {@code Marker}.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, String)
     */
    public void warn(final Marker m, final Arg0 d) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get().toString(locale));
        }
    }

    /**
     * Logs a warning message using the provided {@code Marker}.
     *
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, String)
     */
    public void warn(final Marker m, final Arg0 d, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get().toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1> void warn(final Marker m, final Arg1<T1> d, final T1 a1) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1> void warn(final Marker m, final Arg1<T1> d, final T1 a1, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2> void warn(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2> void warn(final Marker m, final Arg2<T1, T2> d, final T1 a1, final T2 a2,
            final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3> void warn(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3> void warn(final Marker m, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4> void warn(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4> void warn(final Marker m, final Arg4<T1, T2, T3, T4> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5> void warn(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5> void warn(final Marker m, final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6> void warn(final Marker m, final Arg6<T1, T2, T3, T4, T5, T6> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void warn(final Marker m, final Arg6<T1, T2, T3, T4, T5, T6> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void warn(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void warn(final Marker m,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(final Marker m,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1, final T2 a2, final T3 a3,
            final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @see org.slf4j.Logger#warn(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warn(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param <T5>
     *            The type of the fifth message argument.
     * @param <T6>
     *            The type of the sixth message argument.
     * @param <T7>
     *            The type of the seventh message argument.
     * @param <T8>
     *            The type of the eighth message argument.
     * @param <T9>
     *            The type of the ninth message argument.
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param a5
     *            The fifth message argument.
     * @param a6
     *            The sixth message argument.
     * @param a7
     *            The seventh message argument.
     * @param a8
     *            The eighth message argument.
     * @param a9
     *            The ninth message argument.
     * @param t
     *            The throwable to log.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warn(final Marker m,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8,
            final T9 a9, final Throwable t) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale), t);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param m
     *            The marker information associated with this log message.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#warn(String)
     */
    public void warn(final Marker m, final ArgN d, final Object... args) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(args).toString(locale));
        }
    }

    /**
     * Logs a warning message with an accompanying exception.
     *
     * @param m
     *            The marker warnrmation associated with this log message.
     * @param d
     *            The message descriptor.
     * @param t
     *            The throwable to log.
     * @param args
     *            The message arguments.
     * @see org.slf4j.Logger#warn(String, Throwable)
     */
    public void warn(final Marker m, final ArgN d, final Throwable t, final Object... args) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(args).toString(locale), t);
        }
    }

}
