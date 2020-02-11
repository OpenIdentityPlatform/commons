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
 *      Copyright 2009 Sun Microsystems, Inc.
 *      Portions copyright 2011-2012 ForgeRock AS
 */

package org.forgerock.i18n;

import java.io.Serializable;
import java.util.Formattable;
import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;

import org.forgerock.i18n.LocalizableMessageDescriptor.AbstractLocalizableMessageDescriptor;

/**
 * A localizable message whose {@code String} representation can be retrieved in
 * one or more locales. A message is localized each time it is converted to a
 * {@code String} using one of its {@link #toString} methods.
 * <p>
 * Localizable messages are particularly useful in situations where a message a
 * destined for multiple recipients, potentially in different locales. For
 * example, a server application may record a message in its log file using its
 * default locale, but also send the same message to the client using the
 * client's locale (if known).
 * <p>
 * In most cases messages are intended for use in a locale-sensitive manner
 * although this class defines convenience methods for creating non-localizable
 * messages whose {@code String} representation is always the same regardless of
 * the requested locale.
 * <p>
 * This class implements {@code CharSequence} so that messages can be supplied
 * as arguments to other messages. This way messages can be composed of
 * fragments of other messages if necessary.
 *
 * @see LocalizableMessageBuilder
 */
public final class LocalizableMessage implements CharSequence, Formattable,
        Comparable<LocalizableMessage>, Serializable {

    /**
     * Generated serialization ID.
     */
    private static final long serialVersionUID = 8011606572832995899L;

    /**
     * Represents an empty message string.
     */
    public static final LocalizableMessage EMPTY = LocalizableMessage.raw("");

    // Variable used to workaround a bug in AIX Java 1.6
    // TODO: remove this code once the JDK issue referenced in 3077 is
    // closed.
    private static final boolean IS_AIX_POST5 = isAIXPost5();

    /**
     * Creates an non-localizable message whose {@code String} representation is
     * always the same regardless of the requested locale.
     * <p>
     * Note that the types for {@code args} must be consistent with any argument
     * specifiers appearing in {@code formatString} according to the rules of
     * {@link java.util.Formatter}. A mismatch in type information will cause
     * this message to render without argument substitution. Before using this
     * method you should be sure that the message you are creating is not locale
     * sensitive. If it is locale sensitive consider defining an appropriate
     * {@link LocalizableMessageDescriptor}.
     * <p>
     * This method handles the special case where a {@code CharSequence} needs
     * to be converted directly to {@code LocalizableMessage} as follows:
     *
     * <pre>
     * String s = ...;
     *
     * // Both of these are equivalent:
     * LocalizableMessage m = LocalizableMessage.raw(s);
     * LocalizableMessage m = LocalizableMessage.raw("%s", s);
     * </pre>
     *
     * @param formatString
     *            The raw message format string.
     * @param args
     *            The raw message parameters.
     * @return A non-localizable messages whose {@code String} representation is
     *         always the same regardless of the requested locale.
     * @throws NullPointerException
     *             If {@code formatString} was {@code null}.
     */
    public static LocalizableMessage raw(final CharSequence formatString,
            final Object... args) {
        if (formatString == null) {
            throw new NullPointerException("formatString was null");
        }

        /*
         * Experience with OpenDJ (see OPENDJ-142) has shown that this method is
         * heavily abused in the single argument case where a developer wishes
         * to convert a String to a LocalizableMessage:
         */
        // String s = ...;
        // LocalizableMessage m = LocalizableMessage.raw(s);

        /*
         * This will have unexpected behavior if the string contains a
         * formatting character such as "%".
         */
        if (args == null || args.length == 0) {
            return LocalizableMessageDescriptor.RAW0.get(formatString);
        } else {
            return new LocalizableMessageDescriptor.Raw(formatString).get(args);
        }
    }

    /**
     * Creates a new message whose content is the {@code String} representation
     * of the provided {@code Object}.
     *
     * @param object
     *            The object to be converted to a message, may be {@code null}.
     * @return The new message.
     */
    public static LocalizableMessage valueOf(final Object object) {
        if (object instanceof LocalizableMessage) {
            return (LocalizableMessage) object;
        } else if (object instanceof LocalizableMessageBuilder) {
            return ((LocalizableMessageBuilder) object).toMessage();
        } else {
            return raw(String.valueOf(object));
        }
    }

    /**
     * Returns whether we are running post 1.5 on AIX or not.
     *
     * @return {@code true} if we are running post 1.5 on AIX and {@code false}
     *         otherwise.
     */
    private static boolean isAIXPost5() {
        // TODO: remove this code once the JDK issue referenced in 3077 is
        // closed.
        boolean isJDK15 = false;
        try {
            final String javaRelease = System.getProperty("java.version");
            isJDK15 = javaRelease.startsWith("1.5");
        } catch (final Throwable t) {
            System.err.println("Cannot get the java version: " + t);
        }
        final boolean isAIX = "aix".equalsIgnoreCase(System
                .getProperty("os.name"));
        return !isJDK15 && isAIX;
    }

    // Descriptor of this message.
    private final AbstractLocalizableMessageDescriptor descriptor;

    // Values used to replace argument specifiers in the format string.
    private final Object[] args;

    /**
     * Creates a new parameterized message instance. See the class header for
     * instructions on how to create messages outside of this package.
     *
     * @param descriptor
     *            The message descriptor.
     * @param args
     *            The message parameters.
     */
    LocalizableMessage(final AbstractLocalizableMessageDescriptor descriptor,
            final Object... args) {
        this.descriptor = descriptor;
        this.args = args;
    }

    /**
     * Returns the {@code char} value at the specified index of the
     * {@code String} representation of this message in the default locale.
     *
     * @param index
     *            The index of the {@code char} value to be returned.
     * @return The specified {@code char} value.
     * @throws IndexOutOfBoundsException
     *             If the {@code index} argument is negative or not less than
     *             {@code length()}.
     */
    public char charAt(final int index) {
        return charAt(Locale.getDefault(), index);
    }

    /**
     * Returns the {@code char} value at the specified index of the
     * {@code String} representation of this message in the specified locale.
     *
     * @param locale
     *            The locale.
     * @param index
     *            The index of the {@code char} value to be returned.
     * @return The specified {@code char} value.
     * @throws IndexOutOfBoundsException
     *             If the {@code index} argument is negative or not less than
     *             {@code length()}.
     * @throws NullPointerException
     *             If {@code locale} was {@code null}.
     */
    public char charAt(final Locale locale, final int index) {
        return toString(locale).charAt(index);
    }

    /**
     * Compares this message with the specified message for order in the default
     * locale. Returns a negative integer, zero, or a positive integer as this
     * object is less than, equal to, or greater than the specified object.
     *
     * @param message
     *            The message to be compared.
     * @return A negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(final LocalizableMessage message) {
        return toString().compareTo(message.toString());
    }

    /**
     * Returns {@code true} if the provided object is a message whose
     * {@code String} representation is equal to the {@code String}
     * representation of this message in the default locale.
     *
     * @param o
     *            The object to be compared for equality with this message.
     * @return {@code true} if this message is the equal to {@code o}, otherwise
     *         {@code false}.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof LocalizableMessage) {
            final LocalizableMessage message = (LocalizableMessage) o;
            return toString().equals(message.toString());
        } else {
            return false;
        }
    }

    /**
     * Formats this message using the provided {@link Formatter}.
     *
     * @param formatter
     *            The {@link Formatter}.
     * @param flags
     *            The flags modify the output format. The value is interpreted
     *            as a bitmask. Any combination of the following flags may be
     *            set: {@link java.util.FormattableFlags#LEFT_JUSTIFY},
     *            {@link java.util.FormattableFlags#UPPERCASE}, and
     *            {@link java.util.FormattableFlags#ALTERNATE}. If no flags are
     *            set, the default formatting of the implementing class will
     *            apply.
     * @param width
     *            The minimum number of characters to be written to the output.
     *            If the length of the converted value is less than the
     *            {@code width} then the output will be padded by white space
     *            until the total number of characters equals width. The padding
     *            is at the beginning by default. If the
     *            {@link java.util.FormattableFlags#LEFT_JUSTIFY} flag is set
     *            then the padding will be at the end. If {@code width} is
     *            {@code -1} then there is no minimum.
     * @param precision
     *            The maximum number of characters to be written to the output.
     *            The precision is applied before the width, thus the output
     *            will be truncated to {@code precision} characters even if the
     *            {@code width} is greater than the {@code precision}. If
     *            {@code precision} is {@code -1} then there is no explicit
     *            limit on the number of characters.
     * @throws IllegalFormatException
     *             If any of the parameters are invalid. For specification of
     *             all possible formatting errors, see the <a
     *             href="../util/Formatter.html#detail">Details</a> section of
     *             the formatter class specification.
     */
    public void formatTo(final Formatter formatter, final int flags,
            final int width, final int precision) {
        // Ignores flags, width and precision for now.
        // see javadoc for Formattable
        final Locale l = formatter.locale();
        formatter.format(l, descriptor.getFormatString(l), args);
    }

    /**
     * Returns the hash code value for this message calculated using the hash
     * code of the {@code String} representation of this message in the default
     * locale.
     *
     * @return The hash code value for this message.
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns the length of the {@code String} representation of this message
     * in the default locale.
     *
     * @return The length of the {@code String} representation of this message
     *         in the default locale.
     */
    public int length() {
        return length(Locale.getDefault());
    }

    /**
     * Returns the length of the {@code String} representation of this message
     * in the specified locale.
     *
     * @param locale
     *            The locale.
     * @return The length of the {@code String} representation of this message
     *         in the specified locale.
     * @throws NullPointerException
     *             If {@code locale} was {@code null}.
     */
    public int length(final Locale locale) {
        return toString(locale).length();
    }

    /**
     * Returns the ordinal associated with this message, or {@code -1} if
     * undefined. A message can be uniquely identified by its resource name and
     * ordinal.
     * <p>
     * This may be useful when an application wishes to identify the source of a
     * message. For example, a logging implementation could log the resource
     * name in addition to the ordinal in order to unambiguously identify a
     * message in a locale independent way.
     *
     * @return The ordinal associated with this descriptor, or {@code -1} if
     *         undefined.
     * @see LocalizableMessage#resourceName()
     */
    public int ordinal() {
        return descriptor.ordinal();
    }

    /**
     * Returns the name of the resource in which this message is defined. A
     * message can be uniquely identified by its resource name and ordinal.
     * <p>
     * This may be useful when an application wishes to identify the source of a
     * message. For example, a logging implementation could log the resource
     * name in addition to the ordinal in order to unambiguously identify a
     * message in a locale independent way.
     * <p>
     * The resource name may be used for obtaining named loggers, e.g. using
     * SLF4J's {@code org.slf4j.LoggerFactory#getLogger(String name)}.
     *
     * @return The name of the resource in which this message is defined, or
     *         {@code null} if this message is a raw message and its source is
     *         undefined.
     * @see LocalizableMessage#ordinal()
     */
    public String resourceName() {
        return descriptor.resourceName();
    }

    /**
     * Returns a new {@code CharSequence} which is a subsequence of the
     * {@code String} representation of this message in the default locale. The
     * subsequence starts with the {@code char} value at the specified index and
     * ends with the {@code char} value at index {@code end - 1} . The length
     * (in {@code char}s) of the returned sequence is {@code end - start}, so if
     * {@code start == end} then an empty sequence is returned.
     *
     * @param start
     *            The start index, inclusive.
     * @param end
     *            The end index, exclusive.
     * @return The specified subsequence.
     * @throws IndexOutOfBoundsException
     *             If {@code start} or {@code end} are negative, if {@code end}
     *             is greater than {@code length()}, or if {@code start} is
     *             greater than {@code end}.
     */
    public CharSequence subSequence(final int start, final int end) {
        return subSequence(Locale.getDefault(), start, end);
    }

    /**
     * Returns a new {@code CharSequence} which is a subsequence of the
     * {@code String} representation of this message in the specified locale.
     * The subsequence starts with the {@code char} value at the specified index
     * and ends with the {@code char} value at index {@code end - 1} . The
     * length (in {@code char}s) of the returned sequence is {@code end - start}
     * , so if {@code start == end} then an empty sequence is returned.
     *
     * @param locale
     *            The locale.
     * @param start
     *            The start index, inclusive.
     * @param end
     *            The end index, exclusive.
     * @return The specified subsequence.
     * @throws IndexOutOfBoundsException
     *             If {@code start} or {@code end} are negative, if {@code end}
     *             is greater than {@code length()}, or if {@code start} is
     *             greater than {@code end}.
     * @throws NullPointerException
     *             If {@code locale} was {@code null}.
     */
    public CharSequence subSequence(final Locale locale, final int start,
            final int end) {
        return toString(locale).subSequence(start, end);
    }

    /**
     * Returns the {@code String} representation of this message in the default
     * locale.
     *
     * @return The {@code String} representation of this message.
     */
    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    /**
     * Returns the {@code String} representation of this message in the
     * specified locale.
     *
     * @param locale
     *            The locale.
     * @return The {@code String} representation of this message.
     * @throws NullPointerException
     *             If {@code locale} was {@code null}.
     */
    @SuppressWarnings("resource")
    public String toString(final Locale locale) {
        String s;
        final String fmt = descriptor.getFormatString(locale);
        if (descriptor.requiresFormatter()) {
            try {
                // TODO: remove this code once the JDK issue referenced in 3077
                // is closed.
                if (IS_AIX_POST5) {
                    // Java 6 in AIX Formatter does not handle properly
                    // Formattable arguments; this code is a workaround for the
                    // problem.
                    boolean changeType = false;
                    for (final Object o : args) {
                        if (o instanceof Formattable) {
                            changeType = true;
                            break;
                        }
                    }
                    if (changeType) {
                        final Object[] newArgs = new Object[args.length];
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Formattable) {
                                newArgs[i] = args[i].toString();
                            } else {
                                newArgs[i] = args[i];
                            }
                        }
                        s = new Formatter(locale).format(locale, fmt, newArgs)
                                .toString();
                    } else {
                        s = new Formatter(locale).format(locale, fmt, args)
                                .toString();
                    }
                } else {
                    s = new Formatter(locale).format(locale, fmt, args)
                            .toString();
                }
            } catch (final IllegalFormatException e) {
                // This should not happen with any of our internal messages.
                // However, this may happen for raw messages that have a
                // mismatch between argument specifier type and argument type.
                s = fmt;
            }
        } else {
            s = fmt;
        }
        if (s == null) {
            s = "";
        }
        return s;
    }

}
