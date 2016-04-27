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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for the email account verification stage.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "emailValidation";

    private String emailServiceUrl;
    private Map<Locale, String> subjectTranslations;
    private String from;
    private Map<Locale, String> messageTranslations;
    private String mimeType;
    private String verificationLink;
    private String verificationLinkToken;
    private String identityEmailField;

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
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setEmailServiceUrl(String emailServiceUrl) {
        this.emailServiceUrl = emailServiceUrl;
        return this;
    }

    /**
     * Gets the subject map for the verification email.
     *
     * @return the email subject map (locale to string)
     */
    public Map<Locale, String> getSubjectTranslations() {
        return subjectTranslations;
    }

    /**
     * Sets the subject map for the verification email.
     *
     * @param subjectTranslations
     *         the email subject map (locale to string)
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setSubjectTranslations(Map<Locale, String> subjectTranslations) {
        this.subjectTranslations = subjectTranslations;
        return this;
    }

    /**
     * Gets the message map for the verification email.
     *
     * @return the email message map (locale to string)
     */
    public Map<Locale, String> getMessageTranslations() {
        return messageTranslations;
    }

    /**
     * Sets the message map for the verification email.
     *
     * @param messageTranslations
     *         the email message map (locale to string)
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setMessageTranslations(Map<Locale, String> messageTranslations) {
        this.messageTranslations = messageTranslations;
        return this;
    }

    /**
     * Gets the mime-type of the email message.
     *
     * @return the message mime-type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the message mime-type.
     *
     * @param mimeType
     *         the message mime-type
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * Gets the from part for the verification email.
     *
     * @return the email from field
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the from part for the verification email.
     *
     * @param from
     *         the email from field
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setFrom(String from) {
        this.from = from;
        return this;
    }

    /**
     * Gets the string token representing where the verification URL should be substituted.
     *
     * @return the verification URL string token
     */
    public String getVerificationLinkToken() {
        return verificationLinkToken;
    }

    /**
     * Sets the string token representing where the verification URL should be substituted.
     *
     * @param verificationLinkToken
     *         the verification URL string token
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setVerificationLinkToken(String verificationLinkToken) {
        this.verificationLinkToken = verificationLinkToken;
        return this;
    }

    /**
     * Gets the verification URL to be passed into the email body.
     *
     * @return the verification URL
     */
    public String getVerificationLink() {
        return verificationLink;
    }

    /**
     * Sets the verification URL to be passed into the email body.
     *
     * @param verificationLink
     *         the verification URL
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setVerificationLink(String verificationLink) {
        this.verificationLink = verificationLink;
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
    public VerifyEmailAccountConfig setIdentityEmailField(String identityEmailField) {
        this.identityEmailField = identityEmailField;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return VerifyEmailAccountStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof VerifyEmailAccountConfig)) {
            return false;
        }

        VerifyEmailAccountConfig that = (VerifyEmailAccountConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(emailServiceUrl, that.emailServiceUrl)
                && Objects.equals(subjectTranslations, that.subjectTranslations)
                && Objects.equals(from, that.from)
                && Objects.equals(messageTranslations, that.messageTranslations)
                && Objects.equals(mimeType, that.mimeType)
                && Objects.equals(verificationLink, that.verificationLink)
                && Objects.equals(verificationLinkToken, that.verificationLinkToken)
                && Objects.equals(identityEmailField, that.identityEmailField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(),
                emailServiceUrl, subjectTranslations, from, messageTranslations, mimeType,
                verificationLink, verificationLinkToken, identityEmailField);
    }

}
