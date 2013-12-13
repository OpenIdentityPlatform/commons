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

package org.forgerock.jaspi.exceptions;

import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class JaspiAuthExceptionTest {

    @Test
    public void shouldCreateJaspiAuthExceptionWithMessage() {

        //Given

        //When
        JaspiAuthException e = new JaspiAuthException("MESSAGE");

        //Then
        assertNotNull(e);
        assertEquals(e.getMessage(), "MESSAGE");
    }

    @Test
    public void shouldCreateJaspiAuthExceptionWithThrowable() {

        //Given
        Exception cause = new Exception("MESSAGE");

        //When
        JaspiAuthException e = new JaspiAuthException(cause);

        //Then
        assertNotNull(e);
        assertEquals(e.getMessage(), "MESSAGE");
    }

    @Test
    public void shouldCreateJaspiAuthExceptionWithMessageAndCause() {

        //Given
        Exception cause = new Exception("CAUSE_MESSAGE");

        //When
        JaspiAuthException e = new JaspiAuthException("MESSAGE", cause);

        //Then
        assertNotNull(e);
        assertEquals(e.getMessage(), "MESSAGE");
    }

    @Test
    public void shouldGetDetailMessage() {

        //Given
        JaspiAuthException e = new JaspiAuthException("MESSAGE");

        //When
        String message = e.getMessage();

        //Then
        assertEquals(message, "MESSAGE");
    }

    @Test
    public void shouldGetCauseMessageWhenOnlyCauseSet() {

        //Given
        Throwable cause = mock(Throwable.class);
        given(cause.getMessage()).willReturn("CAUSE");

        JaspiAuthException e = new JaspiAuthException(cause);

        //When
        String message = e.getMessage();

        //Then
        assertEquals(message, "CAUSE");
    }

    @Test
    public void shouldGetDetailMessageWhenDetailMessageAndCauseSet() {

        //Given
        Throwable cause = mock(Throwable.class);
        JaspiAuthException e = new JaspiAuthException("MESSAGE", cause);

        given(cause.getMessage()).willReturn("CAUSE");

        //When
        String message = e.getMessage();

        //Then
        assertEquals(message, "MESSAGE");
    }
}
