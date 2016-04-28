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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Error;
import org.forgerock.api.models.Errors;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ApiDocGeneratorTest {

    private static final String API_DESCRIPTION_PATH = "frapi_test_index_description.adoc";
    private static final String CUSTOM_API_DESCRIPTION = "\n\nCustom API description.\n\n";
    private static final String DEFAULT_API_DESCRIPTION = "Default API description.";

    private Path inputDirPath;
    private Path outputDirPath;

    @BeforeClass
    public void beforeClass() throws IOException {
        final String className = ApiDocGeneratorTest.class.getSimpleName();
        inputDirPath = Files.createTempDirectory(className + "_input_");
        outputDirPath = Files.createTempDirectory(className + "_output_");
    }

    @AfterClass
    public void afterClass() throws IOException {
        // delete temp dirs
        for (final Path path : Arrays.asList(inputDirPath, outputDirPath)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
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
    public void testExecuteWithUnversionedPaths() throws Exception {
        final Path testOutputDirPath = outputDirPath.resolve("testExecute");
        final ApiDescription apiDescription = createApiDescription(false);
        final ApiDocGenerator apiDocGenerator = new ApiDocGenerator(testOutputDirPath);
        apiDocGenerator.execute(apiDescription);

        // check for output-dir for default API description file
        final Path outputApiDescriptionPath = testOutputDirPath.resolve(API_DESCRIPTION_PATH);
        final String outputApiDescription = new String(Files.readAllBytes(outputApiDescriptionPath), UTF_8);
        assertThat(outputApiDescription).contains(DEFAULT_API_DESCRIPTION);
    }

    @Test
    public void testExecuteWithVersionedPaths() throws Exception {
        final Path testOutputDirPath = outputDirPath.resolve("testExecuteWithVersionedPaths");
        final ApiDescription apiDescription = createApiDescription(true);
        final ApiDocGenerator apiDocGenerator = new ApiDocGenerator(outputDirPath.resolve(testOutputDirPath));
        apiDocGenerator.execute(apiDescription);

        // check for output-dir for default API description file
        final Path outputApiDescriptionPath = testOutputDirPath.resolve(API_DESCRIPTION_PATH);
        final String outputApiDescription = new String(Files.readAllBytes(outputApiDescriptionPath), UTF_8);
        assertThat(outputApiDescription).contains(DEFAULT_API_DESCRIPTION);
    }

    @Test
    public void testExecuteWithInputOverrides() throws Exception {
        // create description file in input-dir
        final Path testInputDirPath = inputDirPath.resolve("testExecuteWithInputOverrides");
        Files.createDirectory(testInputDirPath);
        final Path inputApiDescriptionPath = testInputDirPath.resolve(API_DESCRIPTION_PATH);
        Files.createFile(inputApiDescriptionPath);
        Files.write(inputApiDescriptionPath, CUSTOM_API_DESCRIPTION.getBytes(UTF_8));

        // write API descriptor files to output-dir
        final ApiDescription apiDescription = createApiDescription(false);
        final Path testOutputDirPath = outputDirPath.resolve("testExecuteWithInputOverrides");
        final ApiDocGenerator apiDocGenerator = new ApiDocGenerator(testInputDirPath, testOutputDirPath);
        apiDocGenerator.execute(apiDescription);

        // check for input-dir description file in output-dir
        final Path outputApiDescriptionPath = testOutputDirPath.resolve(API_DESCRIPTION_PATH);
        final String outputApiDescription = new String(Files.readAllBytes(outputApiDescriptionPath), UTF_8);
        assertThat(outputApiDescription).isEqualTo(CUSTOM_API_DESCRIPTION);
    }

    private static ApiDescription createApiDescription(final boolean versioned) {
        final Schema schema = Schema.schema()
                .schema(json(object(field("type", "object"))))
                .build();

        final Schema schemaFromRef = Schema.schema()
                .reference(
                        Reference.reference().value("frapi:test#/Definitions/mySchema").build())
                .build();

        final Schema errorDetailSchema = Schema.schema()
                .schema(json(object(
                        field("type", "object"),
                        field("properties", object(
                                field("reason", object(
                                        field("type", "string")
                                ))
                        )))))
                .build();

        final String[] supportedLocales = new String[]{"en"};
        final Error notFoundError = Error.error()
                .code(404)
                .description("Custom not-found error.")
                .schema(errorDetailSchema)
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

        final Create createIdFromServer = Create.create()
                .description("Default description for create (ID from server).")
                .mode(CreateMode.ID_FROM_SERVER)
                .build();

        final Create createIdFromClient = Create.create()
                .description("Default description for create (ID from client).")
                .mode(CreateMode.ID_FROM_CLIENT)
                .singleton(true)
                .build();

        final Read read = Read.read()
                .description("Default description for read.")
                .supportedLocales(supportedLocales)
                .stability(Stability.STABLE)
                .error(notFoundError)
                .parameter(parameter1)
                .parameter(parameter2)
                .build();

        final Update update = Update.update()
                .description("Default description for update.")
                .build();

        final Delete delete = Delete.delete()
                .description("Default description for delete.")
                .build();

        final Patch patch = Patch.patch()
                .description("Default description for patch.")
                .operations(PatchOperation.ADD, PatchOperation.COPY)
                .build();

        final Query expressionQuery = Query.query()
                .type(QueryType.EXPRESSION)
                .description("Default description for expression query.")
                .pagingMode(PagingMode.COOKIE, PagingMode.OFFSET)
                .countPolicy(CountPolicy.EXACT, CountPolicy.ESTIMATE)
                .supportedSortKeys("key1", "key2")
                .build();

        final Query filterQuery = Query.query()
                .type(QueryType.FILTER)
                .description("Default description for filter query.")
                .pagingMode(PagingMode.COOKIE, PagingMode.OFFSET)
                .countPolicy(CountPolicy.EXACT, CountPolicy.ESTIMATE)
                .queryableFields("field1", "field2")
                .supportedSortKeys("key1", "key2")
                .build();

        final Query id1Query = Query.query()
                .type(QueryType.ID)
                .queryId("id1")
                .description("Default description for id1 query.")
                .build();

        final Query id2Query = Query.query()
                .type(QueryType.ID)
                .queryId("id2")
                .description("Default description for id2 query.")
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .description("Default description for action1.")
                .request(schema)
                .response(schemaFromRef)
                .build();
        final Action action2 = Action.action()
                .name("action2")
                .description("Default description for action2.")
                .response(schema)
                .build();

        final Resource resourceV1 = Resource.resource()
                .description("Default description for resourceV1.")
                .resourceSchema(schema)
                .create(createIdFromServer)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .action(action1)
                .queries(asList(expressionQuery, filterQuery, id1Query, id2Query))
                .mvccSupported(true)
                .build();
        final Resource resourceV2 = Resource.resource()
                .description("Default description for resourceV2.")
                .resourceSchema(schema)
                .create(createIdFromClient)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .action(action1)
                .action(action2)
                .queries(asList(expressionQuery, filterQuery, id1Query, id2Query))
                .mvccSupported(false)
                .build();

        final Definitions definitions = Definitions.definitions()
                .put("mySchema", schema)
                .build();

        final Errors errors = Errors.errors()
                .put("notFound", Error.error().code(404).description("Not Found").build())
                .put("internalServerError", Error.error().code(500).description("Internal Server Error").build())
                .build();

        final VersionedPath versionedPath;
        if (versioned) {
            versionedPath = VersionedPath.versionedPath()
                    .put(version(1), resourceV1)
                    .put(version(2), resourceV2)
                    .build();
        } else {
            versionedPath = VersionedPath.versionedPath()
                    .put(VersionedPath.UNVERSIONED, resourceV1)
                    .build();
        }

        final Paths paths = Paths.paths()
                .put("/testPath", versionedPath)
                .build();
        return ApiDescription.apiDescription()
                .id("frapi:test")
                .version("1.0")
                .description(DEFAULT_API_DESCRIPTION)
                .definitions(definitions)
                .paths(paths)
                .errors(errors)
                .build();
    }

}
