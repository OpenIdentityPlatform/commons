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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.selfservice.stages.terms;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for terms and conditions stage.
 *
 * @since 21.0.0
 */
public final class TermsAndConditionsConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "termsAndConditions";

    private Map<Locale, String> termsTranslations;

    /**
     * Gets the map of terms and conditions.
     *
     * @return the terms and conditions map (locale to string)
     */
    public Map<Locale, String> getTermsTranslations() {
        return termsTranslations;
    }

    /**
     * Sets the terms and conditions map.
     *
     * @param termsTranslations
     *         the terms and conditions map (locale to string)
     *
     * @return this config instance
     */
    public TermsAndConditionsConfig setTermsTranslations(Map<Locale, String> termsTranslations) {
        this.termsTranslations = termsTranslations;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return TermsAndConditionsStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TermsAndConditionsConfig)) {
            return false;
        }

        TermsAndConditionsConfig that = (TermsAndConditionsConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(termsTranslations, that.termsTranslations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), termsTranslations);
    }

}
