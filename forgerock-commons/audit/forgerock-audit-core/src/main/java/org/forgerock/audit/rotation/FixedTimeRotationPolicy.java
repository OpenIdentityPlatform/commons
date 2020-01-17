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
package org.forgerock.audit.rotation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.forgerock.util.time.Duration;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rotates audit files at fixed times throughout the day.
 */
public class FixedTimeRotationPolicy implements RotationPolicy {
    private static final Logger logger = LoggerFactory.getLogger(FixedTimeRotationPolicy.class);
    private final List<Duration> dailyRotationTimes;

    /**
     * Constructs a {@link FixedTimeRotationPolicy} given a list of milliseconds after midnight to rotateIfNeeded the
     * files.
     *
     * @param rotationTimes List of {@link Duration} objects specifying the time after midnight to rotate the log file.
     */
    public FixedTimeRotationPolicy(final List<Duration> rotationTimes) {
        dailyRotationTimes = rotationTimes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldRotateFile(RotatableObject rotatable) {
        final DateTime currentTime = new DateTime();
        final DateTime midnight = new DateMidnight().toDateTime();
        for (final Duration dailyRotationTime : dailyRotationTimes) {
            final DateTime nextRotationTime = midnight.plus(dailyRotationTime.to(TimeUnit.MILLISECONDS));
            if (currentTime.isAfter(nextRotationTime) && rotatable.getLastRotationTime().isBefore(nextRotationTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the list of times since midnight that rotation will occur at.
     * @return The list of times as {@code Duration} instances.
     */
    public List<Duration> getDailyRotationTimes() {
        return dailyRotationTimes;
    }
}
