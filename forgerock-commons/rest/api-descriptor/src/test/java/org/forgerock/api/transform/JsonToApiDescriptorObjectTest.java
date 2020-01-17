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

package org.forgerock.api.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.util.Json;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Json to {@link ApiDescription} deserializer test
 */
public class JsonToApiDescriptorObjectTest {

    private static final LocalizableString DESCRIPTION = new LocalizableString(
            "Users can have devices, but the devices are their own resources.");

    private static final File[] EXAMPLE_FILES = new File("docs/examples").listFiles();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModules(new Json.JsonValueModule(), new Json.LocalizableStringModule());

    @Test
    public void subResourcesJsonToApiDescriptorPropertiesTest() throws IOException {

        File file = Paths.get("docs/examples/sub-resources.json").toFile();

        ApiDescription apiDescription = OBJECT_MAPPER.readValue(file, ApiDescription.class);

        assertThat(apiDescription.getDescription().toTranslatedString(new PreferredLocales()))
                .isEqualTo(DESCRIPTION.toTranslatedString(new PreferredLocales()));
        assertThat(apiDescription.getDefinitions().getNames()).hasSize(2);

        //schema assertions
        assertThat(apiDescription.getDefinitions().get("user").getReference()).isNull();
        assertThat(apiDescription.getDefinitions().get("user").getSchema()).hasSize(5);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("description")
                .asString()).isEqualTo("User with device sub-resources");
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties")).hasSize(6);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("_id")).hasSize(4);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("_id")
                .get("title").asString()).isEqualTo("Unique Identifier");
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("_rev")).hasSize(3);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("_rev")
                .get("title").asString()).isEqualTo("Revision Identifier");
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("uid")).hasSize(3);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("uid")
                .get("title").asString()).isEqualTo("User unique identifier");
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("name")).hasSize(3);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("name")
                .get("title").asString()).isEqualTo("User name");
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("password"))
                .hasSize(3);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("password")
                .get("description").asString()).isEqualTo("Password of the user");
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("devices")).hasSize(6);
        assertThat(apiDescription.getDefinitions().get("user").getSchema().get("properties").get("devices")
                .get("items").get("$ref").asString()).isEqualTo("#/definitions/device");
        assertThat(apiDescription.getDefinitions().get("device").getReference()).isNull();
        assertThat(apiDescription.getDefinitions().get("device").getSchema()).hasSize(5);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("description").asString())
                .isEqualTo("Device");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties")).hasSize(7);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("_id")).hasSize(4);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("_id")
                .get("title").asString()).isEqualTo("Unique Identifier");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("_rev")).hasSize(3);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("_rev")
                .get("title").asString()).isEqualTo("Revision Identifier");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("did")).hasSize(2);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("did")
                .get("title").asString()).isEqualTo("Unique Identifier of the device");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("name")).hasSize(2);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("name")
                .get("title").asString()).isEqualTo("Device name");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("type")).hasSize(2);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("type")
                .get("title").asString()).isEqualTo("Device type");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("stolen"))
                .hasSize(3);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("stolen")
                .get("title").asString()).isEqualTo("Stolen flag");
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("rollOutDate"))
                .hasSize(3);
        assertThat(apiDescription.getDefinitions().get("device").getSchema().get("properties").get("rollOutDate")
                .get("title").asString()).isEqualTo("Roll-out date");
        //services
        assertThat(apiDescription.getServices().getNames()).hasSize(4);
        assertThat(apiDescription.getServices().get("devices:1.0").getResourceSchema().getReference()
                .getValue()).isEqualTo("#/definitions/device");
        assertThat(apiDescription.getServices().get("devices:1.0").getDescription()).isNotNull();
        assertThat(apiDescription.getServices().get("devices:1.0").getCreate().getMode())
                .isEqualTo(CreateMode.ID_FROM_SERVER);
        assertThat(apiDescription.getServices().get("devices:1.0").getCreate().getSupportedLocales()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getCreate().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getRead()).isNull();
        assertThat(apiDescription.getServices().get("devices:1.0").getUpdate()).isNull();
        assertThat(apiDescription.getServices().get("devices:1.0").getDelete()).isNull();
        assertThat(apiDescription.getServices().get("devices:1.0").getPatch()).isNull();
        assertThat(apiDescription.getServices().get("devices:1.0").getActions()).hasSize(0);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()).hasSize(1);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getType())
                .isEqualTo(QueryType.FILTER);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getPagingModes()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getCountPolicies()).hasSize(1);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getCountPolicies()[0])
                .isEqualTo(CountPolicy.NONE);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getQueryableFields()).hasSize(5);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getSupportedLocales()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getParameters()).isNull();
        assertThat(apiDescription.getServices().get("devices:1.0").getQueries()[0].getStability()).isNull();

        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getCreate().getMode())
                .isEqualTo(CreateMode.ID_FROM_CLIENT);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getCreate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getCreate().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getRead().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getRead().getApiErrors()).hasSize(3);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getUpdate().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("Update a device");
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getUpdate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getUpdate().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getDelete().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("Delete a device");
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getDelete().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getDelete().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getPatch().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("Patch a device");
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getPatch().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getPatch().getApiErrors())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getPatch().getOperations()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getPatch().getOperations()[0])
                .isEqualTo(PatchOperation.ADD);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getPatch().getOperations()[1])
                .isEqualTo(PatchOperation.REMOVE);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getActions()).hasSize(1);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getActions()[0].getName())
                .isEqualTo("markAsStolen");
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getActions()[0].getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:1.0").getItems().getActions()[0].getApiErrors())
                .hasSize(3);

        assertThat(apiDescription.getServices().getNames()).hasSize(4);
        assertThat(apiDescription.getServices().get("devices:2.0").getResourceSchema().getReference().getValue())
                .isEqualTo("#/definitions/device");
        assertThat(apiDescription.getServices().get("devices:2.0").getDescription()).isNotNull();
        assertThat(apiDescription.getServices().get("devices:2.0").getCreate().getMode())
                .isEqualTo(CreateMode.ID_FROM_SERVER);
        assertThat(apiDescription.getServices().get("devices:2.0").getCreate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getCreate().getApiErrors())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getRead()).isNull();
        assertThat(apiDescription.getServices().get("devices:2.0").getUpdate()).isNull();
        assertThat(apiDescription.getServices().get("devices:2.0").getDelete()).isNull();
        assertThat(apiDescription.getServices().get("devices:2.0").getPatch()).isNull();
        assertThat(apiDescription.getServices().get("devices:2.0").getActions()).hasSize(0);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()).hasSize(1);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getType())
                .isEqualTo(QueryType.FILTER);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getPagingModes()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getCountPolicies()).hasSize(1);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getCountPolicies()[0])
                .isEqualTo(CountPolicy.NONE);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getQueryableFields()).hasSize(5);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getSupportedLocales()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getParameters()).isNull();
        assertThat(apiDescription.getServices().get("devices:2.0").getQueries()[0].getStability()).isNull();

        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getCreate().getMode())
                .isEqualTo(CreateMode.ID_FROM_CLIENT);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getCreate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getCreate().getApiErrors())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getRead().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getRead().getApiErrors()).hasSize(3);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getUpdate().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("Update a device");
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getUpdate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getUpdate().getApiErrors())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getDelete().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("Delete a device");
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getDelete().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getDelete().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getPatch().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("Patch a device");
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getPatch().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getPatch().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getPatch().getOperations()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getPatch().getOperations()[0])
                .isEqualTo(PatchOperation.ADD);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getPatch().getOperations()[1])
                .isEqualTo(PatchOperation.REMOVE);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()).hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()[0].getName())
                .isEqualTo("markAsStolen");
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()[0].getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()[0].getApiErrors())
                .hasSize(3);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()[1].getName())
                .isEqualTo("rollOut");
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()[1].getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("devices:2.0").getItems().getActions()[1].getApiErrors())
                .hasSize(3);

        assertThat(apiDescription.getServices().get("users:1.0").getResourceSchema().getReference().getValue())
                .isEqualTo("#/definitions/user");
        assertThat(apiDescription.getServices().get("users:1.0").getDescription()).isNotNull();
        assertThat(apiDescription.getServices().get("users:1.0").getCreate().getMode())
                .isEqualTo(CreateMode.ID_FROM_SERVER);
        assertThat(apiDescription.getServices().get("users:1.0").getCreate().getSupportedLocales()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getCreate().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getRead()).isNull();
        assertThat(apiDescription.getServices().get("users:1.0").getUpdate()).isNull();
        assertThat(apiDescription.getServices().get("users:1.0").getDelete()).isNull();
        assertThat(apiDescription.getServices().get("users:1.0").getPatch()).isNull();
        assertThat(apiDescription.getServices().get("users:1.0").getActions()).hasSize(0);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()).hasSize(1);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getType())
                .isEqualTo(QueryType.FILTER);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getPagingModes()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getCountPolicies()).hasSize(1);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getCountPolicies()[0])
                .isEqualTo(CountPolicy.NONE);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getQueryableFields()).hasSize(3);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getSupportedLocales()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getParameters()).isNull();
        assertThat(apiDescription.getServices().get("users:1.0").getQueries()[0].getStability()).isNull();

        assertThat(apiDescription.getServices().get("users:1.0").getItems().getCreate().getMode())
                .isEqualTo(CreateMode.ID_FROM_CLIENT);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getCreate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getCreate().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getRead().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getRead().getApiErrors()).hasSize(3);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getUpdate().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("User update operation");
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getUpdate().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getUpdate().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getDelete().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("User delete operation");
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getDelete().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getDelete().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getPatch().getDescription()
                .toTranslatedString(new PreferredLocales()))
                .isEqualTo("User patch operation");
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getPatch().getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getPatch().getApiErrors()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getPatch().getOperations()).hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getPatch().getOperations()[0])
                .isEqualTo(PatchOperation.ADD);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getPatch().getOperations()[1])
                .isEqualTo(PatchOperation.REMOVE);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getActions()).hasSize(1);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getActions()[0].getName())
                .isEqualTo("resetPassword");
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getActions()[0].getSupportedLocales())
                .hasSize(2);
        assertThat(apiDescription.getServices().get("users:1.0").getItems().getActions()[0].getApiErrors())
                .hasSize(3);

        //errors
        assertThat(apiDescription.getErrors().getNames()).hasSize(2);
        assertThat(apiDescription.getErrors().getNames().contains("badRequest")).isTrue();
        assertThat(apiDescription.getErrors().getNames().contains("unauthorized")).isTrue();

        //paths
        assertThat(apiDescription.getPaths().getNames()).hasSize(2);
        assertThat(apiDescription.getPaths().get("/users").getVersions()).hasSize(2);
        assertThat(apiDescription.getPaths().get("/admins").getVersions()).hasSize(1);

    }

    @Test(dataProvider = "exampleFilesProvider")
    public void testExample(File example) throws Exception {
        System.out.println(example.getName());
        ApiDescription description = OBJECT_MAPPER.readValue(example, ApiDescription.class);
        String jsonFromDescription = writeApiDescriptiontoJson(description);
        ApiDescription descriptionFromJsonString = OBJECT_MAPPER.readValue(jsonFromDescription, ApiDescription.class);
        assertThat(description).isEqualTo(descriptionFromJsonString);

        Object descriptionObjectFromFile = OBJECT_MAPPER.readValue(example, Object.class);
        Object descriptionObjectFromJsonString = OBJECT_MAPPER.readValue(jsonFromDescription, Object.class);

        assertThat(descriptionObjectFromFile).isEqualTo(descriptionObjectFromJsonString);
    }

    @DataProvider
    public Object[][] exampleFilesProvider() throws Exception {
        Object[][] retVal = new Object[EXAMPLE_FILES.length][1];
        for (int i = 0; i < EXAMPLE_FILES.length; i++) {
            retVal[i] = new Object[] {EXAMPLE_FILES[i]};
        }
        return retVal;
    }

    private String writeApiDescriptiontoJson(ApiDescription apiDescription) throws IOException {
        ObjectWriter ow = OBJECT_MAPPER.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(apiDescription);
    }

}

