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

import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.json.resource.RoutingMode.EQUALS;
import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;
import static org.forgerock.util.Reject.ifFalse;

/**
 * A handler for the routes to different versions of the same resource.
 *
 * @see Router
 * @see VersionRouter
 * @since 2.4.0
 */
public final class VersionHandler {

    private final VersionRouter router;
    private final RoutingMode mode;
    private final String uriTemplate;
    private final VersionRouter.VersionRouterImpl versionRouter;
    private RequestHandlerType requestHandlerType;
    private volatile Route route;

    /**
     * Constructs a new VersionHandler.
     *
     * @param router An instance of the parent router.
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param versionRouter A new instance of the version router.
     * @param uriTemplate The URI template for the resource.
     */
    VersionHandler(VersionRouter router, RoutingMode mode, VersionRouter.VersionRouterImpl versionRouter, String uriTemplate) {
        this.router = router;
        this.mode = mode;
        this.versionRouter = versionRouter;
        this.uriTemplate = uriTemplate;
    }

    /**
     * Adds a new version of a resource which will be handled by the provided collection resource provider. New
     * versions may be added while the router is processing requests.
     *
     * @param version The version of the resource.
     * @param provider The collection resource provider to which matching requests will be routed.
     * @return This versioned route.
     */
    public VersionHandler addVersion(String version, CollectionResourceProvider provider) {
        return addVersion(RequestHandlerType.COLLECTION, STARTS_WITH, version, newCollection(provider));
    }

    /**
     * Adds a new version of a resource which will be handled by the provided singleton resource provider. New
     * versions may be added while the router is processing requests.
     *
     * @param version The version of the resource.
     * @param provider The singleton resource provider to which matching requests will be routed.
     * @return This versioned route.
     */
    public VersionHandler addVersion(String version, SingletonResourceProvider provider) {
        return addVersion(RequestHandlerType.SINGLETON, EQUALS, version, newSingleton(provider));
    }

    /**
     * Adds a new version of a resource which will be handled by the provided request handler. New versions may be
     * added while the router is processing requests.
     *
     * @param version The version of the resource.
     * @param handler The request handler to which matching requests will be routed.
     * @return This versioned route.
     */
    public VersionHandler addVersion(String version, RequestHandler handler) {
        Reject.ifNull(mode, "Routing mode is not set. Incorrect use of VersionRouter#addRoute(String), "
                + "use VersionRouter#addRoute(RoutingMode, String) instead and specify the RoutingMode to use with the "
                + "RequestHandler.");
        return addVersion(RequestHandlerType.GENERIC, mode, version, handler);
    }

    private VersionHandler addVersion(RequestHandlerType type, RoutingMode mode, String version,
            RequestHandler handler) {
        versionRouter.addVersion(version, handler);
        addRoute(type, mode);
        return this;
    }

    /**
     * <P>If the route to the resource URI has not been added, this method will add it. This is only ever done once for
     * the very first call.</p>
     *
     * <p>Subsequent calls will be validated to ensure that the resource handler has not changed type, i.e. from a
     * CollectionResourceProvider to a SingletonResourceProvider.</p>
     *
     * @param type The resource handler type.
     * @param mode Indicates how the URI template should be matched against resource names.
     */
    private void addRoute(RequestHandlerType type, RoutingMode mode) {
        validateRequestHandlerType(type);
        if (route == null) {
            route = router.addRoute(mode, uriTemplate, versionRouter);
        }
    }

    private void validateRequestHandlerType(RequestHandlerType type) {
        if (requestHandlerType != null) {
            ifFalse(type.equals(requestHandlerType), "Incompatible request handler types, " + requestHandlerType
                    + " and " + type);
        } else {
            requestHandlerType = type;
        }
    }

    private static enum RequestHandlerType {
        COLLECTION,
        SINGLETON,
        GENERIC
    }
}
