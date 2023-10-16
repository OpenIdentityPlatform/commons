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

package org.forgerock.selfservice.stages.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * Utility class for Locales.
 *
 * @since 0.8.0
 */
public final class LocaleUtils {

    private LocaleUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Using the user's preferred locales (for example, from the "Accept-Language" header in the HTTP context),
     * select the most optimal (string) translation from the map. If it is omitted or matches no supported locales, use the default locale setting.
	 * If there is nothing acceptable, throw an exception.
     *
     * @param preferredLocales
     *         the preferred locales
     * @param translations
     *         Map of locales to strings
     *
     * @return the most appropriate string given the above
     *
     * @throws IllegalArgumentException
     *         If an acceptable string cannot be found
     */
    public static String getTranslationFromLocaleMap(
            PreferredLocales preferredLocales, Map<Locale, String> translations) {
        List<Locale> locales = getTargetLocales(preferredLocales);
        for (Locale locale : locales) {
            String translation = getTranslation(locales, locale, translations);
            if (translation != null) {
                return translation;
            }
        }
        throw new IllegalArgumentException("Cannot find suitable translation from given choices");
    }

    /**
     * Returns a list of target locales. The default locale(s) will be included at the end of the list.
     * If {@link Locale.ROOT} is specified, the default locale(s) will be included in its place. In this case, the default locale(s) will not be appended to the end of the list.
	 * <p>The default locale is specified by:
	 * <ol>
	 * <li>The <tt>org.forgerock.selfservice.defaultLocale</tt> system property.</li>
	 * <li>The JVM's default locale.</li>
	 * <ol>
	 * </p>
     *
     * @param preferredLocales
     *         the preferred locales
     *
     * @return the target locales, with the default locale(s) included.
     */
    private static List<Locale> getTargetLocales(PreferredLocales preferredLocales) {
        Locale defaultLocale = Locale.forLanguageTag(System.getProperty("org.forgerock.selfservice.defaultLocale", "en-US"));
        boolean defaultsAdded = false;
        List<Locale> locales = new ArrayList<>();
        for (Locale locale : preferredLocales.getLocales()) {
            if (locale.equals(Locale.ROOT) && !defaultsAdded) {
                locales.add(defaultLocale);
                locales.add(Locale.getDefault());
                defaultsAdded = true;
                continue;
            }
            if (!locale.equals(Locale.ROOT))
                locales.add(locale);
        }
        if (!defaultsAdded) {
            locales.add(defaultLocale);
            locales.add(Locale.getDefault());
        }
        return locales;
    }

    /**
     * Retrieves the appropriate translation.
     *
     * @param locales
     *         The preferred locales
     * @param locale
     *         The desired locale
     * @param translations
     *         The translations
     *
     * @return A translation or null if not available
     */
    public static String getTranslation(List<Locale> locales, Locale locale, Map<Locale, String> translations) {
        String translation = translations.get(locale);
        if (translation != null) {
            return translation;
        }
        String languageTag = locale.toLanguageTag();
        int parentTagEnd = languageTag.lastIndexOf("-");
        if (parentTagEnd > -1) {
            Locale parent = Locale.forLanguageTag(languageTag.substring(0, parentTagEnd));
            if (!locales.contains(parent)) {
                return getTranslation(locales, parent, translations);
            }
        }
        return null;
    }

}
