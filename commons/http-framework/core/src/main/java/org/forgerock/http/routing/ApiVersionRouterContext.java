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

package org.forgerock.http.routing;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValueFunctions.enumConstant;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

/**
 * A {@link Context} which is created when a request is and has been routed
 * based on resource API version. The context includes:
 * <ul>
 *     <li>the default version behaviour, if the request does not contain a
 *     resource API version</li>
 *     <li>whether a warning is issued to the client, if the request does not
 *     contain a resource API version</li>
 *     <li>the API version of the framework that was used to handle the request</li>
 *     <li>the API version of the resource that was routed to</li>
 * </ul>
 */
public class ApiVersionRouterContext extends AbstractContext {

    private static final String DEFAULT_VERSION_BEHAVIOUR = "defaultVersionBehaviour";
    private static final String WARNING_ENABLED = "warningEnabled";

    private Version protocolVersion;
    private Version resourceVersion;

    /**
     * Creates a new resource API version routing context having the provided
     * parent, default versioning behaviour and whether warnings will be
     * issued.
     *
     * @param parent The parent context.
     * @param defaultVersionBehaviour The default version behaviour.
     * @param warningEnabled Whether warnings will be issued to the client.
     */
    public ApiVersionRouterContext(Context parent, DefaultVersionBehaviour defaultVersionBehaviour,
            boolean warningEnabled) {
        this(parent);
        if (defaultVersionBehaviour != null) {
            data.put(DEFAULT_VERSION_BEHAVIOUR, defaultVersionBehaviour.toString());
        }
        data.put(WARNING_ENABLED, warningEnabled);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext The JSON representation from which this context's
     *                     attributes should be parsed.
     * @param classLoader The {@code ClassLoader} which can properly resolve
     *                    the persisted class-name.
     */
    public ApiVersionRouterContext(JsonValue savedContext, ClassLoader classLoader) {
        super(savedContext, classLoader);
        if (data.isDefined("protocolVersion")) {
            this.protocolVersion = version(data.get("protocolVersion").asString());
        }
        if (data.isDefined("resourceVersion")) {
            this.protocolVersion = version(data.get("resourceVersion").asString());
        }
    }

    /**
     * Creates a new resource API version routing context having the provided
     * parent.
     *
     * @param parent The parent context.
     */
    ApiVersionRouterContext(Context parent) {
        super(parent, "apiVersionRouter");
    }

    /**
     * Gets the default version behaviour if the request does not contain a
     * resource API version.
     *
     * @return The default version behaviour.
     */
    public DefaultVersionBehaviour getDefaultVersionBehaviour() {
        if (data.isDefined(DEFAULT_VERSION_BEHAVIOUR) && data.get(DEFAULT_VERSION_BEHAVIOUR).isNotNull()) {
            return data.get(DEFAULT_VERSION_BEHAVIOUR).as(enumConstant(DefaultVersionBehaviour.class));
        } else {
            return null;
        }
    }

    /**
     * Gets whether a warning should be issued to the calling client if the
     * request does not contain a resource API version.
     *
     * @return {@code true} if warnings should be issued to the client.
     */
    public boolean isWarningEnabled() {
        return data.get(WARNING_ENABLED).defaultTo(true).asBoolean();
    }

    /**
     * Sets the protocol API version of the framework used to handle the request.
     *
     * @param protocolVersion The framework protocol API version.
     */
    public void setProtocolVersion(Version protocolVersion) {
        this.protocolVersion = protocolVersion;
        data.put("protocolVersion", protocolVersion.toString());
    }

    /**
     * Gets the protocol API version of the framework used to handle the request.
     *
     * @return The framework protocol API version.
     */
    public Version getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Sets the API version of the resource that the request was routed to.
     *
     * @param resourceVersion The resource API version.
     */
    void setResourceVersion(Version resourceVersion) {
        this.resourceVersion = resourceVersion;
        data.put("resourceVersion", resourceVersion.toString());
    }

    /**
     * Gets the API version of the resource that the request was routed to.
     *
     * @return The resource API version.
     */
    public Version getResourceVersion() {
        return resourceVersion;
    }
}
