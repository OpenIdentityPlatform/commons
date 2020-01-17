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

package org.forgerock.authz.modules.oauth2;

import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class OAuth2ExceptionTest {

    @Test
    public void shouldCreateWithMessage() {

        //Given


        //When
        OAuth2Exception exception = new OAuth2Exception("MESSAGE");

        //Then
        assertNotNull(exception);
        assertEquals(exception.getMessage(), "MESSAGE");
    }

    @Test
    public void shouldCreateWithMessageAndCause() {

        //Given
        Throwable cause = mock(Throwable.class);

        //When
        OAuth2Exception exception = new OAuth2Exception("MESSAGE", cause);

        //Then
        assertNotNull(exception);
        assertEquals(exception.getMessage(), "MESSAGE");
        assertEquals(exception.getCause(), cause);
    }
}
