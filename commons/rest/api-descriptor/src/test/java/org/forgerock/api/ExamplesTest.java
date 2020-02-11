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

import static org.forgerock.api.jackson.JacksonUtils.*;

import java.io.File;
import java.io.FilenameFilter;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.jackson.JacksonUtils;

public class ExamplesTest {

    private JsonSchema schema;

    @BeforeClass
    public void setup() throws Exception {
        // Unfortunately this currently produces a trivial schema that just has {@code "type": "any"}
        // for the {@code Errors}, {@code Definitions} and {@code Paths} classes.
        //
        // Raised with the jackson project:
        // - https://github.com/FasterXML/jackson-module-jsonSchema/pull/100
        // - https://github.com/FasterXML/jackson-databind/pull/1177
        schema = schemaFor(ApiDescription.class);
        new ObjectMapper().writer()
                .withDefaultPrettyPrinter()
                .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .writeValue(System.out, schema);
    }

    @DataProvider
    public Object[][] examples() throws Exception {
        File examplesDirectory = new File("docs/examples");
        if (!examplesDirectory.exists()) {
            return new Object[0][];
        }
        String[] examples = examplesDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        Object[][] result = new Object[examples.length][];
        for (int i = 0; i < examples.length; i++) {
            String content = Files.toString(new File(examplesDirectory, examples[i]), Charsets.UTF_8);
            result[i] = new Object[] {examples[i], content};
        }
        return result;
    }

    @Test(dataProvider = "examples")
    public void testExample(String filename, String content) throws Exception {
        JacksonUtils.validateJsonToSchema(content, schema);
    }

}
