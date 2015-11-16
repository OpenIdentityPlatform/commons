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
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;
import static org.forgerock.selfservice.stages.utils.LocaleUtils.getTranslationFromLocaleMap;

import org.forgerock.json.JsonPointer;
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
import org.forgerock.selfservice.core.IllegalStageTagException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenCallback;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Having retrieved the email address from the context or in response to the initial requirements, verifies the
 * validity of the email address with the user who submitted the requirements via an email flow.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountStage implements ProgressStage<VerifyEmailAccountConfig> {

    static final String REQUIREMENT_KEY_EMAIL = "mail";

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
        Reject.ifNull(config.getEmailServiceUrl(), "Email service url should be configured");
        Reject.ifNull(config.getMessageTranslations(), "Email message should be configured");
        Reject.ifNull(config.getSubjectTranslations(), "Email subject should be configured");
        Reject.ifNull(config.getVerificationLink(), "Verification link should be configured");
        Reject.ifNull(config.getVerificationLinkToken(), "Verification link token should be configured");
        Reject.ifNull(config.getIdentityEmailField(), "Identity email field should be configured");

        if (context.containsState(EMAIL_FIELD)) {
            return RequirementsBuilder
                    .newEmptyRequirements();
        }

        return RequirementsBuilder
                .newInstance("Verify your email address")
                .addRequireProperty(REQUIREMENT_KEY_EMAIL, "Email address")
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
        final String mail = getEmailAsString(context, config);

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

    private String getEmailAsString(ProcessContext context, VerifyEmailAccountConfig config)
            throws BadRequestException {
        if (!context.containsState(EMAIL_FIELD)) {
            JsonValue email = context.getInput().get(REQUIREMENT_KEY_EMAIL);
            String mail = getEmailAsString(email);
            context.putState(EMAIL_FIELD, mail);

            updateUserIfAvailable(context, config, mail);

            return mail;
        }

        JsonValue email = context.getState(EMAIL_FIELD);
        return getEmailAsString(email);
    }

    private void updateUserIfAvailable(ProcessContext context, VerifyEmailAccountConfig config, String mail)
            throws BadRequestException {
        if (context.containsState(USER_FIELD)) {
            JsonValue user = context.getState(USER_FIELD);
            JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser == null) {
                user.put(new JsonPointer(config.getIdentityEmailField()), mail);
            } else if (!emailFieldUser.asString().equalsIgnoreCase(mail)) {
                throw new BadRequestException("Email address mismatch");
            }
        }
    }

    private String getEmailAsString(JsonValue email) throws BadRequestException {
        if (email == null || isEmpty(email.asString())) {
            throw new BadRequestException("mail should not be empty");
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

        PreferredLocales preferredLocales = processContext.getRequest().getPreferredLocales();
        String subjectText = getTranslationFromLocaleMap(preferredLocales, config.getSubjectTranslations());
        String bodyText = getTranslationFromLocaleMap(preferredLocales, config.getMessageTranslations());

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
