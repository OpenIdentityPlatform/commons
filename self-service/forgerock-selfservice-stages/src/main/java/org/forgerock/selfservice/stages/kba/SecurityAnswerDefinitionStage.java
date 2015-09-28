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
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;
import static org.forgerock.selfservice.stages.utils.RequirementsBuilder.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.Reject;

/**
 * Stage is responsible for supplying the KBA questions to the user and capturing the answers provided by the user.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionStage implements ProgressStage<SecurityAnswerDefinitionConfig> {

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

        List<KbaQuestion> questions = config.questionsAsList();
        Reject.ifTrue(questions.isEmpty(), "KBA questions are not defined");
        List<Map<String, Object>> kbaQuestions = convertToCollections(questions);

        return RequirementsBuilder
                .newInstance("Knowledge based questions")
                .addRequireProperty("kba",
                        newArray(
                                oneOf(
                                        json(object(field("$ref", "#/definitions/systemQuestion"))),
                                        json(object(field("$ref", "#/definitions/userQuestion")))))
                                .addCustomField("questions", json(kbaQuestions)))
                .addDefinition("systemQuestion",
                        newObject("System Question")
                                .addRequireProperty("questionId", "Id of predefined question")
                                .addRequireProperty("answer", "Answer to the referenced question")
                                .addCustomField("additionalProperties", json(false)))
                .addDefinition("userQuestion",
                        newObject("User Question")
                                .addRequireProperty("customQuestion", "Question defined by the user")
                                .addRequireProperty("answer", "Answer to the question")
                                .addCustomField("additionalProperties", json(false)))
                .build();
    }

    private List<Map<String, Object>> convertToCollections(List<KbaQuestion> questions) {
        List<Map<String, Object>> jsonValueList = new ArrayList<>();
        for (KbaQuestion kbaQuestion : questions) {
            Map<String, Object> qMap = convertToCollections(kbaQuestion);
            jsonValueList.add(qMap);
        }
        return jsonValueList;
    }

    private Map<String, Object> convertToCollections(KbaQuestion kbaQuestion) {
        Map<String, Object> results = new HashMap<>();
        results.put("id", kbaQuestion.getId());
        results.put("question", kbaQuestion.questionsAsMap());
        return results;
    }

    @Override
    public StageResponse advance(ProcessContext context, SecurityAnswerDefinitionConfig config)
            throws ResourceException {

        JsonValue kba = context.getInput().get("kba");
        if (kba.isNull()) {
            throw new BadRequestException("KBA has not been specified");
        }

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

}
