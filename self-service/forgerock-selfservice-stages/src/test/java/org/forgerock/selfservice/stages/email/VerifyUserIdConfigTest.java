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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.forgerock.selfservice.core.config.StageConfig;
import org.testng.annotations.Test;

/**
 * Unit test for {@link VerifyUserIdConfig}.
 *
 * @since 0.2.0
 */
public class VerifyUserIdConfigTest {

    @Test
    public void testConfigFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(
                new NamedType(VerifyUserIdConfig.class, VerifyUserIdConfig.NAME)
        );
        StageConfig<?> config = mapper.readValue(getClass().getResource("/userIdValidation.json"), StageConfig.class);

        assertThat(config).isInstanceOf(VerifyUserIdConfig.class);
        VerifyUserIdConfig verifyUserIdConfig = (VerifyUserIdConfig) config;

        assertVerifyUserIdConfigHelper(verifyUserIdConfig);
    }

    @Test
    public void testChainedEmailConfigThroughSetters() {
        VerifyUserIdConfig verifyUserIdConfig = new VerifyUserIdConfig(new EmailAccountConfig())
                .setQueryFields(new HashSet<>(Arrays.asList("uid", "mail")))
                .setIdentityIdField("userId")
                .setIdentityEmailField("email")
                .setIdentityServiceUrl("/users")
                .setEmailServiceUrl("/email")
                .setEmailFrom("noreply@example.com")
                .setEmailSubject("Verify your email address")
                .setEmailMessage("Is this correct")
                .setEmailVerificationLinkToken("abc123")
                .setEmailVerificationLink("/verifyemail");

        assertVerifyUserIdConfigHelper(verifyUserIdConfig);
    }

    private void assertVerifyUserIdConfigHelper(VerifyUserIdConfig verifyUserIdConfig) {
        assertThat(verifyUserIdConfig.getIdentityServiceUrl()).isEqualTo("/users");
        assertThat(verifyUserIdConfig.getIdentityIdField()).isEqualTo("userId");
        assertThat(verifyUserIdConfig.getIdentityEmailField()).isEqualTo("email");
        assertThat(verifyUserIdConfig.getEmailServiceUrl()).isEqualTo("/email");
        assertThat(verifyUserIdConfig.getEmailSubject()).isEqualTo("Verify your email address");
        assertThat(verifyUserIdConfig.getEmailMessage()).isEqualTo("Is this correct");
        assertThat(verifyUserIdConfig.getEmailFrom()).isEqualTo("noreply@example.com");
        assertThat(verifyUserIdConfig.getEmailVerificationLink()).isEqualTo("/verifyemail");
        assertThat(verifyUserIdConfig.getEmailVerificationLinkToken()).isEqualTo("abc123");
    }
}


