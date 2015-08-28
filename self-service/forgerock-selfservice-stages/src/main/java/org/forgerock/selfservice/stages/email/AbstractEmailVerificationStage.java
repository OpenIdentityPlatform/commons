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
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.exceptions.IllegalStageTagException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;

import javax.inject.Inject;
import java.util.UUID;

/**
 * This stage is intended to be sub classed to define the initial client interaction
 * in order to retrieve an email address. The account associated with the email address
 * is verified via an email link callback.
 *
 * @since 0.1.0
 */
abstract class AbstractEmailVerificationStage<C extends AbstractEmailVerificationConfig> implements ProgressStage<C> {

    private static final String VALIDATE_CODE_TAG = "validateCode";

    protected final ConnectionFactory connectionFactory;

    /**
     * Constructs a new email stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public AbstractEmailVerificationStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public final StageResponse advance(ProcessContext context, C config) throws ResourceException {
        switch (context.getStageTag()) {
        case INITIAL_TAG:
            return sendEmail(context, config);
        case VALIDATE_CODE_TAG:
            return validateCode(context);
        }

        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse sendEmail(final ProcessContext context,
                                    final C config) throws ResourceException {

        StageResponse.Builder builder = StageResponse.newBuilder();
        final String mail = getEmailAddress(context, config, builder);
        final String code = UUID.randomUUID().toString();

        JsonValue requirements = RequirementsBuilder
                .newInstance("Verify emailed code")
                .addRequireProperty("code", "Enter code emailed")
                .build();

        SnapshotTokenCallback callback = new SnapshotTokenCallback() {

            @Override
            public void snapshotTokenPreview(ProcessContext context,
                                             String snapshotToken) throws ResourceException {
                sendEmail(context.getHttpContext(), snapshotToken, code, mail, config);
            }

        };

        return builder
                .addState("code", code)
                .addState(EMAIL_FIELD, mail)
                .setStageTag(VALIDATE_CODE_TAG)
                .setRequirements(requirements)
                .setCallback(callback)
                .build();
    }

    /**
     * Given the current context containing input from the previous requirements, resolve the email address.
     *
     * @param context
     *         the current context
     * @param config
     *         the stage config
     * @param builder
     *         the stage response builder should it be required
     *
     * @return the email address
     *
     * @throws ResourceException
     *         if some expected state or input is invalid
     */
    protected abstract String getEmailAddress(ProcessContext context, C config,
                                              StageResponse.Builder builder) throws ResourceException;

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

    private void sendEmail(Context httpContext, String snapshotToken, String code,
                           String email, C config) throws ResourceException {

        String emailUrl = config.getEmailVerificationLink() + "&token=" + snapshotToken + "&code=" + code;
        String message = config.getEmailMessage().replace(config.getEmailVerificationLinkToken(), emailUrl);

        try (Connection connection = connectionFactory.getConnection()) {
            ActionRequest request = Requests
                    .newActionRequest(config.getEmailServiceUrl(), "send")
                    .setContent(
                            json(
                                    object(
                                            field("to", email),
                                            field("from", config.getEmailFrom()),
                                            field("subject", config.getEmailSubject()),
                                            field("message", message))));

            connection.action(httpContext, request);
        }
    }

}
