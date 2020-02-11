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

import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.util.Reject;

import java.util.Map;
import java.util.Objects;

/**
 * Defines the common configurations for the KBA stages.
 *
 * @since 0.2.0
 */
abstract class AbstractKbaStageConfig<C extends AbstractKbaStageConfig<C>> implements StageConfig {

    private KbaConfig config;

    AbstractKbaStageConfig(KbaConfig config) {
        Reject.ifNull(config);
        this.config = config;
    }

    /**
     * Gets the kba questions in the following format.
     * <pre>Map&lt;id,Map&lt;locale,question&gt;&gt;</pre>
     *
     * @return property name
     */
    public Map<String, Map<String, String>> getQuestions() {
        return config.getQuestions();
    }

    /**
     * Sets the kba questions.
     *
     * @param questions
     *         the kba question
     *
     * @return this config instance
     */
    public C setQuestions(Map<String, Map<String, String>> questions) {
        config.setQuestions(questions);
        return self();
    }

    /**
     * Gets the property name in user json instance where kba details will be set.
     *
     * @return property name
     */
    public String getKbaPropertyName() {
        return config.getKbaPropertyName();
    }

    /**
     * Sets the property name in user json instance where kba details will be set.
     *
     * @param kbaPropertyName
     *         the property name
     *
     * @return this config instance
     */
    public C setKbaPropertyName(String kbaPropertyName) {
        config.setKbaPropertyName(kbaPropertyName);
        return self();
    }

    /**
     * Returns this object, as its actual type.
     *
     * @return this object
     */
    protected abstract C self();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractKbaStageConfig<?> that = (AbstractKbaStageConfig<?>) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), config);
    }

}
