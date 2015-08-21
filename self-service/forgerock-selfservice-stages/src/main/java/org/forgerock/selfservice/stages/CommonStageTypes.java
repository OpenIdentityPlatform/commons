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

import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;

/**
 * Stage types used by common progress stages.
 *
 * @since 0.1.0
 */
public final class CommonStageTypes {

    private CommonStageTypes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Verify email stage type.
     */
    public static final StageType<VerifyEmailAccountConfig> VERIFY_EMAIL_TYPE =
            StageType.valueOf("emailValidation", VerifyEmailAccountConfig.class);

    /**
     * Verify user Id stage type.
     */
    public static final StageType<VerifyUserIdConfig> VERIFY_USER_ID_TYPE =
            StageType.valueOf("userIdValidation", VerifyUserIdConfig.class);

    /**
     * Reset password stage type.
     */
    public static final StageType<ResetStageConfig> RESET_TYPE =
            StageType.valueOf("resetStage", ResetStageConfig.class);


}
