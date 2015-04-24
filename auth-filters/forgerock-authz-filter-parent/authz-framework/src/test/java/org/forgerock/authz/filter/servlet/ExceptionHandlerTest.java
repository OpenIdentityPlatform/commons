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

package org.forgerock.authz.filter.servlet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ExceptionHandlerTest {

    private ExceptionHandler exceptionHandler;

    private ResponseHandler responseHandler;
    private HttpServletResponse res;

    @BeforeMethod
    public void setUp() {

        responseHandler = mock(ResponseHandler.class);
        res = mock(HttpServletResponse.class);

        exceptionHandler = new ExceptionHandler(responseHandler, res);
    }

    @Test
    public void shouldWriteToResponseWithReason() throws ServletException, IOException {

        //Given
        Exception exception = mock(Exception.class);
        PrintWriter writer = mock(PrintWriter.class);
        JsonValue jsonResponse = json(object());

        given(exception.getMessage()).willReturn("EXCEPTION_MESSAGE");
        given(responseHandler.getWriter(res)).willReturn(writer);
        given(responseHandler.getJsonErrorResponse("EXCEPTION_MESSAGE", null)).willReturn(jsonResponse);

        //When
        Promise<Void, ServletException> promise = exceptionHandler.apply(exception);

        //Then
        verify(res).reset();
        verify(res).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(writer).write(jsonResponse.toString());
        assertTrue(promise.isDone());
        assertNull(promise.getOrThrowUninterruptibly());
    }

    @Test
    public void shouldNotWriteToResponseIfCommitted() throws ServletException, IOException {

        //Given
        Exception exception = mock(Exception.class);

        given(res.isCommitted()).willReturn(true);

        //When
        Promise<Void, ServletException> promise = exceptionHandler.apply(exception);

        //Then
        verify(res, never()).reset();
        verify(res, never()).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(promise.isDone());
        assertNull(promise.getOrThrowUninterruptibly());
    }

    @Test
    public void shouldHandleIOException() throws ServletException, IOException {

        //Given
        Exception exception = mock(Exception.class);

        doThrow(IOException.class).when(responseHandler).getWriter(res);

        //When
        Promise<Void, ServletException> promise = exceptionHandler.apply(exception);

        //Then
        assertTrue(promise.isDone());
        try {
            promise.getOrThrowUninterruptibly();
            fail();
        } catch (ServletException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }
}
