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

package com.forgerock.api.beans;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgerock.api.ApiValidationException;
import org.testng.annotations.Test;

public class ApiDescriptionTest {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    @Test(expectedExceptions = ApiValidationException.class)
    public void testFailedValidation_idMissing() {
        final Map<String, Error> errors = new HashMap<>();
        errors.put("internalServerError", Error.error("Unexpected error", 500).build());

        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .errors(errors)
                .build();
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testFailedValidation_minimumRequirements() {
        final ApiDescription apiDescription = ApiDescription.apiDescription()
                .id("frapi:test")
                .build();
    }

    @Test
    public void testVersionedPaths() throws JsonProcessingException {
        final Schema responseSchema = Schema.schema().build();

        final Action action1 = Action.action().name("action1").response(responseSchema).build();
        final Action action2 = Action.action().name("action2").response(responseSchema).build();

        final Resource resourceV1 = Resource.resource()
                .action(action1)
                .build();

        final Resource resourceV2 = Resource.resource()
                .action(action1)
                .action(action2)
                .build();

        final VersionedPath versionedPath = VersionedPath.versionedPath()
                .put("1.0", resourceV1)
                .put("2.0", resourceV2)
                .build();

        final Map<String, VersionedPath> paths = new HashMap<>();
        paths.put("/testPath", versionedPath);

        final ApiDescription apiDescription = ApiDescription.apiDescriptionWithVersionedPaths()
                .id("frapi:test")
                .paths(paths)
                .build();

        // TODO this JSON output is just for development purposes at the moment
        System.out.println(objectMapper.writeValueAsString(apiDescription));
    }

}
