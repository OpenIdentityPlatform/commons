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
package org.forgerock.selfservice.stages.user;

import org.forgerock.selfservice.core.config.StageConfig;

import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the user query stage.
 *
 * @since 0.5.0
 */
public final class UserQueryConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "userQuery";

    private Set<String> validQueryFields;
    private String identityServiceUrl;
    private String identityIdField;
    private String identityEmailField;
    private String identityUsernameField;

    /**
     * Gets the set of query fields to be used when looking up the user.
     *
     * @return query fields
     */
    public Set<String> getValidQueryFields() {
        return validQueryFields;
    }

    /**
     * Sets the set of query fields to be used when looking up the user.
     *
     * @param validQueryFields
     *         query fields
     *
     * @return this config instance
     */
    public UserQueryConfig setValidQueryFields(Set<String> validQueryFields) {
        this.validQueryFields = validQueryFields;
        return this;
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
    public UserQueryConfig setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
        return this;
    }

    /**
     * Gets the field name for the identity id.
     *
     * @return the identity id field name
     */
    public String getIdentityIdField() {
        return identityIdField;
    }

    /**
     * Sets the field name for the identity id.
     *
     * @param identityIdField
     *         the identity id field name
     *
     * @return this config instance
     */
    public UserQueryConfig setIdentityIdField(String identityIdField) {
        this.identityIdField = identityIdField;
        return this;
    }

    /**
     * Gets the field name for the identity email address.
     *
     * @return the identity email address field name
     */
    public String getIdentityEmailField() {
        return identityEmailField;
    }

    /**
     * Sets the field name for the identity email address.
     *
     * @param identityEmailField
     *         the identity email address field name
     *
     * @return this config instance
     */
    public UserQueryConfig setIdentityEmailField(String identityEmailField) {
        this.identityEmailField = identityEmailField;
        return this;
    }

    /**
     * Gets the field name for the identity username.
     *
     * @return the identity username field name
     */
    public String getIdentityUsernameField() {
        return identityUsernameField;
    }

    /**
     * Sets the field name for the identity username.
     *
     * @param identityUsernameField
     *         the identity username field name
     *
     * @return this config instance
     */
    public UserQueryConfig setIdentityUsernameField(String identityUsernameField) {
        this.identityUsernameField = identityUsernameField;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return UserQueryStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UserQueryConfig)) {
            return false;
        }

        UserQueryConfig that = (UserQueryConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(validQueryFields, that.validQueryFields)
                && Objects.equals(identityServiceUrl, that.identityServiceUrl)
                && Objects.equals(identityIdField, that.identityIdField)
                && Objects.equals(identityEmailField, that.identityEmailField)
                && Objects.equals(identityUsernameField, that.identityUsernameField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(),
                validQueryFields, identityServiceUrl, identityIdField,
                identityEmailField, identityUsernameField);
    }

}

