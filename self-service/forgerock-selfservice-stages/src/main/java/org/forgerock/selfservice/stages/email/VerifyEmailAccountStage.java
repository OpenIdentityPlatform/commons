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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.stages.SelfService;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;

import javax.inject.Inject;

/**
 * Having retrieved the email address in response to the initial requirements, verifies the
 * validity of the email address with the user who submitted the requirements via an email flow.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountStage extends AbstractEmailVerificationStage<VerifyEmailAccountConfig> {

    /**
     * Constructs a new stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public VerifyEmailAccountStage(@SelfService ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, VerifyEmailAccountConfig config) {
        return RequirementsBuilder
                .newInstance("Verify your email address")
                .addRequireProperty("mail", "Email address")
                .build();
    }

    @Override
    protected String getEmailAddress(ProcessContext context, VerifyEmailAccountConfig config,
            StageResponse.Builder builder) throws ResourceException {
        String email = context
                .getInput()
                .get("mail")
                .asString();

        if (isEmpty(email)) {
            throw new BadRequestException("mail is missing");
        }

        return email;
    }

}
