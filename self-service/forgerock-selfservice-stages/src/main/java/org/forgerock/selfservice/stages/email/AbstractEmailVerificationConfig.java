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

/**
 * Defines the basic contract for the email verification configuration.
 *
 * @since 0.1.0
 */
interface AbstractEmailVerificationConfig extends StageConfig {

    /**
     * Gets the URL for the email service.
     *
     * @return the email service URL
     */
    String getEmailServiceUrl();

    /**
     * Gets the subject part for the reset email.
     *
     * @return the email subject
     */
    String getEmailSubject();

    /**
     * Gets the message for the reset email.
     *
     * @return the email message
     */
    String getEmailMessage();

    /**
     * Gets the from part for the reset email.
     *
     * @return the email from field
     */
    String getEmailFrom();

    /**
     * Gets the string token representing where the reset URL should be substituted.
     *
     * @return the reset URL string token
     */
    String getEmailVerificationLinkToken();

    /**
     * Gets the reset URL to be passed into the email body.
     *
     * @return the reset URL
     */
    String getEmailVerificationLink();

}
