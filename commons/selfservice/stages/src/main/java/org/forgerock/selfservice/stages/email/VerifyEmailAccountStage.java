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
 * Portions copyright 2024 3A Systems LLC.
 */

package org.forgerock.selfservice.stages.email;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.QUERYSTRING_PARAMS_FIELD;
import static org.forgerock.selfservice.stages.utils.LocaleUtils.getTranslationFromLocaleMap;

import org.forgerock.http.MutableUri;
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
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.UUID;

/**
 * Having retrieved the email address from the context or in response to the initial requirements, verifies the
 * validity of the email address with the user who submitted the requirements via an email flow.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountStage implements ProgressStage<VerifyEmailAccountConfig> {

    private static final Logger logger = LoggerFactory.getLogger(VerifyEmailAccountStage.class);

    static final String REQUIREMENT_KEY_EMAIL = "mail";
    static final String REQUIREMENT_KEY_QUERYSTRING_PARAMS = "querystringParams";

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

        if (context.containsState(EMAIL_FIELD) && context.containsState(REQUIREMENT_KEY_QUERYSTRING_PARAMS)) {
            return RequirementsBuilder
                    .newEmptyRequirements();
        }

        RequirementsBuilder reqBuilder = RequirementsBuilder
                .newInstance("Verify your email address");

        if (!context.containsState(EMAIL_FIELD)) {
            reqBuilder
                .addRequireProperty(REQUIREMENT_KEY_EMAIL, "Email address");
        }

        if (!context.containsState(QUERYSTRING_PARAMS_FIELD)) {
            reqBuilder
                .addProperty(REQUIREMENT_KEY_QUERYSTRING_PARAMS, "hidden", "Querystring params");
        }

        return reqBuilder
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
        if (!context.containsState(QUERYSTRING_PARAMS_FIELD) && context.getInput() != null)
        {
            JsonValue querystringParams = context
                .getInput()
                .get(REQUIREMENT_KEY_QUERYSTRING_PARAMS);

            if (querystringParams != null && querystringParams.asString() != null)
                context.putState(QUERYSTRING_PARAMS_FIELD, querystringParams.asString());
        }

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

        // Generate the verification URL
        String emailUrl = config.getVerificationLink() + "&token=" + snapshotToken + "&code=" + code;

        // Apply the stored querystring parameters, if any
        String modifiedEmailUrl = emailUrl;
        if (processContext.getState(QUERYSTRING_PARAMS_FIELD) != null)
        {
            try {
                String querystringParams = processContext
                        .getState(QUERYSTRING_PARAMS_FIELD)
                        .asString();
                if (querystringParams != null && querystringParams.length() > 0)
                {
                    // Parse the stored query string and remove any unwanted fields, such as realm, token or code
                    Hashtable<String, String> querystringParamsMap = parseQueryString(querystringParams);
                    querystringParamsMap.remove("realm");
                    querystringParamsMap.remove("token");
                    querystringParamsMap.remove("code");

                    // Concat the configuration and the stored query string parameter maps
                    MutableUri emailUri = new MutableUri(emailUrl);
                    Hashtable<String, String> paramsToPassMap = new Hashtable<String, String>();
                    paramsToPassMap.putAll(parseQueryString(emailUri.getRawQuery()));
                    paramsToPassMap.putAll(querystringParamsMap);

                    // Set the new URI querystring parameters by joining the map items
                    String querystringToPass = joinQueryString(paramsToPassMap);

                    emailUri.setRawQuery(querystringToPass);
                    modifiedEmailUrl = emailUri.toString();
                }
            }
            catch (URISyntaxException e)
            {
                logger.error("sendEmail - Error adding querystringParams to emailUrl", e);
            }
        }

        PreferredLocales preferredLocales = processContext.getRequest().getPreferredLocales();
        String subjectText = getTranslationFromLocaleMap(preferredLocales, config.getSubjectTranslations());
        String bodyText = getTranslationFromLocaleMap(preferredLocales, config.getMessageTranslations());
        bodyText = bodyText.replace(config.getVerificationLinkToken(), modifiedEmailUrl);

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

    private static Hashtable<String, String> parseQueryString(String query) {
        Hashtable<String, String> params = new Hashtable<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0];
                if (key.length() > 0)
                {
                    String value = keyValue.length > 1 ? keyValue[1] : "";
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    private static String joinQueryString(Hashtable<String, String> dict) {
        String output = new String();
        for (String key : dict.keySet()) {
            output = output + "&" + key + "=" + dict.get(key);
        }
        return output.length() > 0 ? output.substring(1, output.length()): output;
    }
}
