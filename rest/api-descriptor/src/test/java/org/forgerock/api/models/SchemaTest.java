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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.forgerock.api.annotations.EnumTitle;
import org.forgerock.api.annotations.PropertyOrder;
import org.forgerock.api.annotations.PropertyPolicies;
import org.forgerock.api.enums.WritePolicy;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class SchemaTest {

    @Test
    public void testTranslationTransformer() throws Exception {
        ApiDescription apiDescription = apiDescription()
                .id("frapi:test")
                .version("1.0")
                .description(new LocalizableString("Default API description."))
                .build();

        Schema fromAnno = Schema.fromAnnotation(
                IdentifiedResponse.class.getAnnotation(org.forgerock.api.annotations.Schema.class),
                apiDescription, this.getClass());


        JsonValue schema = fromAnno.getSchema();

        String result = new String(Json.writeJson(schema.getObject()), StandardCharsets.UTF_8);

        assertThat(result).doesNotContain("i18n:");
        assertThat(result).contains("Json schema description");
    }

    @org.forgerock.api.annotations.Schema(schemaResource = "i18njsonschema.json")
    private static final class IdentifiedResponse {
        public String uid;
        public String name;
    }

    @Test
    public void testType() throws Exception {
        JsonValue schema = Schema.newBuilder().type(Described.class).build().getSchema();

        assertThat(schema).isObject();

        assertThat(schema).isObject().hasObject("properties")
                .hasObject("myfield")
                .contains(
                        entry("type", "object"),
                        entry("description", "It's my field"),
                        entry("propertyOrder", 5),
                        entry("writePolicy", "WRITE_ON_CREATE"));
        assertThat(schema).isObject().hasObject("properties")
                .hasObject("enumfield")
                .doesNotContain("description")
                .doesNotContain("propertyOrder")
                .doesNotContain("writePolicy")
                .contains("type", "string")
                .hasObject("options")
                .hasArray("enum_titles")
                .containsExactly("One", "Two", "Three");
    }

    private enum MyEnum {
        @EnumTitle("One")
        ONE,
        @EnumTitle("Two")
        TWO,
        @EnumTitle("Three")
        THREE
    }

    private static final class Described {
        @JsonPropertyDescription("It's my field")
        @PropertyPolicies(write = WritePolicy.WRITE_ON_CREATE)
        @PropertyOrder(5)
        public Map<String, Object> myfield;
        public MyEnum enumfield;
    }
}