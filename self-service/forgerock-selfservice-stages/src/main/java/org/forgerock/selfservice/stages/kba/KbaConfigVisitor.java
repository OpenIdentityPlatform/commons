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

package org.forgerock.selfservice.stages.kba;

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfigVisitor;

/**
 * Visitor that builds KBA flow stages using visited KBA configs.
 *
 * @since 0.3.0
 */
public interface KbaConfigVisitor extends StageConfigVisitor {

    /**
     * Builds a security answer definition stage bound to the security answer definition config.
     *
     * @param config
     *         security answer definition config
     *
     * @return security answer definition stage binding
     */
    ProgressStageBinder<?> build(SecurityAnswerDefinitionConfig config);

    /**
     * Builds a security answer verification stage bound to the security answer verification config.
     *
     * @param config
     *         security answer verification config
     *
     * @return security answer verifications stage binding
     */
    ProgressStageBinder<?> build(SecurityAnswerVerificationConfig config);

}
