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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Given a version string, parses out the version information and
 * provides an easy way to retrieve the verified version information.
 * <p/>
 * Expects the version string to be in the format:
 * <code>versionType=majorNumber.minorNumber; versionType=majorNumber.minorNumber; ...</code>
 */
public final class VersionMap {

    // Pattern matches: text=123.123; text=123.123
    private static final Pattern API_VERSION_REGEX = Pattern.compile("^\\w+=\\d+\\.\\d+(;\\s*\\w+=\\d+\\.\\d+)*$");

    private static final String DELIMITER = ";\\s*";
    private static final String EQUALS = "=";

    private final Map<VersionType, String> versionMap;

    private VersionMap(final Map<VersionType, String> versionMap) {
        this.versionMap = versionMap;
    }

    /**
     * Given the interested version type, returns the corresponding version value in the format major.minor.
     *
     * @param type
     *         The version type
     *
     * @return The corresponding version value (major.minor) else null if no match
     */
    public String getVersion(final VersionType type) {
        return versionMap.get(type);
    }

    /**
     * Given a valid version string, parses out the version information
     * and constructs a new  {@link VersionMap} instance to represent it.
     * <p/>
     * Expects version string to be in the format:
     * <code>
     * versionType=majorNumber.minorNumber; versionType=majorNumber.minorNumber
     * </code>
     *
     * @param versionString
     *         A valid version string
     *
     * @return A corresponding {@link VersionMap}
     *
     * @throws NullPointerException
     *         If a null version string is passed
     * @throws IllegalArgumentException
     *         If an invalid version string is passed
     */
    public static VersionMap valueOf(final String versionString) {
        Reject.ifNull(versionString);
        Reject.ifTrue(versionString.isEmpty(), "Version string is empty");

        if (!API_VERSION_REGEX.matcher(versionString).matches()) {
            throw new IllegalArgumentException("Version string is in an invalid format");
        }

        final Map<VersionType, String> versions = new HashMap<VersionType, String>();
        final String[] versionTypes = versionString.split(DELIMITER);

        for (String versionType : versionTypes) {
            final String[] versionParts = versionType.split(EQUALS);
            final VersionType type = VersionType.getType(versionParts[0]);

            if (type == null) {
                // Flag up as warning.
                continue;
            }

            versions.put(type, versionParts[1]);
        }

        return new VersionMap(versions);
    }

}
