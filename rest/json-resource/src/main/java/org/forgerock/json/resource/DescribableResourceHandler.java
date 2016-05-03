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
import static org.forgerock.api.models.Resource.*;
import static org.forgerock.api.models.Reference.*;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Services;
import org.forgerock.services.context.ApiContext;
import org.forgerock.services.descriptor.Describable;

/**
 * A resource handler that implements describable for a possibly annotated type. This class should be used by internal
 * CREST {@link RequestHandler}s that are wrapping a type that uses (or may use) annotations to describe its API - for
 * example, the Interface handlers and Annotated handlers that the {@link Resources} class uses.
 */
abstract class DescribableResourceHandler implements Describable<ApiDescription> {

    private final Resource resource;
    private final ApiDescription definitionDescriptions;
    private final String resourceId;

    private static final String SERVICES_REFERENCE = "#/services/%s\"";

    DescribableResourceHandler(Class<?> type, Resource.AnnotatedTypeVariant varient) {
        // This ApiDescription can have a dummy ID and version because we are never going to expose it - it is used to
        // collect top-level definitions that are referenced in the resource. Any referenced definitions and errors
        // are added to the ApiDescription that has a proper ID and version (once they are known) in the api method
        // below.
        this.definitionDescriptions = apiDescription().id("fake:id").version("0.0").build();
        this.resource = Resource.fromAnnotatedType(type, varient, definitionDescriptions);
        org.forgerock.api.annotations.RequestHandler annotation =
                type.getAnnotation(org.forgerock.api.annotations.RequestHandler.class);
        this.resourceId = annotation != null ? annotation.id() : null;
    }

    @Override
    public final ApiDescription api(ApiContext<ApiDescription> apiContext) {
        ApiDescription.Builder builder = ApiDescription.apiDescription()
                .definitions(definitionDescriptions.getDefinitions())
                .errors(definitionDescriptions.getErrors())
                .id(apiContext.getApiId());

                Resource resource;
                if (resourceId != null) {
                    builder.services(Services.services().put(resourceId, this.resource).build());
                    resource = resource().reference(reference().value(String.format(SERVICES_REFERENCE, resourceId)).build()).build();
                } else {
                    resource = this.resource;
                }

                return builder.paths(paths().put("", versionedPath().put(UNVERSIONED, resource).build()).build()).build();
    }
}
