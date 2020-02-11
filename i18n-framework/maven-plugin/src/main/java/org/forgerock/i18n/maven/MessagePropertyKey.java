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
 *      Copyright 2008 Sun Microsystems, Inc.
 *      Portions copyright 2011 ForgeRock AS
 */

package org.forgerock.i18n.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A representation of a message property key contained in a message property
 * file. A key comprises of an upper-case name and an optional ordinal:
 * <ul>
 * <li>{@code NAME} is an upper-case string containing characters and the
 * underscore character for describing the purpose of the message.</li>
 * <li>{@code ORDINAL} is an integer that makes the message unique within the
 * property file.</li>
 * </ul>
 * Message property keys have the following string representation and are parsed
 * using the {@code valueOf(String)} method.
 *
 * <pre>
 * NAME[_ORDINAL]
 * </pre>
 *
 * If no ordinal is provided then it will default to {@code -1}. Ordinals should
 * be used for messages may be used for support purposes since they provide a
 * language independent means for identifying the message.
 */
final class MessagePropertyKey implements Comparable<MessagePropertyKey> {
    // Message property keys must contain an upper-case name, optionally
    // containing underscore characters, followed by an optional ordinal.
    private static final Pattern PATTERN = Pattern
            .compile("^([A-Z][A-Z0-9_]*?)(_([0-9]+))?$");

    /**
     * Parses a message property key from a string value.
     *
     * @param keyString
     *            The property key string.
     * @return The parsed message property key.
     * @throws IllegalArgumentException
     *             If the message property string had an invalid syntax.
     */
    static MessagePropertyKey valueOf(final String keyString) {
        final Matcher matcher = PATTERN.matcher(keyString);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Error processing "
                            + keyString
                            + ". The provided key string must be of the form NAME[_ORDINAL]");
        }

        if (matcher.group(3) == null) {
            // No ordinal.
            return new MessagePropertyKey(keyString, -1);
        } else {
            final String name = matcher.group(1);
            final int ordinal = Integer.parseInt(matcher.group(3));
            return new MessagePropertyKey(name, ordinal);
        }
    }

    // The message name.
    private final String name;

    // The ordinal will be -1 if none was specified.
    private final int ordinal;

    // Pattern for searching the key in source files.
    private final Pattern startRegex;
    private final Pattern midRegex;
    private final Pattern endRegex;

    /**
     * Creates a new message property key with the provided name and ordinal.
     *
     * @param name
     *            The name of the message key.
     * @param ordinal
     *            The ordinal of the message key, or {@code -1} if none was
     *            provided.
     */
    private MessagePropertyKey(final String name, final int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
        this.startRegex = Pattern.compile(name + "[^A-Z0-9_].*");
        this.midRegex = Pattern.compile(".*[^A-Z0-9_]" + name + "[^A-Z0-9_].*");
        this.endRegex = Pattern.compile(".*[^A-Z0-9_]" + name);
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final MessagePropertyKey k) {
        if (ordinal == k.ordinal) {
            return name.compareTo(k.name);
        } else {
            return ordinal - k.ordinal;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof MessagePropertyKey) {
            final MessagePropertyKey k = (MessagePropertyKey) obj;
            return this.compareTo(k) == 0;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 31 * name.hashCode() + ordinal;
    }

    /**
     * Returns {@code true} if this message property key is present in the
     * provided line of text.
     *
     * @param line
     *            The line of text.
     * @return {@code true} if this message property key is present in the
     *         provided line of text.
     */
    boolean isPresent(final String line) {
        if (!line.contains(name)) { // Avoid regex if possible.
            return false;
        } else if (midRegex.matcher(line).matches()) { // Most likely regex.
            return true;
        } else if (endRegex.matcher(line).matches()) {
            return true;
        } else if (startRegex.matcher(line).matches()) {
            return true;
        } else {
            return line.equals(name); // Unlikely, but for completeness.
        }
    }

    /**
     * Returns the name of this message property key with the ordinal. This
     * method is equivalent to calling:
     *
     * <pre>
     * getName(true);
     * </pre>
     *
     * @return The name of this message property key.
     */
    @Override
    public String toString() {
        return getName(true);
    }

    /**
     * Returns the name of this message property key without the ordinal. This
     * method is equivalent to calling:
     *
     * <pre>
     * getName(false);
     * </pre>
     *
     * @return The name of this message property key.
     */
    String getName() {
        return getName(false);
    }

    /**
     * Returns the name of this message property key optionally including the
     * ordinal.
     *
     * @param includeOrdinal
     *            {@code true} if the ordinal should be appended to the key
     *            name.
     * @return The name of this message property key.
     */
    String getName(final boolean includeOrdinal) {
        if (!includeOrdinal || ordinal < 0) {
            return name;
        } else {
            final StringBuilder builder = new StringBuilder(name);
            builder.append("_");
            builder.append(ordinal);
            return builder.toString();
        }
    }

    /**
     * Returns the ordinal of this message property key.
     *
     * @return The ordinal of this message property key, or {@code -1} if it
     *         does not have an ordinal.
     */
    int getOrdinal() {
        return ordinal;
    }

}
