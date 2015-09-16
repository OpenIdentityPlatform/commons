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
package org.forgerock.selfservice.stages.kba;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.forgerock.selfservice.stages.CommonStateFields.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link SecurityAnswerDefinitionStage}.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionStageTest {

    private static final String KBA_QUESTION_1 = "What was your pet's name?";
    private static final String KBA_QUESTION_2 = "Who was your first employer?";
    private static final String KBA_QUESTION_3 = "What is my favorite author?";

    private SecurityAnswerDefinitionStage securityAnswerDefinitionStage;
    @Mock
    private ProcessContext context;

    private SecurityAnswerDefinitionConfig config;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;
    @Mock
    private ResourceResponse queryResponse;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newKbaConfig();
        securityAnswerDefinitionStage = new SecurityAnswerDefinitionStage(factory);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(
                json(object(field("questions",
                        array(KBA_QUESTION_1, KBA_QUESTION_2))))
        );
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        JsonValue jsonValue = securityAnswerDefinitionStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("KBA details");
        assertThat(jsonValue).stringAt("properties/kba/items/description").isEqualTo("KBA questions");
        assertThat(jsonValue).stringAt("properties/kba/items/properties/selectedQuestion/type")
                .isEqualTo("string");
        assertThat(jsonValue).stringAt("properties/kba/items/properties/customQuestion/type")
                .isEqualTo("string");
        assertThat(jsonValue).stringAt("properties/kba/questions/0").isEqualTo(KBA_QUESTION_1);
        assertThat(jsonValue).stringAt("properties/kba/questions/1").isEqualTo(KBA_QUESTION_2);
    }

    @Test
    public void testAdvanceWithoutUserInState() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueKba());
        given(factory.getConnection()).willReturn(connection);
        given(context.getState(SecurityAnswerDefinitionStage.CTX_KEY_KBA_QUESTIONS))
                .willReturn(newJsonValueContextKbaQuestions());

        // When
        securityAnswerDefinitionStage.advance(context, config);

        // Then
        ArgumentCaptor<JsonValue> createRequestArgumentCaptor =  ArgumentCaptor.forClass(JsonValue.class);
        verify(context, times(2))   //1. when the empty empty object is pushed 2. when updated user json is pushed
                .putState(eq(USER_FIELD), createRequestArgumentCaptor.capture());
        JsonValue userJson = createRequestArgumentCaptor.getValue();

        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/0/customQuestion")
                .isEqualTo(KBA_QUESTION_3);
        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/0/answer").isEqualTo("a1");
        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/1/selectedQuestion")
                .isEqualTo(KBA_QUESTION_2);
        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/1/answer").isEqualTo("a2");

    }

    @Test
    public void testAdvanceWithUserInState() throws Exception {
        // Given
        given(context.getInput()).willReturn(newJsonValueKba());
        given(context.getState(USER_FIELD)).willReturn(newJsonValueUser());
        given(factory.getConnection()).willReturn(connection);
        given(context.getState(SecurityAnswerDefinitionStage.CTX_KEY_KBA_QUESTIONS))
                .willReturn(newJsonValueContextKbaQuestions());

        // When
        securityAnswerDefinitionStage.advance(context, config);

        // Then
        ArgumentCaptor<JsonValue> createRequestArgumentCaptor =  ArgumentCaptor.forClass(JsonValue.class);
        verify(context, times(1)).putState(eq(USER_FIELD), createRequestArgumentCaptor.capture());
        JsonValue userJson = createRequestArgumentCaptor.getValue();

        assertThat(userJson).stringAt("givenName").isEqualTo("testUser");
        assertThat(userJson).stringAt("sn").isEqualTo("testUserSecondName");
        assertThat(userJson).stringAt("password").isEqualTo("passwordTobeEncrypted");

        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/0/customQuestion")
                .isEqualTo(KBA_QUESTION_3);
        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/0/answer").isEqualTo("a1");
        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/1/selectedQuestion")
                .isEqualTo(KBA_QUESTION_2);
        assertThat(userJson).stringAt(config.getKbaPropertyName() + "/1/answer").isEqualTo("a2");
    }

    private SecurityAnswerDefinitionConfig newKbaConfig() {
        return new SecurityAnswerDefinitionConfig()
                .setKbaServiceUrl("/kba/questions/0")
                .setKbaPropertyName("kba1");
    }

    private JsonValue newJsonValueContextKbaQuestions() {
        Map<String, String> kbaQuestions = new HashMap<>();
        kbaQuestions.put("0", KBA_QUESTION_1);
        kbaQuestions.put("1", KBA_QUESTION_2);
        return json(kbaQuestions);
    }

    private JsonValue newJsonValueUser() {
        return json(
                object(
                        field("givenName", "testUser"),
                        field("sn", "testUserSecondName"),
                        field("password", "passwordTobeEncrypted")));
    }

    private JsonValue newJsonValueKba() {
        return json(
                object(
                        field("kba", array(
                                object(
                                        field("customQuestion", KBA_QUESTION_3),
                                        field("answer", "a1")),
                                object(
                                        field("selectedQuestion", 1),
                                        field("answer", "a2"))))));
    }
}
