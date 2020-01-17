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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.filter.api;

import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class AuthorizationResultTest {

    @Test
    public void shouldCreateSuccessfulAuthorizationResult() {

        //Given

        //When
        AuthorizationResult authorizationResult = AuthorizationResult.accessPermitted();

        //Then
        assertTrue(authorizationResult.isAuthorized());
        assertNull(authorizationResult.getReason());
        assertNull(authorizationResult.getDetail());
    }

    @Test
    public void shouldCreateFailedAuthorizationResult() {

        //Given
        String reason = "REASON";

        //When
        AuthorizationResult authorizationResult = AuthorizationResult.accessDenied(reason);

        //Then
        assertFalse(authorizationResult.isAuthorized());
        assertEquals(authorizationResult.getReason(), reason);
        assertNull(authorizationResult.getDetail());
    }

    @Test
    public void shouldCreateFailedAuthorizationResultWithDetail() {

        //Given
        String reason = "REASON";
        JsonValue detail = json(object());

        //When
        AuthorizationResult authorizationResult = AuthorizationResult.accessDenied(reason, detail);

        //Then
        assertFalse(authorizationResult.isAuthorized());
        assertEquals(authorizationResult.getReason(), reason);
        assertEquals(authorizationResult.getDetail(), detail);
    }
}
