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

package org.forgerock.selfservice.stages.dynamic;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.config.StageConfigException;

import javax.inject.Inject;

/**
 * Handles the instance retrieval of the progress stage represented by the dynamic stage config. As stage config
 * typing is lost during runtime, this implementation attempts to better report any potential class cast exception
 * that may result due to improper configuration.
 *
 * @since 0.7.0
 */
public final class DynamicConfigVisitorImpl implements DynamicConfigVisitor {

    private final DynamicProgressStageProvider dynamicStageProvider;

    /**
     * Constructs a new dynamic configuration visitor.
     *
     * @param dynamicStageProvider
     *         provider responsible for the retrieval of progress stages
     */
    @Inject
    public DynamicConfigVisitorImpl(DynamicProgressStageProvider dynamicStageProvider) {
        this.dynamicStageProvider = dynamicStageProvider;
    }

    @Override
    public ProgressStageBinder<?> build(DynamicStageConfig config) {
        try {
            // The progress stage must be able to consume a dynamic stage config.
            @SuppressWarnings("unchecked")
            Class<? extends ProgressStage<DynamicStageConfig>> typedProgressStageClass =
                    (Class<? extends ProgressStage<DynamicStageConfig>>) Class
                            .forName(config.getProgressStageClassName())
                            .asSubclass(ProgressStage.class);

            ProgressStage<DynamicStageConfig> stage = dynamicStageProvider.get(typedProgressStageClass);
            return ProgressStageBinder.bind(new ProxyProgressStage(stage), config);

        } catch (ClassNotFoundException cnfE) {
            throw new StageConfigException("Configured progress stage class not found", cnfE);
        } catch (ClassCastException ccE) {
            throw new StageConfigException("Configured class name does not represent a progress stage", ccE);
        }
    }

    /*
     * As stage config typing cannot be declared here, it is handled at runtime.
     * In the case of invalid configuration this may result in a class cast exception.
     * This proxy captures such exceptions and reports more a helpful exception.
     */
    private static final class ProxyProgressStage implements ProgressStage<DynamicStageConfig> {

        private final ProgressStage<DynamicStageConfig> actualStage;

        private ProxyProgressStage(ProgressStage<DynamicStageConfig> actualStage) {
            this.actualStage = actualStage;
        }

        @Override
        public JsonValue gatherInitialRequirements(
                ProcessContext context, DynamicStageConfig config) throws ResourceException {
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
                ProcessContext context, DynamicStageConfig config) throws ResourceException {
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
