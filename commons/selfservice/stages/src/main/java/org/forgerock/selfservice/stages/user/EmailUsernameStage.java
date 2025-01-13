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
package org.forgerock.selfservice.stages.user;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USERNAME_FIELD;
import static org.forgerock.selfservice.stages.utils.LocaleUtils.getTranslationFromLocaleMap;

import jakarta.inject.Inject;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Stage is responsible for retrieving the user name.
 *
 * @since 0.8.0
 */
public final class EmailUsernameStage implements ProgressStage<EmailUsernameConfig> {

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public EmailUsernameStage(@SelfService ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, EmailUsernameConfig config)
            throws ResourceException {
        Reject.ifNull(config.getEmailServiceUrl(), "Email service url should be configured");
        Reject.ifNull(config.getMessageTranslations(), "Email message should be configured");
        Reject.ifNull(config.getSubjectTranslations(), "Email subject should be configured");
        Reject.ifNull(config.getUsernameToken(), "User name token should be configured");

        Reject.ifFalse(context.containsState(USERNAME_FIELD),
                "Retrieve username stage expects user Id in the context");
        Reject.ifFalse(context.containsState(EMAIL_FIELD),
                "Retrieve username stage expects email Id in the context");

        return RequirementsBuilder.newEmptyRequirements();
    }

    @Override
    public StageResponse advance(ProcessContext context, final EmailUsernameConfig config)
            throws ResourceException {
        String email = context.getState(EMAIL_FIELD).asString();
        String username = context.getState(USERNAME_FIELD).asString();

        sendEmail(context, username, email, config);

        return StageResponse.newBuilder().build();
    }

    private void sendEmail(ProcessContext processContext, String username, String email, EmailUsernameConfig config)
            throws ResourceException {
        PreferredLocales preferredLocales = processContext.getRequest().getPreferredLocales();
        String subjectText = getTranslationFromLocaleMap(preferredLocales, config.getSubjectTranslations());
        String bodyText = getTranslationFromLocaleMap(preferredLocales, config.getMessageTranslations());

        bodyText = bodyText.replace(config.getUsernameToken(), username);

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
