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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for the email based user name retrieval stage.
 *
 * @since 0.8.0
 */
public final class EmailUsernameConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "emailUsername";

    private String emailServiceUrl;
    private Map<Locale, String> subjectTranslations;
    private String from;
    private Map<Locale, String> messageTranslations;
    private String mimeType;
    private String usernameToken;

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
    public EmailUsernameConfig setEmailServiceUrl(String emailServiceUrl) {
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
    public EmailUsernameConfig setSubjectTranslations(Map<Locale, String> subjectTranslations) {
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
    public EmailUsernameConfig setMessageTranslations(Map<Locale, String> messageTranslations) {
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
    public EmailUsernameConfig setMimeType(String mimeType) {
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
    public EmailUsernameConfig setFrom(String from) {
        this.from = from;
        return this;
    }

    /**
     * Gets the string token representing where the user name should be substituted.
     *
     * @return the user name string token
     */
    public String getUsernameToken() {
        return usernameToken;
    }

    /**
     * Sets the string token representing where the user name should be substituted.
     *
     * @param usernameToken
     *         the user name string token
     *
     * @return this config instance
     */
    public EmailUsernameConfig setUsernameToken(String usernameToken) {
        this.usernameToken = usernameToken;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return EmailUsernameStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof EmailUsernameConfig)) {
            return false;
        }

        EmailUsernameConfig that = (EmailUsernameConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(emailServiceUrl, that.emailServiceUrl)
                && Objects.equals(subjectTranslations, that.subjectTranslations)
                && Objects.equals(from, that.from)
                && Objects.equals(messageTranslations, that.messageTranslations)
                && Objects.equals(mimeType, that.mimeType)
                && Objects.equals(usernameToken, that.usernameToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(),
                emailServiceUrl, subjectTranslations, from, messageTranslations,
                mimeType, usernameToken);
    }

}
