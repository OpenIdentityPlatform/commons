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

package org.forgerock.selfservice.stages.terms;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.selfservice.stages.utils.LocaleUtils.getTranslationFromLocaleMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Presents a Terms and Condition blurb to the user for acceptance.
 *
 * @since 21.0.0
 */
public final class TermsAndConditionsStage implements ProgressStage<TermsAndConditionsConfig> {

    private static final String ACCEPT = "accept";
    private static final String TERMS = "terms";

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, TermsAndConditionsConfig config) {
        Reject.ifNull(config.getTermsTranslations(), "Terms and Condition text should be configured");

        PreferredLocales preferredLocales = context.getRequest().getPreferredLocales();
        String terms = getTranslationFromLocaleMap(preferredLocales, config.getTermsTranslations());

        return RequirementsBuilder
                .newInstance("Accept Terms and Condition")
                .addRequireProperty(ACCEPT, "Accept")
                .addCustomField(TERMS, json(terms))
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, TermsAndConditionsConfig config) throws ResourceException {
        String accepted = context
                .getInput()
                .get(ACCEPT)
                .asString();

        if (!Boolean.valueOf(accepted)) {
            throw new BadRequestException("Must accept terms and conditions");
        }

        return StageResponse
                .newBuilder()
                .build();
    }

}
