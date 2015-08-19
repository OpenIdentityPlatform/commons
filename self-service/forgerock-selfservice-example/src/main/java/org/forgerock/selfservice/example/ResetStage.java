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

package org.forgerock.selfservice.example;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.Reject;

/**
 * The reset password stage.
 *
 * @since 0.1.0
 */
public final class ResetStage implements ProgressStage<ResetConfig> {

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
                                               ResetConfig config) throws ResourceException {
        Reject.ifFalse(context.containsState("userId"), "Reset stage expects userId in the context");

        return RequirementsBuilder
                .newInstance("Reset password")
                .addRequireProperty("password", "Password")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, ResetConfig config) throws ResourceException {
        String userId = context.getState("userId");
        String password = context
                .getInput()
                .get("password")
                .asString();

        if (isEmpty(password)) {
            throw new BadRequestException("password is missing from input");
        }

        System.out.println("Reset password for " + userId + " to " + password);
        // todo: call out to CREST

        return StageResponse
                .newBuilder()
                .build();
    }

    @Override
    public StageType<ResetConfig> getStageType() {
        return ResetConfig.TYPE;
    }

}
