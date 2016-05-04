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

package org.forgerock.json.resource;

import static org.forgerock.api.models.ApiDescription.*;
import static org.forgerock.api.models.Paths.*;
import static org.forgerock.api.models.VersionedPath.*;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Resource;
import org.forgerock.services.context.ApiContext;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Reject;

/**
 * A resource handler that implements describable for a possibly annotated type. This class should be used by internal
 * CREST {@link RequestHandler}s that are wrapping a type that uses (or may use) annotations to describe its API - for
 * example, the Interface handlers and Annotated handlers that the {@link Resources} class uses.
 */
final class DescribableResourceHandler implements Describable<ApiDescription> {

    private final ApiDescription definitionDescriptions;
    private ApiDescription api;
    private Resource resource;

    DescribableResourceHandler() {
        // This ApiDescription can have a dummy ID and version because we are never going to expose it - it is used to
        // collect top-level definitions that are referenced in the resource. Any referenced definitions and errors
        // are added to the ApiDescription that has a proper ID and version (once they are known) in the api method
        // below.
        this.definitionDescriptions = apiDescription().id("fake:id").version("0.0").build();
    }

    ApiDescription getDefinitionDescriptions() {
        return definitionDescriptions;
    }

    void describes(Resource resource) {
        Reject.rejectStateIfTrue(this.resource != null, "Already described API");
        if (resource != null) {
            this.resource = resource;
        }
    }

    @Override
    public final ApiDescription api(ApiContext<ApiDescription> apiContext) {
        Reject.rejectStateIfTrue(resource == null, "Not yet described API");
        if (api == null) {
            api = ApiDescription.apiDescription()
                    .definitions(definitionDescriptions.getDefinitions())
                    .errors(definitionDescriptions.getErrors())
                    .id(apiContext.getApiId())
                    .version(apiContext.getApiVersion())
                    .paths(paths().put("", versionedPath().put(UNVERSIONED, resource).build()).build())
                    .build();
        }
        return api;
    }
}
