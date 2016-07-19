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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.util.time;

import static java.lang.Integer.signum;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.time.Duration.UNLIMITED;
import static org.forgerock.util.time.Duration.ZERO;
import static org.forgerock.util.time.Duration.duration;
import static org.forgerock.util.time.DurationAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class DurationTest {

    @Test
    public void testOneMinute() throws Exception {
        assertThat(duration("1 minute")).isEqualTo(1L, TimeUnit.MINUTES);
    }

    @Test
    public void testTwoMinutesAndTwentySeconds() throws Exception {
        assertThat(duration("2 minutes and 20 seconds"))
                .isEqualTo(140L, TimeUnit.SECONDS);
    }

    @Test
    public void testTwoMinutesAndTwentySeconds2() throws Exception {
        assertThat(duration("2 minutes, 20 seconds"))
                .isEqualTo(140L, TimeUnit.SECONDS);
    }

    @Test
    public void testTwoMinutesAndTwentySeconds3() throws Exception {
        assertThat(duration("   2     minutes   and    20   seconds   "))
                .isEqualTo(140L, TimeUnit.SECONDS);
    }

    @Test
    public void testThreeDays() throws Exception {
        assertThat(duration("3 days"))
                .isEqualTo(3L, TimeUnit.DAYS);
    }

    @Test
    public void testCompact() throws Exception {
        assertThat(duration("3d,2h,1m"))
                .isEqualTo(4441L, TimeUnit.MINUTES);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidInput() throws Exception {
        duration(" 3 3 minutes");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidInput2() throws Exception {
        duration("minutes");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidInput3() throws Exception {
        duration("   ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnrecognizedTimeUnit() throws Exception {
        duration("3 blah");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMisUsedUnlimitedKeyword() throws Exception {
        duration("unlimited and 3 minutes");
    }

    @Test
    public void testKnownTimeUnits() throws Exception {

        assertThat(duration("1 days")).isEqualTo(1L, TimeUnit.DAYS);
        assertThat(duration("1 day")).isEqualTo(1L, TimeUnit.DAYS);
        assertThat(duration("1 d")).isEqualTo(1L, TimeUnit.DAYS);

        assertThat(duration("1 hours")).isEqualTo(1L, TimeUnit.HOURS);
        assertThat(duration("1 hour")).isEqualTo(1L, TimeUnit.HOURS);
        assertThat(duration("1 h")).isEqualTo(1L, TimeUnit.HOURS);

        assertThat(duration("1 minutes")).isEqualTo(1L, TimeUnit.MINUTES);
        assertThat(duration("1 minute")).isEqualTo(1L, TimeUnit.MINUTES);
        assertThat(duration("1 min")).isEqualTo(1L, TimeUnit.MINUTES);
        assertThat(duration("1 m")).isEqualTo(1L, TimeUnit.MINUTES);

        assertThat(duration("1 seconds")).isEqualTo(1L, TimeUnit.SECONDS);
        assertThat(duration("1 second")).isEqualTo(1L, TimeUnit.SECONDS);
        assertThat(duration("1 sec")).isEqualTo(1L, TimeUnit.SECONDS);
        assertThat(duration("1 s")).isEqualTo(1L, TimeUnit.SECONDS);

        assertThat(duration("1 milliseconds")).isEqualTo(1L, TimeUnit.MILLISECONDS);
        assertThat(duration("1 millisecond")).isEqualTo(1L, TimeUnit.MILLISECONDS);
        assertThat(duration("1 millisec")).isEqualTo(1L, TimeUnit.MILLISECONDS);
        assertThat(duration("1 millis")).isEqualTo(1L, TimeUnit.MILLISECONDS);
        assertThat(duration("1 milli")).isEqualTo(1L, TimeUnit.MILLISECONDS);
        assertThat(duration("1 ms")).isEqualTo(1L, TimeUnit.MILLISECONDS);

        assertThat(duration("1 microseconds")).isEqualTo(1L, TimeUnit.MICROSECONDS);
        assertThat(duration("1 microsecond")).isEqualTo(1L, TimeUnit.MICROSECONDS);
        assertThat(duration("1 microsec")).isEqualTo(1L, TimeUnit.MICROSECONDS);
        assertThat(duration("1 micros")).isEqualTo(1L, TimeUnit.MICROSECONDS);
        assertThat(duration("1 micro")).isEqualTo(1L, TimeUnit.MICROSECONDS);
        assertThat(duration("1 us")).isEqualTo(1L, TimeUnit.MICROSECONDS);
        assertThat(duration("1 \u03BCs")).isEqualTo(1L, TimeUnit.MICROSECONDS); // lowercase-greek-mu

        assertThat(duration("1 nanoseconds")).isEqualTo(1L, TimeUnit.NANOSECONDS);
        assertThat(duration("1 nanosecond")).isEqualTo(1L, TimeUnit.NANOSECONDS);
        assertThat(duration("1 nanosec")).isEqualTo(1L, TimeUnit.NANOSECONDS);
        assertThat(duration("1 nanos")).isEqualTo(1L, TimeUnit.NANOSECONDS);
        assertThat(duration("1 nano")).isEqualTo(1L, TimeUnit.NANOSECONDS);
        assertThat(duration("1 ns")).isEqualTo(1L, TimeUnit.NANOSECONDS);

        assertThat(duration(1L, TimeUnit.NANOSECONDS)).isEqualTo(1L, TimeUnit.NANOSECONDS);
    }

    @Test
    public void shouldRecognizeZeroDuration() throws Exception {
        assertThat(duration("0 ns")).isZero();
        assertThat(duration("0 day and 0 ms")).isZero();
        assertThat(duration("0 s, 0 ms")).isZero();

        assertThat(duration("zero")).isZero();
        assertThat(duration("disabled")).isZero();

        assertThat(Duration.ZERO.toString()).isEqualTo("ZERO");

        assertThat(Duration.duration(0L, TimeUnit.NANOSECONDS)).isZero();
    }

    @Test
    public void shouldSupportUnlimitedDuration() throws Exception {
        assertThat(duration("unlimited")).isUnlimited();
        assertThat(duration("indefinite")).isUnlimited();
        assertThat(duration("infinity")).isUnlimited();
        assertThat(duration("undefined")).isUnlimited();
        assertThat(duration("none")).isUnlimited();
    }

    @Test
    public void shouldConvertValue() throws Exception {
        assertThat(duration("1 hour").convertTo(TimeUnit.SECONDS))
                .isEqualTo(3600L, TimeUnit.SECONDS);
        assertThat(duration("1 hour").to(TimeUnit.SECONDS))
                .isEqualTo(3600L);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void negativeDurationNotSupported() {
        Duration.duration(-1, TimeUnit.DAYS);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullDurationNotSupported() {
        Duration.duration((Long) null, TimeUnit.DAYS);
    }

    @Test
    public void testUnlimited() {
        assertThat(Duration.UNLIMITED.getUnit()).isEqualTo(TimeUnit.DAYS);
        assertThat(Duration.UNLIMITED.getValue()).isEqualTo(Long.MAX_VALUE);
        assertThat(Duration.UNLIMITED.to(TimeUnit.DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Duration.UNLIMITED.convertTo(TimeUnit.DAYS)).isEqualTo(Duration.UNLIMITED);
        assertThat(Duration.UNLIMITED.toString()).isEqualTo("UNLIMITED");
    }

    @Test
    public void testToString() {
        assertThat(duration("1 minute").toString()).isEqualTo("1 MINUTES");
    }

    @DataProvider
    private Object[][] durations() {
        //@formatter:off
        return new Object[][] {
            { ZERO,                                       ZERO,                                        0 },
            { UNLIMITED,                                  UNLIMITED,                                   0 },
            { ZERO,                                       UNLIMITED,                                  -1 },
            { UNLIMITED,                                  ZERO,                                        1 },
            { ZERO,                                       duration(3, SECONDS),                       -1 },
            { duration(3, SECONDS),                       ZERO,                                        1 },
            { duration(3, SECONDS),                       duration(3, SECONDS),                        0 },
            { duration(3, SECONDS),                       duration(5, SECONDS),                       -1 },
            { duration(3, SECONDS),                       duration(5, DAYS),                          -1 },
            { duration(5, DAYS),                          duration(3, SECONDS),                        1 },
            { duration(1, DAYS),                          duration(24 * 60 * 60, SECONDS),             0 },
            { duration(Long.MAX_VALUE, NANOSECONDS),      duration(1, MILLISECONDS),                   1 },
            { duration(Long.MAX_VALUE, NANOSECONDS),      duration(Long.MAX_VALUE, DAYS),             -1 },
            { duration(Long.MAX_VALUE, NANOSECONDS),      duration(Long.MAX_VALUE - 1, DAYS),         -1 },
            { duration(Long.MAX_VALUE - 1, NANOSECONDS),  duration(Long.MAX_VALUE, DAYS),             -1 },
            // 9223372036854 ms -> 9223372036854000000 ns. So let's compare with 1 more millisecond (that will
            // cause an overflow during the conversion from ms to ns)
            { duration(9223372036854L + 1, MILLISECONDS), duration(9223372036854000000L, NANOSECONDS), 1 }
        };
        //@formatter:on
    }

    @Test(dataProvider = "durations")
    public void shouldCompare(Duration left, Duration right, int expected) throws Exception {
        assertThat(signum(left.compareTo(right))).isEqualTo(expected);
    }
}
