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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.MissingResourceException;
import java.util.Objects;

/**
 * Represents a String which could be localizable. If it is localizable it needs to be in the following format,
 * {@code i18n:bundle#key} which is a URI where:
 * <ul>
 *    <li>{@code i18n:} is the scheme specifying that the string is localizable</li>
 *    <li>{@code bundle} is the path of the bundle in the classpath (optional, if missing,
 *    a {@link Class} has to be provided and will be used as resource bundle name)</li>
 *    <li>{@code key}, the fragment, is the key of the translated string</li>
 * </ul>
 * This class attempts to make the i18n work for an OSGi environment, by encapsulating the name of a resource bundle,
 * the key in the resource bundle to use, and the {@code ClassLoader} in which the resource bundle can be found, in the
 * assumption that when it comes to serializing this object, the calling code (e.g. HttpFrameworkServlet and the
 * Grizzly HandlerAdapter) is in a different classloader and so will not have direct access to the resource bundle.
 * <p>
 *     A default value {@code LocalizableString} can be provided so that if the key is not found in the bundle, another
 *     value can be specified, which could be either another bundle reference, or a plain value.
 * </p>
 */
public class LocalizableString {

    /**
     * A constant used to indicate a string should be translated.
     */
    public static final String TRANSLATION_KEY_PREFIX = "i18n:";

    private final ClassLoader loader;
    private final String value;
    private final URI resource;
    private final LocalizableString defaultValue;

    /**
     * String only constructor for non-localizable {@code String} values.
     * @param value a string
     */
    public LocalizableString(String value) {
        this(value, (ClassLoader) null);
    }

    /**
     * Constructor for potentially localizable {@code String}.
     * If resource bundle name not provided in the {@code value}, then the provided {@code type} name will be
     * used instead.
     * @param value the String ({@literal i18n:#key.name} is accepted here)
     * @param type class used to support relative resource bundle lookup (must not be {@code null})
     */
    public LocalizableString(String value, Class<?> type) {
        // Integrates class name as the resource name in the value
        this(value.replace(":#", ":" + type.getName().replace(".", "/") + "#"), type.getClassLoader());
    }

    /**
     * Constructor for potentially localizable {@code String}.
     * @param value the String
     * @param loader the {@code ClassLoader} where the string definition should be obtained
     */
    public LocalizableString(String value, ClassLoader loader) {
        this(value, loader, null);
    }

    /**
     * Constructor for potentially localizable {@code String}. If a default value is not specified, if the {@code key}
     * is a valid URI, its fragment will be used, and otherwise the whole {@code key} value will be used.
     * @param key the localizable key
     * @param loader the {@code ClassLoader} where the string definition should be obtained
     * @param defaultValue the default value to use if not localizable.
     */
    public LocalizableString(String key, ClassLoader loader, LocalizableString defaultValue) {
        this.loader = loader;
        this.value = key;
        this.defaultValue = defaultValue;

        URI resource = null;
        if (value != null && value.startsWith(TRANSLATION_KEY_PREFIX) && loader != null) {
            try {
                resource = new URI(value);
            } catch (URISyntaxException e) {
                // empty - use original value
            }
        }
        this.resource = resource;
    }

    /**
     * Returns the contained string, translated if applicable.
     * @param locales The preferred locales for the translation.
     * @return the translated string
     */
    public String toTranslatedString(PreferredLocales locales) {
        if (resource == null) {
            return this.value;
        } else {
            try {
                return locales.getBundleInPreferredLocale(resource.getSchemeSpecificPart(), loader)
                        .getString(resource.getFragment());
            } catch (MissingResourceException e) {
                // the bundle wasn't found, so we use the default value, or return the fragment
                return defaultValue == null ? resource.getFragment() : defaultValue.toTranslatedString(locales);
            }
        }
    }

    /**
     * The default toString method. No translation is applied.
     * @return the untranslated string value
     */
    public String toString() {
        return defaultValue == null ? value : "[" + value + "], default [" + defaultValue + "]";
    }

    /**
     * The default equals operation.
     * @param o another object
     * @return true if the other object is equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LocalizableString that = (LocalizableString) o;

        return value.equals(that.value) && Objects.equals(defaultValue, that.defaultValue);

    }

    /**
     * Default hashcode implementation.
     * @return the hashcode of the string
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, defaultValue);
    }
}
