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

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.Test;

public class AcceptLanguageHeaderTest {

    @Test
    public void testAcceptLanguageHeaderMultipleLanguages() {
        // Given
        AcceptLanguageHeader header = AcceptLanguageHeader.valueOf(new PreferredLocales(asList(
                Locale.forLanguageTag("en"),
                Locale.forLanguageTag("fr-FR"),
                Locale.forLanguageTag("fr")
        )));

        // When
        String languageHeader = header.getValues().iterator().next();

        // Then
        assertThat(languageHeader).isEqualTo("en;q=1,fr-FR;q=0.9,fr;q=0.8");
    }

    @Test
    public void testEmptyAcceptLanguageHeadersReturnsNull() {
        assertThat(AcceptLanguageHeader.valueOf(Collections.<String>emptySet())).isNull();
        assertThat(AcceptLanguageHeader.valueOf((Set<String>) null)).isNull();
    }

    @Test
    public void testValueOfAcceptLanguageHeaders() {
        // Given
        Set<String> headerValues = new HashSet<>(Arrays.asList("en,fr;q=1.0,de-DE;q=0.7", "pl;q=0.8"));

        // When
        AcceptLanguageHeader header = AcceptLanguageHeader.valueOf(headerValues);

        // Then
        assertThat(header.getLocales().getLocales()).containsExactly(
                Locale.forLanguageTag("en"),
                Locale.forLanguageTag("fr"),
                Locale.forLanguageTag("pl"),
                Locale.forLanguageTag("de-DE")
        );
    }

    @Test
    public void testQualityDecrement() {
        assertThat(AcceptLanguageHeader.getQualityStep(0)).isEqualTo(BigDecimal.ONE);
        assertThat(AcceptLanguageHeader.getQualityStep(1)).isEqualTo(BigDecimal.ONE);
        assertThat(AcceptLanguageHeader.getQualityStep(2)).isEqualTo(BigDecimal.ONE.divide(BigDecimal.TEN));
        assertThat(AcceptLanguageHeader.getQualityStep(12)).isEqualTo(BigDecimal.ONE.divide(BigDecimal.TEN.pow(2)));
    }

}
