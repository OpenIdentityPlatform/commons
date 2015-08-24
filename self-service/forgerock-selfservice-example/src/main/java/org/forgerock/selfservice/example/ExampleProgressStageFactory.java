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

package org.forgerock.selfservice.example;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.exceptions.StageConfigException;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountStage;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdStage;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationStage;
import org.forgerock.selfservice.stages.reset.ResetStage;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic progress stage factory.
 *
 * @since 0.1.0
 */
final class ExampleProgressStageFactory implements ProgressStageFactory {

    private final Map<Class<? extends StageConfig>, ProgressStage<?>> progressStages;

    /**
     * Creates a new basic progress stage factory.
     */
    ExampleProgressStageFactory(ConnectionFactory connectionFactory) {
        progressStages = new HashMap<>();
        safePut(VerifyEmailAccountConfig.class, new VerifyEmailAccountStage(connectionFactory));
        safePut(VerifyUserIdConfig.class, new VerifyUserIdStage(connectionFactory));
        safePut(ResetStageConfig.class, new ResetStage(connectionFactory));
        safePut(UserRegistrationConfig.class, new UserRegistrationStage(connectionFactory));
    }

    /*
     * The generics ensure the expected stage config is enforced at compile time.
     */
    private <C extends StageConfig> void safePut(Class<C> expectedConfigType, ProgressStage<C> stage) {
        progressStages.put(expectedConfigType, stage);
    }

    @Override
    public <C extends StageConfig> ProgressStage<C> get(C config) {
        ProgressStage<?> untypedStage = progressStages.get(config.getClass());

        if (untypedStage == null) {
            throw new StageConfigException("Unable to find matching stage for config " + config.getName());
        }

        @SuppressWarnings("unchecked") // Type safety is enforced by safePut
        ProgressStage<C> typedStage = (ProgressStage<C>) untypedStage;
        return typedStage;
    }

}
