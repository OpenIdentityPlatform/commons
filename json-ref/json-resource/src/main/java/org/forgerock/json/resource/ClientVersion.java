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

import org.forgerock.json.resource.descriptor.Version;
import org.forgerock.util.Reject;

import java.util.regex.Pattern;

/**
 * Given a version string, parses out the version information and
 * provides an easy way to retrieve the verified version information.
 * <p/>
 * Expects the version string to be in the format:
 * <code>versionType=majorNumber.minorNumber,versionType=majorNumber.minorNumber,...</code>
 */
public final class ClientVersion {

    private final Version protocolVersion;
    private final Version resourceVersion;

    private ClientVersion(final Builder builder) {
        protocolVersion = builder.protocolVersion;
        resourceVersion = builder.resourceVersion;
    }

    /**
     * @return The protocol version
     */
    public Version getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * @return The resource version
     */
    public Version getResourceVersion() {
        return resourceVersion;
    }


    public static final class Builder {

        // Pattern matches: text=123.123,text=123.123
        private static final Pattern API_VERSION_REGEX =
                Pattern.compile("^(protocol|resource)=\\d+\\.\\d+(\\s*,\\s*(protocol|resource)=\\d+\\.\\d+)?$");

        private static final String PROTOCOL_VERSION = "protocol";
        private static final String RESOURCE_VERSION = "resource";

        private static final String DELIMITER = "\\s*,\\s*";
        private static final String EQUALS = "=";

        private Version protocolVersion;
        private Version resourceVersion;

        /**
         * Given a valid version string, parses out the version information
         * and constructs a new  {@link ClientVersion} instance to represent it.
         * <p/>
         * Expects version string to be in the format:
         * <code>
         * versionType=majorNumber.minorNumber;versionType=majorNumber.minorNumber
         * </code>
         *
         * @param versionString
         *         A valid version string
         *
         * @return A corresponding {@link ClientVersion}
         *
         * @throws NullPointerException
         *         If a null version string is passed
         * @throws IllegalArgumentException
         *         If an invalid version string is passed
         */
        public Builder parseVersionString(final String versionString) {
            if (versionString == null || versionString.isEmpty()) {
                return this;
            }

            Reject.ifFalse(API_VERSION_REGEX.matcher(versionString).matches(),
                    "Version string is in an invalid format");

            final String[] versionTypes = versionString.split(DELIMITER);

            for (String versionType : versionTypes) {
                final String[] versionParts = versionType.split(EQUALS);

                if (PROTOCOL_VERSION.equalsIgnoreCase(versionParts[0])) {
                    protocolVersion = Version.valueOf(versionParts[1]);
                } else if (RESOURCE_VERSION.equalsIgnoreCase(versionParts[0])) {
                    resourceVersion = Version.valueOf(versionParts[1]);
                }
            }
            return this;
        }

        public Builder setProtocolVersionIfNull(final Version protocolVersion) {
            Reject.ifNull(protocolVersion);
            if (this.protocolVersion == null) {
                this.protocolVersion = protocolVersion;
            }
            return this;
        }

        public Builder setResourceVersionIfNull(final Version resourceVersion) {
            Reject.ifNull(resourceVersion);
            if (this.resourceVersion == null) {
                this.resourceVersion = resourceVersion;
            }
            return this;
        }

        public ClientVersion build() {
            return new ClientVersion(this);
        }

    }

}
