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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.header;

import static org.forgerock.http.header.HeaderUtil.parseSingleValuedHeader;
import static org.forgerock.http.routing.Version.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Message;
import org.forgerock.http.routing.Version;
import org.forgerock.util.Pair;

/**
 * Processes the <strong>{@code Accept-API-Version}</strong> message header.
 * Represents the accepted protocol and resource versions.
 */
public final class AcceptApiVersionHeader implements Header {

    /**
     * Constructs a new header, initialized from the specified message.
     *
     * @param message The message to initialize the header from.
     * @return The parsed header.
     */
    public static AcceptApiVersionHeader valueOf(Message message) {
        return valueOf(parseSingleValuedHeader(message, NAME));
    }

    /**
     * Constructs a new header, initialized from the specified string value.
     *
     * @param string The value to initialize the header from.
     * @return The parsed header.
     */
    public static AcceptApiVersionHeader valueOf(String string) {
        Pair<Version, Version> parsedValue = parse(string);
        return new AcceptApiVersionHeader(parsedValue.getFirst(), parsedValue.getSecond());
    }

    static Pair<Version, Version> parse(String string) {
        if (string == null || string.isEmpty()) {
            return Pair.empty();
        }

        Matcher matcher = EXPECTED_VERSION_FORMAT.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Version string is in an invalid format: " + string);
        }

        Version protocolVersion = null;
        Version resourceVersion = null;
        if (matcher.group(1) != null) {
            protocolVersion = version(matcher.group(1));
        } else if (matcher.group(3) != null) {
            resourceVersion = version(matcher.group(3));
        } else {
            protocolVersion = version(matcher.group(5));
            resourceVersion = version(matcher.group(7));
        }
        return Pair.of(protocolVersion, resourceVersion);
    }

    /** The name of this header. */
    public static final String NAME = "Accept-API-Version";
    private static final String PROTOCOL = "protocol";
    private static final String RESOURCE = "resource";
    private static final String EQUALS = "=";

    /**
     * Regex that accepts a comma separated protocol and resource version. The
     * protocol version, if present, MUST come first followed, optionally, by
     * the resource version. The version {@code String}s can either by
     * {@code Integer}s or {@code Double}s. It is invalid to contain neither a
     * protocol or resource version.
     *
     * Pattern matches:
     * <ul>
     *     <li>protocol=123.123,resource=123.123</li>
     *     <li>protocol=123,resource=123</li>
     *     <li>protocol=123.123</li>
     *     <li>protocol=123</li>
     *     <li>resource=123.123</li>
     *     <li>resource=123</li>
     * </ul>
     *
     * Pattern does not matches:
     * <ul>
     *     <li>resource=123.123,protocol=123.123</li>
     *     <li>protocol=123.123.123,resource=123.123.123</li>
     *     <li>protocol=123.123resource=123.123</li>
     *     <li>protocol=123.123 resource=123.123</li>
     *     <li>protocol= resource=</li>
     * </ul>
     */
    private static final String PROTOCOL_VERSION_REGEX = PROTOCOL + "=(\\d+(\\.\\d+)?)";
    private static final String RESOURCE_VERSION_REGEX = RESOURCE + "=(\\d+(\\.\\d+)?)";
    private static final Pattern EXPECTED_VERSION_FORMAT =
            Pattern.compile("^" + PROTOCOL_VERSION_REGEX + "$|^"
                    + RESOURCE_VERSION_REGEX + "$|^"
                    + PROTOCOL_VERSION_REGEX + "," + RESOURCE_VERSION_REGEX + "$");

    private Version protocolVersion;
    private Version resourceVersion;

    private AcceptApiVersionHeader(Version protocolVersion, Version resourceVersion) {
        this.protocolVersion = protocolVersion;
        this.resourceVersion = resourceVersion;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Gets the acceptable protocol version.
     *
     * @return The acceptable protocol version.
     */
    public Version getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Gets the acceptable resource version.
     *
     * @return The acceptable resource version.
     */
    public Version getResourceVersion() {
        return resourceVersion;
    }

    /**
     * Will set the accepted protocol version, if not provided in the
     * {@literal Accept-API-Version} header.
     *
     * @param version The default protocol version.
     * @return The accept api version header.
     */
    public AcceptApiVersionHeader withDefaultProtocolVersion(Version version) {
        if (protocolVersion == null && version != null) {
            this.protocolVersion = version;
        }
        return this;
    }

    /**
     * Will set the accepted resource version, if not provided in the
     * {@literal Accept-API-Version} header.
     *
     * @param version The default resource version.
     * @return The accept api version header.
     */
    public AcceptApiVersionHeader withDefaultResourceVersion(Version version) {
        if (resourceVersion == null && version != null) {
            this.resourceVersion = version;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AcceptApiVersionHeader that = (AcceptApiVersionHeader) o;
        return Objects.equals(protocolVersion, that.protocolVersion)
                && Objects.equals(resourceVersion, that.resourceVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolVersion, resourceVersion);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (protocolVersion != null) {
            sb.append(PROTOCOL).append(EQUALS).append(protocolVersion.toString());
        }
        if (protocolVersion != null && resourceVersion != null) {
            sb.append(",");
        }
        if (resourceVersion != null) {
            sb.append(RESOURCE).append(EQUALS).append(resourceVersion.toString());
        }
        return sb.toString();
    }
}
