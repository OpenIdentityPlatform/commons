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
package org.forgerock.selfservice.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.selfservice.stages.user.EmailUsernameConfig;
import org.forgerock.selfservice.stages.user.RetrieveUsernameConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.selfservice.stages.user.UserQueryConfig;

/**
 * Encapsulation of custom configuration for deserializing JSON to {@link ProcessInstanceConfig}.
 *
 * @since 0.2.0
 */
final class JsonConfig {

    private final ClassLoader classLoader;
    private final ObjectMapper mapper;

    JsonConfig(ClassLoader classLoader) {
        this.classLoader = classLoader;
        mapper = new ObjectMapper();
        mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                /* TODO when on Jackson 2.6.x
                .setTypeFactory(TypeFactory.defaultInstance().withClassLoader(classLoader))
                */
                .registerModule(
                        new SimpleModule("SelfServiceModule", Version.unknownVersion())
                                .addDeserializer(StorageType.class, new StorageTypeDeserializer()))
                .registerSubtypes(
                        // stage config object mapping
                        new NamedType(CaptchaStageConfig.class, CaptchaStageConfig.NAME),
                        new NamedType(ResetStageConfig.class, ResetStageConfig.NAME),
                        new NamedType(UserRegistrationConfig.class, UserRegistrationConfig.NAME),
                        new NamedType(UserQueryConfig.class, UserQueryConfig.NAME),
                        new NamedType(VerifyEmailAccountConfig.class, VerifyEmailAccountConfig.NAME),
                        new NamedType(UserDetailsConfig.class, UserDetailsConfig.NAME),
                        new NamedType(SecurityAnswerDefinitionConfig.class, SecurityAnswerDefinitionConfig.NAME),
                        new NamedType(SecurityAnswerVerificationConfig.class, SecurityAnswerVerificationConfig.NAME),
                        new NamedType(RetrieveUsernameConfig.class, RetrieveUsernameConfig.NAME),
                        new NamedType(EmailUsernameConfig.class, EmailUsernameConfig.NAME),

                        // token handler config object mapping
                        new NamedType(JwtTokenHandlerConfig.class, JwtTokenHandlerConfig.TYPE));
    }

    /**
     * Builds ProcessInstanceConfig instance from a JsonValue instance.
     *
     * @param json
     *         the value to be converted
     * @return ProcessInstanceConfig
     *         the one built from the provided json
     */
    ProcessInstanceConfig buildProcessInstanceConfig(JsonValue json) {

        /* TODO when on Jackson 2.6.x, the class loader wil be set above and we'll just
        return mapper.convertValue(json.getObject(), ProcessInstanceConfig.class);
         */

        // Jackson 2.5 and lower
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return mapper.convertValue(json.getObject(), ProcessInstanceConfig.class);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }
}
