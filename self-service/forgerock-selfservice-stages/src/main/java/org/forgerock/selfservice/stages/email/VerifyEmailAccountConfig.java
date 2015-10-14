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

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfig;

import java.util.Locale;
import java.util.Map;

/**
 * Configuration for the email account verification stage.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountConfig implements StageConfig<VerifyEmailAccountConfigVisitor> {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "emailValidation";

    private String emailServiceUrl;
    private Map<Locale, String> subjectMap;
    private String from;
    private Map<Locale, String> messageMap;
    private String mimeType;
    private String verificationLink;
    private String verificationLinkToken;

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
    public Map<Locale, String> getSubjectMap() {
        return subjectMap;
    }

    /**
     * Sets the subject map for the verification email.
     *
     * @param subjectMap
     *         the email subject map (locale to string)
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setSubjectMap(Map<Locale, String> subjectMap) {
        this.subjectMap = subjectMap;
        return this;
    }

    /**
     * Gets the message map for the verification email.
     *
     * @return the email message map (locale to string)
     */
    public Map<Locale, String> getMessageMap() {
        return messageMap;
    }

    /**
     * Sets the message map for the verification email.
     *
     * @param messageMap
     *         the email message map (locale to string)
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setMessageMap(Map<Locale, String> messageMap) {
        this.messageMap = messageMap;
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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ProgressStageBinder<?> accept(VerifyEmailAccountConfigVisitor visitor) {
        return visitor.build(this);
    }

}
