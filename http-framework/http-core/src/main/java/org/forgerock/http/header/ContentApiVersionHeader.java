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

import static java.util.Collections.*;
import static org.forgerock.http.header.HeaderUtil.*;

import java.util.List;

import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Message;
import org.forgerock.http.routing.Version;
import org.forgerock.util.Pair;
import org.forgerock.util.Reject;

/**
 * Processes the <strong>{@code Content-API-Version}</strong> message header.
 * Represents the protocol and resource versions of the returned content.
 */
public final class ContentApiVersionHeader extends Header {

    /**
     * Constructs a new header, initialized from the specified message.
     *
     * @param message The message to initialize the header from.
     * @return The parsed header.
     */
    public static ContentApiVersionHeader valueOf(Message message) {
        String headerValue = parseSingleValuedHeader(message, NAME);
        return valueOf(headerValue);
    }

    /**
     * Constructs a new header, initialized from the specified string.
     *
     * @param headerValue The value to initialize the header from.
     * @return The parsed header.
     */
    public static ContentApiVersionHeader valueOf(String headerValue) {
        Pair<Version, Version> parsedValue = AcceptApiVersionHeader.parse(headerValue);
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
    public List<String> getValues() {
        if (protocolVersion == null) {
            return singletonList(String.format(RESOURCE + "=%s", resourceVersion));
        } else {
            return singletonList(String.format(PROTOCOL + "=%s," + RESOURCE + "=%s", protocolVersion, resourceVersion));
        }
    }

    static class Factory extends AbstractSingleValuedHeaderFactory<ContentApiVersionHeader> {

        @Override
        public ContentApiVersionHeader parse(String value) {
            return valueOf(value);
        }
    }
}
