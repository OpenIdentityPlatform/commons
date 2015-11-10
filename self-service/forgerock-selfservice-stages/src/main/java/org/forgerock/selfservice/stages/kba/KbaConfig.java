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

import java.util.Map;
import java.util.Objects;

/**
 * Represents a single KBA question in various Locales.
 *
 * @since 0.2.0
 */
public final class KbaConfig {

    private Map<String, Map<String, String>> questions;

    private String kbaPropertyName;

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
    public KbaConfig setKbaPropertyName(String kbaPropertyName) {
        this.kbaPropertyName = kbaPropertyName;
        return this;
    }

    /**
     * Gets the kba questions.
     *
     * @return property name
     */
    public Map<String, Map<String, String>> getQuestions() {
        return questions;
    }

    /**
     * Sets the kba questions.
     *
     * @param questions
     *         the kba question
     *
     * @return this config instance
     */
    public KbaConfig setQuestions(Map<String, Map<String, String>> questions) {
        this.questions = questions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof KbaConfig)) {
            return false;
        }

        KbaConfig kbaConfig = (KbaConfig) o;
        return Objects.equals(questions, kbaConfig.questions)
                && Objects.equals(kbaPropertyName, kbaConfig.kbaPropertyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questions, kbaPropertyName);
    }

}
