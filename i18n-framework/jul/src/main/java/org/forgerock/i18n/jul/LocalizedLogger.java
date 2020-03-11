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

package org.forgerock.i18n.jul;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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

/**
 * A logger implementation which formats and localizes messages before
 * forwarding them to an underlying Java {@link Logger}. For performance reasons
 * this implementation will only localize and format messages if logging has
 * been enabled for the associated log level and marker (if present).
 */
public final class LocalizedLogger {
    /**
     * Returns a localized logger which will forward log messages to an
     * anonymous Java {@code Logger} obtained by calling
     * {@link Logger#getAnonymousLogger()} . The messages will be localized
     * using the default locale.
     *
     * @return The localized logger.
     * @see Logger#getAnonymousLogger()
     */
    public static LocalizedLogger getLocalizedAnonymousLogger() {
        final Logger logger = Logger.getAnonymousLogger();
        return new LocalizedLogger(logger, Locale.getDefault());
    }

    /**
     * Returns a localized logger which will forward log messages to the
     * provided Java {@code Logger}. The messages will be localized using the
     * default locale.
     *
     * @param logger
     *            The wrapped Java {@code Logger}.
     * @return The localized logger.
     */
    public static LocalizedLogger getLocalizedLogger(final Logger logger) {
        return new LocalizedLogger(logger, Locale.getDefault());
    }

    /**
     * Returns a localized logger which will forward log messages to the named
     * Java {@code Logger} obtained by calling {@link Logger#getLogger(String)}.
     * The messages will be localized using the default locale.
     *
     * @param name
     *            The name of the wrapped Java {@code Logger}.
     * @return The localized logger.
     * @see Logger#getLogger(String)
     */
    public static LocalizedLogger getLocalizedLogger(final String name) {
        final Logger logger = Logger.getLogger(name);
        return new LocalizedLogger(logger, Locale.getDefault());
    }

    private final Logger logger;

    private final Locale locale;

    /**
     * Creates a new localized logger which will log localizable messages to the
     * provided Java {@code Logger} in the specified locale.
     *
     * @param logger
     *            The underlying Java {@code Logger} wrapped by this logger.
     * @param locale
     *            The locale to which this logger will localize all log
     *            messages.
     */
    LocalizedLogger(final Logger logger, final Locale locale) {
        this.locale = locale;
        this.logger = logger;
    }

    /**
     * Logs a CONFIG message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#config(String)
     */
    public void config(final Arg0 d) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get().toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#config(String)
     */
    public <T1> void config(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2> void config(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3> void config(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3, T4> void config(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3, T4, T5> void config(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3, T4, T5, T6> void config(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void config(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void config(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3, a4, a5, a6, a7, a8)
                    .toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
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
     * @see java.util.logging.Logger#config(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void config(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs a CONFIG message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#config(String)
     */
    public void config(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(d.get(args).toString(locale));
        }
    }

    /**
     * Logs a CONFIG message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#config(String)
     */
    public void config(final LocalizableMessage m) {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config(m.toString(locale));
        }
    }

    /**
     * Logs a FINE message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#fine(String)
     */
    public void fine(final Arg0 d) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get().toString(locale));
        }
    }

    /**
     * Logs a FINE message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1> void fine(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2> void fine(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3> void fine(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3, T4> void fine(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3, T4, T5> void fine(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3, T4, T5, T6> void fine(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void fine(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void fine(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
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
     * @see java.util.logging.Logger#fine(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void fine(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs a FINE message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#fine(String)
     */
    public void fine(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(d.get(args).toString(locale));
        }
    }

    /**
     * Logs a FINE message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#fine(String)
     */
    public void fine(final LocalizableMessage m) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(m.toString(locale));
        }
    }

    /**
     * Logs a FINER message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#finer(String)
     */
    public void finer(final Arg0 d) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get().toString(locale));
        }
    }

    /**
     * Logs a FINER message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1> void finer(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2> void finer(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3> void finer(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3, T4> void finer(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3, T4, T5> void finer(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3, T4, T5, T6> void finer(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void finer(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void finer(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
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
     * @see java.util.logging.Logger#finer(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void finer(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs a FINER message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#finer(String)
     */
    public void finer(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(d.get(args).toString(locale));
        }
    }

    /**
     * Logs a FINER message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#finer(String)
     */
    public void finer(final LocalizableMessage m) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(m.toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#finest(String)
     */
    public void finest(final Arg0 d) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get().toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1> void finest(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2> void finest(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3> void finest(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3, T4> void finest(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3, T4, T5> void finest(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3, T4, T5, T6> void finest(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void finest(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void finest(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3, a4, a5, a6, a7, a8)
                    .toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
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
     * @see java.util.logging.Logger#finest(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void finest(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs a FINEST message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#finest(String)
     */
    public void finest(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(d.get(args).toString(locale));
        }
    }

    /**
     * Logs a FINEST message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#finest(String)
     */
    public void finest(final LocalizableMessage m) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(m.toString(locale));
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
     * Returns the underlying Java {@code Logger} wrapped by this logger.
     *
     * @return The underlying Java {@code Logger} wrapped by this logger.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the name of this logger.
     *
     * @return The name of this logger.
     * @see java.util.logging.Logger#getName()
     */
    public String getName() {
        return logger.getName();
    }

    /**
     * Logs an INFO message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#info(String)
     */
    public void info(final Arg0 d) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get().toString(locale));
        }
    }

    /**
     * Logs an INFO message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#info(String)
     */
    public <T1> void info(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2> void info(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3> void info(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3, T4> void info(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5> void info(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6> void info(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void info(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void info(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
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
     * @see java.util.logging.Logger#info(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs an INFO message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#info(String)
     */
    public void info(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(d.get(args).toString(locale));
        }
    }

    /**
     * Logs an INFO message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#info(String)
     */
    public void info(final LocalizableMessage m) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(m.toString(locale));
        }
    }

    /**
     * Returns {@code true} if this logger will log messages at the specified
     * level.
     *
     * @param level
     *            The log level.
     * @return {@code true} if this logger will log messages at the specified
     *         level, otherwise {@code false}.
     * @see java.util.logging.Logger#isLoggable(Level)
     */
    public boolean isLoggable(final Level level) {
        return logger.isLoggable(level);
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#log(Level,String)
     */
    public void log(final Level level, final Arg0 d) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get().toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public void log(final Level level, final Arg0 d, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get().toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1> void log(final Level level, final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1> void log(final Level level, final Arg1<T1> d, final T1 a1,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2> void log(final Level level, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2> void log(final Level level, final Arg2<T1, T2> d,
            final T1 a1, final T2 a2, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3> void log(final Level level, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3> void log(final Level level, final Arg3<T1, T2, T3> d,
            final T1 a1, final T2 a2, final T3 a3, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param level
     *            The log level.
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
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3, T4> void log(final Level level,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param level
     *            The log level.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3, T4> void log(final Level level,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3, T4, T5> void log(final Level level,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3, T4, T5> void log(final Level level,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5).toString(locale),
                    thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3, T4, T5, T6> void log(final Level level,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void log(final Level level,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5, a6).toString(locale),
                    thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void log(final Level level,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5, a6, a7)
                    .toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void log(final Level level,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5, a6, a7)
                    .toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void log(final Level level,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(level)) {
            logger.log(level,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void log(final Level level,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale),
                    thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @see java.util.logging.Logger#log(Level,String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void log(final Level level,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
                    .toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void log(final Level level,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
                    .toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#log(Level,String)
     */
    public void log(final Level level, final ArgN d, final Object... args) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(args).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param thrown
     *            The Throwable associated with log message.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public void log(final Level level, final ArgN d, final Throwable thrown,
            final Object... args) {
        if (logger.isLoggable(level)) {
            logger.log(level, d.get(args).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#log(Level,String)
     */
    public void log(final Level level, final LocalizableMessage m) {
        if (logger.isLoggable(level)) {
            logger.log(level, m.toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param m
     *            The pre-formatted message.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#log(Level,String,Throwable)
     */
    public void log(final Level level, final LocalizableMessage m,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.log(level, m.toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg0 d) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get().toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param d
     *            The message descriptor.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg0 d, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get().toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1> void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1> void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg1<T1> d, final T1 a1,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2> void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg2<T1, T2> d, final T1 a1,
            final T2 a2) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, d.get(a1, a2)
                    .toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2> void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg2<T1, T2> d, final T1 a1,
            final T2 a2, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, d.get(a1, a2)
                    .toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3> void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, d.get(a1, a2, a3)
                    .toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3> void logp(final Level level, final String sourceClass,
            final String sourceMethod, final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, d.get(a1, a2, a3)
                    .toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3, T4> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, d.get(a1, a2, a3, a4)
                    .toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param a1
     *            The first message argument.
     * @param a2
     *            The second message argument.
     * @param a3
     *            The third message argument.
     * @param a4
     *            The fourth message argument.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3, T4> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg4<T1, T2, T3, T4> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, d.get(a1, a2, a3, a4)
                    .toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3, T4, T5> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3, T4, T5> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg5<T1, T2, T3, T4, T5> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3, T4, T5, T6> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param d
     *            The message descriptor.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale),
                    thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
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
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
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
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void logp(final Level level,
            final String sourceClass, final String sourceMethod,
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9, final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(locale),
                    thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public void logp(final Level level, final String sourceClass,
            final String sourceMethod, final ArgN d, final Object... args) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(args).toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param d
     *            The message descriptor.
     * @param thrown
     *            The Throwable associated with log message.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public void logp(final Level level, final String sourceClass,
            final String sourceMethod, final ArgN d, final Throwable thrown,
            final Object... args) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod,
                    d.get(args).toString(locale), thrown);
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#logp(Level,String,String,String)
     */
    public void logp(final Level level, final String sourceClass,
            final String sourceMethod, final LocalizableMessage m) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, m.toString(locale));
        }
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level
     *            The log level.
     * @param sourceClass
     *            The name of class that issued the logging request.
     * @param sourceMethod
     *            The name of class that issued the logging request.
     * @param m
     *            The pre-formatted message.
     * @param thrown
     *            The Throwable associated with log message.
     * @see java.util.logging.Logger#logp(Level,String,String,String,Throwable)
     */
    public void logp(final Level level, final String sourceClass,
            final String sourceMethod, final LocalizableMessage m,
            final Throwable thrown) {
        if (logger.isLoggable(level)) {
            logger.logp(level, sourceClass, sourceMethod, m.toString(locale),
                    thrown);
        }
    }

    /**
     * Logs a SEVERE message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#severe(String)
     */
    public void severe(final Arg0 d) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get().toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1> void severe(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2> void severe(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3> void severe(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3, T4> void severe(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3, T4, T5> void severe(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3, T4, T5, T6> void severe(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void severe(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void severe(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3, a4, a5, a6, a7, a8)
                    .toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
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
     * @see java.util.logging.Logger#severe(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void severe(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs a SEVERE message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#severe(String)
     */
    public void severe(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(d.get(args).toString(locale));
        }
    }

    /**
     * Logs a SEVERE message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#severe(String)
     */
    public void severe(final LocalizableMessage m) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(m.toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
     *
     * @param d
     *            The message descriptor.
     * @see java.util.logging.Logger#warning(String)
     */
    public void warning(final Arg0 d) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get().toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param d
     *            The message descriptor.
     * @param a1
     *            The first message argument.
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1> void warning(final Arg1<T1> d, final T1 a1) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2> void warning(final Arg2<T1, T2> d, final T1 a1, final T2 a2) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3> void warning(final Arg3<T1, T2, T3> d, final T1 a1,
            final T2 a2, final T3 a3) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3, T4> void warning(final Arg4<T1, T2, T3, T4> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3, a4).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3, T4, T5> void warning(final Arg5<T1, T2, T3, T4, T5> d,
            final T1 a1, final T2 a2, final T3 a3, final T4 a4, final T5 a5) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3, a4, a5).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3, T4, T5, T6> void warning(
            final Arg6<T1, T2, T3, T4, T5, T6> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7> void warning(
            final Arg7<T1, T2, T3, T4, T5, T6, T7> d, final T1 a1, final T2 a2,
            final T3 a3, final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> void warning(
            final Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(
                    locale));
        }
    }

    /**
     * Logs a WARNING message.
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
     * @see java.util.logging.Logger#warning(String)
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warning(
            final Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, final T1 a1,
            final T2 a2, final T3 a3, final T4 a4, final T5 a5, final T6 a6,
            final T7 a7, final T8 a8, final T9 a9) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
                    locale));
        }
    }

    /**
     * Logs a WARNING message.
     *
     * @param d
     *            The message descriptor.
     * @param args
     *            The message arguments.
     * @see java.util.logging.Logger#warning(String)
     */
    public void warning(final ArgN d, final Object... args) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(d.get(args).toString(locale));
        }
    }

    /**
     * Logs a WARNING message.
     *
     * @param m
     *            The pre-formatted message.
     * @see java.util.logging.Logger#warning(String)
     */
    public void warning(final LocalizableMessage m) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(m.toString(locale));
        }
    }

}
