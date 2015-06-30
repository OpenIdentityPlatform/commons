/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the License.
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.forgerock.http.protocol.Header;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A header class representing the Accept-Language HTTP header. String values will include quality
 * attributes to communicate order of preference expressed in the list of {@code Locale} objects
 * contained within.
 */
public class AcceptLanguageHeader implements Header {

    public static AcceptLanguageHeader valueOf(PreferredLocales locales) {
        return new AcceptLanguageHeader(locales);
    }

    public static AcceptLanguageHeader valueOf(List<Locale> locales) {
        return valueOf(new PreferredLocales(locales));
    }

    private final PreferredLocales locales;

    private AcceptLanguageHeader(PreferredLocales locales) {
        this.locales = locales;
    }

    /**
     * Returns the {@code PreferredLocales} instance that represents this header.
     */
    public PreferredLocales getLocales() {
        return locales;
    }

    @Override
    public String getName() {
        return "Accept-Language";
    }

    @Override
    public String toString() {
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

        return valueString.toString();
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
}
