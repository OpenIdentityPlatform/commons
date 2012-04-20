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
 *      Copyright 2011 ForgeRock AS
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
 */
public final class LocalizedLogger {
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

    private final Logger logger;

    private final Locale locale;

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
            logger.debug(d.get().toString(locale));
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
            logger.debug(d.get(a1).toString(locale));
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
            logger.debug(d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void debug(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void debug(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void debug(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void debug(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7> void debug(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isDebugEnabled()) {
            logger.debug(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
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
            logger.debug(d.get(args).toString(locale));
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
            logger.debug(m.toString(locale));
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
    public <T1, T2> void debug(final Marker m, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void debug(final Marker m, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void debug(final Marker m,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void debug(final Marker m,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5).toString(locale));
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
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isDebugEnabled(m)) {
            logger.debug(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
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
     * Logs an error message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#error(String)
     */
    public void error(final Arg0 d) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get().toString(locale));
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
            logger.error(d.get(a1).toString(locale));
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
            logger.error(d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void error(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void error(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void error(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void error(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7> void error(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isErrorEnabled()) {
            logger.error(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
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
            logger.error(d.get(args).toString(locale));
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
            logger.error(m.toString(locale));
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
    public <T1, T2> void error(final Marker m, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void error(final Marker m, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void error(final Marker m,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void error(final Marker m,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5).toString(locale));
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
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isErrorEnabled(m)) {
            logger.error(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
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
            logger.info(d.get().toString(locale));
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
            logger.info(d.get(a1).toString(locale));
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
            logger.info(d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void info(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void info(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void info(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void info(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7> void info(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> void info(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isInfoEnabled()) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
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
            logger.info(d.get(args).toString(locale));
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
            logger.info(m.toString(locale));
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
    public <T1, T2> void info(final Marker m, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void info(final Marker m, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void info(final Marker m,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void info(final Marker m,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void info(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isInfoEnabled(m)) {
            logger.info(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
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
            logger.trace(d.get().toString(locale));
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
            logger.trace(d.get(a1).toString(locale));
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
            logger.trace(d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void trace(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void trace(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void trace(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void trace(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7> void trace(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isTraceEnabled()) {
            logger.trace(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
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
            logger.trace(d.get(args).toString(locale));
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
            logger.trace(m.toString(locale));
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
    public <T1, T2> void trace(final Marker m, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void trace(final Marker m, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void trace(final Marker m,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void trace(final Marker m,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5).toString(locale));
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
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isTraceEnabled(m)) {
            logger.trace(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
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
     * Logs a warning message.
     *
     * @param d
     *            The message descriptor.
     * @see org.slf4j.Logger#warn(String)
     */
    public void warn(final Arg0 d) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get().toString(locale));
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
            logger.warn(d.get(a1).toString(locale));
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
            logger.warn(d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void warn(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void warn(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void warn(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void warn(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7> void warn(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isWarnEnabled()) {
            logger.warn(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
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
            logger.warn(d.get(args).toString(locale));
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
            logger.warn(m.toString(locale));
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
    public <T1, T2> void warn(final Marker m, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2).toString(locale));
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
    public <T1, T2, T3> void warn(final Marker m, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3).toString(locale));
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
    public <T1, T2, T3, T4> void warn(final Marker m,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4).toString(locale));
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
    public <T1, T2, T3, T4, T5> void warn(final Marker m,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5).toString(locale));
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
    public <T1, T2, T3, T4, T5, T6> void warn(final Marker m,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
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
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
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
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
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
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isWarnEnabled(m)) {
            logger.warn(m,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
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

}
