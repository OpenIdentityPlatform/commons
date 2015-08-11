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

package org.forgerock.selfservice.stages.email;

import static org.forgerock.selfservice.core.ServiceUtils.EMPTY_TAG;

import org.forgerock.http.context.RootContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.core.exceptions.IllegalInputException;
import org.forgerock.selfservice.core.exceptions.IllegalStageTagException;
import org.forgerock.selfservice.core.exceptions.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotAuthor;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.query.QueryFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Email stage.
 *
 * @since 0.1.0
 */
public class EmailStage implements ProgressStage<EmailStageConfig> {

    private static final String VALIDATE_LINK_TAG = "validateLinkTag";

    private final ConnectionFactory connectionFactory;

    public EmailStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, EmailStageConfig config) {
        return RequirementsBuilder
                .newInstance("Reset your password")
                .addRequireProperty("userId", "Identifier for the user")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, EmailStageConfig config,
                                 SnapshotAuthor snapshotAuthor) throws IllegalInputException {
        switch (context.getStageTag()) {
        case EMPTY_TAG:
            return sendEmail(context, config, snapshotAuthor);
        case VALIDATE_LINK_TAG:
            return validateLink(context);
        }

        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse sendEmail(ProcessContext context,
                                    EmailStageConfig config,
                                    SnapshotAuthor snapshotAuthor) throws IllegalInputException {
        String userId = context
                .getInput()
                .get("userId")
                .asString();

        if (userId == null || userId.isEmpty()) {
            throw new IllegalInputException("userId is missing");
        }

        JsonValue user = findUser(userId, config);

        if (user == null) {
            throw new IllegalInputException("Unknown user");
        }

        userId = user
                .get(config.getIdentityIdField())
                .asString();
        String mail = user
                .get(config.getIdentityEmailField())
                .asString();
        String code = UUID.randomUUID().toString();

        context = ProcessContext
                .newBuilder(context)
                .addState("userId", userId)
                .addState("mail", mail)
                .addState("code", code)
                .setStageTag(VALIDATE_LINK_TAG)
                .build();

        String snapshotToken = snapshotAuthor.captureSnapshotOf(context);
        System.out.printf("Email sent for %s to %s with token %s and code %s\n", userId, mail, snapshotToken, code);

        JsonValue requirements = RequirementsBuilder
                .newInstance("Verify email address")
                .addRequireProperty("code", "Enter code emailed to address provided")
                .build();

        return StageResponse
                .newBuilder()
                .setRequirements(requirements)
                .setStageTag(VALIDATE_LINK_TAG)
                .build();
    }

    private StageResponse validateLink(ProcessContext context) throws IllegalInputException {
        String userId = context.getState("userId");
        String mail = context.getState("mail");
        String code = context.getState("code");

        if (userId == null || userId.isEmpty()) {
            throw new IllegalInputException("Missing user Id");
        }

        if (mail == null || mail.isEmpty()) {
            throw new IllegalInputException("Missing email address");
        }

        if (code == null || code.isEmpty()) {
            throw new IllegalInputException("Missing code");
        }

        String submittedCode = context
                .getInput()
                .get("code")
                .asString();

        if (submittedCode == null || submittedCode.isEmpty()) {
            throw new IllegalInputException("Input code is missing");
        }

        if (!code.equals(submittedCode)) {
            throw new IllegalInputException("Invalid code");
        }

        System.out.println("Token valid!");

        return StageResponse
                .newBuilder()
                .build();
    }

    private JsonValue findUser(String identifier, EmailStageConfig config) {
        try {
            Connection connection = connectionFactory.getConnection();

            QueryRequest request = Requests.newQueryRequest(config.getIdentityServiceUrl());

            request.setQueryFilter(
                    QueryFilter.or(
                            QueryFilter.equalTo(new JsonPointer(config.getIdentityIdField()), identifier),
                            QueryFilter.equalTo(new JsonPointer(config.getIdentityEmailField()), identifier)));

            final List<JsonValue> user = new ArrayList<>();
            connection.query(new RootContext(), request, new QueryResourceHandler() {

                @Override
                public boolean handleResource(ResourceResponse resourceResponse) {
                    user.add(resourceResponse.getContent());
                    return true;
                }

            });

            if (user.size() > 1) {
                throw new StageConfigException("More than one user identified");
            }

            return user.isEmpty() ? null : user.get(0);
        } catch (ResourceException rE) {
            throw new StageConfigException(rE.getMessage());
        }
    }

    @Override
    public StageType<EmailStageConfig> getStageType() {
        return EmailStageConfig.TYPE;
    }

}
