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
 * Configuration for the KBA Security Answer Verification Stage.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerVerificationConfig extends AbstractKbaStageConfig<SecurityAnswerVerificationConfig> {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "kbaSecurityAnswerVerificationStage";

    private String identityServiceUrl;

    private int numberOfQuestionsUserMustAnswer;

    /**
     * Creates a new SecurityAnswerVerificationConfig.
     *
     * @param kbaConfig
     *         the kba configuration
     */
    public SecurityAnswerVerificationConfig(@JsonProperty("kbaConfig") KbaConfig kbaConfig) {
        super(kbaConfig);
    }

    /**
     * Gets the URL for the identity service.
     *
     * @return the identity service URL
     */
    public String getIdentityServiceUrl() {
        return identityServiceUrl;
    }

    /**
     * Sets the URL for the identity service.
     *
     * @param identityServiceUrl
     *         the identity service URL
     *
     * @return this config instance
     */
    public SecurityAnswerVerificationConfig setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
        return this;
    }

    /**
     * Gets the number of questions that user must answer.
     *
     * @return the number of questions to be answered
     */
    public int getNumberOfQuestionsUserMustAnswer() {
        return numberOfQuestionsUserMustAnswer;
    }

    /**
     * Sets the number of questions that user must answer.
     *
     * @param numberOfQuestionsUserMustAnswer
     *         the number of questions to be answered
     *
     * @return this config instance
     */
    public SecurityAnswerVerificationConfig setNumberOfQuestionsUserMustAnswer(int numberOfQuestionsUserMustAnswer) {
        this.numberOfQuestionsUserMustAnswer = numberOfQuestionsUserMustAnswer;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return SecurityAnswerVerificationStage.class.getName();
    }

    @Override
    public SecurityAnswerVerificationConfig self() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        SecurityAnswerVerificationConfig that = (SecurityAnswerVerificationConfig) o;
        return Objects.equals(numberOfQuestionsUserMustAnswer, that.numberOfQuestionsUserMustAnswer)
                && Objects.equals(identityServiceUrl, that.identityServiceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), identityServiceUrl, numberOfQuestionsUserMustAnswer);
    }

}
