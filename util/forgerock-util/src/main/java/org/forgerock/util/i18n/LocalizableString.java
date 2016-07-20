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

/**
 * Represents a String which could be localizable. If it is localizable it needs to be in the following format,
 * {@code i18n:bundle#key} which is a URI where:
 * <ul>
 *    <li>{@code i18n:} is the scheme specifying that the string is localizable</li>
 *    <li>{@code bundle} is the path of the bundle in the classpath</li>
 *    <li>{@code key}, the fragment, is the key of the translated string</li>
 * </ul>
 * This class attempts to make the i18n work for an OSGi environment, by encapsulating the name of a resource bundle,
 * the key in the resource bundle to use, and the {@code ClassLoader} in which the resource bundle can be found, in the
 * assumption that when it comes to serializing this object, the calling code (e.g. HttpFrameworkServlet and the
 * Grizzly HandlerAdapter) is in a different classloader and so will not have direct access to the resource bundle.
 */
public class LocalizableString {

    /**
     * A constant used to indicate a string should be translated.
     */
    public static final String TRANSLATION_KEY_PREFIX = "i18n:";

    private final ClassLoader loader;
    private final String value;
    private final URI resource;

    /**
     * String only constructor for non-localizable {@code String} values.
     * @param value a string
     */
    public LocalizableString(String value) {
        this(value, null);
    }

    /**
     * Constructor for potentially localizable {@code String}.
     * @param value the String
     * @param loader the {@code ClassLoader} where the string definition should be obtained
     */
    public LocalizableString(String value, ClassLoader loader) {
        this.loader = loader;
        this.value = value;

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
                // the bundle wasn't found, so we return the fragment
                return resource.getFragment();
            }

        }
    }

    /**
     * The default toString method. No translation is applied.
     * @return the untranslated string value
     */
    public String toString() {
        return this.value;
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

        return value.equals(that.value);

    }

    /**
     * Default hashcode implementation.
     * @return the hashcode of the string
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
