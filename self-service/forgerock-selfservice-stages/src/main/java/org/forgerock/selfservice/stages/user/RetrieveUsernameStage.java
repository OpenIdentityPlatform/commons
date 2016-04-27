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

package org.forgerock.selfservice.stages.user;

import static org.forgerock.selfservice.stages.CommonStateFields.USERNAME_FIELD;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;

/**
 * Stage is responsible for retrieving the username.
 *
 * @since 0.7.0
 */
public final class RetrieveUsernameStage implements ProgressStage<RetrieveUsernameConfig> {

    static final String KEY_ADDITIONS_USERNAME = "userName";

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, RetrieveUsernameConfig config)
            throws ResourceException {
        Reject.ifFalse(context.containsState(USERNAME_FIELD),
                "Retrieve username stage expects user name in the context");

        return RequirementsBuilder.newEmptyRequirements();
    }

    @Override
    public StageResponse advance(ProcessContext context, RetrieveUsernameConfig config) throws ResourceException {
        String userName = context.getState(USERNAME_FIELD).asString();

        context.putSuccessAddition(KEY_ADDITIONS_USERNAME, userName);

        return StageResponse.newBuilder().build();
    }

}
