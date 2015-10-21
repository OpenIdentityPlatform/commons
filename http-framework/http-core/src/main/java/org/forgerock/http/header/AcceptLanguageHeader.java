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

package org.forgerock.http.header;

import static java.util.Collections.*;
import static org.forgerock.http.header.HeaderUtil.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.forgerock.http.protocol.Header;
import org.forgerock.util.Pair;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A header class representing the Accept-Language HTTP header. String values will include quality
 * attributes to communicate order of preference expressed in the list of {@code Locale} objects
 * contained within.
 */
public final class AcceptLanguageHeader extends Header {

    /**
     * The name of the header.
     */
    public static final String NAME = "Accept-Language";
    private static final Comparator<Pair<Locale, BigDecimal>> LOCALES_QUALITY_COMPARATOR =
        new Comparator<Pair<Locale, BigDecimal>>() {
            @Override
            public int compare(Pair<Locale, BigDecimal> o1, Pair<Locale, BigDecimal> o2) {
                return o2.getSecond().compareTo(o1.getSecond());
            }
        };

    /**
     * Creates an accept language header representation for a {@code PreferredLocales} instance.
     * @param locales The preferred locales.
     * @return The header.
     */
    public static AcceptLanguageHeader valueOf(PreferredLocales locales) {
        return new AcceptLanguageHeader(locales);
    }

    /**
     * Create a header from a list of preferred {@code Locale} instances.
     * @param locales The preferred locales.
     * @return The header.
     */
    public static AcceptLanguageHeader valueOf(List<Locale> locales) {
        return valueOf(new PreferredLocales(locales));
    }

    /**
     * Create a header from a list of preferred {@code Locale} language tags.
     * @param languageTags The preferred locale language tags.
     * @return The header.
     */
    public static AcceptLanguageHeader valueOf(String... languageTags) {
        List<Locale> locales = new ArrayList<>();
        for (String languageTag : languageTags) {
            locales.add(Locale.forLanguageTag(languageTag));
        }
        return valueOf(new PreferredLocales(locales));
    }

    /**
     * Create a header from a list of header values.
     * @param headerValues The Accept-Language header values.
     * @return The header.
     */
    public static AcceptLanguageHeader valueOf(Set<String> headerValues) {
        if (headerValues == null || headerValues.isEmpty()) {
            return null;
        }

        List<Pair<Locale, BigDecimal>> localeWeightings = new ArrayList<>();
        for (String language : split(join(headerValues, ','), ',')) {
            List<String> values = split(language, ';');
            BigDecimal quality = BigDecimal.ONE;
            if (values.size() == 2) {
                String[] parameter = parseParameter(values.get(1).trim());
                if (!"q".equals(parameter[0])) {
                    throw new IllegalArgumentException("Unrecognised parameter: " + parameter[0]);
                }
                quality = new BigDecimal(parameter[1].trim());
            } else if (values.size() != 1) {
                throw new IllegalArgumentException("Unrecognised parameter(s): " + language);
            }
            localeWeightings.add(Pair.of(Locale.forLanguageTag(values.get(0).trim()), quality));
        }

        sort(localeWeightings, LOCALES_QUALITY_COMPARATOR);
        List<Locale> locales = new ArrayList<>(localeWeightings.size());
        for (Pair<Locale, BigDecimal> locale : localeWeightings) {
            locales.add(locale.getFirst());
        }
        return new AcceptLanguageHeader(new PreferredLocales(locales));
    }

    private final PreferredLocales locales;

    private AcceptLanguageHeader(PreferredLocales locales) {
        this.locales = locales;
    }

    /**
     * Returns the {@code PreferredLocales} instance that represents this header.
     * @return The instance.
     */
    public PreferredLocales getLocales() {
        return locales;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getValues() {
        StringBuilder valueString = new StringBuilder();
        final List<Locale> locales = this.locales.getLocales();
        BigDecimal qualityStep = getQualityStep(locales.size());
        BigDecimal quality = BigDecimal.ONE;

        for (Locale locale : locales) {
            if (valueString.length() != 0) {
                valueString.append(",");
            }
            valueString.append(locale.equals(Locale.ROOT) ? "*" : locale.toLanguageTag())
                    .append(";q=")
                    .append(quality.toString());
            quality = quality.subtract(qualityStep);
        }

        return singletonList(valueString.toString());
    }

    static BigDecimal getQualityStep(int numberLocales) {
        if (numberLocales <= 1) {
            return BigDecimal.ONE;
        }
        // Find the value to decrement quality by
        // This results in 0.1 for up to 10 locales, 0.01 for up to 100, etc.
        int nextPowerOfTen = (int) Math.ceil(Math.log10((double) numberLocales));
        return BigDecimal.ONE.divide(BigDecimal.TEN.pow(nextPowerOfTen));
    }

    static class Factory extends HeaderFactory<AcceptLanguageHeader> {

        @Override
        public AcceptLanguageHeader parse(String value) {
            return valueOf(singleton(value));
        }

        @Override
        public AcceptLanguageHeader parse(List<String> values) {
            return valueOf(new LinkedHashSet<>(values));
        }
    }
}
