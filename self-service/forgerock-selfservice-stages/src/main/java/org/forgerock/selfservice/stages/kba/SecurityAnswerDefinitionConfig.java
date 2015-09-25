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

/**
 * Configuration for the KBA stage.
 *
 * @since 0.2.0
 */
public final class SecurityAnswerDefinitionConfig implements StageConfig {

    public static final String NAME = "kbaStage";

    private String kbaServiceUrl;

    private String kbaPropertyName;

    /**
     * Gets the URL for the REST endpoint for fetching the KBA questions.
     *
     * @return the KBA service URL
     */
    public String getKbaServiceUrl() {
        return kbaServiceUrl;
    }

    /**
     * Sets the URL for the REST endpoint to fetch the KBA questions.
     *
     * @param kbaServiceUrl
     *         the KBA service URL
     *
     * @return this config instance
     */
    public SecurityAnswerDefinitionConfig setKbaServiceUrl(String kbaServiceUrl) {
        this.kbaServiceUrl = kbaServiceUrl;
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
