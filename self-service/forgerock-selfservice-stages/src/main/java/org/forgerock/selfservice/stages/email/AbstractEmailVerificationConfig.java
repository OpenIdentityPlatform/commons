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

import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.util.Reject;

/**
 * Defines the basic contract for the email verification configuration.
 *
 * @since 0.1.0
 */
abstract class AbstractEmailVerificationConfig<C extends AbstractEmailVerificationConfig<C>> implements StageConfig {

    private EmailAccountConfig emailAccountConfig;

    AbstractEmailVerificationConfig(EmailAccountConfig emailConfig) {
        Reject.ifNull(emailConfig);
        this.emailAccountConfig = emailConfig;
    }

    /**
     * Gets the URL for the email service.
     *
     * @return the email service URL
     */
    String getEmailServiceUrl() {
        return emailAccountConfig.getServiceUrl();
    }

    /**
     * Gets the subject part for the verification email.
     *
     * @return the email subject
     */
    String getEmailSubject() {
        return emailAccountConfig.getSubject();
    }

    /**
     * Gets the message for the verification email.
     *
     * @return the email message
     */
    String getEmailMessage() {
        return emailAccountConfig.getMessage();
    }

    /**
     * Gets the mime-type of the message.
     *
     * @return the email mime-type
     */
    String getEmailMimeType() {
        return emailAccountConfig.getMimeType();
    }

    /**
     * Gets the from part for the verification email.
     *
     * @return the email from field
     */
    String getEmailFrom() {
        return emailAccountConfig.getFrom();
    }

    /**
     * Gets the string token representing where the verification URL should be substituted.
     *
     * @return the verification URL string token
     */
    String getEmailVerificationLinkToken() {
        return emailAccountConfig.getVerificationLinkToken();
    }

    /**
     * Gets the verification URL to be passed into the email body.
     *
     * @return the verification URL
     */
    String getEmailVerificationLink() {
        return emailAccountConfig.getVerificationLink();
    }

    /**
     * Sets the URL for the email service.
     *
     * @param emailServiceUrl
     *         the email service URL
     *
     * @return this config instance
     */
    public C setEmailServiceUrl(String emailServiceUrl) {
        emailAccountConfig.setServiceUrl(emailServiceUrl);
        return self();
    }

    /**
     * Sets the subject part for the verification email.
     *
     * @param emailSubject
     *         the email subject
     *
     * @return this config instance
     */
    public C setEmailSubject(String emailSubject) {
        emailAccountConfig.setSubject(emailSubject);
        return self();
    }

    /**
     * Sets the message part for the verification email.
     *
     * @param emailMessage
     *         the email message
     *
     * @return this config instance
     */
    public C setEmailMessage(String emailMessage) {
        emailAccountConfig.setMessage(emailMessage);
        return self();
    }

    /**
     * Sets the message mime-type for the verification email.
     *
     * @param mimeType
     *         the email message mime-type
     *
     * @return this config instance
     */
    public C setEmailMimeType(String mimeType) {
        emailAccountConfig.setMimeType(mimeType);
        return self();
    }

    /**
     * Sets the from part for the verification email.
     *
     * @param emailFrom
     *         the email from field
     *
     * @return this config instance
     */
    public C setEmailFrom(String emailFrom) {
        emailAccountConfig.setFrom(emailFrom);
        return self();
    }

    /**
     * Sets the string token representing where the verification URL should besubstitutedd.
     *
     * @param emailVerificationLinkToken
     *         the verification URL string token
     *
     * @return this config instance
     */
    public C setEmailVerificationLinkToken(String emailVerificationLinkToken) {
        emailAccountConfig.setVerificationLinkToken(emailVerificationLinkToken);
        return self();
    }

    /**
     * Sets the verification URL to be passed into the email body.
     *
     * @param emailVerificationLink
     *         the verification URL
     *
     * @return this config instance
     */
    public C setEmailVerificationLink(String emailVerificationLink) {
        emailAccountConfig.setVerificationLink(emailVerificationLink);
        return self();
    }

    /**
     * Gets the name of the stage configuration.
     *
     * @return the config name
     */
    public abstract String getName();

    /**
     * Returns this object, as its actual type.
     *
     * @return this object
     */
    public abstract C self();
}
