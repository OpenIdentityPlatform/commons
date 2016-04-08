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

package org.forgerock.api.markup;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Error;
import org.forgerock.api.models.Errors;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ApiDocGeneratorTest {

    private Path outputDirPath;

    @BeforeClass
    public void beforeClass() throws IOException {
        outputDirPath = Files.createTempDirectory(ApiDocGeneratorTest.class.getSimpleName());
    }

    @AfterClass
    public void afterClass() throws IOException {
        // delete temp dir
        if (outputDirPath != null) {
            Files.walkFileTree(outputDirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Test
    public void testExecute() throws Exception {
        final ApiDescription apiDescription = createApiDescription(false);
        final ApiDocGenerator apiDocGenerator = new ApiDocGenerator(outputDirPath.resolve("testExecute"));
        apiDocGenerator.execute(apiDescription);
    }

    @Test
    public void testExecuteWithVersionedPaths() throws Exception {
        final ApiDescription apiDescription = createApiDescription(true);
        final ApiDocGenerator apiDocGenerator = new ApiDocGenerator(
                outputDirPath.resolve("testExecuteWithVersionedPaths"));
        apiDocGenerator.execute(apiDescription);
    }

    private static ApiDescription createApiDescription(final boolean versioned) {
        final Schema schema = Schema.schema()
                .schema(json(object()))
                .build();

        final String[] supportedLocales = new String[]{"en"};
        final Error notFoundError = Error.error()
                .code(404)
                .description("Custom not-found error.")
                .build();
        final Parameter parameter1 = Parameter.parameter()
                .name("param1")
                .description("Description for param 1")
                .type("string")
                .source(ParameterSource.ADDITIONAL)
                .defaultValue("default")
                .required(true)
                .build();
        final Parameter parameter2 = Parameter.parameter()
                .name("param2")
                .description("Description for param 2")
                .type("string")
                .source(ParameterSource.ADDITIONAL)
                .enumValues("enum1", "enum2")
                .enumTitles("first enum", "second enum")
                .build();

        final Read read = Read.read()
                .description("Default description for read.")
                .supportedLocales(supportedLocales)
                .stability(Stability.STABLE)
                .error(notFoundError)
                .parameter(parameter1)
                .parameter(parameter2)
                .build();

        final Query query = Query.query()
                .type(QueryType.EXPRESSION)
                .description("Default description for query.")
                .pagingMode(PagingMode.COOKIE, PagingMode.OFFSET)
                .countPolicy(CountPolicy.EXACT, CountPolicy.ESTIMATE)
                .queryableFields("field1", "field2")
                .supportedSortKeys("key1", "key2")
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .description("Default description for action1.")
                .response(schema)
                .build();
        final Action action2 = Action.action()
                .name("action2")
                .description("Default description for action2.")
                .response(schema)
                .build();

        final Resource resourceV1 = Resource.resource()
                .description("Default description for resourceV1.")
                .resourceSchema(schema)
                .action(action1)
                .read(read)
                .query(query)
                .build();
        final Resource resourceV2 = Resource.resource()
                .description("Default description for resourceV2.")
                .resourceSchema(schema)
                .action(action1)
                .action(action2)
                .read(read)
                .query(query)
                .build();

        final Definitions definitions = Definitions.definitions()
                .put("def", schema)
                .build();

        final Errors errors = Errors.errors()
                .put("notFound", Error.error().code(404).description("Not Found").build())
                .put("internalServerError", Error.error().code(500).description("Internal Server Error").build())
                .build();

        if (versioned) {
            final VersionedPath versionedPath = VersionedPath.versionedPath()
                    .put("1.0", resourceV1)
                    .put("2.0", resourceV2)
                    .build();

            final Paths<VersionedPath> paths = Paths.paths(VersionedPath.class)
                    .put("/testPath", versionedPath)
                    .build();
            return ApiDescription.apiDescriptionWithVersionedPaths()
                    .id("frapi:test")
                    .description("My Description")
                    .definitions(definitions)
                    .paths(paths)
                    .errors(errors)
                    .build();
        } else {
            final Paths<Resource> paths = Paths.paths(Resource.class)
                    .put("/testPath", resourceV1)
                    .build();
            return ApiDescription.apiDescription()
                    .id("frapi:test")
                    .description("My Description")
                    .definitions(definitions)
                    .paths(paths)
                    .errors(errors)
                    .build();
        }

    }

}
