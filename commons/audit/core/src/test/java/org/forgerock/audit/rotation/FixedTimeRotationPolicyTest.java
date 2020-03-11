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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.time.Duration.duration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.forgerock.util.time.Duration;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

public class FixedTimeRotationPolicyTest {

    public static final String ONE_MINUTE = "1 minute";

    @Test
    public void testRotation() {
        // given
        // rotateIfNeeded 50 ms after midnight
        final List<Duration> rotationTimes = Collections.singletonList(Duration.duration(ONE_MINUTE));
        final FixedTimeRotationPolicy rotationPolicy = new FixedTimeRotationPolicy(rotationTimes);
        final RotatableObject rotatableObject = mock(RotatableObject.class);
        final DateTime midnight = new DateMidnight().toDateTime();
        when(rotatableObject.getLastRotationTime()).thenReturn(midnight);

        // when
        final boolean rotate = rotationPolicy.shouldRotateFile(rotatableObject);

        // then
        final DateTime currentTime = new DateTime();
        if (currentTime.isAfter(midnight.plus(duration(ONE_MINUTE).to(TimeUnit.MILLISECONDS)))) {
            assertThat(rotate).isTrue();
        } else {
            assertThat(rotate).isFalse();
        }
    }
}
