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

package org.forgerock.json.jose.jwt;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JwtClaimsSetKeyTest {

    @Test
    public void shouldGetValue() {

        //Given

        //When
        String value = JwtClaimsSetKey.JTI.value();

        //Then
        assertEquals(value, "jti");
    }

    @Test
    public void shouldToString() {

        //Given

        //When
        String value = JwtClaimsSetKey.IAT.toString();

        //Then
        assertEquals(value, "iat");
    }

    @Test
    public void shouldGetClaimSetKey() {

        //Given

        //When
        JwtClaimsSetKey claimsSetKey = JwtClaimsSetKey.getClaimSetKey("PRN");

        //Then
        assertEquals(claimsSetKey, JwtClaimsSetKey.PRN);
    }

    @Test
    public void shouldGetCustomClaimSetKeyWhenUnknownKeyGiven() {

        //Given

        //When
        JwtClaimsSetKey claimsSetKey = JwtClaimsSetKey.getClaimSetKey("UNKNOWN");

        //Then
        assertEquals(claimsSetKey, JwtClaimsSetKey.CUSTOM);
    }
}
