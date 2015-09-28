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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the KBA stage.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "kbaStage";

    @JsonProperty
    private final List<KbaQuestion> questions;

    private String kbaPropertyName;

    /**
     * Creates a new SecurityAnswerDefinitionConfig.
     */
    public SecurityAnswerDefinitionConfig() {
        questions = new ArrayList<>();
    }

    /**
     * Gets the unmodifiable list view of questions.
     *
     * @return list of KbaQuestions
     */
    public List<KbaQuestion> questionsAsList() {
        return Collections.unmodifiableList(questions);
    }

    /**
     * Adds the KbaQuestion instance to this config.
     *
     * @param question
     *         the KBA question to be added
     *
     * @return this config instance
     */
    public SecurityAnswerDefinitionConfig addQuestion(KbaQuestion question) {
        questions.add(question);
        return this;
    }

    /**
     * Gets the property name in user json instance where kba details will be set.
     *
     * @return property name
     */
    public String getKbaPropertyName() {
        return kbaPropertyName;
    }

    /**
     * Sets the property name in user json instance where kba details will be set.
     *
     * @param kbaPropertyName
     *         the property name
     *
     * @return this config instance
     */
    public SecurityAnswerDefinitionConfig setKbaPropertyName(String kbaPropertyName) {
        this.kbaPropertyName = kbaPropertyName;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

}
