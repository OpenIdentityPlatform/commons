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

/**
 * Configuration for the email account verification stage.
 *
 * @since 0.1.0
 */
public class VerifyEmailAccountConfig implements AbstractEmailVerificationConfig {

    public static final String NAME = "emailValidation";

    private String emailServiceUrl;
    private String emailSubject;
    private String emailMessage;
    private String emailFrom;
    private String emailVerificationLink;
    private String emailVerificationLinkToken;

    @Override
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

    @Override
    public String getEmailSubject() {
        return emailSubject;
    }

    /**
     * Sets the subject part for the reset email.
     *
     * @param emailSubject
     *         the email subject
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
        return this;
    }

    @Override
    public String getEmailMessage() {
        return emailMessage;
    }

    /**
     * Sets the message part for the reset email.
     *
     * @param emailMessage
     *         the email message
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setEmailMessage(String emailMessage) {
        this.emailMessage = emailMessage;
        return this;
    }

    @Override
    public String getEmailFrom() {
        return emailFrom;
    }

    /**
     * Sets the from part for the reset email.
     *
     * @param emailFrom
     *         the email from field
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
        return this;
    }

    @Override
    public String getEmailVerificationLinkToken() {
        return emailVerificationLinkToken;
    }

    /**
     * Sets the string token representing where the reset URL should besubstitutedd.
     *
     * @param emailVerificationLinkToken
     *         the reset URL string token
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setEmailVerificationLinkToken(String emailVerificationLinkToken) {
        this.emailVerificationLinkToken = emailVerificationLinkToken;
        return this;
    }

    @Override
    public String getEmailVerificationLink() {
        return emailVerificationLink;
    }

    /**
     * Sets the reset URL to be passed into the email body.
     *
     * @param emailVerificationLink
     *         the reset URL
     *
     * @return this config instance
     */
    public VerifyEmailAccountConfig setEmailVerificationLink(String emailVerificationLink) {
        this.emailVerificationLink = emailVerificationLink;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

}
