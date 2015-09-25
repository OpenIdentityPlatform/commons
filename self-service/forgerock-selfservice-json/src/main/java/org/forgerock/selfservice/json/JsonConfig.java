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

import java.io.IOException;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;

/**
 * Static utility methods for deserializing config objects from JSON.
 */
public final class JsonConfig {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper
                .registerModule(
                        new SimpleModule("SelfServiceModule", Version.unknownVersion())
                                .addDeserializer(StorageType.class, new StorageTypeDeserializer()))
                .registerSubtypes(
                        // stage config object mapping
                        new NamedType(ResetStageConfig.class, ResetStageConfig.NAME),
                        new NamedType(UserRegistrationConfig.class, UserRegistrationConfig.NAME),
                        new NamedType(VerifyUserIdConfig.class, VerifyUserIdConfig.NAME),
                        new NamedType(VerifyEmailAccountConfig.class, VerifyEmailAccountConfig.NAME),
                        new NamedType(UserDetailsConfig.class, UserDetailsConfig.NAME),
                        new NamedType(SecurityAnswerDefinitionConfig.class, SecurityAnswerDefinitionConfig.NAME),

                        // token handler config object mapping
                        new NamedType(JwtTokenHandlerConfig.class, JwtTokenHandlerConfig.TYPE)
                );
    }

    public static ProcessInstanceConfig buildProcessInstanceConfig(JsonValue json) throws IOException {
        //return objectMapper.readValue(objectMapper.writeValueAsString(json.getObject()), ProcessInstanceConfig.class);
        return objectMapper.convertValue(json.getObject(), ProcessInstanceConfig.class);
    }
}
