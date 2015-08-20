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
import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.stages.email.EmailStage;
import org.forgerock.selfservice.stages.email.EmailStageConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.reset.ResetStage;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic progress stage factory.
 *
 * @since 0.1.0
 */
final class ExampleProgressStageFactory implements ProgressStageFactory {

    private final Map<StageType<?>, ProgressStage<?>> progressStages;

    /**
     * Creates a new basic progress stage factory.
     */
    ExampleProgressStageFactory(ConnectionFactory connectionFactory) {
        progressStages = new HashMap<>();
        progressStages.put(EmailStageConfig.TYPE, new EmailStage(connectionFactory));
        progressStages.put(ResetStageConfig.TYPE, new ResetStage(connectionFactory));
    }

    @Override
    public ProgressStage<?> get(StageType<?> type) {
        return progressStages.get(type);
    }

}
