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

package org.forgerock.api.models;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.*;

import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.api.ApiValidationException;

public class ApiDescriptionTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    @Test(expectedExceptions = ApiValidationException.class)
    public void testFailedValidationIdMissing() {
        final Error error = Error.error()
                .code(500)
                .description("Unexpected error")
                .build();

        final Errors errors = Errors.errors()
                .put("internalServerError", error)
                .build();

        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .errors(errors)
                .build();
    }

    @Test
    public void testSuccessfulValidationMinimumRequirements() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .version("a version")
                .build();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testValidationMinimumRequirementsMissingId() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .version("a version")
                .build();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testValidationMinimumRequirementsMissingVersion() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .build();
    }

    @Test
    public void testVersionedPaths() throws JsonProcessingException {
        final Schema schema = Schema.schema()
                .schema(json(object()))
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .response(schema)
                .build();
        final Action action2 = Action.action()
                .name("action2")
                .response(schema)
                .build();

        final Resource resourceV1 = Resource.resource()
                .action(action1)
                .build();
        final Resource resourceV2 = Resource.resource()
                .action(action1)
                .action(action2)
                .build();

        final VersionedPath versionedPath = VersionedPath.versionedPath()
                .put(version(1), resourceV1)
                .put(version(2), resourceV2)
                .build();

        final Paths paths = Paths.paths()
                .put("/testPath", versionedPath)
                .build();

        final Definitions definitions = Definitions.definitions()
                .put("def", schema)
                .build();

        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .version("a version")
                .description("My Description")
                .definitions(definitions)
                .paths(paths)
                .build();

        // TODO this JSON output is just for development purposes at the moment
        System.out.println(OBJECT_MAPPER.writeValueAsString(apiDescription));
    }

}
