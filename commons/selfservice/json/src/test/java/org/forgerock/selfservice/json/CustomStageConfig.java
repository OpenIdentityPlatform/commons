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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.selfservice.json;

import java.util.Objects;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Represents the config for a no-op stage used in testing.
 *
 * @since 21.0.0.
 */
final class CustomStageConfig implements StageConfig {

    static final String NAME = "CustomStage";

    @Override
    public String getProgressStageClassName() {
        return CustomStage.class.getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CustomStageConfig)) {
            return false;
        }

        CustomStageConfig that = (CustomStageConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName());
    }
}
