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

package org.forgerock.jaspi.runtime.response;

import org.testng.annotations.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;

public class JaspiHttpServletResponseWrapperTest {

    @Test
    public void shouldGetOutputStream() throws IOException {

        //Given
        HttpServletResponse response = mock(HttpServletResponse.class);
        JaspiHttpServletResponseWrapper responseWrapper = new JaspiHttpServletResponseWrapper(response);

        //When
        ServletOutputStream outputStream = responseWrapper.getOutputStream();

        //Then
        assertTrue(JaspiServletOutputStream.class.isAssignableFrom(outputStream.getClass()));
    }

    @Test
    public void shouldGetWriter() throws IOException {

        //Given
        HttpServletResponse response = mock(HttpServletResponse.class);
        JaspiHttpServletResponseWrapper responseWrapper = new JaspiHttpServletResponseWrapper(response);
        PrintWriter printWriter = mock(PrintWriter.class);

        given(response.getWriter()).willReturn(printWriter);

        //When
        PrintWriter writer = responseWrapper.getWriter();

        //Then
        assertTrue(JaspiPrintWriter.class.isAssignableFrom(writer.getClass()));
    }
}
