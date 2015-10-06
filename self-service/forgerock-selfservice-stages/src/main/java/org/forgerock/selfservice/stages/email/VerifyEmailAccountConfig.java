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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for the email account verification stage.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountConfig extends AbstractEmailVerificationConfig<VerifyEmailAccountConfig> {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "emailValidation";

    /**
     * Constructs a new VerifyEmailAccountConfig.
     * @param emailConfig the email configuration.
     */
    @JsonCreator
    public VerifyEmailAccountConfig(@JsonProperty("email") EmailAccountConfig emailConfig) {
        super(emailConfig);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public VerifyEmailAccountConfig self() {
        return this;
    }
}
