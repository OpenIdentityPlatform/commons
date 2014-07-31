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
package org.forgerock.json.resource;

/**
 * Represents some version in the form majorNumber.minorNumber, for instance 2.4.
 */
public final class Version implements Comparable<Version> {

    private static final Version[] DOT_ZERO_CACHE = new Version[10];

    static {
        for (int i = 0; i < DOT_ZERO_CACHE.length; i++) {
            DOT_ZERO_CACHE[i] = new Version(i, 0, null);
        }
    }

    private final int major;
    private final int minor;
    private final String s;

    /**
     * Creates a new version using the provided version information.
     *
     * @param major
     *         Major version number.
     * @param minor
     *         Minor version number.
     *
     * @return The version.
     */
    public static Version valueOf(final int major, final int minor) {
        return valueOf(major, minor, null);
    }

    /**
     * Creates a new version using the provided version information and a minor.
     *
     * @param major
     *         Major version number.
     *
     * @return The version.
     */
    public static Version valueOf(final int major) {
        return valueOf(major, 0);
    }

    private static Version valueOf(final int major, final int minor, final String s) {
        if (minor == 0 && major >= 0 && major < DOT_ZERO_CACHE.length) {
            return DOT_ZERO_CACHE[major];
        }
        return new Version(major, minor, s);
    }

    private Version(final int major, final int minor, final String s) {
        this.major = major;
        this.minor = minor;
        this.s = s != null ? s : (major + "." + minor);
    }

    /**
     * Parses the string argument as a version. The string must be one of the
     * following forms:
     * <p/>
     * <pre>
     * major
     * major.minor
     * </pre>
     *
     * @param s
     *         The string to be parsed as a version.
     *
     * @return The parsed version.
     *
     * @throws IllegalArgumentException
     *         If the string does not contain a parsable version.
     */
    public static Version valueOf(final String s) {
        final String[] fields = s.split("\\.");
        if (fields.length == 0 || fields.length > 3) {
            throw new IllegalArgumentException("Invalid version string " + s);
        }
        final int major = Integer.parseInt(fields[0]);
        final int minor = fields.length > 1 ? Integer.parseInt(fields[1]) : 0;
        return valueOf(major, minor, s);
    }

    /**
     * Returns the major version number.
     *
     * @return The major version number.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return The minor version number.
     */
    public int getMinor() {
        return minor;
    }

    @Override
    public int hashCode() {
        return (31 + major) * 31 + minor;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Version) {
            final Version that = (Version) obj;
            return major == that.major && minor == that.minor;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(final Version that) {
        if (major != that.major) {
            return major - that.major;
        }
        if (minor != that.minor) {
            return minor - that.minor;
        }
        return 0;
    }

    @Override
    public String toString() {
        return s;
    }

}
