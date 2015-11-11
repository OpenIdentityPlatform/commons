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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;
import static org.forgerock.selfservice.core.util.RequirementsBuilder.newArray;
import static org.forgerock.selfservice.core.util.RequirementsBuilder.newObject;
import static org.forgerock.selfservice.core.util.RequirementsBuilder.oneOf;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.util.Answers;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage is responsible for supplying the KBA questions to the user and capturing the answers provided by the user.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionStage extends AbstractKbaStage<SecurityAnswerDefinitionConfig> {

    /**
     * Constructs a new security answer definition stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public SecurityAnswerDefinitionStage(@SelfService ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
            SecurityAnswerDefinitionConfig config) throws ResourceException {

        Map<String, Map<String, String>> questions = config.getQuestions();
        Reject.ifTrue(questions == null || questions.isEmpty(), "KBA questions are not defined");
        List<Map<String, Object>> kbaQuestions = convertToCollections(questions);

        return RequirementsBuilder
                .newInstance("Knowledge based questions")
                .addRequireProperty("kba",
                        newArray(config.getNumberOfAnswersUserMustSet(),
                                oneOf(
                                        json(object(field("$ref", "#/definitions/systemQuestion"))),
                                        json(object(field("$ref", "#/definitions/userQuestion")))))
                                .addCustomField("questions", json(kbaQuestions)))
                .addDefinition(REQUIREMENT_PROPERTY_SYSTEM_QUESTION,
                        newObject("System Question")
                                .addRequireProperty(REQUIREMENT_PROPERTY_QUESTION_ID,
                                        "Id of predefined question")
                                .addRequireProperty(REQUIREMENT_PROPERTY_ANSWER,
                                        "Answer to the referenced question")
                                .addCustomField("additionalProperties", json(false)))
                .addDefinition(REQUIREMENT_PROPERTY_USER_QUESTION,
                        newObject("User Question")
                                .addRequireProperty(REQUIREMENT_PROPERTY_CUSTOM_QUESTION,
                                        "Question defined by the user")
                                .addRequireProperty(REQUIREMENT_PROPERTY_ANSWER, "Answer to the question")
                                .addCustomField("additionalProperties", json(false)))
                .build();
    }

    private List<Map<String, Object>> convertToCollections(Map<String, Map<String, String>> questions) {
        List<Map<String, Object>> jsonValueList = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> mapEntry : questions.entrySet()) {
            Map<String, Object> results = new HashMap<>();
            results.put(REQUIREMENT_PROPERTY_ID, mapEntry.getKey());
            results.put(REQUIREMENT_PROPERTY_QUESTION, mapEntry.getValue());
            jsonValueList.add(results);
        }
        return jsonValueList;
    }

    @Override
    public StageResponse advance(ProcessContext context, SecurityAnswerDefinitionConfig config)
            throws ResourceException {
        JsonValue kba = context.getInput().get("kba").required();
        Reject.ifFalse(kba.size() >= config.getNumberOfAnswersUserMustSet());

        hashAnswers(kba);
        addKbaToContext(context, config, kba);

        return StageResponse.newBuilder().build();
    }

    private void hashAnswers(JsonValue kba) throws InternalServerErrorException {
        List<Object> questions = kba.asList();
        for (int kbaArrayIndex = 0; kbaArrayIndex < questions.size(); kbaArrayIndex++) {
            JsonPointer pointerToAnswer = getPointerToAnswer(kbaArrayIndex);
            JsonValue answerValue = kba.get(pointerToAnswer);
            JsonValue answerHashed = Answers.hashAnswer(cryptoService, answerValue);
            kba.put(pointerToAnswer, answerHashed);
        }
    }

    private JsonPointer getPointerToAnswer(int kbaArrayIndex) {
        return new JsonPointer(kbaArrayIndex + "/" + REQUIREMENT_PROPERTY_ANSWER);
    }

    private void addKbaToContext(ProcessContext context, SecurityAnswerDefinitionConfig config, JsonValue kba) {
        JsonValue user = ensureUserInContext(context);
        user.put(new JsonPointer(config.getKbaPropertyName()), kba);
        context.putState(USER_FIELD, user);
    }

    private JsonValue ensureUserInContext(ProcessContext context) {
        JsonValue user = context.getState(USER_FIELD);
        if (user == null) {
            user = json(object());
            context.putState(USER_FIELD, user);
        }
        return user;
    }

}
