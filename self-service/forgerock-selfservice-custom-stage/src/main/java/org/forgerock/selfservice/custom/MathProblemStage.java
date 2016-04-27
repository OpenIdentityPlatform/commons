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

package org.forgerock.selfservice.custom;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;

/**
 * Progress stages prompts for the solution to a basic math problem.
 *
 * @since 0.7.0
 */
public final class MathProblemStage implements ProgressStage<MathProblemStageConfig> {

    @Override
    public JsonValue gatherInitialRequirements(
            ProcessContext context, MathProblemStageConfig config) throws ResourceException {
        String problem = String.format("What is %d + %d?", config.getLeftValue(), config.getRightValue());

        return RequirementsBuilder
                .newInstance("Math Problem")
                .addRequireProperty("answer", problem)
                .build();
    }

    @Override
    public StageResponse advance(
            ProcessContext context, MathProblemStageConfig config) throws ResourceException {

        JsonValue answer = context.getInput().get("answer");

        if (answer.isNull()) {
            throw new BadRequestException("Required answer is missing");
        }

        int answerValue;

        try {
            answerValue = Integer.parseInt(answer.asString());
        } catch (NumberFormatException nfE) {
            throw new BadRequestException("Supplied answer is not a number", nfE);
        }

        if (config.getLeftValue() + config.getRightValue() != answerValue) {
            throw new BadRequestException("Supplied answer is wrong");
        }

        return StageResponse.newBuilder().build();
    }

}
