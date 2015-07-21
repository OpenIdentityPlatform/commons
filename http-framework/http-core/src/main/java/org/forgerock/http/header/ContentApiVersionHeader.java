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

import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Message;
import org.forgerock.http.routing.Version;
import org.forgerock.util.Pair;
import org.forgerock.util.Reject;

/**
 * Processes the <strong>{@code Content-API-Version}</strong> message header.
 * Represents the protocol and resource versions of the returned content.
 */
public final class ContentApiVersionHeader implements Header {

    /**
     * Constructs a new header, initialized from the specified message.
     *
     * @param message The message to initialize the header from.
     * @return The parsed header.
     */
    public static ContentApiVersionHeader valueOf(Message message) {
        Pair<Version, Version> parsedValue = AcceptApiVersionHeader.parse(parseSingleValuedHeader(message, NAME));
        return new ContentApiVersionHeader(parsedValue.getFirst(), parsedValue.getSecond());
    }

    /** The name of this header. */
    public static final String NAME = "Content-API-Version";
    private static final String PROTOCOL = "protocol";
    private static final String RESOURCE = "resource";

    private final Version protocolVersion;
    private final Version resourceVersion;

    /**
     * Constructs a new header, initialized with the specified protocol and
     * resource versions.
     *
     * @param protocolVersion The protocol version of the content of the
     *                        returned content.
     * @param resourceVersion The resource version of the returned content.
     */
    public ContentApiVersionHeader(Version protocolVersion, Version resourceVersion) {
        Reject.ifNull(resourceVersion);
        this.protocolVersion = protocolVersion;
        this.resourceVersion = resourceVersion;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Gets the protocol version of the content of the returned content.
     *
     * @return The protocol version of the content of the returned content.
     */
    public Version getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Gets the resource version of the returned content.
     *
     * @return The resource version of the returned content.
     */
    public Version getResourceVersion() {
        return resourceVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContentApiVersionHeader that = (ContentApiVersionHeader) o;
        return !(protocolVersion != null ? !protocolVersion.equals(that.protocolVersion) : that.protocolVersion != null)
                && resourceVersion.equals(that.resourceVersion);
    }

    @Override
    public int hashCode() {
        int result = protocolVersion != null ? protocolVersion.hashCode() : 0;
        result = 31 * result + resourceVersion.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (protocolVersion == null) {
            return String.format(RESOURCE + "=%s",
                    resourceVersion.toString());
        } else {
            return String.format(PROTOCOL + "=%s," + RESOURCE + "=%s",
                    protocolVersion.toString(),
                    resourceVersion.toString());
        }
    }
}
