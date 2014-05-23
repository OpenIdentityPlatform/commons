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

package org.forgerock.authz.filter.servlet;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.forgerock.authz.filter.api.AuthorizationResult.failure;
import static org.forgerock.authz.filter.api.AuthorizationResult.success;
import static org.forgerock.json.fluent.JsonValue.field;
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

public class SuccessHandlerTest {

    private SuccessHandler successHandler;

    private ResultHandler resultHandler;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private FilterChain chain;

    @BeforeMethod
    public void setUp() {

        resultHandler = mock(ResultHandler.class);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        successHandler = new SuccessHandler(resultHandler, req, res, chain);
    }

    @Test
    public void shouldCallDoFilter() throws ServletException, IOException {

        //Given
        AuthorizationResult result = success();

        //When
        Promise<Void, ServletException> promise = successHandler.apply(result);

        //Then
        verify(chain).doFilter(req, res);
        assertTrue(promise.isDone());
        assertNull(promise.getOrThrowUninterruptibly());
    }

    @Test
    public void shouldReturnServletExceptionWhenDoFilterThrowsIOException() throws ServletException, IOException {

        //Given
        AuthorizationResult result = success();

        doThrow(IOException.class).when(chain).doFilter(req, res);

        //When
        Promise<Void, ServletException> promise = successHandler.apply(result);

        //Then
        assertTrue(promise.isDone());
        try {
            promise.getOrThrowUninterruptibly();
            fail();
        } catch (ServletException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void shouldReturnServletExceptionWhenDoFilterThrowsServletException() throws ServletException, IOException {

        //Given
        AuthorizationResult result = success();

        doThrow(ServletException.class).when(chain).doFilter(req, res);

        //When
        Promise<Void, ServletException> promise = successHandler.apply(result);

        //Then
        assertTrue(promise.isDone());
        try {
            promise.getOrThrowUninterruptibly();
            fail();
        } catch (ServletException e) {
            //Expected exception
        }
    }

    @Test
    public void shouldNotCallDoFilterIfNotAuthorizedAndRespondWithReasonAndDetail() throws ServletException,
            IOException {

        //Given
        JsonValue detail = json(object(field("INTERNAL", "VALUE")));
        AuthorizationResult result = failure("REASON", detail);
        PrintWriter writer = mock(PrintWriter.class);
        JsonValue jsonResponse = json(object());

        given(resultHandler.getWriter(res)).willReturn(writer);
        given(resultHandler.getJsonForbiddenResponse("REASON", detail)).willReturn(jsonResponse);

        //When
        Promise<Void, ServletException> promise = successHandler.apply(result);

        //Then
        verify(chain, never()).doFilter(req, res);
        verify(res).reset();
        verify(res).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(writer).write(jsonResponse.toString());
        assertTrue(promise.isDone());
        assertNull(promise.getOrThrowUninterruptibly());
    }

    @Test
    public void shouldNotCallDoFilterIfNotAuthorizedAndNotWriteToResponseIfCommitted() throws ServletException,
            IOException {

        //Given
        AuthorizationResult result = failure("REASON");

        given(res.isCommitted()).willReturn(true);

        //When
        Promise<Void, ServletException> promise = successHandler.apply(result);

        //Then
        verify(chain, never()).doFilter(req, res);
        verify(res, never()).reset();
        verify(res, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(promise.isDone());
        assertNull(promise.getOrThrowUninterruptibly());
    }
}
