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

package org.forgerock.selfservice.custom;

import org.forgerock.selfservice.core.config.StageConfig;

import java.util.Objects;

/**
 * Represents the config for a simple math problem to be solved.
 *
 * @since 0.7.0
 */
public final class MathProblemStageConfig implements StageConfig {

    private final static String NAME = "MathProblem";

    private int leftValue;
    private int rightValue;

    /**
     * Gets the left hand side of the math problem.
     *
     * @return left hand value
     */
    public int getLeftValue() {
        return leftValue;
    }

    /**
     * Sets the left hand side of the math problem.
     *
     * @param leftValue
     *         left hand value
     *
     * @return this config
     */
    public MathProblemStageConfig setLeftValue(int leftValue) {
        this.leftValue = leftValue;
        return this;
    }

    /**
     * Gets the right hand side of the math problem.
     *
     * @return right hand value
     */
    public int getRightValue() {
        return rightValue;
    }

    /**
     * Sets the right hand side of the math problem.
     *
     * @param rightValue
     *         right hand value
     *
     * @return this config
     */
    public MathProblemStageConfig setRightValue(int rightValue) {
        this.rightValue = rightValue;
        return this;
    }

    @Override
    public String getProgressStageClassName() {
        return MathProblemStage.class.getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), leftValue, rightValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MathProblemStageConfig)) {
            return false;
        }

        MathProblemStageConfig that = (MathProblemStageConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(leftValue, that.leftValue)
                && Objects.equals(rightValue, that.rightValue);
    }
}
