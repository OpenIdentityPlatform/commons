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

package org.forgerock.selfservice.stages.dynamic;

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfigVisitor;

/**
 * Visitor that dynamically builds progress stages based of the class name defined in the config.
 *
 * @since 0.7.0
 */
public interface DynamicConfigVisitor extends StageConfigVisitor {

    /**
     * Builds a progress stage as defined in the config and is then bound to that config.
     * <p/>
     * The defined progress stage must be able to consume the type of config passed.
     *
     * @param config
     *         dynamic stage config
     *
     * @return dynamic progress stage and config binding
     */
    ProgressStageBinder<?> build(DynamicStageConfig config);

}
