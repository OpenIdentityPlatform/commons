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

package org.forgerock.http;

import java.util.List;

import org.forgerock.http.routing.Version;

/**
 * A producer of API Descriptions. The class will provide the ability to mutate existing descriptor objects in order to
 * amend paths and versions, and can merge a list of descriptors into one descriptor. It will also add generic
 * description information to a descriptor object.
 *
 * @param <D> The type of descriptor object that the producer supports.
 */
public interface ApiProducer<D> {

    /**
     * Mutate the provided descriptor to add the specified path.
     * @param descriptor The descriptor to be mutated.
     * @param parentPath The path to add to the descriptor.
     * @return The new descriptor.
     */
    D withPath(D descriptor, String parentPath);

    /**
     * Mutate the provided descriptor to add the specified version.
     * @param descriptor The descriptor to be mutated.
     * @param version The version to apply to the resource.
     * @return The new descriptor.
     */
    D withVersion(D descriptor, Version version);

    /**
     * Merge the provided descriptors into a single descriptor.
     * @param descriptors The descriptors to be merged.
     * @return The merged descriptor.
     */
    D merge(List<D> descriptors);

    /**
     * Add common API Info to the descriptor.
     * @param descriptor The descriptor.
     * @return The modified descriptor.
     */
    D addApiInfo(D descriptor);

    /**
     * Create a child producer with the same type, but with the extra ID fragment.
     * @param idFragment The fragment of the ID for this producer.
     * @return The new producer.
     */
    ApiProducer<D> newChildProducer(String idFragment);

}
