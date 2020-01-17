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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.selfservice.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.terms.TermsAndConditionsConfig;
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

    /* Define "standard" NamedTypes, or type-name mappings here.  They will be augmented with custom type
     * mappings passed in from the client or integrating product.
     */
    private static final NamedType[] DEFAULT_NAMED_TYPES = new NamedType[] {
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
        new NamedType(TermsAndConditionsConfig.class, TermsAndConditionsConfig.NAME),

        // token handler config object mapping
        new NamedType(JwtTokenHandlerConfig.class, JwtTokenHandlerConfig.TYPE)
    };

    private final ObjectMapper mapper;

    JsonConfig(ClassLoader classLoader) {
        this(classLoader, new HashMap<String, Class<? extends StageConfig>>());
    }

    JsonConfig(ClassLoader classLoader, Map<String, Class<? extends StageConfig>> stageConfigMappings) {
        final List<NamedType> namedTypes = new ArrayList<>(Arrays.asList(DEFAULT_NAMED_TYPES));
        // add custom stage config mapping to default named types
        for (Map.Entry<String, Class<? extends StageConfig>> entry : stageConfigMappings.entrySet()) {
            namedTypes.add(new NamedType(entry.getValue(), entry.getKey()));
        }
        mapper = new ObjectMapper();
        mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setTypeFactory(TypeFactory.defaultInstance().withClassLoader(classLoader))
                .registerModule(
                        new SimpleModule("SelfServiceModule", Version.unknownVersion())
                                .addDeserializer(StorageType.class, new StorageTypeDeserializer()))
                .registerSubtypes(namedTypes.toArray(new NamedType[0]));
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
        return mapper.convertValue(json.getObject(), ProcessInstanceConfig.class);
    }
}
