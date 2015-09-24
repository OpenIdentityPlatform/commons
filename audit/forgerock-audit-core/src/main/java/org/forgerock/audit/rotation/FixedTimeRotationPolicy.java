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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rotates audit files at fixed times throughout the day.
 */
public class FixedTimeRotationPolicy implements RotationPolicy {
    private static final Logger logger = LoggerFactory.getLogger(FixedTimeRotationPolicy.class);
    private List<org.forgerock.util.time.Duration> rotationTimes = new LinkedList<>();

    /**
     * Constructs a {@link FixedTimeRotationPolicy} given a list of milliseconds after midnight to rotateIfNeeded the files.
     *
     * @param rotationTimes List of milliseconds after midnight to rotateIfNeeded the log file. For example a list consisting of
     *                      [10,20,30] will rotateIfNeeded the log file 10, 20, and 30 milliseconds after midnight.
     */
    public FixedTimeRotationPolicy(final List<String> rotationTimes ) {
        for (final String rotationTime : rotationTimes) {
            try {
                this.rotationTimes.add(org.forgerock.util.time.Duration.duration(rotationTime));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid duration for: {}", rotationTime);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldRotateFile(RotatableObject file) {
        final DateTime currentTime = new DateTime();
        final DateTime midnight = new DateMidnight().toDateTime();
        for (final org.forgerock.util.time.Duration msSinceBeginningOfDay : rotationTimes) {
            final DateTime nextRotationTime = midnight.plus(msSinceBeginningOfDay.to(TimeUnit.MILLISECONDS));
            if (currentTime.isAfter(nextRotationTime) && file.getLastRotationTime().isBefore(nextRotationTime)) {
                return true;
            }
        }
        return false;
    }
}
