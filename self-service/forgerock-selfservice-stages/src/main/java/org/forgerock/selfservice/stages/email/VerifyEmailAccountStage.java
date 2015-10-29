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

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.exceptions.IllegalStageTagException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.selfservice.stages.SelfService;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.i18n.PreferredLocales;

import javax.inject.Inject;

/**
 * Having retrieved the email address from the context or in response to the initial requirements, verifies the
 * validity of the email address with the user who submitted the requirements via an email flow.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountStage implements ProgressStage<VerifyEmailAccountConfig> {

    private static final String VALIDATE_CODE_TAG = "validateCode";

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public VerifyEmailAccountStage(@SelfService ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, VerifyEmailAccountConfig config) {
        if (context.containsState(EMAIL_FIELD)) {
            return RequirementsBuilder
                    .newEmptyRequirements();
        }

        return RequirementsBuilder
                .newInstance("Verify your email address")
                .addRequireProperty("mail", "Email address")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, VerifyEmailAccountConfig config) throws ResourceException {
        switch (context.getStageTag()) {
        case INITIAL_TAG:
            return sendEmail(context, config);
        case VALIDATE_CODE_TAG:
            return validateCode(context);
        }

        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse sendEmail(ProcessContext context, final VerifyEmailAccountConfig config)
            throws ResourceException {
        final String mail = getEmailAsString(context);

        final String code = UUID.randomUUID().toString();
        context.putState("code", code);

        JsonValue requirements = RequirementsBuilder
                .newInstance("Verify emailed code")
                .addRequireProperty("code", "Enter code emailed")
                .build();

        SnapshotTokenCallback callback = new SnapshotTokenCallback() {

            @Override
            public void snapshotTokenPreview(ProcessContext context, String snapshotToken)
                    throws ResourceException {
                sendEmail(context, snapshotToken, code, mail, config);
            }

        };

        return StageResponse.newBuilder()
                .setStageTag(VALIDATE_CODE_TAG)
                .setRequirements(requirements)
                .setCallback(callback)
                .build();
    }

    private String getEmailAsString(ProcessContext context) throws InternalServerErrorException {
        if (!context.containsState(EMAIL_FIELD)) {
            JsonValue email = context.getInput().get("mail");
            String mail = getEmailAsString(email);
            context.putState(EMAIL_FIELD, mail);
            return mail;
        }

        JsonValue email = context.getState(EMAIL_FIELD);
        return getEmailAsString(email);
    }

    private String  getEmailAsString(JsonValue email) throws InternalServerErrorException {
        if (email == null || isEmpty(email.asString())) {
            throw new InternalServerErrorException("mail should not be empty");
        }
        return email.asString();
    }

    private StageResponse validateCode(ProcessContext context) throws ResourceException {
        String originalCode = context
                .getState("code")
                .asString();

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

    private void sendEmail(ProcessContext processContext, String snapshotToken, String code,
                           String email, VerifyEmailAccountConfig config) throws ResourceException {

        String emailUrl = config.getVerificationLink() + "&token=" + snapshotToken + "&code=" + code;
        Map<Locale, String> messageMap = config.getMessageTranslations();
        Map<Locale, String> subjectMap = config.getSubjectTranslations();

        PreferredLocales preferredLocales = processContext.getPreferredLocales();
        if (preferredLocales == null) {
            throw ResourceException.newResourceException(ResourceException.BAD_REQUEST,
                    "No locales available in the context");
        }

        String subjectText = null;
        String bodyText = null;
        try {
            subjectText = preferredLocales.getTranslationFromLocaleMap(subjectMap);
        } catch (IllegalArgumentException iae) {
            throw ResourceException.newResourceException(ResourceException.BAD_REQUEST,
                    "No translations available for email subject");
        }

        try {
            bodyText = preferredLocales.getTranslationFromLocaleMap(messageMap);
        } catch (IllegalArgumentException iae) {
            throw ResourceException.newResourceException(ResourceException.BAD_REQUEST,
                    "No translations available for email body");
        }

        bodyText = bodyText.replace(config.getVerificationLinkToken(), emailUrl);

        try (Connection connection = connectionFactory.getConnection()) {
            ActionRequest request = Requests
                    .newActionRequest(config.getEmailServiceUrl(), "send")
                    .setContent(
                            json(
                                    object(
                                            field("to", email),
                                            field("from", config.getFrom()),
                                            field("subject", subjectText),
                                            field("type", config.getMimeType()),
                                            field("body", bodyText))));

            connection.action(processContext.getRequestContext(), request);
        }
    }

}
