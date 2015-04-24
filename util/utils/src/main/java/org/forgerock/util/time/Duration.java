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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.util.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.util.Reject.checkNotNull;

/**
 * Represents a duration in english. Cases is not important, plurals units are accepted.
 * Notice that negative durations are not supported.
 *
 * <code>
 *     6 days
 *     59 minutes and 1 millisecond
 *     1 minute and 10 seconds
 *     42 millis
 *     unlimited
 *     none
 *     zero
 * </code>
 */
public class Duration {

    /**
     * Special duration that represents an unlimited duration (or indefinite).
     */
    private static final Duration UNLIMITED = new Duration(Long.MAX_VALUE, DAYS);

    /**
     * Special duration that represents a zero-length duration.
     */
    private static final Duration ZERO = new Duration(0L, SECONDS);

    /**
     * Tokens that represents the unlimited duration.
     */
    private static final Set<String> UNLIMITED_TOKENS = new TreeSet<String>(
            String.CASE_INSENSITIVE_ORDER);
    static {
        UNLIMITED_TOKENS.addAll(asList("unlimited", "indefinite", "infinity", "undefined"));
    }

    /**
     * Tokens that represents the zero duration.
     */
    private static final Set<String> ZERO_TOKENS = new TreeSet<String>(
            String.CASE_INSENSITIVE_ORDER);
    static {
        ZERO_TOKENS.addAll(asList("zero", "disabled"));
    }

    private Long number;
    private TimeUnit unit;

    /**
     * Builds a new Duration.
     * @param number number of time unit (cannot be {@literal null}).
     * @param unit TimeUnit to express the duration in (cannot be {@literal null}).
     */
    public Duration(final Long number, final TimeUnit unit) {
        this.number = checkNotNull(number);
        this.unit = checkNotNull(unit);
    }

    /**
     * Builds a new {@link Duration} that will represents the given duration expressed in english.
     *
     * @param value
     *         natural speech duration
     * @return a new {@link Duration}
     * @throws IllegalArgumentException
     *         if the input string is incorrectly formatted.
     */
    public static Duration duration(final String value) {
        List<Duration> composite = new ArrayList<Duration>();

        // Split around ',' and ' and ' patterns
        String[] fragments = value.split(",| and ");

        // If there is only 1 fragment and that it matches the recognized "unlimited" tokens
        if (fragments.length == 1) {
            String trimmed = fragments[0].trim();
            if (UNLIMITED_TOKENS.contains(trimmed)) {
                // Unlimited Duration
                return UNLIMITED;
            } else if (ZERO_TOKENS.contains(trimmed)) {
                // Zero-length Duration
                return ZERO;
            }
        }

        // Build the normal duration
        for (String fragment : fragments) {
            fragment = fragment.trim();

            if ("".equals(fragment)) {
                throw new IllegalArgumentException("Cannot parse empty duration, expecting '<value> <unit>' pattern");
            }

            // Parse the number part
            int i = 0;
            StringBuilder numberSB = new StringBuilder();
            while (Character.isDigit(fragment.charAt(i))) {
                numberSB.append(fragment.charAt(i));
                i++;
            }

            // Ignore whitespace
            while (Character.isWhitespace(fragment.charAt(i))) {
                i++;
            }

            // Parse the time unit part
            StringBuilder unitSB = new StringBuilder();
            while ((i < fragment.length()) && Character.isLetter(fragment.charAt(i))) {
                unitSB.append(fragment.charAt(i));
                i++;
            }
            Long number = Long.valueOf(numberSB.toString());
            TimeUnit unit = parseTimeUnit(unitSB.toString());

            composite.add(new Duration(number, unit));
        }

        // Merge components of the composite together
        Duration duration = new Duration(0L, DAYS);
        for (Duration elements : composite) {
            duration.merge(elements);
        }

        // If someone used '0 ms' for example
        if (duration.number == 0L) {
            return ZERO;
        }

        return duration;
    }

    /**
     * Aggregates this Duration with the given Duration. Littlest {@link TimeUnit} will be used as a common ground.
     *
     * @param duration
     *         other Duration
     */
    private void merge(final Duration duration) {
        // find littlest unit
        // conversion will happen on the littlest unit otherwise we loose details
        if (unit.ordinal() > duration.unit.ordinal()) {
            // Other duration is smaller than me
            number = duration.unit.convert(number, unit) + duration.number;
            unit = duration.unit;
        } else {
            // Other duration is greater than me
            number = unit.convert(duration.number, duration.unit) + number;
        }
    }

    private static Map<String, TimeUnit> TIME_UNITS = new HashMap<String, TimeUnit>();
    static {
        for (String days : asList("days", "day", "d")) {
            TIME_UNITS.put(days, DAYS);
        }
        for (String hours : asList("hours", "hour", "h")) {
            TIME_UNITS.put(hours, HOURS);
        }
        for (String minutes : asList("minutes", "minute", "min", "m")) {
            TIME_UNITS.put(minutes, MINUTES);
        }
        for (String seconds : asList("seconds", "second", "sec", "s")) {
            TIME_UNITS.put(seconds, SECONDS);
        }
        for (String ms : asList("milliseconds", "millisecond", "millisec", "millis", "milli", "ms")) {
            TIME_UNITS.put(ms, MILLISECONDS);
        }
        for (String us : asList("microseconds", "microsecond", "microsec", "micros", "micro", "us")) {
            TIME_UNITS.put(us, MICROSECONDS);
        }
        for (String ns : asList("nanoseconds", "nanosecond", "nanosec", "nanos", "nano", "ns")) {
            TIME_UNITS.put(ns, NANOSECONDS);
        }
    }

    /**
     * Parse the given input string as a {@link TimeUnit}.
     */
    private static TimeUnit parseTimeUnit(final String unit) {
        final String lowercase = unit.toLowerCase(Locale.ENGLISH);
        final TimeUnit timeUnit = TIME_UNITS.get(lowercase);
        if (timeUnit != null) {
            return timeUnit;
        }
        throw new IllegalArgumentException(format("TimeUnit %s is not recognized", unit));
    }

    /**
     * Returns the number of {@link TimeUnit} this duration represents.
     *
     * @return the number of {@link TimeUnit} this duration represents.
     */
    public long getValue() {
        return number;
    }

    /**
     * Returns the {@link TimeUnit} this duration is expressed in.
     *
     * @return the {@link TimeUnit} this duration is expressed in.
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Convert the current duration to a given {@link TimeUnit}.
     * Conversions from finer to coarser granularities truncate, so loose precision.
     *
     * @param targetUnit
     *         target unit of the conversion.
     * @return converted duration
     * @see TimeUnit#convert(long, TimeUnit)
     */
    public Duration convertTo(TimeUnit targetUnit) {
        return new Duration(targetUnit.convert(number, unit), targetUnit);
    }

    /**
     * Convert the current duration to a number of given {@link TimeUnit}.
     * Conversions from finer to coarser granularities truncate, so loose precision.
     *
     * @param targetUnit
     *         target unit of the conversion.
     * @return converted duration value
     * @see TimeUnit#convert(long, TimeUnit)
     */
    public long to(TimeUnit targetUnit) {
        return convertTo(targetUnit).getValue();
    }

    /**
     * Returns {@literal true} if this Duration represents an unlimited duration.
     *
     * @return {@literal true} if this Duration represents an unlimited duration.
     */
    public boolean isUnlimited() {
        return this == UNLIMITED;
    }

    /**
     * Returns {@literal true} if this Duration represents a zero-length duration.
     *
     * @return {@literal true} if this Duration represents a zero-length duration.
     */
    public boolean isZero() {
        return this == ZERO;
    }
}
