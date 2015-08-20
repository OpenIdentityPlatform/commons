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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;

import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
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
import org.forgerock.selfservice.core.exceptions.IllegalStageTagException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Email stage.
 *
 * @since 0.1.0
 */
public final class EmailStage implements ProgressStage<EmailStageConfig> {

    private static final String VALIDATE_CODE_TAG = "validateCodeTag";

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new email stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public EmailStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, EmailStageConfig config) {
        return RequirementsBuilder
                .newInstance("Reset your password")
                .addRequireProperty("username", "Username (either user Id or email)")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, EmailStageConfig config) throws ResourceException {
        switch (context.getStageTag()) {
        case INITIAL_TAG:
            return sendEmail(context, config);
        case VALIDATE_CODE_TAG:
            return validateCode(context);
        }

        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse sendEmail(final ProcessContext context,
                                    final EmailStageConfig config) throws ResourceException {
        String username = context
                .getInput()
                .get("username")
                .asString();

        if (isEmpty(username)) {
            throw new BadRequestException("username is missing");
        }

        JsonValue user = findUser(context.getHttpContext(), username, config);

        if (user == null) {
            throw new BadRequestException("Unable to find associated account");
        }

        String userId = user
                .get(config.getIdentityIdField())
                .asString();

        final String mail = user
                .get(config.getIdentityEmailField())
                .asString();

        final String code = UUID.randomUUID().toString();

        JsonValue requirements = RequirementsBuilder
                .newInstance("Verify user account")
                .addRequireProperty("code", "Enter code emailed")
                .build();

        SnapshotTokenCallback callback = new SnapshotTokenCallback() {

            @Override
            public void snapshotTokenPreview(ProcessContext context,
                                             String snapshotToken) throws ResourceException {
                sendEmail(context.getHttpContext(), snapshotToken, code, mail, config);
            }

        };

        return StageResponse
                .newBuilder()
                .addState(USER_ID_FIELD, userId)
                .addState("code", code)
                .setStageTag(VALIDATE_CODE_TAG)
                .setRequirements(requirements)
                .setCallback(callback)
                .build();
    }

    private StageResponse validateCode(ProcessContext context) throws ResourceException {
        String originalCode = context.getState("code");

        String submittedCode = context
                .getInput()
                .get("code")
                .asString();

        if (isEmpty(submittedCode)) {
            throw new BadRequestException("Input code is missing");
        }

        if (!originalCode.equals(submittedCode)) {
            throw new BadRequestException("Invalid code");
        }

        return StageResponse
                .newBuilder()
                .build();
    }

    private JsonValue findUser(Context httpContext, String identifier,
                               EmailStageConfig config) throws ResourceException {

        QueryRequest request = Requests
                .newQueryRequest(config.getIdentityServiceUrl())
                .setQueryFilter(
                        QueryFilter.or(
                                QueryFilter.equalTo(new JsonPointer(config.getIdentityIdField()), identifier),
                                QueryFilter.equalTo(new JsonPointer(config.getIdentityEmailField()), identifier)));

        final List<JsonValue> user = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection()) {
            connection.query(httpContext, request, new QueryResourceHandler() {

                @Override
                public boolean handleResource(ResourceResponse resourceResponse) {
                    user.add(resourceResponse.getContent());
                    return true;
                }

            });
        }

        return user.isEmpty() ? null : user.get(0);
    }

    private void sendEmail(Context httpContext, String snapshotToken, String code, String mail,
                           EmailStageConfig config) throws ResourceException {

        String emailUrl = config.getEmailResetUrl() + "&token=" + snapshotToken + "&code=" + code;
        String message = config.getEmailMessage().replace(config.getEmailResetUrlToken(), emailUrl);

        try (Connection connection = connectionFactory.getConnection()) {
            ActionRequest request = Requests
                    .newActionRequest(config.getEmailServiceUrl(), "send")
                    .setContent(
                            json(
                                    object(
                                            field("to", mail),
                                            field("from", config.getEmailFrom()),
                                            field("subject", config.getEmailSubject()),
                                            field("message", message))));

            connection.action(httpContext, request);
        }
    }

    @Override
    public StageType<EmailStageConfig> getStageType() {
        return EmailStageConfig.TYPE;
    }

}
