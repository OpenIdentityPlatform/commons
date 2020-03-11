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
 *      Portions copyright 2011 ForgeRock AS
 */

package org.forgerock.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * An opaque handle to a localizable message.
 */
public final class LocalizableMessageDescriptor {
    /**
     * Subclass for creating messages with no arguments.
     */
    public static final class Arg0 extends AbstractLocalizableMessageDescriptor {

        /**
         * Cached copy of the message created by this descriptor. We can get
         * away with this for the zero argument message because it is immutable.
         */
        private final LocalizableMessage message;

        private final boolean requiresFormat;

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg0(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
            final Object[] args = {};
            message = new LocalizableMessage(this, args);
            requiresFormat = containsArgumentLiterals(getFormatString());
        }

        /**
         * Creates a localizable message.
         *
         * @return The localizable message.
         */
        public LocalizableMessage get() {
            return message;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return requiresFormat;
        }

        /**
         * Indicates whether or not formatting should be applied to the given
         * format string. Note that a format string might have literal
         * specifiers (%% or %n for example) that require formatting but are not
         * replaced by arguments.
         *
         * @param s
         *            Candidate for formatting.
         * @return {@code true} if the format string requires formatting.
         */
        private boolean containsArgumentLiterals(final String s) {
            return s.contains("%%") || s.contains("%n"); // match Formatter
                                                         // literals
        }
    }

    /**
     * Subclass for creating messages with one argument.
     *
     * @param <T1>
     *            The type of the first message argument.
     */
    public static final class Arg1<T1> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg1(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1) {
            final Object[] args = { a1 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with two arguments.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     */
    public static final class Arg2<T1, T2> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg2(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2) {
            final Object[] args = { a1, a2 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with three arguments.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     */
    public static final class Arg3<T1, T2, T3> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg3(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3) {
            final Object[] args = { a1, a2, a3 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with four arguments.
     *
     * @param <T1>
     *            The type of the first message argument.
     * @param <T2>
     *            The type of the second message argument.
     * @param <T3>
     *            The type of the third message argument.
     * @param <T4>
     *            The type of the fourth message argument.
     */
    public static final class Arg4<T1, T2, T3, T4> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg4(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         * @param a4
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3,
                final T4 a4) {
            final Object[] args = { a1, a2, a3, a4 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with five arguments.
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
     */
    public static final class Arg5<T1, T2, T3, T4, T5> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg5(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         * @param a4
         *            A message argument.
         * @param a5
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3,
                final T4 a4, final T5 a5) {
            final Object[] args = { a1, a2, a3, a4, a5 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with six arguments.
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
     */
    public static final class Arg6<T1, T2, T3, T4, T5, T6> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg6(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         * @param a4
         *            A message argument.
         * @param a5
         *            A message argument.
         * @param a6
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3,
                final T4 a4, final T5 a5, final T6 a6) {
            final Object[] args = { a1, a2, a3, a4, a5, a6 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with seven arguments.
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
     */
    public static final class Arg7<T1, T2, T3, T4, T5, T6, T7> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg7(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         * @param a4
         *            A message argument.
         * @param a5
         *            A message argument.
         * @param a6
         *            A message argument.
         * @param a7
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3,
                final T4 a4, final T5 a5, final T6 a6, final T7 a7) {
            final Object[] args = { a1, a2, a3, a4, a5, a6, a7 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with eight arguments.
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
     */
    public static final class Arg8<T1, T2, T3, T4, T5, T6, T7, T8> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg8(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         * @param a4
         *            A message argument.
         * @param a5
         *            A message argument.
         * @param a6
         *            A message argument.
         * @param a7
         *            A message argument.
         * @param a8
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3,
                final T4 a4, final T5 a5, final T6 a6, final T7 a7, final T8 a8) {
            final Object[] args = { a1, a2, a3, a4, a5, a6, a7, a8 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with nine arguments.
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
     */
    public static final class Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends
            AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public Arg9(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param a1
         *            A message argument.
         * @param a2
         *            A message argument.
         * @param a3
         *            A message argument.
         * @param a4
         *            A message argument.
         * @param a5
         *            A message argument.
         * @param a6
         *            A message argument.
         * @param a7
         *            A message argument.
         * @param a8
         *            A message argument.
         * @param a9
         *            A message argument.
         */
        public LocalizableMessage get(final T1 a1, final T2 a2, final T3 a3,
                final T4 a4, final T5 a5, final T6 a6, final T7 a7,
                final T8 a8, final T9 a9) {
            final Object[] args = { a1, a2, a3, a4, a5, a6, a7, a8, a9 };
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Subclass for creating messages with an any number of arguments. In
     * general this class should be used when a message needs to be defined with
     * more arguments that can be handled with the current number of subclasses
     */
    public static final class ArgN extends AbstractLocalizableMessageDescriptor {

        /**
         * Creates a parameterized instance.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        public ArgN(final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            super(sourceClass, resourceName, key, ordinal);
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param args
         *            The message arguments.
         */
        public LocalizableMessage get(final Object... args) {
            return new LocalizableMessage(this, args);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return true;
        }

    }

    /**
     * Base class for all message descriptors.
     */
    abstract static class AbstractLocalizableMessageDescriptor {
        /**
         * Container for caching the last locale specific format string.
         */
        private static final class CachedFormatString {
            private final Locale locale;

            private final String formatString;

            private CachedFormatString(final Locale locale,
                    final String formatString) {
                this.locale = locale;
                this.formatString = formatString;
            }
        }

        // Used for accessing format string from the resource bundle.
        private final String key;

        /*
         * The class in which this descriptor is defined. This class will be
         * used to obtain the ClassLoader for retrieving the ResourceBundle. The
         * class may also be retrieved in order to uniquely identify the source
         * of a message, for example using getClass().getPackage().getName().
         */
        private final Class<?> sourceClass;

        /*
         * The name of the resource bundle containing the localizable message.
         */
        private final String resourceName;

        /*
         * The ordinal associated with this descriptor or -1 if undefined. A
         * message can be uniquely identified by its ordinal and class.
         */
        private final int ordinal;

        // It's ok if there are race conditions.
        private CachedFormatString cachedFormatString = null;

        /**
         * Creates a parameterized message descriptor.
         *
         * @param sourceClass
         *            The class in which this descriptor is defined. This class
         *            will be used to obtain the {@code ClassLoader} for
         *            retrieving the {@code ResourceBundle}. The class may also
         *            be retrieved in order to uniquely identify the source of a
         *            message, for example using
         *            {@code getClass().getPackage().getName()}.
         * @param resourceName
         *            The name of the resource bundle containing the localizable
         *            message.
         * @param key
         *            The resource bundle property key.
         * @param ordinal
         *            The ordinal associated with this descriptor or {@code -1}
         *            if undefined. A message can be uniquely identified by its
         *            ordinal and class.
         */
        private AbstractLocalizableMessageDescriptor(
                final Class<?> sourceClass, final String resourceName,
                final String key, final int ordinal) {
            this.sourceClass = sourceClass;
            this.resourceName = resourceName;
            this.key = key;
            this.ordinal = ordinal;
        }

        /**
         * Returns the format string which should be used when creating the
         * string representation of this message using the default locale.
         *
         * @return The format string.
         */
        final String getFormatString() {
            return getFormatString(Locale.getDefault());
        }

        /**
         * Returns the format string which should be used when creating the
         * string representation of this message using the specified locale.
         *
         * @param locale
         *            The locale.
         * @return The format string.
         * @throws NullPointerException
         *             If {@code locale} was {@code null}.
         */
        String getFormatString(final Locale locale) {
            if (locale == null) {
                throw new NullPointerException("locale was null");
            }

            // Fast path.
            final CachedFormatString cfs = cachedFormatString;
            if (cfs != null && cfs.locale == locale) {
                return cfs.formatString;
            }

            // There's a potential race condition here but it's benign - we'll
            // just do a bit more work than needed.
            final ResourceBundle bundle = getBundle(locale);
            final String formatString = bundle.getString(key);
            cachedFormatString = new CachedFormatString(locale, formatString);

            return formatString;
        }

        /**
         * Returns the ordinal associated with this message, or {@code -1} if
         * undefined. A message can be uniquely identified by its resource name
         * and ordinal.
         * <p>
         * This may be useful when an application wishes to identify the source
         * of a message. For example, a logging implementation could log the
         * resource name in addition to the ordinal in order to unambiguously
         * identify a message in a locale independent way.
         *
         * @return The ordinal associated with this descriptor, or {@code -1} if
         *         undefined.
         */
        public final int ordinal() {
            return ordinal;
        }

        /**
         * Indicates whether or not this descriptor format string should be
         * processed by {@code Formatter} during string rendering.
         *
         * @return {@code true} if a {@code Formatter} should be used, otherwise
         *         {@code false}.
         */
        abstract boolean requiresFormatter();

        /**
         * Returns the name of the resource in which this message is defined. A
         * message can be uniquely identified by its resource name and ordinal.
         * <p>
         * This may be useful when an application wishes to identify the source
         * of a message. For example, a logging implementation could log the
         * resource name in addition to the ordinal in order to unambiguously
         * identify a message in a locale independent way.
         * <p>
         * The resource name may be used for obtaining named loggers, e.g. using
         * SLF4J's {@code org.slf4j.LoggerFactory#getLogger(String name)}.
         *
         * @return The name of the resource in which this message is defined, or
         *         {@code null} if this message is a raw message and its source
         *         is undefined.
         */
        public final String resourceName() {
            return resourceName;
        }

        private ResourceBundle getBundle(final Locale locale) {
            return ResourceBundle.getBundle(resourceName,
                    locale == null ? Locale.getDefault() : locale,
                    sourceClass.getClassLoader());
        }
    }

    /**
     * A descriptor for creating a raw message from a {@code String}. In general
     * this descriptor should NOT be used internally.
     */
    static final class Raw extends AbstractLocalizableMessageDescriptor {

        private final String formatString;

        private final boolean requiresFormatter;

        /**
         * Creates a parameterized instance.
         *
         * @param formatString
         *            The format string.
         */
        Raw(final CharSequence formatString) {
            super(Void.class, null, null, -1);
            this.formatString = formatString.toString();
            this.requiresFormatter = this.formatString.matches(".*%.*");
        }

        /**
         * Creates a message with arguments that will replace format specifiers
         * in the associated format string when the message is rendered to
         * string representation.
         *
         * @return The localizable message containing the provided arguments.
         * @param args
         *            The message arguments.
         */
        LocalizableMessage get(final Object... args) {
            return new LocalizableMessage(this, args);
        }

        /**
         * Overridden in order to bypass the resource bundle plumbing and return
         * the format string directly.
         */
        @Override
        String getFormatString(final Locale locale) {
            return this.formatString;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean requiresFormatter() {
            return this.requiresFormatter;
        }

    }

    /**
     * Cached zero arg raw message descriptor.
     */
    static final LocalizableMessageDescriptor.Raw RAW0 = new LocalizableMessageDescriptor.Raw(
            "%s");

    // Prevent instantiation.
    private LocalizableMessageDescriptor() {
        // Do nothing.
    }

}
