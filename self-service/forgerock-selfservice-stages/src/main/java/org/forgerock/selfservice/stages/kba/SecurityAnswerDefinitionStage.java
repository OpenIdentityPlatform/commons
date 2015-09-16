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
import static org.forgerock.selfservice.stages.utils.RequirementsBuilder.*;
import static org.forgerock.selfservice.stages.CommonStateFields.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.services.context.Context;

/**
 * Stage is responsible for supplying the KBA questions to the user and capturing the answers provided by the user.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionStage implements ProgressStage<SecurityAnswerDefinitionConfig> {

    static final String CTX_KEY_KBA_QUESTIONS = "kbaQuestions";

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new KBA stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public SecurityAnswerDefinitionStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
                                               SecurityAnswerDefinitionConfig config) throws ResourceException {

        JsonValue kbaJsonValue = getKbaQuestionsFromRestEndpoint(context.getHttpContext(), config);
        List<String> kbaQuestions = kbaJsonValue.get(new JsonPointer("questions")).asList(String.class);
        Map<String, String> questions = getKbaQuestionsWithIndex(kbaQuestions);
        if (questions.size() == 0) {
            throw new BadRequestException("There are no valid KBA questions defined. Service URL:"
                    + config.getKbaServiceUrl());
        }
        context.putState(CTX_KEY_KBA_QUESTIONS, questions);

        return RequirementsBuilder
                .newInstance("KBA details")
                .addRequireProperty("kba",
                        newArray(
                                newObject("KBA questions")
                                        .addProperty("selectedQuestion",
                                                "Reference to the unique id of predefined question")
                                        .addProperty("customQuestion", "Question defined by the user")
                                        .addProperty("answer", "Answer to the referenced question"))
                                .addCustomField("questions", json(kbaQuestions)))
                .build();
    }

    private JsonValue getKbaQuestionsFromRestEndpoint(Context httpContext,
                                                      SecurityAnswerDefinitionConfig config) throws ResourceException {
        JsonValue kbaJsonValue;
        ReadRequest request = Requests.newReadRequest(config.getKbaServiceUrl());
        try (Connection connection = connectionFactory.getConnection()) {
            ResourceResponse readResponse = connection.read(httpContext, request);
            kbaJsonValue = readResponse.getContent();
        }
        if (kbaJsonValue == null) {
            throw new BadRequestException("KBA questions are not defined. Service URL:" + config.getKbaServiceUrl());
        }
        return kbaJsonValue;
    }

    private Map<String, String> getKbaQuestionsWithIndex(List<String> kbaQuestions) {
        Map<String, String> questionsIndexed = new HashMap<>();
        int index = 0;
        for (String q : kbaQuestions) {
            String question = trimToNull(q);
            if (question == null) {
                continue;
            }
            questionsIndexed.put(String.valueOf(index++), question);
        }
        return questionsIndexed;
    }

    private String trimToNull(String str) {
        if (str != null) {
            str = str.trim();
            if (str.length() > 0) {
                return str;
            }
        }
        return null;
    }

    @Override
    public StageResponse advance(ProcessContext context, SecurityAnswerDefinitionConfig config)
            throws ResourceException {

        JsonValue kba = context.getInput().get("kba");
        if (kba.isNull()) {
            throw new BadRequestException("KBA has not been specified");
        }

        JsonValue kbaQuestions = context.getState(CTX_KEY_KBA_QUESTIONS);
        Map<String, String> questions = kbaQuestions.asMap(String.class);
        kba = replaceKbaQuestionIndex(kba, questions);

        addKbaToContext(context, config, kba);

        return StageResponse.newBuilder().build();
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

    private JsonValue replaceKbaQuestionIndex(JsonValue kba, Map<String, String> kbaQuestions) {
        for (Map.Entry<String, String> entry : kbaQuestions.entrySet()) {
            String index = entry.getKey();
            String question = entry.getValue();
            JsonPointer pointerPredefinedQuestion = new JsonPointer(index + "/selectedQuestion");
            JsonValue predefinedQuestion = kba.get(pointerPredefinedQuestion);
            if (predefinedQuestion != null) {
                kba.put(pointerPredefinedQuestion, question);
            }
        }
        return kba;
    }

}
