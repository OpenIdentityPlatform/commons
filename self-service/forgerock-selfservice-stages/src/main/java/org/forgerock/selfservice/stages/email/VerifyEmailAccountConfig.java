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
import org.forgerock.selfservice.stages.CommonStageTypes;

/**
 * Configuration for the email account verification stage.
 *
 * @since 0.1.0
 */
public final class VerifyEmailAccountConfig implements AbstractEmailVerificationConfig {

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
     */
    public void setEmailServiceUrl(String emailServiceUrl) {
        this.emailServiceUrl = emailServiceUrl;
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
     */
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
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
     */
    public void setEmailMessage(String emailMessage) {
        this.emailMessage = emailMessage;
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
     */
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
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
     */
    public void setEmailVerificationLinkToken(String emailVerificationLinkToken) {
        this.emailVerificationLinkToken = emailVerificationLinkToken;
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
     */
    public void setEmailVerificationLink(String emailVerificationLink) {
        this.emailVerificationLink = emailVerificationLink;
    }

    @Override
    public StageType<?> getStageType() {
        return CommonStageTypes.VERIFY_EMAIL_TYPE;
    }

}
