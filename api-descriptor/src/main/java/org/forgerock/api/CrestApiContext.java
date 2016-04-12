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

package org.forgerock.api;

import static java.util.Collections.*;
import static org.forgerock.api.models.ApiDescription.*;
import static org.forgerock.api.models.Definitions.*;
import static org.forgerock.api.models.Errors.*;
import static org.forgerock.api.models.Paths.*;
import static org.forgerock.api.models.VersionedPath.*;

import java.util.List;
import java.util.Set;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Errors;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.http.routing.Version;
import org.forgerock.services.context.ApiContext;
import org.forgerock.services.context.Context;

/**
 * An {@code ApiContext} implementation for CREST resources, that provides {@code ApiDescription} descriptors.
 */
public class CrestApiContext extends ApiContext<ApiDescription> {

    /**
     * Construct a new context.
     * @param parent The parent context.
     * @param idFragment The API ID fragment for this context.
     */
    public CrestApiContext(Context parent, String idFragment) {
        super(parent, idFragment);
    }

    @Override
    public ApiDescription withPath(ApiDescription apiDescription, String id, String path) {
        Paths.Builder paths = paths();
        Set<String> names = apiDescription.getPaths().getNames();
        for (String subpath : names) {
            paths.put(subpath.equals("") ? path : path + "/" + subpath, apiDescription.getPaths().get(subpath));
        }
        return apiDescription()
                .definitions(apiDescription.getDefinitions())
                .errors(apiDescription.getErrors())
                .paths(paths.build())
                .id(id)
                .build();
    }

    @Override
    public ApiDescription withVersion(ApiDescription apiDescription, String id, Version version) {
        Paths.Builder paths = paths();
        Set<String> names = apiDescription.getPaths().getNames();
        for (String path : names) {
            VersionedPath versionedPath = apiDescription.getPaths().get(path);
            if (!singleton(UNVERSIONED).equals(versionedPath.getVersions())) {
                paths.put(path, versionedPath().put(version, versionedPath.get(UNVERSIONED)).build());
            } else {
                throw new IllegalStateException("Trying to version something already versioned: " + versionedPath);
            }
        }
        return apiDescription()
                .definitions(apiDescription.getDefinitions())
                .errors(apiDescription.getErrors())
                .paths(paths.build())
                .id(id)
                .build();
    }

    @Override
    public ApiDescription merge(String id, List<ApiDescription> descriptions) {
        Paths.Builder paths = paths();
        Definitions.Builder definitions = definitions();
        Errors.Builder errors = errors();
        for (ApiDescription description : descriptions) {
            for (String definition : description.getDefinitions().getNames()) {
                definitions.put(definition, description.getDefinitions().get(definition));
            }
            for (String error : description.getErrors().getNames()) {
                errors.put(error, description.getErrors().get(error));
            }
            for (String path : description.getPaths().getNames()) {
                paths.merge(path, description.getPaths().get(path));
            }
        }
        return apiDescription()
                .definitions(definitions.build())
                .errors(errors.build())
                .paths(paths.build())
                .id(id)
                .build();
    }

    @Override
    public ApiContext<ApiDescription> newChildContext(String idFragment) {
        return new CrestApiContext(this, idFragment);
    }
}
