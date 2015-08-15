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

import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the email stage.
 *
 * @since 0.1.0
 */
public final class EmailStageConfig implements StageConfig {

    /**
     * Email stage type.
     */
    public static final StageType<EmailStageConfig> TYPE =
            StageType.valueOf("emailValidation", EmailStageConfig.class);

    private String identityServiceUrl;
    private String identityIdField;
    private String identityEmailField;

    private String emailServiceUrl;
    private String emailSubject;
    private String emailMessage;
    private String emailFrom;
    private String emailResetUrl;
    private String emailResetUrlToken;

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
     * Gets the URL for the email service.
     *
     * @return the email service URL
     */
    public String getEmailServiceUrl() {
        return emailServiceUrl;
    }

    /**
     * Sets the URL for the email service.
     *
     * @param emailServiceUrl
     *         the email service URL
     */
    public void setEmailServiceUrl(String emailServiceUrl) {
        this.emailServiceUrl = emailServiceUrl;
    }

    /**
     * Gets the subject part for the reset email.
     *
     * @return the email subject
     */
    public String getEmailSubject() {
        return emailSubject;
    }

    /**
     * Sets the subject part for the reset email.
     *
     * @param emailSubject
     *         the email subject
     */
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    /**
     * Gets the message for the reset email.
     *
     * @return the email message
     */
    public String getEmailMessage() {
        return emailMessage;
    }

    /**
     * Sets the message part for the reset email.
     *
     * @param emailMessage
     *         the email message
     */
    public void setEmailMessage(String emailMessage) {
        this.emailMessage = emailMessage;
    }

    /**
     * Gets the from part for the reset email.
     *
     * @return the email from field
     */
    public String getEmailFrom() {
        return emailFrom;
    }

    /**
     * Sets the from part for the reset email.
     *
     * @param emailFrom
     *         the email from field
     */
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    /**
     * Gets the string token representing where the reset URL should be substituted.
     *
     * @return the reset URL string token
     */
    public String getEmailResetUrlToken() {
        return emailResetUrlToken;
    }

    /**
     * Sets the string token representing where the reset URL should besubstitutedd.
     *
     * @param emailResetUrlToken
     *         the reset URL string token
     */
    public void setEmailResetUrlToken(String emailResetUrlToken) {
        this.emailResetUrlToken = emailResetUrlToken;
    }

    /**
     * Gets the reset URL to be passed into the email body.
     *
     * @return the reset URL
     */
    public String getEmailResetUrl() {
        return emailResetUrl;
    }

    /**
     * Sets the reset URL to be passed into the email body.
     *
     * @param emailResetUrl
     *         the reset URL
     */
    public void setEmailResetUrl(String emailResetUrl) {
        this.emailResetUrl = emailResetUrl;
    }

    @Override
    public StageType<?> getStageType() {
        return TYPE;
    }

}
