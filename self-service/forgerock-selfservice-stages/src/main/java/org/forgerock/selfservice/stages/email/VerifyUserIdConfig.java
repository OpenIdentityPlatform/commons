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

package org.forgerock.selfservice.stages.email;

import org.forgerock.util.Reject;

import java.util.Set;

/**
 * Configuration for the user Id verification stage.
 *
 * @since 0.1.0
 */
public final class VerifyUserIdConfig implements AbstractEmailVerificationConfig {

    private static final String NAME = "userIdValidation";

    private final VerifyEmailAccountConfig emailStageConfig;
    private String identityServiceUrl;
    private String identityIdField;
    private String identityEmailField;
    private Set<String> queryFields;

    /**
     * Constructs a new configuration.
     *
     * @param verifyEmailAccountConfig
     *         the email configuration
     */
    public VerifyUserIdConfig(VerifyEmailAccountConfig verifyEmailAccountConfig) {
        Reject.ifNull(verifyEmailAccountConfig);
        this.emailStageConfig = verifyEmailAccountConfig;
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
     */
    public void setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
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
     */
    public void setIdentityIdField(String identityIdField) {
        this.identityIdField = identityIdField;
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
     */
    public void setIdentityEmailField(String identityEmailField) {
        this.identityEmailField = identityEmailField;
    }

    /**
     * Gets the set of query fields to be used when looking up the user.
     *
     * @return query fields
     */
    public Set<String> getQueryFields() {
        return queryFields;
    }

    /**
     * Sets the set of query fields to be used when looking up the user.
     *
     * @param queryFields
     *         query fields
     */
    public void setQueryFields(Set<String> queryFields) {
        this.queryFields = queryFields;
    }

    @Override
    public String getEmailServiceUrl() {
        return emailStageConfig.getEmailServiceUrl();
    }

    @Override
    public String getEmailSubject() {
        return emailStageConfig.getEmailSubject();
    }

    @Override
    public String getEmailMessage() {
        return emailStageConfig.getEmailMessage();
    }

    @Override
    public String getEmailFrom() {
        return emailStageConfig.getEmailFrom();
    }

    @Override
    public String getEmailVerificationLinkToken() {
        return emailStageConfig.getEmailVerificationLinkToken();
    }

    @Override
    public String getEmailVerificationLink() {
        return emailStageConfig.getEmailVerificationLink();
    }

    @Override
    public String getName() {
        return NAME;
    }

}
