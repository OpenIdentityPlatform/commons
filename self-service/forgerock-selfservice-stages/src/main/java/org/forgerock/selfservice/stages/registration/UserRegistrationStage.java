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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;

import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.Reject;

import javax.inject.Inject;

/**
 * Stage is responsible for request a new user json representation and passing it off
 * to the underlying service. It expects the "mail" field to be populated in the context
 * which it uses to verify against the email address specified in the passed user object.
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
    public UserRegistrationStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
                                               UserRegistrationConfig config) throws ResourceException {
        Reject.ifFalse(context.containsState(EMAIL_FIELD), "User registration stage expects mail in the context");

        return RequirementsBuilder
                .newInstance("New user details")
                .addRequireProperty(USER_ID_FIELD, "New user Id")
                .addRequireProperty("user", "object", "User details")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, UserRegistrationConfig config) throws ResourceException {
        String userId = context
                .getInput()
                .get(USER_ID_FIELD)
                .asString();

        if (isEmpty(userId)) {
            throw new BadRequestException("userId has not been specified");
        }

        JsonValue user = context
                .getInput()
                .get("user");

        if (user.isNull()) {
            throw new BadRequestException("user has not been specified");
        }

        String email = context.getState(EMAIL_FIELD);
        user.put(new JsonPointer(config.getIdentityEmailField()), email);
        createUser(context.getHttpContext(), userId, user, config);

        return StageResponse
                .newBuilder()
                .build();
    }

    private void createUser(Context httpContext, String userId, JsonValue user,
                            UserRegistrationConfig config) throws ResourceException {
        try (Connection connection = connectionFactory.getConnection()) {
            CreateRequest request = Requests.newCreateRequest(config.getIdentityServiceUrl(), userId, user);
            connection.create(httpContext, request);
        }
    }

}
