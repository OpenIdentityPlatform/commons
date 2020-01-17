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
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.config.StageConfigException;
import org.forgerock.util.Reject;

/**
 * Progress stage binder is responsible for creating bindings between the stage configs and their consuming stages.
 *
 * @since 0.8.0
 */
final class ProgressStageBinder {

    private final ProgressStageProvider provider;
    private final ClassLoader classLoader;

    ProgressStageBinder(ProgressStageProvider provider, ClassLoader classLoader) {
        this.provider = provider;
        this.classLoader = classLoader;
    }

    ProgressStageBinding<?> getBinding(StageConfig config) {
        Reject.ifNull(config, "Stage config instance is expected");
        Reject.ifNull(config.getProgressStageClassName(), "Progress stage class name is expected");

        try {
            // Safe cast as all progress stages can consume stage configs.
            @SuppressWarnings("unchecked")
            Class<? extends ProgressStage<StageConfig>> typedProgressStageClass =
                    (Class<? extends ProgressStage<StageConfig>>) Class
                            .forName(config.getProgressStageClassName(), true, classLoader)
                            .asSubclass(ProgressStage.class);

            ProgressStage<StageConfig> stage = provider.get(typedProgressStageClass);
            return ProgressStageBinding.bind(new ProxyProgressStage(stage), config);

        } catch (ClassNotFoundException cnfE) {
            throw new StageConfigException("Configured progress stage class not found", cnfE);
        } catch (ClassCastException ccE) {
            throw new StageConfigException("Configured class name does not represent a progress stage", ccE);
        }
    }

    /*
     * Due to reflection stage config typing cannot be defined here, it is handled at runtime.
     * In the case of invalid configuration this may result in a class cast exception.
     * This proxy captures such exceptions and reports more a helpful exception.
     */
    private static final class ProxyProgressStage implements ProgressStage<StageConfig> {

        private final ProgressStage<StageConfig> actualStage;

        private ProxyProgressStage(ProgressStage<StageConfig> actualStage) {
            this.actualStage = actualStage;
        }

        @Override
        public JsonValue gatherInitialRequirements(
                ProcessContext context, StageConfig config) throws ResourceException {
            try {
                return actualStage.gatherInitialRequirements(context, config);
            } catch (ClassCastException ccE) {
                throw new InternalServerErrorException(
                        "Configured progress stage is unable to consume config of type "
                                + config.getClass().getName(), ccE);
            }
        }

        @Override
        public StageResponse advance(
                ProcessContext context, StageConfig config) throws ResourceException {
            try {
                return actualStage.advance(context, config);
            } catch (ClassCastException ccE) {
                throw new InternalServerErrorException(
                        "Configured progress stage is unable to consume config of type "
                                + config.getClass().getName(), ccE);
            }
        }

    }

}
