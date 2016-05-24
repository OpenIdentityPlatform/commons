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

package org.forgerock.json.resource.descriptor.examples.provider.version2;

import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.descriptor.examples.model.User;
import org.forgerock.json.resource.descriptor.examples.provider.version1.UserCollectionProviderV1;

/**
 * Example collection provider class with API descriptor annotations.
 */
@CollectionProvider(details = @Handler(
        id = "users:2.0",
        title = "Users",
        description = "Users example service version 2.0 has the same features as the 1.0 but the subresource "
                + "is pointing to the devices service version 2.0",
        resourceSchema = @Schema(fromType = User.class),
        mvccSupported = true),
        pathParam = @Parameter(name = "userId", type = "string", description = "The user ID from the path"))
public class UserCollectionProviderV2 extends UserCollectionProviderV1 {

    /**
     * Default constructor.
     * @param memoryBackend The resource collection provider base
     * @param deviceProvider The user device provider
     */
    public UserCollectionProviderV2(MemoryBackend memoryBackend, DeviceCollectionProviderV2 deviceProvider) {
        super(memoryBackend, deviceProvider);
    }

}
