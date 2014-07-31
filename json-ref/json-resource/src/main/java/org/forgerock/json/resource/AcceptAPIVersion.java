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
package org.forgerock.json.resource;

import org.forgerock.util.Reject;

import java.util.regex.Pattern;

/**
 * Represents the accepted protocol and resource versions.
 *
 * @since 2.4.0
 */
public final class AcceptAPIVersion {

    private final Version protocolVersion;
    private final Version resourceVersion;

    private AcceptAPIVersion(final Builder builder) {
        protocolVersion = builder.protocolVersion;
        resourceVersion = builder.resourceVersion;
    }

    /**
     * @return The acceptable protocol version
     */
    public Version getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * @return The acceptable resource version
     */
    public Version getResourceVersion() {
        return resourceVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AcceptAPIVersion)) {
            return false;
        }

        AcceptAPIVersion other = (AcceptAPIVersion)obj;

        if (protocolVersion == null ?
                other.protocolVersion != null : !protocolVersion.equals(other.protocolVersion)) {
            return false;
        }

        if (resourceVersion == null ?
                other.resourceVersion != null : !resourceVersion.equals(other.resourceVersion)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (protocolVersion == null ? 0 : protocolVersion.hashCode());
        result = 31 * result + (resourceVersion == null ? 0 : resourceVersion.hashCode());
        return result;
    }

    /**
     * Builder to assist with the construction of a {@link AcceptAPIVersion}.
     *
     * @since 2.4.0
     */
    public static final class Builder {

        // Pattern matches: text=123.123,text=123.123
        private static final Pattern EXPECTED_VERSION_FORMAT =
                Pattern.compile("^\\w+=\\d+\\.\\d+(\\s*,\\s*\\w+=\\d+\\.\\d+)?$");

        private static final String PROTOCOL_VERSION = "protocol";
        private static final String RESOURCE_VERSION = "resource";

        private static final String DELIMITER = "\\s*,\\s*";
        private static final String EQUALS = "=";

        private Version protocolVersion;
        private Version resourceVersion;

        /**
         * Attempts to parse the passed version string, constructing corresponding {@link Version} objects as required.
         * If the version string is null or empty then no {@link Version} objects are created. Otherwise the expected
         * format is <code>text=majorNumber.minorNumber,text=majorNumber.minorNumber</code>.
         *
         * @param versionString
         *         The version string
         *
         * @return The builder instance
         *
         * @throws IllegalArgumentException
         *         If the string is an invalid format
         */
        public Builder parseVersionString(final String versionString) {
            if (versionString == null || versionString.isEmpty()) {
                return this;
            }

            Reject.ifFalse(EXPECTED_VERSION_FORMAT.matcher(versionString).matches(),
                    "Version string is in an invalid format: " + versionString);

            final String[] versionEntries = versionString.split(DELIMITER);

            for (String versionPair : versionEntries) {
                final String[] versionParts = versionPair.split(EQUALS);
                final String versionType = versionParts[0];
                final String versionValue = versionParts[1];

                if (PROTOCOL_VERSION.equalsIgnoreCase(versionType)) {
                    protocolVersion = Version.valueOf(versionValue);
                } else if (RESOURCE_VERSION.equalsIgnoreCase(versionType)) {
                    resourceVersion = Version.valueOf(versionValue);
                } else {
                    throw new IllegalArgumentException("Unknown version type: " + versionType);
                }
            }

            return this;
        }

        /**
         * Sets the accepted protocol version if it's not already set.
         *
         * @param protocolVersion
         *         Non-null protocol version
         *
         * @return The builder instance
         */
        public Builder setProtocolVersionIfNull(final Version protocolVersion) {
            Reject.ifNull(protocolVersion);
            if (this.protocolVersion == null) {
                this.protocolVersion = protocolVersion;
            }
            return this;
        }

        /**
         * Sets the accepted resource version if it's not already set.
         *
         * @param resourceVersion
         *         Non-null resource version
         *
         * @return The builder instance
         */
        public Builder setResourceVersionIfNull(final Version resourceVersion) {
            Reject.ifNull(resourceVersion);
            if (this.resourceVersion == null) {
                this.resourceVersion = resourceVersion;
            }
            return this;
        }

        /**
         * Builds a new {@link AcceptAPIVersion} instance.
         *
         * @return The new {@link AcceptAPIVersion} instance.
         */
        public AcceptAPIVersion build() {
            return new AcceptAPIVersion(this);
        }

    }

}
