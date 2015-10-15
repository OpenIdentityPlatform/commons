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

package org.forgerock.selfservice.example;

import org.forgerock.http.Client;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.stages.CommonConfigVisitor;
import org.forgerock.selfservice.stages.captcha.CaptchaStage;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountStage;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdStage;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionStage;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationStage;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationStage;
import org.forgerock.selfservice.stages.reset.ResetStage;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.selfservice.stages.user.UserDetailsStage;

import javax.inject.Inject;

/**
 * Example stage config visitor.
 *
 * @since 0.3.0
 */
final class ExampleStageConfigVisitor implements CommonConfigVisitor {

    private final ConnectionFactory connectionFactory;
    private final Client httpClient;

    @Inject
    public ExampleStageConfigVisitor(ConnectionFactory connectionFactory, Client httpClient) {
        this.connectionFactory = connectionFactory;
        this.httpClient = httpClient;
    }

    @Override
    public ProgressStageBinder<?> build(ResetStageConfig config) {
        return ProgressStageBinder.bind(new ResetStage(connectionFactory), config);
    }

    @Override
    public ProgressStageBinder<?> build(SecurityAnswerDefinitionConfig config) {
        return ProgressStageBinder.bind(new SecurityAnswerDefinitionStage(connectionFactory), config);
    }

    @Override
    public ProgressStageBinder<?> build(SecurityAnswerVerificationConfig config) {
        return ProgressStageBinder.bind(new SecurityAnswerVerificationStage(connectionFactory), config);
    }

    @Override
    public ProgressStageBinder<?> build(UserDetailsConfig config) {
        return ProgressStageBinder.bind(new UserDetailsStage(), config);
    }

    @Override
    public ProgressStageBinder<?> build(UserRegistrationConfig config) {
        return ProgressStageBinder.bind(new UserRegistrationStage(connectionFactory), config);
    }

    @Override
    public ProgressStageBinder<?> build(VerifyEmailAccountConfig config) {
        return ProgressStageBinder.bind(new VerifyEmailAccountStage(connectionFactory), config);
    }

    @Override
    public ProgressStageBinder<?> build(VerifyUserIdConfig config) {
        return ProgressStageBinder.bind(new VerifyUserIdStage(connectionFactory), config);
    }

    @Override
    public ProgressStageBinder<?> build(CaptchaStageConfig config) {
        return ProgressStageBinder.bind(new CaptchaStage(httpClient), config);
    }

    @Override
    public ProgressStageBinder<?> build(StageConfig<?> config) {
        throw new UnsupportedOperationException("Unknown config type " + config.getName());
    }

}
