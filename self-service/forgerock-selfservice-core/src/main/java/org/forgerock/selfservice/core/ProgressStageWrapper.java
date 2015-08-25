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

package org.forgerock.selfservice.core;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.util.Reject;

/**
 * Wraps the progress stage up with the current configuration which removes
 * the need to pass both the stage and the configuration around.
 *
 * @since 0.1.0
 */
final class ProgressStageWrapper<C extends StageConfig> {

    private final ProgressStage<C> delegatedStage;
    private final C stageConfig;

    ProgressStageWrapper(ProgressStage<C> delegatedStage, C stageConfig) {
        Reject.ifNull(delegatedStage, stageConfig);
        this.delegatedStage = delegatedStage;
        this.stageConfig = stageConfig;
    }

    JsonValue gatherInitialRequirements(ProcessContext context) throws ResourceException {
        return delegatedStage.gatherInitialRequirements(context, stageConfig);
    }

    StageResponse advance(ProcessContext context) throws ResourceException {
        return delegatedStage.advance(context, stageConfig);
    }

    String getName() {
        return stageConfig.getName();
    }

}
