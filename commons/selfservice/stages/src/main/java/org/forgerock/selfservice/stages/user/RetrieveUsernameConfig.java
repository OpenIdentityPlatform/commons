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

package org.forgerock.selfservice.stages.user;

import java.util.Objects;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the retrieve username stage.
 *
 * @since 0.7.0
 */
public final class RetrieveUsernameConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "retrieveUsername";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return RetrieveUsernameStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof RetrieveUsernameConfig)) {
            return false;
        }

        RetrieveUsernameConfig that = (RetrieveUsernameConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName());
    }

}
