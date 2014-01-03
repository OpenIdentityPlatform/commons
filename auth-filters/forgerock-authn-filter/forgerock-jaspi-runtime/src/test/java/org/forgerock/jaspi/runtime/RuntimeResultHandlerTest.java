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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class RuntimeResultHandlerTest {

    private RuntimeResultHandler resultHandler;

    @BeforeMethod
    public void setUp() {
        resultHandler = new RuntimeResultHandler();
    }

    @Test
    public void shouldHandleValidateRequestWithSuccessAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SUCCESS;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        boolean result = resultHandler.handleValidateRequestResult(authStatus, response);

        //Then
        assertTrue(result);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldHandleValidateRequestWithSendSuccessAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SEND_SUCCESS;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        boolean result = resultHandler.handleValidateRequestResult(authStatus, response);

        //Then
        assertFalse(result);
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldHandleValidateRequestWithSendFailureAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SEND_FAILURE;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        boolean result = resultHandler.handleValidateRequestResult(authStatus, response);

        //Then
        assertFalse(result);
        verify(response).setStatus(401);
    }

    @Test
    public void shouldHandleValidateRequestWithSendContinueAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SEND_CONTINUE;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        boolean result = resultHandler.handleValidateRequestResult(authStatus, response);

        //Then
        assertFalse(result);
        verify(response).setStatus(100);
    }

    @Test (expectedExceptions = AuthException.class)
    public void handleValidateRequestShouldThrowAuthExceptionWithFailureAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.FAILURE;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        resultHandler.handleValidateRequestResult(authStatus, response);

        //Then
        fail();
    }

    @Test
    public void shouldHandleSecureResponseWithSendSuccessAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SEND_SUCCESS;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        resultHandler.handleSecureResponseResult(authStatus, response);

        //Then
        verifyZeroInteractions(response);
    }

    @Test
    public void shouldHandleSecureResponseWithSendFailureAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SEND_FAILURE;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        resultHandler.handleSecureResponseResult(authStatus, response);

        //Then
        verify(response).setStatus(500);
    }

    @Test
    public void shouldHandleSecureResponseWithSendContinueAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SEND_CONTINUE;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        resultHandler.handleSecureResponseResult(authStatus, response);

        //Then
        verify(response).setStatus(100);
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldHandleSecureResponseWithSuccessAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.SUCCESS;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        resultHandler.handleSecureResponseResult(authStatus, response);

        //Then
        fail();
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldHandleSecureResponseWithFailureAuthStatus() throws AuthException {

        //Given
        AuthStatus authStatus = AuthStatus.FAILURE;
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        resultHandler.handleValidateRequestResult(authStatus, response);

        //Then
        fail();
    }
}
