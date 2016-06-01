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
package org.forgerock.api.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Class to do the translation of a string using resource bundle.
 */
public final class Translator {

    /**
     * Description prefix that has to be followed by resource boundle name and the resource boundle key.
     * Translatable description value should look like:
     * TRANSLATION_KEY_PREFIX + "resource boundle name" + "#somekey"
     * forexample: "i18n:api-dictionary#read_operation_desc1"
     */
    public static final String TRANSLATION_KEY_PREFIX = "i18n:";

    private static ThreadLocal<Locale> threadLocalLocale = new ThreadLocal<>();
    private static ThreadLocal<ClassLoader> threadLocalClassLoader = new ThreadLocal<>();

    /**
     * Constructor with locale.
     */
    public Translator() {
    }

    /**
     * Translates the string parameter if starts with the {@code TRANSLATION_KEY_PREFIX} and
     * if it contains a key in the resource bundle.
     * @param description Translatable string
     * @return Translated string
     */
    public String translate(String description) {
        return hasTranslationKeyPrefix(description)
                ? doTranslate(description.substring(TRANSLATION_KEY_PREFIX.length()))
                : description;
    }

    private String doTranslate(String description) {
        String[] descriptionSplit = description.split("#");
        ResourceBundle dictionary = ResourceBundle.getBundle(
                descriptionSplit[0],
                threadLocalLocale.get(),
                threadLocalClassLoader.get());
        String resourceKey = descriptionSplit[1];
        return dictionary.getString(resourceKey);
    }

    private boolean hasTranslationKeyPrefix(String s) {
        return !ValidationUtil.isEmpty(s) && s.startsWith(TRANSLATION_KEY_PREFIX);
    }

    /**
     * Static setter of the locale. The locale will be stored in static ThreadLocale object.
     * @param locale The locale
     */
    public static void setLocale(Locale locale) {
        threadLocalLocale.set(locale);
    }

    /**
     * Static setter of the classLoader. The classloader will needed to get the resource boundle.
     * @param classLoader ClassLoader
     */
    public static void setClassLoader(ClassLoader classLoader) {
        threadLocalClassLoader.set(classLoader);
    }

    /**
     * Static classLoader remover.
     */
    public static void removeClassLoader() {
        threadLocalClassLoader.remove();
    }
}
