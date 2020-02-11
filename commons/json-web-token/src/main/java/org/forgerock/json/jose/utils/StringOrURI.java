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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.utils;

import java.net.URI;
import java.net.URISyntaxException;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;

/**
 * This class provides an utility method for validating that a String is either an arbitrary string without any ":"
 * characters or if the String does contain a ":" character then the String is a valid URI.
 *
 * @see <a href="http://tools.ietf.org/html/draft-jones-json-web-token-10#section-2">StringOrURI</a>
 *
 * @since 2.0.0
 */
public final class StringOrURI {

    /**
     * Private constructor.
     */
    private StringOrURI() {
    }

    /**
     * Validates that the given String is either an arbitrary string without any ":" characters, otherwise validates
     * that the String is a valid URI.
     *
     * @param s The String to validate.
     * @throws JwtRuntimeException if the given String contains a ":" character and is not a valid URI.
     */
    public static void validateStringOrURI(String s) {
        if (s != null && s.contains(":")) {
            try {
                new URI(s);
            } catch (URISyntaxException e) {
                throw new JwtRuntimeException(e);
            }
        }
    }
}
