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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class ResponseHandlerTest {

    private ResponseHandler responseHandler;

    @BeforeMethod
    public void setUp() {
        responseHandler = new ResponseHandler();
    }

    @Test
    public void shouldGetJsonForbiddenResponseWithMessage() {

        //Given

        //When
        JsonValue response = responseHandler.getJsonForbiddenResponse("MESSAGE", null);

        //Then
        assertEquals((int) response.get("code").asInteger(), 403);
        assertEquals(response.get("reason").asString(), "Forbidden");
        assertEquals(response.get("message").asString(), "MESSAGE");
        assertFalse(response.isDefined("detail"));
    }

    @Test
    public void shouldGetJsonForbiddenResponseWithMessageAndDetail() {

        //Given

        //When
        JsonValue response = responseHandler.getJsonForbiddenResponse("MESSAGE",
                json(object(field("INTERNAL", "VALUE"))));

        //Then
        assertEquals((int) response.get("code").asInteger(), 403);
        assertEquals(response.get("reason").asString(), "Forbidden");
        assertEquals(response.get("message").asString(), "MESSAGE");
        assertEquals(response.get("detail").get("INTERNAL").asString(), "VALUE");
    }

    @Test
    public void shouldGetJsonErrorResponseWithMessage() {

        //Given

        //When
        JsonValue response = responseHandler.getJsonErrorResponse("MESSAGE", null);

        //Then
        assertEquals((int) response.get("code").asInteger(), 500);
        assertEquals(response.get("reason").asString(), "Internal Error");
        assertEquals(response.get("message").asString(), "MESSAGE");
        assertFalse(response.isDefined("detail"));
    }

    @Test
    public void shouldGetJsonErrorResponseWithMessageAndDetail() {

        //Given

        //When
        JsonValue response = responseHandler.getJsonErrorResponse("MESSAGE",
                json(object(field("INTERNAL", "VALUE"))));

        //Then
        assertEquals((int) response.get("code").asInteger(), 500);
        assertEquals(response.get("reason").asString(), "Internal Error");
        assertEquals(response.get("message").asString(), "MESSAGE");
        assertEquals(response.get("detail").get("INTERNAL").asString(), "VALUE");
    }

    @Test
    public void shouldGetWriterIfResponseNotCommitted() throws ServletException, IOException {

        //Given
        HttpServletResponse resp = mock(HttpServletResponse.class);
        PrintWriter writer = mock(PrintWriter.class);

        given(resp.isCommitted()).willReturn(false);
        given(resp.getWriter()).willReturn(writer);

        //When
        PrintWriter printWriter = responseHandler.getWriter(resp);

        //Then
        assertEquals(printWriter, writer);
    }

    @Test
    public void getWriterShouldReturnNullIfResponseCommitted() throws ServletException,
            IOException {

        //Given
        HttpServletResponse resp = mock(HttpServletResponse.class);

        given(resp.isCommitted()).willReturn(true);

        //When
        PrintWriter writer = responseHandler.getWriter(resp);

        //Then
        assertNull(writer);
    }

    @Test
    public void getWriterShouldReturnOutputStream() throws ServletException, IOException {

        //Given
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);

        given(resp.isCommitted()).willReturn(false);
        doThrow(IllegalStateException.class).when(resp).getWriter();
        given(resp.getOutputStream()).willReturn(outputStream);

        //When
        PrintWriter writer = responseHandler.getWriter(resp);

        //Then
        assertNotNull(writer);
    }
}
