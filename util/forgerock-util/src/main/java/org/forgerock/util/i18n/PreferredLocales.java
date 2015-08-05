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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class encapsulates an ordered list of preferred locales, and the logic
 * to use those to retrieve i18n {@code ResourceBundle}s.
 * <p>
 * {@code ResourceBundle}s are found by iterating over the preferred locales
 * and returning the first resource bundle for which a non-ROOT locale is
 * available, that is not listed later in the list, or the ROOT locale if no
 * better match is found.
 * <p>
 * For example, given available locales of {@code en} and {@code fr}:
 * <ul>
 *     <li>
 *         Preferred locales of {@code fr-FR, en}, resource bundle for locale
 *         {@code fr} is returned.
 *     </li>
 *     <li>
 *         Preferred locales of {@code fr-FR, en, fr}, resource bundle for locale
 *         {@code en} is returned ({@code fr} is listed lower than {@code en}).
 *     </li>
 *     <li>
 *         Preferred locales of {@code de}, resource bundle for the ROOT locale
 *         is returned.
 *     </li>
 * </ul>
 */
public class PreferredLocales {

    private final List<Locale> locales;

    /**
     * Create a new preference of locales by copying the provided locales list.
     * @param locales The list of locales that are preferred, with the first item the most preferred.
     */
    public PreferredLocales(List<Locale> locales) {
        if (locales == null || locales.isEmpty()) {
            locales = Collections.singletonList(Locale.ROOT);
        }
        this.locales = Collections.unmodifiableList(locales);
    }

    /**
     * Create a new, empty preference of locales.
     */
    public PreferredLocales() {
        this(null);
    }

    /**
     * The preferred locale, i.e. the head of the preferred locales list.
     * @return The most-preferred locale.
     */
    public Locale getPreferredLocale() {
        return locales.get(0);
    }

    /**
     * The ordered list of preferred locales.
     * @return A mutable copy of the preferred locales list.
     */
    public List<Locale> getLocales() {
        return locales;
    }

    /**
     * Get a {@code ResourceBundle} using the preferred locale list and using the provided
     * {@code ClassLoader}.
     * @param bundleName The of the bundle to load.
     * @param classLoader The {@code ClassLoader} to use to load the bundle.
     * @return The bundle in the best matching locale.
     */
    public ResourceBundle getBundleInPreferredLocale(String bundleName, ClassLoader classLoader) {
        for (Locale locale : locales) {
            ResourceBundle candidate = ResourceBundle.getBundle(bundleName, locale, classLoader);
            if (matches(locale, candidate.getLocale())) {
                return candidate;
            }
        }
        return ResourceBundle.getBundle(bundleName, Locale.ROOT, classLoader);
    }

    /**
     * Is the candidate locale the best match for the requested locale? Exclude {@code Locale.ROOT} unless it
     * is the requested locale, as it should be the fallback only when all locales are tried.
     */
    private boolean matches(Locale requested, Locale candidate) {
        return candidate.equals(requested) || (!Locale.ROOT.equals(candidate) && !locales.contains(candidate));
    }


}
