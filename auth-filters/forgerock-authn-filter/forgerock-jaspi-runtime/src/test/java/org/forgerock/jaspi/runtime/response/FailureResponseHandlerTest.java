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

package org.forgerock.jaspi.runtime.response;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;

import org.forgerock.guava.common.net.MediaType;
import org.forgerock.jaspi.runtime.HttpServletMessageInfo;
import org.forgerock.jaspi.runtime.ResourceExceptionHandler;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public class FailureResponseHandlerTest {

    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_XML = "text/xml";
    private static final String TEXT_PLAIN = "text/plain";

    private FailureResponseHandler handler;

    @BeforeMethod
    public void setup() throws Exception {
        this.handler = new FailureResponseHandler();
    }

    @Test
    public void jsonHandlerShouldRenderException() throws Exception {
        //Given
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        doReturn(new PrintWriter(sw)).when(response).getWriter();
        ResourceException jre = ResourceException.getException(ResourceException.BAD_REQUEST, "BAD_REQUEST");
        jre.setDetail(json(object(field("DETAIL_KEY", "DETAIL_VALUE"))));

        //When
        new FailureResponseHandler.JsonResourceExceptionHandler().write(jre, response);

        //Then
        String responseText = sw.toString();
        assertTrue(responseText.contains("400"));
        assertTrue(responseText.contains("BAD_REQUEST"));
        assertTrue(responseText.contains("DETAIL_KEY"));
        assertTrue(responseText.contains("DETAIL_VALUE"));
        verify(response).setContentType("application/json");

    }

    @DataProvider(name = "content-types")
    public static Object[][] contentTypes() {
        return new Object[][] {
                { APPLICATION_XML, APPLICATION_XML },
                { "application/svg+xml", APPLICATION_JSON },
                { TEXT_XML, APPLICATION_XML },
                { TEXT_PLAIN, APPLICATION_JSON },
                { "application/json; q=0.8, application/xml", APPLICATION_XML },
                { "application/json; q=0.6, application/xml; q=0.8", APPLICATION_XML },
                { "application/json; q=0.9, application/xml; q=0.8", APPLICATION_JSON },
                { "*/*; q=1.0, application/json; q=0.6, application/xml; q=0.8", APPLICATION_JSON },
                { "*/*; q=1.0, application/json; q=0.6, application/xml; q=1.0", APPLICATION_XML },
                { null, APPLICATION_JSON }
        };
    }

    @Test(dataProvider = "content-types")
    public void shouldUseExceptionHandlerIfAvailable(String accept, String expected) throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        doReturn(new PrintWriter(sw)).when(response).getWriter();

        doReturn(accept).when(request).getHeader("Accept");

        handler.registerExceptionHandler(TestExceptionHandler.class);

        ResourceException jre = ResourceException.getException(ResourceException.BAD_REQUEST, "BAD_REQUEST");
        jre.setDetail(json(object(field("DETAIL_KEY", "DETAIL_VALUE"))));

        //When
        handler.handle(jre, new HttpServletMessageInfo(request, response));

        //Then
        verify(response).setStatus(400);
        verify(response).setContentType(expected);
    }

    public static final class TestExceptionHandler implements ResourceExceptionHandler {

        public List<MediaType> handles() {
            return Arrays.asList(MediaType.parse(APPLICATION_XML), MediaType.parse(TEXT_XML));
        }

        public void write(ResourceException exception, HttpServletResponse response) throws IOException {
            response.setContentType(APPLICATION_XML);
            response.getWriter().write("Hello "+exception.getCode());
        }

    }
}