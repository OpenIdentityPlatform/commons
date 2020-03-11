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

package org.forgerock.selfservice.stages.registration;

import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

import javax.inject.Inject;

/**
 * Stage is responsible for registering the user supplied data using the underlying service.
 * It expects the "user" and userId fields to be populated in the context.
 *
 * @since 0.1.0
 */
public final class UserRegistrationStage implements ProgressStage<UserRegistrationConfig> {

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new user registration stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public UserRegistrationStage(@SelfService ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
            UserRegistrationConfig config) throws ResourceException {
        Reject.ifFalse(context.containsState(USER_FIELD),
                "User registration stage expects user in the context");

        return RequirementsBuilder
                .newEmptyRequirements();
    }

    @Override
    public StageResponse advance(ProcessContext context, UserRegistrationConfig config) throws ResourceException {

        JsonValue user = context.getState(USER_FIELD);
        createUser(context.getRequestContext(), user, config);

        return StageResponse
                .newBuilder()
                .build();
    }

    private void createUser(Context requestContext, JsonValue user,
            UserRegistrationConfig config) throws ResourceException {
        try (Connection connection = connectionFactory.getConnection()) {
            CreateRequest request = Requests.newCreateRequest(config.getIdentityServiceUrl(), user);
            connection.create(requestContext, request);
        }
    }

}
