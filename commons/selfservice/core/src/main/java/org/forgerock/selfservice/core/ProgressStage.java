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

package org.forgerock.selfservice.core;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Progress stage represents a single stage within the overall advance flow.
 * <br />
 * The method {@link ProgressStage#gatherInitialRequirements(ProcessContext, StageConfig)} is invoke first and provides
 * an opportunity for the stage to return some initial requirements. The method
 * {@link ProgressStage#advance(ProcessContext, StageConfig)} is repeatedly after for every
 * {@link StageResponse} that is returned containing some requirements. Stage tags can be used in the stage response to
 * help track progress in the stage itself. State can also be added to the stage response to pass data throughout the
 * flow.
 *
 * @param <C>
 *         represents the subtype of stage config that the concrete progress stage is expecting
 *
 * @since 0.1.0
 */
public interface ProgressStage<C extends StageConfig> {

    /**
     * Response for defining any initial requirements the stage may have.
     * <br />
     * An empty json object implies no initial requirements.
     *
     * @param context
     *         the current process context
     * @param config
     *         the stage configuration
     *
     * @return json value representing the requirements or empty json object for no requirements
     *
     * @throws ResourceException
     *         if some expected state is invalid
     */
    JsonValue gatherInitialRequirements(ProcessContext context, C config) throws ResourceException;

    /**
     * Advance the progress stage.
     *
     * @param context
     *         the current process context
     * @param config
     *         the stage configuration
     *
     * @return the result of invoking this stage
     *
     * @throws ResourceException
     *         if some expected state or input is invalid
     */
    StageResponse advance(ProcessContext context, C config) throws ResourceException;

}
