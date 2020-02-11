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

package org.forgerock.selfservice.stages.kba;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for the KBA Security Answer Definition Stage.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionConfig extends AbstractKbaStageConfig<SecurityAnswerDefinitionConfig> {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "kbaSecurityAnswerDefinitionStage";

    private int numberOfAnswersUserMustSet;

    /**
     * Creates a new SecurityAnswerDefinitionConfig.
     *
     * @param kbaConfig
     *         the kba configuration
     */
    public SecurityAnswerDefinitionConfig(@JsonProperty("kbaConfig") KbaConfig kbaConfig) {
        super(kbaConfig);
    }

    /**
     * Gets the number of answers that user must set.
     *
     * @return the number of answers to be set
     */
    public int getNumberOfAnswersUserMustSet() {
        return numberOfAnswersUserMustSet;
    }

    /**
     * Sets the number of answers that user must set.
     *
     * @param numberOfAnswersUserMustSet
     *         the number of answers to be set
     *
     * @return this config instance
     */
    public SecurityAnswerDefinitionConfig setNumberOfAnswersUserMustSet(int numberOfAnswersUserMustSet) {
        this.numberOfAnswersUserMustSet = numberOfAnswersUserMustSet;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return SecurityAnswerDefinitionStage.class.getName();
    }

    @Override
    public SecurityAnswerDefinitionConfig self() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        SecurityAnswerDefinitionConfig that = (SecurityAnswerDefinitionConfig) o;
        return Objects.equals(numberOfAnswersUserMustSet, that.numberOfAnswersUserMustSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), numberOfAnswersUserMustSet);
    }

}
