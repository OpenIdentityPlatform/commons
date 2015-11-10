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

package org.forgerock.selfservice.stages.reset;

import java.util.Objects;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the password reset stage.
 *
 * @since 0.1.0
 */
public final class ResetStageConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "resetStage";

    private String identityServiceUrl;
    private String identityPasswordField;

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
    public ResetStageConfig setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
        return this;
    }

    /**
     * Gets the password field identifier.
     *
     * @return the password field name
     */
    public String getIdentityPasswordField() {
        return identityPasswordField;
    }

    /**
     * Sets the password field identifier.
     *
     * @param identityPasswordField
     *         the password field name
     *
     * @return this config instance
     */
    public ResetStageConfig setIdentityPasswordField(String identityPasswordField) {
        this.identityPasswordField = identityPasswordField;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return ResetStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResetStageConfig)) {
            return false;
        }

        ResetStageConfig that = (ResetStageConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(identityServiceUrl, that.identityServiceUrl)
                && Objects.equals(identityPasswordField, that.identityPasswordField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(),
                identityServiceUrl, identityPasswordField);
    }

}
