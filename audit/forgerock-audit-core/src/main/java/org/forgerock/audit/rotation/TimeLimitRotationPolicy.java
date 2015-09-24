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
package org.forgerock.audit.rotation;

import org.forgerock.util.time.Duration;
import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

/**
 * Creates a rotation policy based on a time duration. Once the duration has passed the policy will indicate a
 * file rotation is necessary.
 */
public class TimeLimitRotationPolicy implements RotationPolicy {
    private final Duration rotationInterval;

    /**
     * Constructs a TimeLimitRotationPolicy with a given {@link Duration}
     * @param rotationInterval
     */
    public TimeLimitRotationPolicy(final Duration rotationInterval) {
        this.rotationInterval = rotationInterval;
    }

    /**
     * Checks whether or not a {@link RotatableObject} needs rotation.
     * @param file The file to be checked.
     * @return True - If the {@link RotatableObject} needs rotation.
     *         False - If the {@link RotatableObject} doesn't need rotation.
     */
    @Override
    public boolean shouldRotateFile(RotatableObject file) {
        final org.joda.time.Duration timeSinceLastRotation =
                new org.joda.time.Duration(file.getLastRotationTime(), DateTime.now());
        return !rotationInterval.isZero()
                && timeSinceLastRotation.getMillis() >= rotationInterval.convertTo(TimeUnit.MILLISECONDS).getValue();
    }

    /**
     * Gets the rotation duration interval.
     * @return The interval as a {@link Duration}.
     */
    public Duration getRotationInterval() {
        return rotationInterval;
    }
}
