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

package org.forgerock.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.api.models.ApiError.apiError;
import static org.forgerock.api.models.Definitions.definitions;
import static org.forgerock.api.models.Errors.errors;
import static org.forgerock.api.models.Reference.reference;
import static org.forgerock.api.models.Resource.resource;
import static org.forgerock.api.models.Schema.schema;
import static org.forgerock.api.models.Services.services;
import static org.forgerock.json.JsonValue.*;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.testng.annotations.Test;

public class ReferenceResolverTest {

    private static final String EXT_API_ID = "frapi:test:externalApi";
    private static final String LOCAL_API_ID = "frapi:test:localApi";

    @Test
    public void testDefinitionsChain() {
        final ApiDescription externalDescription = apiDescription()
                .id(EXT_API_ID)
                .version("1.0")
                .definitions(definitions()
                        .put("externalDefinition", schema()
                                .reference(reference()
                                        .value(LOCAL_API_ID + "#/definitions/three")
                                        .build())
                                .build())
                        .build())
                .build();

        final ApiDescription apiDescription = apiDescription()
                .id(LOCAL_API_ID)
                .version("1.0")
                .definitions(definitions()
                        .put("one", schema()
                                .reference(reference()
                                        .value("#/definitions/two")
                                        .build())
                                .build())
                        .put("two", schema()
                                .reference(reference()
                                        .value(EXT_API_ID + "#/definitions/externalDefinition")
                                        .build())
                                .build())
                        .put("three", schema()
                                .schema(json(object(field("title", "three"))))
                                .build())
                        .build())
                .build();

        final ReferenceResolver resolver = new ReferenceResolver(apiDescription)
                .register(externalDescription);

        // one --> two --> externalDefinition --> three
        final Schema schema = resolver.getDefinition(reference()
                .value("#/definitions/one")
                .build());
        assertThat(schema.getSchema().get("title").asString()).isEqualTo("three");

        // bad reference
        assertThat(resolver.getDefinition(reference().value("invalidRef").build())).isNull();
        assertThat(resolver.getDefinition(reference().value("#/definitions/doesNotExist").build())).isNull();
    }

    @Test
    public void testErrorsChain() {
        final ApiDescription externalDescription = apiDescription()
                .id(EXT_API_ID)
                .version("1.0")
                .errors(errors()
                        .put("externalError", apiError()
                                .reference(reference()
                                        .value(LOCAL_API_ID + "#/errors/three")
                                        .build())
                                .build())
                        .build())
                .build();

        final ApiDescription apiDescription = apiDescription()
                .id(LOCAL_API_ID)
                .version("1.0")
                .errors(errors()
                        .put("one", apiError()
                                .reference(reference()
                                        .value("#/errors/two")
                                        .build())
                                .build())
                        .put("two", apiError()
                                .reference(reference()
                                        .value(EXT_API_ID + "#/errors/externalError")
                                        .build())
                                .build())
                        .put("three", apiError()
                                .code(404)
                                .description("Not Found")
                                .build())
                        .build())
                .build();

        final ReferenceResolver resolver = new ReferenceResolver(apiDescription)
                .register(externalDescription);

        // one --> two --> externalDefinition --> three
        final ApiError error = resolver.getError(reference()
                .value("#/errors/one")
                .build());
        assertThat(error.getCode()).isEqualTo(404);

        // bad reference
        assertThat(resolver.getError(reference().value("invalidRef").build())).isNull();
        assertThat(resolver.getError(reference().value("#/errors/doesNotExist").build())).isNull();
    }

    @Test
    public void testServicesChain() {
        final ApiDescription externalDescription = apiDescription()
                .id(EXT_API_ID)
                .version("1.0")
                .services(services()
                        .put("externalDefinition", resource()
                                .reference(reference()
                                        .value(LOCAL_API_ID + "#/services/three")
                                        .build())
                                .build())
                        .build())
                .build();

        final ApiDescription apiDescription = apiDescription()
                .id(LOCAL_API_ID)
                .version("1.0")
                .services(services()
                        .put("one", resource()
                                .reference(reference()
                                        .value("#/services/two")
                                        .build())
                                .build())
                        .put("two", resource()
                                .reference(reference()
                                        .value(EXT_API_ID + "#/services/externalDefinition")
                                        .build())
                                .build())
                        .put("three", resource()
                                .mvccSupported(false)
                                .action(action()
                                        .name("myAction")
                                        .build())
                                .build())
                        .build())
                .build();

        final ReferenceResolver resolver = new ReferenceResolver(apiDescription)
                .register(externalDescription);

        // one --> two --> externalDefinition --> three
        final Resource resource = resolver.getService(reference()
                .value("#/services/one")
                .build());
        assertThat(resource.getActions()[0].getName()).isEqualTo("myAction");

        // bad reference
        assertThat(resolver.getService(reference().value("invalidRef").build())).isNull();
        assertThat(resolver.getService(reference().value("#/services/doesNotExist").build())).isNull();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testDefinitionsEndlessLoop() {
        final ApiDescription apiDescription = apiDescription()
                .id(LOCAL_API_ID)
                .version("1.0")
                .definitions(definitions()
                        .put("one", schema()
                                .reference(reference()
                                        .value("#/definitions/two")
                                        .build())
                                .build())
                        .put("two", schema()
                                .reference(reference()
                                        .value("#/definitions/one")
                                        .build())
                                .build())
                        .build())
                .build();

        final ReferenceResolver resolver = new ReferenceResolver(apiDescription);

        // one --> two --> one
        final Schema schema = resolver.getDefinition(reference()
                .value("#/definitions/one")
                .build());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testErrorsEndlessLoop() {
        final ApiDescription apiDescription = apiDescription()
                .id(LOCAL_API_ID)
                .version("1.0")
                .errors(errors()
                        .put("one", apiError()
                                .reference(reference()
                                        .value("#/errors/two")
                                        .build())
                                .build())
                        .put("two", apiError()
                                .reference(reference()
                                        .value("#/errors/one")
                                        .build())
                                .build())
                        .build())
                .build();

        final ReferenceResolver resolver = new ReferenceResolver(apiDescription);

        // // one --> two --> one
        final ApiError error = resolver.getError(reference()
                .value("#/errors/one")
                .build());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testServicesEndlessLoop() {
        final ApiDescription apiDescription = apiDescription()
                .id(LOCAL_API_ID)
                .version("1.0")
                .services(services()
                        .put("one", resource()
                                .reference(reference()
                                        .value("#/services/two")
                                        .build())
                                .build())
                        .put("two", resource()
                                .reference(reference()
                                        .value("#/services/one")
                                        .build())
                                .build())
                        .build())
                .build();

        final ReferenceResolver resolver = new ReferenceResolver(apiDescription);

        // // one --> two --> one
        final Resource resource = resolver.getService(reference()
                .value("#/services/one")
                .build());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testUniqueApiIdentifier() {
        final ReferenceResolver resolver = new ReferenceResolver(
                apiDescription()
                        .id(LOCAL_API_ID)
                        .version("1.0")
                        .build());

        // duplicate LOCAL_API_ID will trigger exception
        resolver.registerAll(
                apiDescription()
                        .id(EXT_API_ID)
                        .version("1.0")
                        .build(),
                apiDescription()
                        .id(LOCAL_API_ID)
                        .version("1.0")
                        .build());
    }
}
