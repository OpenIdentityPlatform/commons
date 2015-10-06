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
 * Simple email account configuration that may be used in any stage requiring email validation.
 *
 * @since 0.2.0
 */
public final class EmailAccountConfig {

    private String serviceUrl;
    private String subject;
    private String from;
    private String message;
    private String mimeType;
    private String verificationLink;
    private String verificationLinkToken;

    /**
     * Gets the URL for the email service.
     *
     * @return the email service URL
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Sets the URL for the email service.
     *
     * @param emailServiceUrl
     *         the email service URL
     *
     * @return this config instance
     */
    public EmailAccountConfig setServiceUrl(String emailServiceUrl) {
        this.serviceUrl = emailServiceUrl;
        return this;
    }

    /**
     * Gets the subject part for the verification email.
     *
     * @return the email subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject part for the verification email.
     *
     * @param subject
     *         the email subject
     *
     * @return this config instance
     */
    public EmailAccountConfig setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Gets the message for the verification email.
     *
     * @return the email message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message part for the verification email.
     *
     * @param message
     *         the email message
     *
     * @return this config instance
     */
    public EmailAccountConfig setMessage(String message) {
        this.message = message;
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
    public EmailAccountConfig setMimeType(String mimeType) {
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
    public EmailAccountConfig setFrom(String from) {
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
    public EmailAccountConfig setVerificationLinkToken(String verificationLinkToken) {
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
    public EmailAccountConfig setVerificationLink(String verificationLink) {
        this.verificationLink = verificationLink;
        return this;
    }
}
