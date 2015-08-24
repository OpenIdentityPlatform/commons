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

package org.forgerock.selfservice.stages.reset;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;

import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.Reject;

import javax.inject.Inject;

/**
 * The reset password stage.
 *
 * @since 0.1.0
 */
public final class ResetStage implements ProgressStage<ResetStageConfig> {

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new reset stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public ResetStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
                                               ResetStageConfig config) throws ResourceException {
        Reject.ifFalse(context.containsState(USER_ID_FIELD), "Reset stage expects userId in the context");

        return RequirementsBuilder
                .newInstance("Reset password")
                .addRequireProperty("password", "Password")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, ResetStageConfig config) throws ResourceException {
        String userId = context.getState(USER_ID_FIELD);
        String password = context
                .getInput()
                .get("password")
                .asString();

        if (isEmpty(password)) {
            throw new BadRequestException("password is missing from input");
        }

        JsonValue user = readUser(context.getHttpContext(), userId, config);
        user.put(new JsonPointer(config.getIdentityPasswordField()), password);
        updateUser(context.getHttpContext(), userId, user, config);

        return StageResponse
                .newBuilder()
                .build();
    }

    private JsonValue readUser(Context httpContext, String userId,
                               ResetStageConfig config) throws ResourceException {
        try (Connection connection = connectionFactory.getConnection()) {
            ReadRequest request = Requests.newReadRequest(config.getIdentityServiceUrl(), userId);
            ResourceResponse response = connection.read(httpContext, request);
            return response.getContent();
        }
    }

    private void updateUser(Context httpContext, String userId, JsonValue user,
                            ResetStageConfig config) throws ResourceException {
        try (Connection connection = connectionFactory.getConnection()) {
            UpdateRequest request = Requests.newUpdateRequest(config.getIdentityServiceUrl(), userId, user);
            connection.update(httpContext, request);
        }
    }

}
