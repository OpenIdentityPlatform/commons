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
package org.forgerock.selfservice.stages.captcha;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the captcha stage.
 *
 * @since 0.2.0
 */
public final class CaptchaStageConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "captcha";

    private String recaptchaSiteKey;

    private String recaptchaSecretKey;

    private String recaptchaUri;

    /**
     * Gets the uri for verifying recaptcha.
     *
     * @return the uri
     */
    public String getRecaptchaUri() {
        return recaptchaUri;
    }

    /**
     * Sets the uri for verifying recaptcha.
     *
     * @param recaptchaUri
     *         the uri
     *
     * @return this config instance
     */
    public CaptchaStageConfig setRecaptchaUri(String recaptchaUri) {
        this.recaptchaUri = recaptchaUri;
        return this;
    }

    /**
     * Gets the site key for re-captcha.
     *
     * @return the site key
     */
    public String getRecaptchaSiteKey() {
        return recaptchaSiteKey;
    }

    /**
     * Sets the site key for re-captcha.
     *
     * @param recaptchaSiteKey
     *         the site key
     *
     * @return this config instance
     */
    public CaptchaStageConfig setRecaptchaSiteKey(String recaptchaSiteKey) {
        this.recaptchaSiteKey = recaptchaSiteKey;
        return this;
    }

    /**
     * Gets the secret key for re-captcha.
     *
     * @return the secret key
     */
    public String getRecaptchaSecretKey() {
        return recaptchaSecretKey;
    }

    /**
     * Sets the secret key for re-captcha.
     *
     * @param recaptchaSecretKey
     *         the secret key
     *
     * @return this config instance
     */
    public CaptchaStageConfig setRecaptchaSecretKey(String recaptchaSecretKey) {
        this.recaptchaSecretKey = recaptchaSecretKey;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
