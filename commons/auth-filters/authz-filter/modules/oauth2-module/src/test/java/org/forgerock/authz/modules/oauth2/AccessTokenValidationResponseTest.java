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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AccessTokenValidationResponseTest {

    @Test
    public void shouldCreateInvalidResponse() {

        //Given

        //When
        final AccessTokenValidationResponse validationResponse = new AccessTokenValidationResponse(0);

        //Then
        assertFalse(validationResponse.isTokenValid());
        assertTrue(validationResponse.getProfileInformation().isEmpty());
        assertTrue(validationResponse.getTokenScopes().isEmpty());
    }

    @Test
    public void shouldCreateValidResponse() {

        //Given
        long expiryTime = System.currentTimeMillis() + 100;

        //When
        final AccessTokenValidationResponse validationResponse = new AccessTokenValidationResponse(expiryTime);

        //Then
        assertTrue(validationResponse.isTokenValid());
        assertEquals(validationResponse.getExpiryTime(), expiryTime);
        assertTrue(validationResponse.getProfileInformation().isEmpty());
        assertTrue(validationResponse.getTokenScopes().isEmpty());
    }

    @Test
    public void shouldCreateResponseWithScope() {

        //Given
        Set<String> scope = new HashSet<>();

        //When
        final AccessTokenValidationResponse validationResponse =
                new AccessTokenValidationResponse(System.currentTimeMillis() + 100, scope);

        //Then
        assertTrue(validationResponse.isTokenValid());
        assertTrue(validationResponse.getProfileInformation().isEmpty());
        assertEquals(validationResponse.getTokenScopes(), scope);
    }

    @Test
    public void shouldCreateResponseWithScopeAndProfileInfo() {

        //Given
        Map<String, Object> profileInfo = new HashMap<>();
        Set<String> scope = new HashSet<>();

        //When
        final AccessTokenValidationResponse validationResponse =
                new AccessTokenValidationResponse(System.currentTimeMillis() + 100, profileInfo, scope);

        //Then
        assertTrue(validationResponse.isTokenValid());
        assertTrue(validationResponse.getProfileInformation().isEmpty());
        assertEquals(validationResponse.getTokenScopes(), scope);
    }
}
