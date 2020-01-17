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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.selfservice.core.util;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.core.util.RequirementsBuilder.*;

import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

/**
 * Unit test for {@link RequirementsBuilder}.
 *
 * @since 0.2.0
 */
public final class RequirementsBuilderTest {

    @Test
    public void testNewInstance() throws Exception {
        // Given
        RequirementsBuilder builder =
                newInstance("New user details")
                        .addRequireProperty("userId", "New user Id")
                        .addRequireProperty("user",
                                newObject("User details")
                                        .addProperty("kba",
                                                newArray(
                                                        newObject("KBA details")
                                                                .addProperty("question", "integer",
                                                                        "Reference to index of predefined question")
                                                                .addProperty("answer",
                                                                        "Answer to the referenced question"))
                                                        .addCustomField("questions",
                                                                json(array(
                                                                        "What was your pet's name?",
                                                                        "Who was your first employer?")))))
                        .addRequireProperty("kbaV2",
                                newArray(2,
                                        oneOf(
                                                json(object(field("$ref", "#/definitions/systemQuestion"))),
                                                json(object(field("$ref", "#/definitions/userQuestion"))))))
                        .addDefinition("systemQuestion",
                                newObject("System Question")
                                        .addRequireProperty("questionId", "Id of predefined question")
                                        .addRequireProperty("answer", "Answer to the referenced question")
                                        .addCustomField("additionalProperties", json(false)));

        // When
        JsonValue jsonValue = builder.build();

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("New user details");
        assertThat(jsonValue).stringAt("properties/userId/description").isEqualTo("New user Id");
        assertThat(jsonValue).stringAt("properties/user/description").isEqualTo("User details");
        assertThat(jsonValue).stringAt("properties/user/properties/kba/items/description").isEqualTo("KBA details");
        assertThat(jsonValue).stringAt("properties/user/properties/kba/items/properties/question/type")
                .isEqualTo("integer");
        assertThat(jsonValue).stringAt("properties/user/properties/kba/items/properties/question/description")
                .isEqualTo("Reference to index of predefined question");
        assertThat(jsonValue).stringAt("properties/user/properties/kba/questions/0")
                .isEqualTo("What was your pet's name?");
        assertThat(jsonValue).stringAt("properties/user/properties/kba/questions/1")
                .isEqualTo("Who was your first employer?");
        assertThat(jsonValue).integerAt("properties/kbaV2/minItems").isEqualTo(2);
        assertThat(jsonValue).stringAt("properties/kbaV2/items/oneOf/0/$ref")
                .isEqualTo("#/definitions/systemQuestion");
        assertThat(jsonValue).stringAt("definitions/systemQuestion/properties/questionId/description")
                .isEqualTo("Id of predefined question");
    }

    @Test
    public void testNewInstanceProperties() throws Exception {
        // Given
        RequirementsBuilder builder = RequirementsBuilder
                .newInstance("New user details")
                .addRequireProperty("userId", "New user Id")
                .addRequireProperty("user", "object", "User details");

        // When
        JsonValue jsonValue =  builder.build();

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("New user details");
        assertThat(jsonValue).stringAt("properties/userId/description").isEqualTo("New user Id");
        assertThat(jsonValue).stringAt("properties/user/description").isEqualTo("User details");
        assertThat(jsonValue).stringAt("properties/user/type").isEqualTo("object");
    }
}

