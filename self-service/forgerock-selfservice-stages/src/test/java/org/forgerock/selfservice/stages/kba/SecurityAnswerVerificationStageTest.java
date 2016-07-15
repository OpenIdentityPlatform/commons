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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.selfservice.stages.kba;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.crypto.CryptoConstants.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link SecurityAnswerVerificationStage}.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerVerificationStageTest {


    private static final String KBA_QUESTION_1 = "Who is your favorite author?";
    private static final String KBA_QUESTION_2 = "Who was your first employer?";

    private SecurityAnswerVerificationStage securityAnswerVerificationStage;
    @Mock
    private ConnectionFactory factory;
    @Mock
    private Connection connection;
    @Mock
    private ProcessContext context;

    private SecurityAnswerVerificationConfig config;
    @Mock
    private ResourceResponse queryResponse;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = newKbaConfig();
        securityAnswerVerificationStage = new SecurityAnswerVerificationStage(factory);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Security answer verification stage expects userId in the context")
    public void testGatherInitialRequirementsExceptionUserId() throws Exception {
        // When
        securityAnswerVerificationStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Identity service url should be configured")
    public void testGatherInitialRequirementsExceptionServiceUrl() throws Exception {
        // Given
        config = new SecurityAnswerVerificationConfig(new KbaConfig());
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        // When
        securityAnswerVerificationStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "KBA questions should be configured")
    public void testGatherInitialRequirementsExceptionKbaQuestionsNotSet() throws Exception {
        // Given
        config = new SecurityAnswerVerificationConfig(new KbaConfig())
                .setIdentityServiceUrl("/users");
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        // When
        securityAnswerVerificationStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Number of questions user must answer is configured as 0")
    public void testGatherInitialRequirementsExceptionUserMustAnswerCountZero() throws Exception {
        // Given
        KbaConfig kbaConfig = new KbaConfig();
        Map<String, Map<String, String>> questions = new HashMap<>();
        questions.put("1", newKbaQuestion1().get("1"));
        kbaConfig.setQuestions(questions);
        config = new SecurityAnswerVerificationConfig(kbaConfig)
                .setIdentityServiceUrl("/users")
                .setNumberOfQuestionsUserMustAnswer(0);
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        // When
        securityAnswerVerificationStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Insufficient number of questions. Minimum number of questions "
                    + "user must answer: 1, Questions available: 0")
    public void testGatherInitialRequirementsExceptionInsufficientNumberOfQuestions() throws Exception {
        // Given
        config.setNumberOfQuestionsUserMustAnswer(1);
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(newEmptyJsonValue());
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        JsonValue jsonValue = securityAnswerVerificationStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Answer security questions");
        assertThat(jsonValue).stringAt("properties/answer1/userQuestion").isEqualTo(KBA_QUESTION_1);
        assertThat(jsonValue).stringAt("properties/answer1/type").isEqualTo("string");
    }

    @Test
    public void testGatherInitialRequirementsOneCustomAnswer() throws Exception {
        // Given
        config.setNumberOfQuestionsUserMustAnswer(1);
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(newJsonValueUserWithOnlyOneCustomQuestion());
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        JsonValue jsonValue = securityAnswerVerificationStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Answer security questions");
        assertThat(jsonValue).stringAt("properties/answer1/userQuestion").isEqualTo(KBA_QUESTION_1);
        assertThat(jsonValue).stringAt("properties/answer1/type").isEqualTo("string");
    }

    @Test
    public void testGatherInitialRequirementsOneSystemQuestion() throws Exception {
        // Given
        config.setNumberOfQuestionsUserMustAnswer(1);
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(newJsonValueUserWithOnlyOneSystemQuestion());
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        JsonValue jsonValue = securityAnswerVerificationStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Answer security questions");
        assertThat(jsonValue).stringAt("properties/answer1/systemQuestion/en")
                .isEqualTo("Who was your first employer?");
        assertThat(jsonValue).stringAt("properties/answer1/type").isEqualTo("string");
    }

    @Test
    public void testGatherInitialRequirementsTwoAnswers() throws Exception {
        // Given
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(newJsonValueUser());
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        JsonValue jsonValue = securityAnswerVerificationStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Answer security questions");
        assertThat(jsonValue).hasObject("properties/answer1");
        assertThat(jsonValue).hasObject("properties/answer2");
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        given(context.getInput()).willReturn(newJsonValueKbaAnswers());

        given(context.getState(SecurityAnswerVerificationStage.KEY_STATE_ANSWER_VS_QUESTION))
                .willReturn(newQuestionTrackersList());

        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(newJsonValueUser());
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        securityAnswerVerificationStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Answers are not matched")
    public void testAdvanceException() throws Exception {
        // Given
        given(context.containsState(USER_ID_FIELD)).willReturn(true);
        given(context.getState(USER_ID_FIELD)).willReturn(new JsonValue("testUserId"));

        given(context.getInput()).willReturn(newJsonValueKbaWrongAnswers());

        given(context.getState(SecurityAnswerVerificationStage.KEY_STATE_ANSWER_VS_QUESTION))
                .willReturn(newQuestionTrackersList());

        given(factory.getConnection()).willReturn(connection);
        given(queryResponse.getContent()).willReturn(newJsonValueUser());
        given(connection.read(any(Context.class), any(ReadRequest.class))).willReturn(queryResponse);

        // When
        securityAnswerVerificationStage.advance(context, config);
    }

    private SecurityAnswerVerificationConfig newKbaConfig() {
        KbaConfig kbaConfig = new KbaConfig();
        Map<String, Map<String, String>> questions = new HashMap<>();
        questions.put("1", newKbaQuestion1().get("1"));
        questions.put("2", newKbaQuestion2().get("2"));
        questions.put("3", newKbaQuestion3().get("3"));
        kbaConfig.setQuestions(questions);
        return new SecurityAnswerVerificationConfig(kbaConfig)
                .setIdentityServiceUrl("/users")
                .setNumberOfQuestionsUserMustAnswer(2)
                .setKbaPropertyName("kbaInfo");
    }

    private JsonValue newJsonValueUser() {
        return json(
                object(
                        //user details are not used in this stage and hence skipped
                        field("kbaInfo", array(
                                object(
                                        field("customQuestion", newCustomQuestion1()),
                                        field("answer", newHashedAnswer1())),
                                object(
                                        field("questionId", "1"),
                                        field("answer", newHashedAnswerX())),
                                object(
                                        field("questionId", "2"),
                                        field("answer", newHashedAnswer2())),
                                object(
                                        field("questionId", "3"),
                                        field("answer", newHashedAnswerX()))))));
    }

    private JsonValue newJsonValueUserWithOnlyOneCustomQuestion() {
        return json(
                object(
                        //user details are not used in this stage and hence skipped
                        field("kbaInfo", array(
                                object(
                                        field("customQuestion", newCustomQuestion1()),
                                        field("answer", newHashedAnswer1()))))));
    }

    private JsonValue newJsonValueUserWithOnlyOneSystemQuestion() {
        return json(
                object(
                        //user details are not used in this stage and hence skipped
                        field("kbaInfo", array(
                                object(
                                        field("questionId", "2"),
                                        field("answer", newHashedAnswer2()))))));
    }

    private JsonValue newJsonValueKbaAnswers() {
        return json(
                object(
                        field("answer1", "Neal Stephenson"),
                        field("answer2", "Pizza place")));
    }

    private JsonValue newJsonValueKbaWrongAnswers() {
        return json(
                object(
                        field("answer1", "Neal StephensonERR"),
                        field("answer2", "Pizza place")));
    }

    private JsonValue newQuestionTrackersList() {
        Map<String, String> answerVsQuestion = new HashMap<>();
        answerVsQuestion.put("answer1", newCustomQuestion1());
        answerVsQuestion.put("answer2", "2");
        return json(answerVsQuestion);
    }

    private Map<String, Map<String, String>> newKbaQuestion1() {
        Map<String, Map<String, String>> questions = new HashMap<>();
        Map<String, String> locales = new HashMap<>();
        locales.put("en", "What's your favorite color?");
        locales.put("en_GB", "What's your favorite colour?");
        locales.put("fr", "Quelle est votre couleur préférée?");
        questions.put("1", locales);
        return questions;
    }

    private Map<String, Map<String, String>> newKbaQuestion2() {
        Map<String, Map<String, String>> questions = new HashMap<>();
        Map<String, String> locales = new HashMap<>();
        locales.put("en", KBA_QUESTION_2);
        questions.put("2", locales);
        return questions;
    }

    private Map<String, Map<String, String>> newKbaQuestion3() {
        Map<String, Map<String, String>> questions = new HashMap<>();
        Map<String, String> locales = new HashMap<>();
        locales.put("en", "What is your pet’s name");
        questions.put("3", locales);
        return questions;
    }

    private String newCustomQuestion1() {
        return KBA_QUESTION_1;
    }

    private Object newHashedAnswer1() {
        return newHashedAnswer("5ZwZSsqn13LEI4vHhZoI9EcafcO17h06z0wLNzZp2WI3QocVVuQ/eKb5l+wlDGSl");
    }

    private Object newHashedAnswer2() {
        return newHashedAnswer("V10qqk0CyImH0cI7Psb4YNmIEr5iZHgYtCnl4fOvZS2TTMlYdiI8nt1LbpqZiYMt");
    }

    private Object newHashedAnswerX() {
        return newHashedAnswer("not meant for matching");
    }

    private Object newHashedAnswer(String data) {
        return object(
                field(CRYPTO, object(
                        field(CRYPTO_VALUE, object(
                                field(CRYPTO_ALGORITHM, ALGORITHM_SHA_256),
                                field(CRYPTO_DATA, data))),
                        field(CRYPTO_TYPE, STORAGE_TYPE_HASH))));
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

}
