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

package org.forgerock.selfservice.stages;

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.selfservice.stages.user.UserQueryConfig;

/**
 * Helpful decorator class to wrap an underlying visitor to assist with extensibility.
 *
 * @since 0.3.0
 */
public abstract class CommonConfigVisitorDecorator implements CommonConfigVisitor {

    private final CommonConfigVisitor decoratedVisitor;

    /**
     * Constructs a new visitor decorator.
     *
     * @param decoratedVisitor
     *         the visitor to be decorated
     */
    public CommonConfigVisitorDecorator(CommonConfigVisitor decoratedVisitor) {
        this.decoratedVisitor = decoratedVisitor;
    }

    @Override
    public ProgressStageBinder<?> build(CaptchaStageConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(VerifyEmailAccountConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(UserQueryConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(SecurityAnswerDefinitionConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(SecurityAnswerVerificationConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(ResetStageConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(UserDetailsConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(UserRegistrationConfig config) {
        return decoratedVisitor.build(config);
    }

    @Override
    public ProgressStageBinder<?> build(StageConfig<?> config) {
        return decoratedVisitor.build(config);
    }

}
