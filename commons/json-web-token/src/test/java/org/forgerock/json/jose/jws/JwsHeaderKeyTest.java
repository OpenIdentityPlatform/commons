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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.jose.jws;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JwsHeaderKeyTest {

    @Test
    public void shouldGetValue() {

        //Given

        //When
        String value = JwsHeaderKey.JWK.value();

        //Then
        assertEquals(value, "jwk");
    }

    @Test
    public void shouldToString() {

        //Given

        //When
        String value = JwsHeaderKey.KID.toString();

        //Then
        assertEquals(value, "kid");
    }

    @Test
    public void shouldGetHeaderKey() {

        //Given

        //When
        JwsHeaderKey headerKey = JwsHeaderKey.getHeaderKey("X5C");

        //Then
        assertEquals(headerKey, JwsHeaderKey.X5C);
    }

    @Test
    public void shouldGetCustomHeaderKeyWhenUnknownKeyGiven() {

        //Given

        //When
        JwsHeaderKey headerKey = JwsHeaderKey.getHeaderKey("UNKNOWN");

        //Then
        assertEquals(headerKey, JwsHeaderKey.CUSTOM);
    }
}
