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

package org.forgerock.jaspi.runtime.response;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JaspiServletOutputStreamTest {

    private JaspiServletOutputStream servletOutputStream;

    private ServletOutputStream outputStream;

    @BeforeMethod
    public void setUp() {

        outputStream = mock(ServletOutputStream.class);

        servletOutputStream = new JaspiServletOutputStream(outputStream);
    }

    @Test
    public void shouldWrite() throws IOException {

        //Given

        //When
        servletOutputStream.write(1);

        //Then
        verify(outputStream).write(1);
    }

    @Test
    public void shouldClose() throws IOException {

        //Given

        //When
        servletOutputStream.close();

        //Then
        verify(outputStream).close();
    }
}
