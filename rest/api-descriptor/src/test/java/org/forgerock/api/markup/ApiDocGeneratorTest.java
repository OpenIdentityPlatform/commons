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
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.api.markup.asciidoc.AsciiDoc.normalizeName;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.forgerock.api.ApiTestUtil;
import org.forgerock.api.markup.asciidoc.AsciiDoc;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ApiDocGeneratorTest {

    private static final String CUSTOM_API_DESCRIPTION = "\n\nCustom API description.\n\n";

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
    public void testExecuteWithUserDeviceExample() throws Exception {
        final ApiDescription apiDescription = ApiTestUtil.createUserAndDeviceExampleApiDescription();
        final String content = ApiDocGenerator.execute("Users and Devices API", apiDescription, null);

        assertThat(content).isNotEmpty();
        assertThat(AsciiDoc.INCLUDE_PATTERN.matcher(content).find()).isFalse();
    }

    @Test
    public void testExecuteWithUnversionedPaths() throws Exception {
        final Path testOutputDirPath = outputDirPath.resolve("testExecute");
        final ApiDescription apiDescription = ApiTestUtil.createApiDescription(false);
        ApiDocGenerator.execute("Example API Without Resource Versions", apiDescription, null, testOutputDirPath);

        // check for output-dir for default API description file
        final Path outputApiDescriptionPath = testOutputDirPath.resolve(normalizeName(apiDescription.getId(),
                "index_description.adoc"));
        final String outputApiDescription = new String(Files.readAllBytes(outputApiDescriptionPath), UTF_8);
        final String description = apiDescription.getDescription() == null
                ? null
                : apiDescription.getDescription().toTranslatedString(new PreferredLocales());
        assertThat(outputApiDescription).contains(description);
    }

    @Test
    public void testExecuteWithVersionedPaths() throws Exception {
        final Path testOutputDirPath = outputDirPath.resolve("testExecuteWithVersionedPaths");
        final ApiDescription apiDescription = ApiTestUtil.createApiDescription(true);
        ApiDocGenerator.execute("Example API With Resource Versions", apiDescription, null, testOutputDirPath);

        // check for output-dir for default API description file
        final Path outputApiDescriptionPath = testOutputDirPath.resolve(normalizeName(apiDescription.getId(),
                "index_description.adoc"));
        final String outputApiDescription = new String(Files.readAllBytes(outputApiDescriptionPath), UTF_8);
        final String description = apiDescription.getDescription() == null
                ? null
                : apiDescription.getDescription().toTranslatedString(new PreferredLocales());
        assertThat(outputApiDescription).contains(description);
    }

    @Test
    public void testExecuteWithInputOverrides() throws Exception {
        final ApiDescription apiDescription = ApiTestUtil.createApiDescription(false);

        // create description file in input-dir
        final Path testInputDirPath = inputDirPath.resolve("testExecuteWithInputOverrides");
        Files.createDirectory(testInputDirPath);
        final Path inputApiDescriptionPath = testInputDirPath.resolve(normalizeName(apiDescription.getId(),
                "index_description.adoc"));
        Files.createFile(inputApiDescriptionPath);
        Files.write(inputApiDescriptionPath, CUSTOM_API_DESCRIPTION.getBytes(UTF_8));

        // write API descriptor files to output-dir
        final Path testOutputDirPath = outputDirPath.resolve("testExecuteWithInputOverrides");
        ApiDocGenerator.execute("Example API With AsciiDoc Overrides", apiDescription, testInputDirPath,
                testOutputDirPath);

        // check for input-dir description file in output-dir
        final Path outputApiDescriptionPath = testOutputDirPath.resolve(normalizeName(apiDescription.getId(),
                "index_description.adoc"));
        final String outputApiDescription = new String(Files.readAllBytes(outputApiDescriptionPath), UTF_8);
        assertThat(outputApiDescription).isEqualTo(CUSTOM_API_DESCRIPTION);
    }
}
