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
 * Copyright 2012-2014 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.json.fluent.JsonValue;

/**
 * A {@link Context} containing version information about the protocol and resource.
 * A version {@link Context} will be created for each request.
 * <p>
 * For instance the name which identifies the protocol exposed by the {@code json-resource-servlet}
 * module is {@code crest}.
 * <p>
 * Here is an example of the JSON representation of a version context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.AcceptAPIVersionContext",
 *   "parent" : {
 *       ...
 *   },
 *   "protocolName"     : "crest",
 *   "protocolVersion" : "2.0"
 * }
 * </pre>
 */
public final class AcceptAPIVersionContext extends AbstractContext {

    /** a client-friendly name for this context. */
    private static final String CONTEXT_NAME = "version";

    // Persisted attribute names.
    private static final String ATTR_PROTOCOL_NAME = "protocolName";
    private static final String ATTR_PROTOCOL_VERSION = "protocolVersion";
    private static final String ATTR_RESOURCE_VERSION = "resourceVersion";

    private final AcceptAPIVersion acceptVersion;

    /**
     * Creates a new version context having the provided parent and an
     * ID automatically generated using {@code UUID.randomUUID()}.
     *
     * @param parent
     *            The parent context.
     * @param protocolName
     *            The non-null name of the protocol in use
     * @param acceptVersion
     *            The version of the resource
     */
    public AcceptAPIVersionContext(final Context parent, final String protocolName,
                                   final AcceptAPIVersion acceptVersion) {
        super(checkNotNull(parent, "Cannot instantiate AcceptAPIVersionContext with null parent Context"));

        // Cache locally to avoid unnecessary string parsing.
        this.acceptVersion = acceptVersion;
        final Version protocolVersion = acceptVersion.getProtocolVersion();
        final Version resourceVersion = acceptVersion.getResourceVersion();

        data.put(ATTR_PROTOCOL_NAME,
                checkNotNull(protocolName, "Cannot instantiate AcceptAPIVersionContext with null protocolName"));
        data.put(ATTR_PROTOCOL_VERSION,
                checkNotNull(protocolVersion, "Cannot instantiate AcceptAPIVersionContext with null protocolVersion"));

        if (resourceVersion != null) {
            data.put(ATTR_RESOURCE_VERSION, resourceVersion);
        }
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    AcceptAPIVersionContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
        acceptVersion = AcceptAPIVersion
                .newBuilder()
                .withDefaultProtocolVersion(data.get(ATTR_PROTOCOL_VERSION).asString())
                .withDefaultResourceVersion(data.get(ATTR_RESOURCE_VERSION).asString())
                .expectsProtocolVersion()
                .build();
    }

    /**
     * Get this Context's name.
     *
     * @return this object's name
     */
    public String getContextName() {
        return CONTEXT_NAME;
    }

    /**
     * Get the protocol name.
     *
     * @return The protocol name
     */
    public String getProtocolName() {
        return data.get(ATTR_PROTOCOL_NAME).asString();
    }

    /**
     * Get the acceptable protocol version.
     *
     * @return The version of the protocol
     */
    public Version getProtocolVersion() {
        return acceptVersion.getProtocolVersion();
    }

    /**
     * Get the acceptable resource version.
     *
     * @return The resource version or null if not defined
     */
    public Version getResourceVersion() {
        return acceptVersion.getResourceVersion();
    }

}

