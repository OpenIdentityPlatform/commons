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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JwtHeaderKeyTest {

    @Test
    public void shouldGetValue() {

        //Given
        for (final JwtHeaderKey item : JwtHeaderKey.values()) {
            //When
            final String value = item.value();

            //Then
            assertEquals(value, item.name().toLowerCase(Locale.ROOT));
        }
    }

    @Test
    public void shouldToString() {

        //Given
        for (final JwtHeaderKey item : JwtHeaderKey.values()) {
            //When
            final String s = item.toString();

            //Then
            assertEquals(s, item.name().toLowerCase(Locale.ROOT));
        }
    }

    @Test
    public void shouldGetHeaderKey() {

        //Given
        for (final JwtHeaderKey item : JwtHeaderKey.values()) {
            //When
            final JwtHeaderKey headerKey = JwtHeaderKey.getHeaderKey(item.name());

            //Then
            assertEquals(headerKey, item);
        }
    }

    @Test
    public void shouldGetCustomHeaderKeyWhenUnknownKeyGiven() {

        //Given

        //When
        JwtHeaderKey headerKey = JwtHeaderKey.getHeaderKey("UNKNOWN");

        //Then
        assertEquals(headerKey, JwtHeaderKey.CUSTOM);
    }
}
