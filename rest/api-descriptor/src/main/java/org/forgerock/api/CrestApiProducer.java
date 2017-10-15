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
import static org.forgerock.api.models.Services.services;
import static org.forgerock.api.models.VersionedPath.*;

import java.util.List;
import java.util.Set;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Errors;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Services;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.http.routing.Version;
import org.forgerock.http.ApiProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ApiProducer} implementation for CREST resources, that provides {@code ApiDescription} descriptors.
 */
public class CrestApiProducer implements ApiProducer<ApiDescription> {

    private final String version;
    private final String id;
    private final LocalizableString description;
    private static final Logger LOGGER = LoggerFactory.getLogger(CrestApiProducer.class);

    /**
     * Construct a new producer.
     * @param id The API ID fragment for this producer.
     * @param apiVersion The version of the API being described.
     * @param description The API description.
     */
    public CrestApiProducer(String id, String apiVersion, LocalizableString description) {
        this.id = id;
        this.version = apiVersion;
        this.description = description;
    }

    /**
     * Construct a new producer.
     * @param id The API ID fragment for this producer.
     * @param apiVersion The version of the API being described.
     */
    public CrestApiProducer(String id, String apiVersion) {
        this(id, apiVersion, null);
    }

    @Override
    public ApiDescription withPath(ApiDescription api, String parentPath) {
        Paths.Builder paths = paths();
        Set<String> names = api.getPaths().getNames();
        for (String subpath : names) {
            paths.put(subpath.equals("") ? parentPath : parentPath + "/" + subpath,
                    api.getPaths().get(subpath));
        }
        return createApi(api.getDefinitions(), api.getErrors(), api.getServices(), paths.build());
    }

    @Override
    public ApiDescription withVersion(ApiDescription api, Version version) {
        Paths.Builder paths = paths();
        Set<String> names = api.getPaths().getNames();
        for (String path : names) {
            VersionedPath versionedPath = api.getPaths().get(path);
            if (singleton(UNVERSIONED).equals(versionedPath.getVersions())) {
                paths.put(path, versionedPath().put(version, versionedPath.get(UNVERSIONED)).build());
            } else {
                throw new IllegalStateException("Trying to version something already versioned: " + versionedPath);
            }
        }
        return createApi(api.getDefinitions(), api.getErrors(), api.getServices(), paths.build());
    }

    @Override
    public ApiDescription merge(List<ApiDescription> descriptions) {
        Paths.Builder paths = paths();
        Definitions.Builder definitions = definitions();
        Errors.Builder errors = errors();
        Services.Builder services = services();
        for (ApiDescription description : descriptions) {
            if (description != null) {
                try {
                    if (description.getDefinitions() != null) {
                        for (String definition : description.getDefinitions().getNames()) {
                            definitions.put(definition, description.getDefinitions().get(definition));
                        }
                    }
                    if (description.getErrors() != null) {
                        for (String error : description.getErrors().getNames()) {
                            errors.put(error, description.getErrors().get(error));
                        }
                    }
                    if (description.getServices() != null) {
                        for (String service : description.getServices().getNames()) {
                            services.put(service, description.getServices().get(service));
                        }
                    }
                    if (description.getPaths() != null) {
                        for (String path : description.getPaths().getNames()) {
                            paths.merge(path, description.getPaths().get(path));
                        }
                    }
                } catch (RuntimeException re) {
                    LOGGER.error(re.getMessage(), re.fillInStackTrace());
                    throw re;
                }
            }
        }
        return createApi(definitions.build(), errors.build(), services.build(), paths.build());
    }

    @Override
    public ApiDescription addApiInfo(ApiDescription api) {
        return createApi(api.getDefinitions(), api.getErrors(), api.getServices(), api.getPaths());
    }

    private ApiDescription createApi(Definitions definitions, Errors errors, Services services, Paths paths) {
        return apiDescription()
                .definitions(definitions)
                .errors(errors)
                .services(services)
                .paths(paths)
                .id(this.id)
                .version(this.version)
                .description(this.description)
                .build();
    }

    @Override
    public ApiProducer<ApiDescription> newChildProducer(String idFragment) {
        return new CrestApiProducer(id + idFragment, version);
    }
}
