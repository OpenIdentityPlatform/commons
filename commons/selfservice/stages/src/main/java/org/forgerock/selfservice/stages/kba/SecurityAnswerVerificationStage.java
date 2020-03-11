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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;
import static org.forgerock.selfservice.core.util.RequirementsBuilder.newEmptyObject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.crypto.JsonCryptoException;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.util.Answers;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage is responsible for verifying the answers provided by the user for the KBA questions.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerVerificationStage extends AbstractKbaStage<SecurityAnswerVerificationConfig> {

    static final String KEY_STATE_ANSWER_VS_QUESTION = "KEY_STATE_ANSWER_VS_QUESTION";

    private static final String DEFAULT_VALUE_KBA_PROPERTY_NAME = "kba";

    /**
     * Constructs a new security answer verification stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public SecurityAnswerVerificationStage(@SelfService ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, SecurityAnswerVerificationConfig config)
            throws ResourceException {
        Reject.ifFalse(context.containsState(USER_ID_FIELD),
                "Security answer verification stage expects userId in the context");
        String userId = context.getState(USER_ID_FIELD).asString();

        Reject.ifNull(config.getIdentityServiceUrl(),
                "Identity service url should be configured");

        Reject.ifTrue(config.getQuestions() == null || config.getQuestions().size() < 1,
                "KBA questions should be configured");

        Reject.ifTrue(config.getNumberOfQuestionsUserMustAnswer() < 1,
                "Number of questions user must answer is configured as "
                        + config.getNumberOfQuestionsUserMustAnswer());

        JsonValue userKbaQuestions = getKbaAnswersSetDuringRegistration(context, config, userId);

        List<?> shuffledAnswers = new ArrayList<>(userKbaQuestions.asList());
        Collections.shuffle(shuffledAnswers);

        int questionsTobeAsked = Math.min(config.getNumberOfQuestionsUserMustAnswer(), shuffledAnswers.size());
        if (questionsTobeAsked != config.getNumberOfQuestionsUserMustAnswer()) {
            throw new BadRequestException("Insufficient number of questions. "
                    + "Minimum number of questions user must answer: " + config.getNumberOfQuestionsUserMustAnswer()
                    + ", Questions available: " + shuffledAnswers.size());
        }
        JsonValue selectedAnswers = json(shuffledAnswers.subList(0, questionsTobeAsked));

        Map<String, String> answerKeyVsQuestionKey = new HashMap<>();
        RequirementsBuilder builder = RequirementsBuilder.newInstance("Answer security questions");
        generateRequirement(config, selectedAnswers, answerKeyVsQuestionKey, builder);
        putQuestionTrackersToState(context, answerKeyVsQuestionKey);

        return builder.build();
    }

    private void generateRequirement(SecurityAnswerVerificationConfig config, JsonValue selectedAnswers,
            Map<String, String> answerKeyVsQuestionKey, RequirementsBuilder builder) {
        int index = 1;
        for (JsonValue answer : selectedAnswers) {
            String answerKey = REQUIREMENT_PROPERTY_ANSWER + index++;
            JsonValue questionId = answer.get(REQUIREMENT_PROPERTY_QUESTION_ID);
            if (questionId.isNotNull()) {
                Map<String, String> kbaQuestion = config.getQuestions().get(questionId.asString());
                Reject.ifNull(kbaQuestion, "KBA question is not configured for the questionId: "
                        + questionId.asString());
                builder.addRequireProperty(answerKey,
                        newEmptyObject()
                                .addCustomField(REQUIREMENT_PROPERTY_SYSTEM_QUESTION,
                                        json(kbaQuestion))
                                .addCustomField("type", json("string")));
                answerKeyVsQuestionKey.put(answerKey, questionId.asString());
                continue;
            }

            JsonValue customQuestion = answer.get(REQUIREMENT_PROPERTY_CUSTOM_QUESTION);
            if (customQuestion.isNotNull()) {
                builder.addRequireProperty(answerKey,
                        newEmptyObject()
                                .addCustomField(REQUIREMENT_PROPERTY_USER_QUESTION,
                                        json(customQuestion.asString()))
                                .addCustomField("type", json("string")));
                answerKeyVsQuestionKey.put(answerKey, customQuestion.asString());
                continue;
            }

            throw new IllegalStateException("Invalid KBA question format. " + config.getQuestions());
        }
    }

    @Override
    public StageResponse advance(ProcessContext context, SecurityAnswerVerificationConfig config)
            throws ResourceException {

        String userId = context.getState(USER_ID_FIELD).asString();

        JsonValue answersInput = context.getInput();
        Reject.ifTrue(answersInput.isNull() || answersInput.asMap().isEmpty(),
                "Answers for the security questions are not provided");

        JsonValue kbaAnswersSetByUser = getKbaAnswersSetDuringRegistration(context, config, userId);

        Map<String, String> answersTobeProvided = getQuestionTrackersFromState(context);
        for (Map.Entry<String, String> entry : answersTobeProvided.entrySet()) {
            String answerKey = entry.getKey();
            String questionId = entry.getValue();
            matchAnswer(answerKey, questionId, answersInput, kbaAnswersSetByUser);
        }

        return StageResponse.newBuilder().build();
    }

    private JsonValue getKbaAnswersSetDuringRegistration(ProcessContext context,
            SecurityAnswerVerificationConfig config,
            String userId) throws ResourceException {
        JsonValue userJsonValue = readUser(context, config, userId);
        return getKbaAnswersSetDuringRegistration(config, userJsonValue);
    }

    private JsonValue readUser(ProcessContext context,
            SecurityAnswerVerificationConfig config,
            String userId) throws ResourceException {
        ReadRequest request = Requests.newReadRequest(config.getIdentityServiceUrl() + "/" + userId);
        try (Connection connection = connectionFactory.getConnection()) {
            ResourceResponse readResponse = connection.read(context.getRequestContext(), request);
            return readResponse.getContent();
        }
    }

    private JsonValue getKbaAnswersSetDuringRegistration(SecurityAnswerVerificationConfig config,
            JsonValue userJsonValue) {
        JsonValue answers = userJsonValue.get(new JsonPointer(
                (config.getKbaPropertyName() != null)
                        ? config.getKbaPropertyName() : DEFAULT_VALUE_KBA_PROPERTY_NAME));
        return (answers == null) ? json(array()) : answers;
    }


    private void matchAnswer(String answerKey, String questionId, JsonValue answersInput, JsonValue kbaAnswersSetByUser)
            throws InternalServerErrorException, BadRequestException {
        boolean isMatched = match(answerKey, questionId, answersInput, kbaAnswersSetByUser);
        if (!isMatched) {
            throw new BadRequestException("Answers are not matched");
        }
    }

    private boolean match(String answerKey, String questionId, JsonValue answersInput, JsonValue kbaAnswersSetByUser)
            throws InternalServerErrorException {
        if (answersInput.asMap().containsKey(answerKey)) {
            String answerProvidedByUser = answersInput.get(answerKey).asString();
            for (JsonValue answer : kbaAnswersSetByUser) {
                JsonValue qId = answer.get(REQUIREMENT_PROPERTY_QUESTION_ID);
                if (qId.isNotNull() && qId.asString().equals(questionId)) {
                    JsonValue answerSetByUser = answer.get(REQUIREMENT_PROPERTY_ANSWER);
                    return match(answerProvidedByUser, answerSetByUser);
                }

                JsonValue customQuestion = answer.get(REQUIREMENT_PROPERTY_CUSTOM_QUESTION);
                if (customQuestion.isNotNull() && customQuestion.asString().equals(questionId)) {
                    JsonValue answerSetByUser = answer.get(REQUIREMENT_PROPERTY_ANSWER);
                    return match(answerProvidedByUser, answerSetByUser);
                }
            }
        }
        return false;
    }

    private boolean match(String answerProvidedByUser, JsonValue answerSetByUser) throws InternalServerErrorException {
        try {
            return cryptoService.matches(Answers.normaliseAnswer(answerProvidedByUser), answerSetByUser);
        } catch (JsonCryptoException e) {
            throw new InternalServerErrorException("Error while matching the answers", e);
        }
    }

    private void putQuestionTrackersToState(ProcessContext context, Map<String, String> answerKeyVsQuestions) {
        context.putState(KEY_STATE_ANSWER_VS_QUESTION, answerKeyVsQuestions);
    }

    private Map<String, String> getQuestionTrackersFromState(ProcessContext context) {
        Reject.ifNull(context.getState(KEY_STATE_ANSWER_VS_QUESTION),
                "Unable to track the questions asked to the user");
        return context.getState(KEY_STATE_ANSWER_VS_QUESTION).asMap(String.class);
    }

}
