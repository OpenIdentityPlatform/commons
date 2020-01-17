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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.util.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Locale;

import org.testng.annotations.Test;

public class LocalizableStringTest {

    @Test
    public void testNormalStringIsNotTranslated() {
        PreferredLocales locales = new PreferredLocales();
        LocalizableString name = new LocalizableString("locale", getClass().getClassLoader());
        assertThat(name.toTranslatedString(locales)).isEqualTo("locale");
    }

    @Test
    public void testLocalizableStringIsTranslated() {
        PreferredLocales locales = new PreferredLocales();
        LocalizableString name = new LocalizableString("i18n:locales/bundle#locale", getClass().getClassLoader());
        assertThat(name.toTranslatedString(locales)).isEqualTo("ROOT");
    }

    @Test
    public void testLocalizableStringIsNotTranslatedIfLoaderIsNull() {
        PreferredLocales locales = new PreferredLocales();
        LocalizableString name = new LocalizableString("i18n:locales/bundle#locale");
        assertThat(name.toTranslatedString(locales)).isEqualTo("i18n:locales/bundle#locale");
    }

    @Test
    public void testLocalizableStringIsNotTranslatedIfUriIsInvalid() {
        PreferredLocales locales = new PreferredLocales();
        LocalizableString name = new LocalizableString("i18n:nosuch$%^", getClass().getClassLoader());
        assertThat(name.toTranslatedString(locales)).isEqualTo("i18n:nosuch$%^");
    }

    @Test
    public void testLocalizableStringThrowsIfBundleIsInvalid() {
        PreferredLocales locales = new PreferredLocales();
        LocalizableString name = new LocalizableString("i18n:foo#locale", getClass().getClassLoader());
        assertThat(name.toTranslatedString(locales)).isEqualTo("locale");
    }

    @Test
    public void testLocalizableStringIsTranslatedSpecifiedLocales() {
        PreferredLocales locales = new PreferredLocales(Arrays.asList(new Locale("fr")));

        LocalizableString name = new LocalizableString("i18n:locales/bundle#locale", getClass().getClassLoader());
        assertThat(name.toTranslatedString(locales)).isEqualTo("French");
    }
}
