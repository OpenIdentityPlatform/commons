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

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.core.exceptions.IllegalInputException;
import org.forgerock.selfservice.core.snapshot.SnapshotAuthor;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;

/**
 * The reset password stage.
 *
 * @since 0.1.0
 */
public class ResetStage implements ProgressStage<ResetConfig> {

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
                                               ResetConfig config) throws IllegalInputException {
        return RequirementsBuilder
                .newInstance("Reset password")
                .addRequireProperty("password", "Password")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, ResetConfig config,
                                 SnapshotAuthor snapshotAuthor) throws IllegalInputException {

        String emailAddress = context.getState("mail");

        if (emailAddress == null || emailAddress.isEmpty()) {
            throw new IllegalInputException("Missing email address");
        }

        String password = context.getInput().get("password").asString();

        if (password == null || password.isEmpty()) {
            throw new IllegalInputException("Missing password");
        }

        System.out.println("Reset password for " + emailAddress + " to " + password);

        return StageResponse
                .newBuilder()
                .build();
    }

    @Override
    public StageType<ResetConfig> getStageType() {
        return ResetConfig.TYPE;
    }

}
