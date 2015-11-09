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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.forgerock.selfservice.core.config.StageConfig;
import org.testng.annotations.Test;

/**
 * Unit test for {@link CaptchaStageConfig}.
 *
 * @since 0.2.0
 */
public final class CaptchaStageConfigTest {

    @Test
    public void testConfigFromJson() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(
                new NamedType(CaptchaStageConfig.class, CaptchaStageConfig.NAME)
        );

        // When
        StageConfig config = mapper.readValue(getClass().getResource("/captcha.json"), StageConfig.class);

        // Then
        assertThat(config).isInstanceOf(CaptchaStageConfig.class);
        CaptchaStageConfig captchaStageConfig = (CaptchaStageConfig) config;
        assertThat(captchaStageConfig.getRecaptchaSiteKey()).isEqualTo("6Le4og4TAAAAACWetDto1152-_JEhn5aG8NhZ3ej");
        assertThat(captchaStageConfig.getRecaptchaSecretKey()).isEqualTo("6Le4og4TAAAAAFPprcsXlHE9bYYPAMX794A6R3Mv");
        assertThat(captchaStageConfig.getRecaptchaUri()).isEqualTo("https://www.google.com/recaptcha/api/siteverify");
    }

}
