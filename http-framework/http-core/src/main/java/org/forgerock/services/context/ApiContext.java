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

package org.forgerock.services.context;

import java.util.List;

import org.forgerock.http.routing.Version;

/**
 * A context class for API Description. The context will provide the current API ID for the path being
 * described, can mutate existing descriptor objects to amend paths and versions, and can merge a list
 * of descriptors into one descriptor.
 *
 * @param <T> The type of descriptor object that the context supports.
 */
public abstract class ApiContext<T> extends AbstractContext implements Context {

    private final String idFragment;
    private final String apiVersion;

    /**
     * Create a new API Description Context.
     * @param parent The parent context.
     * @param idFragment The ID fragment for this context level.
     * @param apiVersion The version to apply to descriptors.
     */
    public ApiContext(Context parent, String idFragment, String apiVersion) {
        super(parent, "ApiContext");
        this.idFragment = idFragment;
        this.apiVersion = apiVersion;
    }

    /**
     * Mutate the provided descriptor to add the specified path and new id.
     * @param descriptor The descriptor to be mutated.
     * @param apiId The new descriptor's API ID.
     * @param path The path to add to the descriptor.
     * @return The new descriptor.
     */
    public abstract T withPath(T descriptor, String apiId, String path);

    /**
     * Mutate the provided descriptor to add the specified version and new id.
     * @param descriptor The descriptor to be mutated.
     * @param apiId The new descriptor's API ID.
     * @param version The version to apply to the resource.
     * @return The new descriptor.
     */
    public abstract T withVersion(T descriptor, String apiId, Version version);

    /**
     * Merge the provided descriptors into a single descriptor with the specified ID.
     * @param apiId The new descriptor's ID.
     * @param descriptors The descriptors to be merged.
     * @return The merged descriptor.
     */
    public abstract T merge(String apiId, List<T> descriptors);

    /**
     * Get the API ID for this context.
     * @return The ID.
     */
    public String getApiId() {
        if (getParent().containsContext(ApiContext.class)) {
            return getParent().asContext(ApiContext.class).getId() + idFragment;
        }
        return idFragment;
    }

    /**
     * Create a child context with the same type, but with the extra ID fragment.
     * @param idFragment The fragment of the ID for this context.
     * @return The new context.
     */
    public abstract ApiContext<T> newChildContext(String idFragment);

    /**
     * Get the version of the API being described.
     * @return The version.
     */
    public String getApiVersion() {
        return apiVersion;
    }
}
