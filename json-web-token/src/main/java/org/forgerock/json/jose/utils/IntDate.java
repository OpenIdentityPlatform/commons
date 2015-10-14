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

import java.util.Date;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;

/**
 * This class provides utility methods for converting Java Date objects into and from IntDates.
 * <p>
 * Where an IntDate is a JSON numeric value representing the number of seconds from 1970-01-01T0:0:0Z UTC until the
 * specified UTC date/time.
 *
 * @see <a href="http://tools.ietf.org/html/draft-jones-json-web-token-10#section-2">IntDate</a>
 *
 * @since 2.0.0
 */
public final class IntDate {

    /**
     * Private constructor.
     */
    private IntDate() {
    }

    /**
     * Converts a Java Date object into an IntDate.
     * <p>
     * <strong>Note:</strong> Precision is lost in this conversion as the milliseconds are dropped as a part of the
     * conversion.
     *
     * @param date The Java Date to convert.
     * @return The IntDate representation of the Java Date.
     */
    public static long toIntDate(Date date) {
        if (date == null) {
            throw new JwtRuntimeException("Null date cannot be converted to IntDate");
        }
        return date.getTime() / 1000L;
    }

    /**
     * Converts an IntDate into a Java Date object.
     * <p>
     * <strong>Note:</strong> Milliseconds are set to zero on the converted Java Date as an IntDate does not hold
     * millisecond information.
     *
     * @param intDate The IntDate to convert.
     * @return The Java Date representation of the IntDate.
     */
    public static Date fromIntDate(long intDate) {
        return new Date(intDate * 1000L);
    }
}
