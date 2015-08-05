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

package org.forgerock.util.i18n;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.testng.annotations.Test;

public class PreferredLocalesTest {

    static final String BUNDLE_NAME = "locales/bundle";

    @Test
    public void testGetBundleNoPreferredLocale() throws Exception {
        // Given
        PreferredLocales locales = new PreferredLocales(null);

        // When
        ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE_NAME, getClass().getClassLoader());

        // Then
        assertThat(bundle.getString("locale")).isEqualTo("ROOT");
    }

    @Test
    public void testGetBundlePreferredLocale() throws Exception {
        // Given
        PreferredLocales locales = new PreferredLocales(singletonList(Locale.forLanguageTag("fr")));

        // When
        ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE_NAME, getClass().getClassLoader());

        // Then
        assertThat(bundle.getString("locale")).isEqualTo("French");
    }

    @Test
    public void testGetBundlePreferredLocaleNotFound() throws Exception {
        // Given
        PreferredLocales locales = new PreferredLocales(asList(Locale.forLanguageTag("en"), Locale.forLanguageTag("de")));

        // When
        ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE_NAME, getClass().getClassLoader());

        // Then
        assertThat(bundle.getString("locale")).isEqualTo("ROOT");
    }

    @Test
    public void testGetBundlePreferredLocaleUseLast() throws Exception {
        // Given
        PreferredLocales locales = new PreferredLocales(
                asList(Locale.forLanguageTag("en"), Locale.forLanguageTag("de"), Locale.forLanguageTag("fr-FR")));

        // When
        ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE_NAME, getClass().getClassLoader());

        // Then
        assertThat(bundle.getString("locale")).isEqualTo("French");
    }

    @Test
    public void testGetBundlePreferredLocaleSkipFallbackIfListed() throws Exception {
        // Given
        PreferredLocales locales = new PreferredLocales(
                asList(Locale.forLanguageTag("fr-FR"), Locale.forLanguageTag("fr-CA"), Locale.forLanguageTag("fr")));

        // When
        ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE_NAME, getClass().getClassLoader());

        // Then
        assertThat(bundle.getString("locale")).isEqualTo("Canadian French");
    }
}