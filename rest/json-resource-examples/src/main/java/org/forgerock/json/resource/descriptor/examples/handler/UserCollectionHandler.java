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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.resource.descriptor.examples.handler;

import static org.forgerock.http.routing.Version.*;

import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.descriptor.examples.provider.version1.DeviceCollectionProviderV1;
import org.forgerock.json.resource.descriptor.examples.provider.version1.UserCollectionProviderV1;
import org.forgerock.json.resource.descriptor.examples.provider.version2.DeviceCollectionProviderV2;
import org.forgerock.json.resource.descriptor.examples.provider.version2.UserCollectionProviderV2;

/**
 * Default in-memory handler.
 */
public final class UserCollectionHandler {

    private static MemoryBackend userBackend = new MemoryBackend();
    private static MemoryBackend deviceBackend = new MemoryBackend();
    private static DeviceCollectionProviderV1 deviceCollectionProviderV1 =
            new DeviceCollectionProviderV1(deviceBackend);
    private static DeviceCollectionProviderV2 deviceCollectionProviderV2 =
            new DeviceCollectionProviderV2(deviceBackend);
    private static RequestHandler userCollProvV1 =
            Resources.newHandler(new UserCollectionProviderV1(userBackend, deviceCollectionProviderV1));
    private static RequestHandler userCollProvV2 =
            Resources.newHandler(new UserCollectionProviderV2(userBackend, deviceCollectionProviderV2));

    private UserCollectionHandler() {
    }

    /**
     * Creates the route to the different user provider versions.
     * @return The User handler with the routes.
     */
    public static Router getUsersRouter() {
        Router usersRouter = new Router();
        usersRouter.addRoute(version(1), userCollProvV1);
        usersRouter.addRoute(version(2), userCollProvV2);
        return usersRouter;
    }

    /**
     * Creates the route to the different admin provier version (At the moment it supports only version 1.0).
     * @return The Admin handler with the routes.
     */
    public static Router getAdminsRouter() {
        Router adminsRouter = new Router();
        adminsRouter.addRoute(version(1), userCollProvV1);
        return adminsRouter;
    }

}


